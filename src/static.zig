const std = @import("std");

const AST = @import("ast.zig");
const Errors = @import("errors.zig");
const SP = @import("lib/string_pool.zig");
const Typing = @import("typing.zig");

const Env = struct {
    boolType: *Typing.Type,
    charType: *Typing.Type,
    errorType: *Typing.Type,
    floatType: *Typing.Type,
    intType: *Typing.Type,
    stringType: *Typing.Type,
    unitType: *Typing.Type,

    allocator: std.mem.Allocator,
    constraints: Typing.Constraints,
    errors: *Errors.Errors,
    names: std.ArrayList(std.AutoHashMap(*SP.String, Typing.Scheme)),
    pump: Typing.Pump,
    schemes: std.AutoHashMap(*SP.String, Typing.Scheme),
    sp: *SP.StringPool,

    pub fn init(sp: *SP.StringPool, errors: *Errors.Errors) !Env {
        const allocator = sp.allocator;

        var names = std.ArrayList(std.AutoHashMap(*SP.String, Typing.Scheme)).init(allocator);
        try names.append(std.AutoHashMap(*SP.String, Typing.Scheme).init(allocator));

        var schemes = std.AutoHashMap(*SP.String, Typing.Scheme).init(allocator);

        const boolType = try Typing.TagType.new(allocator, try sp.intern("Bool"));
        const charType = try Typing.TagType.new(allocator, try sp.intern("Char"));
        const errorType = try Typing.TagType.new(allocator, try sp.intern("Error"));
        const floatType = try Typing.TagType.new(allocator, try sp.intern("Float"));
        const intType = try Typing.TagType.new(allocator, try sp.intern("Int"));
        const stringType = try Typing.TagType.new(allocator, try sp.intern("String"));
        const unitType = try Typing.TagType.new(allocator, try sp.intern("Unit"));

        try schemes.put(boolType.kind.Tag.name.incRefR(), Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = boolType.incRefR() });
        try schemes.put(charType.kind.Tag.name.incRefR(), Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = charType.incRefR() });
        try schemes.put(errorType.kind.Tag.name.incRefR(), Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = errorType.incRefR() });
        try schemes.put(intType.kind.Tag.name.incRefR(), Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = intType.incRefR() });
        try schemes.put(floatType.kind.Tag.name.incRefR(), Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = floatType.incRefR() });
        try schemes.put(stringType.kind.Tag.name.incRefR(), Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = stringType.incRefR() });
        try schemes.put(unitType.kind.Tag.name.incRefR(), Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = unitType.incRefR() });

        return Env{
            .boolType = boolType,
            .charType = charType,
            .errorType = errorType,
            .floatType = floatType,
            .intType = intType,
            .stringType = stringType,
            .unitType = unitType,

            .allocator = allocator,
            .constraints = Typing.Constraints.init(allocator),
            .errors = errors,
            .names = names,
            .pump = Typing.Pump.init(),
            .schemes = schemes,
            .sp = sp,
        };
    }

    pub fn deinit(self: *Env, allocator: std.mem.Allocator) void {
        self.boolType.decRef(allocator);
        self.charType.decRef(allocator);
        self.errorType.decRef(allocator);
        self.floatType.decRef(allocator);
        self.intType.decRef(allocator);
        self.stringType.decRef(allocator);
        self.unitType.decRef(allocator);

        self.constraints.deinit(allocator);
        for (self.names.items) |*names| {
            var iterator = names.iterator();
            while (iterator.next()) |*entry| {
                entry.key_ptr.*.decRef();
                entry.value_ptr.*.deinit(allocator);
            }
            names.deinit();
        }
        self.names.deinit();

        var iterator2 = self.schemes.iterator();
        while (iterator2.next()) |entry| {
            entry.key_ptr.*.decRef();
            entry.value_ptr.*.deinit(allocator);
        }
        self.schemes.deinit();
    }

    pub fn openScope(self: *Env) !void {
        try self.names.append(std.AutoHashMap(*SP.String, Typing.Scheme).init(self.sp.allocator));
    }

    pub fn closeScope(self: *Env) void {
        var names = self.names.pop();

        var iterator = names.iterator();
        while (iterator.next()) |*entry| {
            entry.key_ptr.*.decRef();
            entry.value_ptr.*.deinit(self.sp.allocator);
        }
        names.deinit();
    }

    pub fn appendError(self: *Env, err: Errors.Error) !void {
        try self.errors.append(err);
    }

    pub fn newName(self: *Env, name: *SP.String, scheme: Typing.Scheme) !void {
        if (self.names.items.len > 0) {
            var scope = &self.names.items[self.names.items.len - 1];

            if (scope.get(name)) |oldScheme| {
                try scope.put(name, scheme);
                oldScheme.deinit(self.sp.allocator);
            } else {
                try scope.put(name.incRefR(), scheme);
            }
        }
    }

    pub fn findNameInScope(self: *Env, name: *SP.String) ?Typing.Scheme {
        if (self.names.getLastOrNull()) |scope| {
            if (scope.get(name)) |scheme| {
                return scheme;
            }
        }

        return null;
    }

    pub fn findName(self: *Env, name: *SP.String) ?Typing.Scheme {
        var i = self.names.items.len;

        while (i > 0) : (i -= 1) {
            var scope = &self.names.items[i - 1];

            if (scope.get(name)) |scheme| {
                return scheme;
            }
        }

        return null;
    }

    pub fn addConstraint(self: *Env, a: *Typing.Type, b: *Typing.Type, locationRange: Errors.LocationRange) !void {
        try self.constraints.add(a, b, locationRange);
    }

    pub fn addDependent(self: *Env, a: *Typing.Type, b: *Typing.Type, locationRange: Errors.LocationRange) !void {
        try self.constraints.addDependency(a, b, locationRange);
    }
};

pub fn analysis(ast: *AST.Expression, sp: *SP.StringPool, errors: *Errors.Errors) !*Typing.Type {
    const allocator = sp.allocator;

    var env = try Env.init(sp, errors);
    defer env.deinit(allocator);

    _ = try expression(ast, &env);

    var subst = try Typing.solver(&env.constraints, &env.pump, env.errors, allocator);
    defer subst.deinit(allocator);

    try applyExpression(ast, &subst, allocator);

    return (ast.type orelse env.errorType).incRefR();
}

fn expression(ast: *AST.Expression, env: *Env) !*Typing.Type {
    switch (ast.kind) {
        .assignment => {
            _ = try expression(ast.kind.assignment.lhs, env);
            _ = try expression(ast.kind.assignment.rhs, env);
        },
        .binaryOp => {
            switch (ast.kind.binaryOp.op) {
                .Plus => {
                    const result = try env.pump.newBound(env.allocator);
                    defer result.decRef(env.allocator);

                    const lhs = try expression(ast.kind.binaryOp.lhs, env);
                    const rhs = try expression(ast.kind.binaryOp.rhs, env);

                    try env.addConstraint(lhs, result, ast.kind.binaryOp.lhs.locationRange);
                    try env.addConstraint(rhs, result, ast.kind.binaryOp.rhs.locationRange);

                    const dependentType = try Typing.OrExtendType.new(env.allocator, env.charType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.floatType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.intType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.stringType.incRefR(), try Typing.OrEmptyType.new(env.allocator)))));
                    defer dependentType.decRef(env.allocator);

                    try env.addDependent(result, dependentType, ast.locationRange);

                    ast.assignType(result.incRefR(), env.allocator);
                },
                .Minus, .Times => {
                    const result = try env.pump.newBound(env.allocator);
                    defer result.decRef(env.allocator);

                    const lhs = try expression(ast.kind.binaryOp.lhs, env);
                    const rhs = try expression(ast.kind.binaryOp.rhs, env);

                    try env.addConstraint(lhs, result, ast.kind.binaryOp.lhs.locationRange);
                    try env.addConstraint(rhs, result, ast.kind.binaryOp.rhs.locationRange);

                    const dependentType = try Typing.OrExtendType.new(env.allocator, env.charType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.floatType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.intType.incRefR(), try Typing.OrEmptyType.new(env.allocator))));
                    defer dependentType.decRef(env.allocator);

                    try env.addDependent(result, dependentType, ast.locationRange);

                    ast.assignType(result.incRefR(), env.allocator);
                },
                else => {
                    try env.appendError(try Errors.undefinedOperatorError(env.sp.allocator, ast.locationRange, ast.kind.binaryOp.op.toString()));
                },
            }
        },
        .call => {
            _ = try expression(ast.kind.call.callee, env);
            for (ast.kind.call.args) |arg| {
                _ = try expression(arg, env);
            }
        },
        .catche => {
            _ = try expression(ast.kind.catche.value, env);
            for (ast.kind.catche.cases) |case| {
                _ = try pattern(case.pattern, env);
                _ = try expression(case.body, env);
            }
        },
        .dot => {
            _ = try expression(ast.kind.dot.record, env);
        },
        .exprs => {
            var last: *Typing.Type = env.errorType;

            for (ast.kind.exprs) |expr| {
                last = try expression(expr, env);
            }

            ast.assignType(last.incRefR(), env.allocator);
        },
        .idDeclaration => {
            const t = try expression(ast.kind.idDeclaration.value, env);

            if (env.findNameInScope(ast.kind.idDeclaration.name)) |_| {
                try env.appendError(try Errors.duplicateDeclarationError(env.sp.allocator, ast.locationRange, ast.kind.idDeclaration.name.slice()));
            } else {
                try env.newName(ast.kind.idDeclaration.name, Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = t.incRefR() });
            }

            ast.assignType(t.incRefR(), env.allocator);
        },
        .identifier => {
            if (env.findName(ast.kind.identifier)) |scheme| {
                ast.assignType(scheme.type.incRefR(), env.allocator);
            } else {
                try env.appendError(try Errors.undefinedNameError(env.sp.allocator, ast.locationRange, ast.kind.identifier.slice()));
            }
        },
        .ifte => {
            for (ast.kind.ifte) |case| {
                if (case.condition) |condition| {
                    _ = try expression(condition, env);
                }
                _ = try expression(case.then, env);
            }
        },
        .indexRange => {
            _ = try expression(ast.kind.indexRange.expr, env);

            if (ast.kind.indexRange.start) |start| {
                _ = try expression(start, env);
            }
            if (ast.kind.indexRange.end) |end| {
                _ = try expression(end, env);
            }
        },
        .indexValue => {
            _ = try expression(ast.kind.indexValue.expr, env);
            _ = try expression(ast.kind.indexValue.index, env);
        },
        .literalBool => ast.assignType(env.boolType.incRefR(), env.allocator),
        .literalChar => ast.assignType(env.charType.incRefR(), env.allocator),
        .literalFloat => ast.assignType(env.floatType.incRefR(), env.allocator),
        .literalFunction => {
            for (ast.kind.literalFunction.params) |param| {
                if (param.default) |d| {
                    _ = try expression(d, env);
                }
            }
            _ = try expression(ast.kind.literalFunction.body, env);
        },
        .literalInt => ast.assignType(env.intType.incRefR(), env.allocator),
        .literalRecord => {
            for (ast.kind.literalRecord) |field| {
                switch (field) {
                    .value => _ = try expression(field.value.value, env),
                    .record => _ = try expression(field.record, env),
                }
            }
        },
        .literalSequence => {
            for (ast.kind.literalSequence) |elem| {
                switch (elem) {
                    .value => _ = try expression(elem.value, env),
                    .sequence => _ = try expression(elem.sequence, env),
                }
            }
        },
        .literalString => ast.assignType(env.stringType.incRefR(), env.allocator),
        .literalVoid => ast.assignType(env.unitType.incRefR(), env.allocator),
        .match => {
            _ = try expression(ast.kind.match.value, env);
            for (ast.kind.match.cases) |case| {
                _ = try pattern(case.pattern, env);
                _ = try expression(case.body, env);
            }
        },
        .notOp => {
            const t = try expression(ast.kind.notOp.value, env);
            if (!t.isBool()) {
                try env.addConstraint(env.boolType, t, ast.kind.notOp.value.locationRange);
            }
            ast.assignType(env.boolType.incRefR(), env.allocator);
        },
        .patternDeclaration => {
            _ = try pattern(ast.kind.patternDeclaration.pattern, env);
            _ = try expression(ast.kind.patternDeclaration.value, env);
        },
        .raise => {
            _ = try expression(ast.kind.raise.expr, env);
        },
        .whilee => {
            _ = try expression(ast.kind.whilee.condition, env);
            _ = try expression(ast.kind.whilee.body, env);
        },
        // else => {},
    }

    return ast.type orelse env.errorType;
}

fn pattern(ast: *AST.Pattern, env: *Env) !*Typing.Type {
    switch (ast.kind) {
        .record => {
            for (ast.kind.record.entries) |field| {
                if (field.pattern) |p| {
                    _ = try pattern(p, env);
                }
            }
        },
        .sequence => {
            for (ast.kind.sequence.patterns) |p| {
                _ = try pattern(p, env);
            }
        },
        else => {},
    }

    return env.errorType;
}

fn applyExpression(ast: *AST.Expression, subst: *Typing.Subst, allocator: std.mem.Allocator) !void {
    if (ast.type) |t| {
        ast.assignType(try t.apply(subst), allocator);
    }

    switch (ast.kind) {
        .assignment => {
            try applyExpression(ast.kind.assignment.lhs, subst, allocator);
            try applyExpression(ast.kind.assignment.rhs, subst, allocator);
        },
        .binaryOp => {
            try applyExpression(ast.kind.binaryOp.lhs, subst, allocator);
            try applyExpression(ast.kind.binaryOp.rhs, subst, allocator);
        },
        .call => {
            try applyExpression(ast.kind.call.callee, subst, allocator);
            for (ast.kind.call.args) |arg| {
                try applyExpression(arg, subst, allocator);
            }
        },
        .catche => {
            try applyExpression(ast.kind.catche.value, subst, allocator);
            for (ast.kind.catche.cases) |case| {
                try applyPattern(case.pattern, subst, allocator);
                try applyExpression(case.body, subst, allocator);
            }
        },
        .dot => {
            _ = try applyExpression(ast.kind.dot.record, subst, allocator);
        },
        .exprs => {
            for (ast.kind.exprs) |expr| {
                try applyExpression(expr, subst, allocator);
            }
        },
        .idDeclaration => {
            try applyExpression(ast.kind.idDeclaration.value, subst, allocator);
        },
        .ifte => {
            for (ast.kind.ifte) |case| {
                if (case.condition) |condition| {
                    try applyExpression(condition, subst, allocator);
                }
                try applyExpression(case.then, subst, allocator);
            }
        },
        .indexRange => {
            try applyExpression(ast.kind.indexRange.expr, subst, allocator);

            if (ast.kind.indexRange.start) |start| {
                try applyExpression(start, subst, allocator);
            }
            if (ast.kind.indexRange.end) |end| {
                try applyExpression(end, subst, allocator);
            }
        },
        .indexValue => {
            try applyExpression(ast.kind.indexValue.expr, subst, allocator);
            try applyExpression(ast.kind.indexValue.index, subst, allocator);
        },
        .literalFunction => {
            for (ast.kind.literalFunction.params) |param| {
                if (param.default) |d| {
                    try applyExpression(d, subst, allocator);
                }
            }
            try applyExpression(ast.kind.literalFunction.body, subst, allocator);
        },
        .literalRecord => {
            for (ast.kind.literalRecord) |field| {
                switch (field) {
                    .value => try applyExpression(field.value.value, subst, allocator),
                    .record => try applyExpression(field.record, subst, allocator),
                }
            }
        },
        .literalSequence => {
            for (ast.kind.literalSequence) |elem| {
                switch (elem) {
                    .value => try applyExpression(elem.value, subst, allocator),
                    .sequence => try applyExpression(elem.sequence, subst, allocator),
                }
            }
        },
        .match => {
            try applyExpression(ast.kind.match.value, subst, allocator);
            for (ast.kind.match.cases) |case| {
                try applyPattern(case.pattern, subst, allocator);
                try applyExpression(case.body, subst, allocator);
            }
        },
        .notOp => try applyExpression(ast.kind.notOp.value, subst, allocator),
        .patternDeclaration => {
            try applyPattern(ast.kind.patternDeclaration.pattern, subst, allocator);
            try applyExpression(ast.kind.patternDeclaration.value, subst, allocator);
        },
        .raise => {
            try applyExpression(ast.kind.raise.expr, subst, allocator);
        },
        .whilee => {
            try applyExpression(ast.kind.whilee.condition, subst, allocator);
            try applyExpression(ast.kind.whilee.body, subst, allocator);
        },
        else => {},
    }
}

fn applyPattern(ast: *AST.Pattern, subst: *Typing.Subst, allocator: std.mem.Allocator) !void {
    switch (ast.kind) {
        .record => {
            for (ast.kind.record.entries) |field| {
                if (field.pattern) |p| {
                    try applyPattern(p, subst, allocator);
                }
            }
        },
        .sequence => {
            for (ast.kind.sequence.patterns) |p| {
                try applyPattern(p, subst, allocator);
            }
        },
        else => {},
    }
}

const TestState = @import("lib/test_state.zig").TestState;

test "!True" {
    var state = try TestState.init();
    defer state.deinit();

    const result = try state.parseAnalyse("!True");
    defer result.?.decRef(state.allocator);

    try std.testing.expect(!state.errors.hasErrors());
    try std.testing.expect(result != null);
}

test "!23" {
    var state = try TestState.init();
    defer state.deinit();

    const result = try state.parseAnalyse("!23");
    defer result.?.decRef(state.allocator);

    try std.testing.expect(state.errors.hasErrors());
    try std.testing.expect(result != null);
}
