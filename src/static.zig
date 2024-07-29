const std = @import("std");

const AST = @import("ast.zig");
const Errors = @import("errors.zig");
const SP = @import("lib/string_pool.zig");
const Typing = @import("typing.zig");

pub const Env = struct {
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

    pub fn deinit(self: *Env) void {
        self.boolType.decRef(self.allocator);
        self.charType.decRef(self.allocator);
        self.errorType.decRef(self.allocator);
        self.floatType.decRef(self.allocator);
        self.intType.decRef(self.allocator);
        self.stringType.decRef(self.allocator);
        self.unitType.decRef(self.allocator);

        self.constraints.deinit(self.allocator);
        for (self.names.items) |*names| {
            var iterator = names.iterator();
            while (iterator.next()) |*entry| {
                entry.key_ptr.*.decRef();
                entry.value_ptr.*.deinit(self.allocator);
            }
            names.deinit();
        }
        self.names.deinit();

        var iterator2 = self.schemes.iterator();
        while (iterator2.next()) |entry| {
            entry.key_ptr.*.decRef();
            entry.value_ptr.*.deinit(self.allocator);
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

    pub fn resetConstraints(self: *Env) void {
        self.constraints.reset(self.allocator);
    }
};

pub fn package(ast: *AST.Package, env: *Env) !void {
    for (ast.exprs) |expr| {
        env.resetConstraints();

        _ = try expression(expr, env);

        if (expr.kind != .declarations) {
            var subst = try Typing.solver(&env.constraints, &env.pump, env.errors, env.allocator);
            defer subst.deinit(env.allocator);

            var state = ApplyASTState{ .subst = &subst, .env = env };
            try applyExpression(expr, &state);
        }
    }
}

fn expression(ast: *AST.Expression, env: *Env) !*Typing.Type {
    switch (ast.kind) {
        .assignment => {
            _ = try expression(ast.kind.assignment.lhs, env);
            _ = try expression(ast.kind.assignment.rhs, env);
        },
        .binaryOp => {
            switch (ast.kind.binaryOp.op) {
                .And, .Or => {
                    const lhs = try expression(ast.kind.binaryOp.lhs, env);
                    const rhs = try expression(ast.kind.binaryOp.rhs, env);

                    if (!lhs.isBool()) {
                        try env.addConstraint(env.boolType, lhs, ast.kind.binaryOp.lhs.locationRange);
                    }
                    if (!rhs.isBool()) {
                        try env.addConstraint(env.boolType, rhs, ast.kind.binaryOp.rhs.locationRange);
                    }
                    ast.assignType(env.boolType.incRefR(), env.allocator);
                },

                .Divide, .Minus, .Power, .Times => {
                    const result = try env.pump.newBound(env.allocator);
                    ast.assignType(result, env.allocator);

                    const lhs = try expression(ast.kind.binaryOp.lhs, env);
                    const rhs = try expression(ast.kind.binaryOp.rhs, env);

                    try env.addConstraint(lhs, result, ast.kind.binaryOp.lhs.locationRange);
                    try env.addConstraint(rhs, result, ast.kind.binaryOp.rhs.locationRange);

                    const dependentType = try Typing.OrExtendType.new(env.allocator, env.charType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.floatType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.intType.incRefR(), try Typing.OrEmptyType.new(env.allocator))));
                    defer dependentType.decRef(env.allocator);

                    try env.addDependent(result, dependentType, ast.locationRange);
                },
                .Equal, .NotEqual => {
                    const common = try env.pump.newBound(env.allocator);
                    defer common.decRef(env.allocator);

                    const lhs = try expression(ast.kind.binaryOp.lhs, env);
                    const rhs = try expression(ast.kind.binaryOp.rhs, env);

                    try env.addConstraint(lhs, common, ast.kind.binaryOp.lhs.locationRange);
                    try env.addConstraint(rhs, common, ast.kind.binaryOp.rhs.locationRange);

                    ast.assignType(env.boolType.incRefR(), env.allocator);
                },
                .LessThan, .LessEqual, .GreaterThan, .GreaterEqual => {
                    const result = try env.pump.newBound(env.allocator);
                    defer result.decRef(env.allocator);

                    const lhs = try expression(ast.kind.binaryOp.lhs, env);
                    const rhs = try expression(ast.kind.binaryOp.rhs, env);

                    try env.addConstraint(lhs, result, ast.kind.binaryOp.lhs.locationRange);
                    try env.addConstraint(rhs, result, ast.kind.binaryOp.rhs.locationRange);

                    const dependentType = try Typing.OrExtendType.new(env.allocator, env.boolType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.charType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.floatType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.intType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.stringType.incRefR(), try Typing.OrExtendType.new(env.allocator, env.unitType.incRefR(), try Typing.OrEmptyType.new(env.allocator)))))));
                    defer dependentType.decRef(env.allocator);

                    try env.addDependent(result, dependentType, ast.locationRange);

                    ast.assignType(env.boolType.incRefR(), env.allocator);
                },
                .Modulo => {
                    const lhs = try expression(ast.kind.binaryOp.lhs, env);
                    if (!lhs.isInt()) {
                        try env.addConstraint(env.intType, lhs, ast.kind.binaryOp.lhs.locationRange);
                    }
                    const rhs = try expression(ast.kind.binaryOp.rhs, env);
                    if (!rhs.isInt()) {
                        try env.addConstraint(env.intType, rhs, ast.kind.binaryOp.rhs.locationRange);
                    }

                    ast.assignType(env.intType.incRefR(), env.allocator);
                },
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
                else => {
                    try env.appendError(try Errors.undefinedOperatorError(env.sp.allocator, ast.locationRange, ast.kind.binaryOp.op.toString()));
                },
            }
        },
        .call => {
            if (ast.kind.call.callee.kind == .identifier) {
                const calleeType = try expression(ast.kind.call.callee, env);
                var parameterTypeItems = std.ArrayList(*Typing.Type).init(env.allocator);
                defer {
                    for (parameterTypeItems.items) |t| {
                        t.decRef(env.allocator);
                    }
                    parameterTypeItems.deinit();
                }

                for (ast.kind.call.args) |arg| {
                    try parameterTypeItems.append((try expression(arg, env)).incRefR());
                }
                const returnType = try env.pump.newBound(env.allocator);

                const t1 = try Typing.TupleType.new(env.allocator, try parameterTypeItems.toOwnedSlice());
                const t2 = try Typing.FunctionType.new(
                    env.allocator,
                    t1,
                    returnType,
                );
                defer t2.decRef(env.allocator);

                try env.addConstraint(
                    calleeType,
                    t2,
                    ast.locationRange,
                );

                ast.assignType(returnType.incRefR(), env.allocator);
            } else {
                try env.appendError(try Errors.notImplementedError(env.sp.allocator, ast.kind.call.callee.locationRange, "Only support for calling local procedures"));
            }
        },
        // .catche => {
        //     _ = try expression(ast.kind.catche.value, env);
        //     for (ast.kind.catche.cases) |case| {
        //         _ = try pattern(case.pattern, env);
        //         _ = try expression(case.body, env);
        //     }
        // },
        .declarations => {
            const declaration = &ast.kind.declarations[0].IdDeclaration;

            const tv = try env.pump.newBound(env.allocator);
            const tvs = try env.allocator.alloc(*Typing.Type, ast.kind.declarations.len);
            defer {
                for (ast.kind.declarations, 0..) |_, idx| {
                    tvs[idx].decRef(env.allocator);
                }
                env.allocator.free(tvs);
            }

            try env.openScope();

            for (ast.kind.declarations, 0..) |decl, idx| {
                tvs[idx] = try env.pump.newBound(env.allocator);

                if (env.findNameInScope(decl.IdDeclaration.name)) |_| {
                    try env.appendError(try Errors.duplicateDeclarationError(env.sp.allocator, ast.locationRange, decl.IdDeclaration.name.slice()));
                } else {
                    try env.newName(decl.IdDeclaration.name, Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = tvs[idx].incRefR() });
                }
            }

            var params = std.ArrayList(AST.FunctionParam).init(env.allocator);
            defer params.deinit();

            try params.append(AST.FunctionParam{ .name = declaration.name.incRefR(), .default = null });
            const newDeclarationExpr = try AST.Expression.create(
                env.allocator,
                AST.ExpressionKind{ .literalFunction = AST.LiteralFunction{ .params = try params.toOwnedSlice(), .restOfParams = null, .body = declaration.value.incRefR() } },
                ast.locationRange,
            );
            newDeclarationExpr.assignType(tv, env.allocator);
            defer newDeclarationExpr.decRef(env.allocator);

            try env.newName(declaration.name, Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = tv });

            const t = try expression(declaration.value, env);

            ast.assignType(t.incRefR(), env.allocator);

            var subst = try Typing.solver(&env.constraints, &env.pump, env.errors, env.allocator);
            defer subst.deinit(env.allocator);

            var state = ApplyASTState{ .subst = &subst, .env = env };
            try applyExpression(ast, &state);

            env.closeScope();

            if (env.findNameInScope(declaration.name)) |_| {
                try env.appendError(try Errors.duplicateDeclarationError(env.sp.allocator, ast.locationRange, declaration.name.slice()));
            } else {
                try env.newName(declaration.name, try declaration.scheme.?.clone(env.allocator));
            }
        },
        // .dot => {
        //     _ = try expression(ast.kind.dot.record, env);
        // },
        .exprs => {
            var last: *Typing.Type = env.errorType;

            for (ast.kind.exprs) |expr| {
                last = try expression(expr, env);
            }

            ast.assignType(last.incRefR(), env.allocator);
        },
        .identifier => {
            if (env.findName(ast.kind.identifier)) |scheme| {
                ast.assignType(try scheme.instantiate(&env.pump, env.allocator), env.allocator);
            } else {
                try env.appendError(try Errors.undefinedNameError(env.sp.allocator, ast.locationRange, ast.kind.identifier.slice()));
            }
        },
        .ifte => {
            const result = try env.pump.newBound(env.allocator);
            ast.assignType(result, env.allocator);

            for (ast.kind.ifte) |case| {
                if (case.condition) |condition| {
                    const conditionType = try expression(condition, env);
                    if (!conditionType.isBool()) {
                        try env.addConstraint(env.boolType, conditionType, condition.locationRange);
                    }
                }
                const thenType = try expression(case.then, env);
                try env.addConstraint(result, thenType, case.then.locationRange);
            }
        },
        // .indexRange => {
        //     _ = try expression(ast.kind.indexRange.expr, env);

        //     if (ast.kind.indexRange.start) |start| {
        //         _ = try expression(start, env);
        //     }
        //     if (ast.kind.indexRange.end) |end| {
        //         _ = try expression(end, env);
        //     }
        // },
        // .indexValue => {
        //     _ = try expression(ast.kind.indexValue.expr, env);
        //     _ = try expression(ast.kind.indexValue.index, env);
        // },
        .literalBool => ast.assignType(env.boolType.incRefR(), env.allocator),
        .literalChar => ast.assignType(env.charType.incRefR(), env.allocator),
        .literalFloat => ast.assignType(env.floatType.incRefR(), env.allocator),
        .literalFunction => {
            try env.openScope();
            defer env.closeScope();

            var functionParamTypes = std.ArrayList(*Typing.Type).init(env.allocator);
            defer functionParamTypes.deinit();

            for (ast.kind.literalFunction.params) |param| {
                const t = try env.pump.newBound(env.allocator);
                try functionParamTypes.append(t);

                if (env.findNameInScope(param.name)) |_| {
                    try env.appendError(try Errors.duplicateDeclarationError(env.allocator, ast.locationRange, param.name.slice()));
                } else {
                    try env.newName(param.name, Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = t.incRefR() });
                }

                if (param.default) |d| {
                    const defaultParamType = try expression(d, env);

                    try env.addConstraint(defaultParamType, t, ast.locationRange);
                }
            }
            const range = try expression(ast.kind.literalFunction.body, env);

            const domain = try Typing.TupleType.new(env.allocator, try functionParamTypes.toOwnedSlice());

            const functionType = try Typing.FunctionType.new(env.allocator, domain, range.incRefR());
            ast.assignType(functionType, env.allocator);
        },
        .literalInt => ast.assignType(env.intType.incRefR(), env.allocator),
        // .literalRecord => {
        //     for (ast.kind.literalRecord) |field| {
        //         switch (field) {
        //             .value => _ = try expression(field.value.value, env),
        //             .record => _ = try expression(field.record, env),
        //         }
        //     }
        // },
        // .literalSequence => {
        //     for (ast.kind.literalSequence) |elem| {
        //         switch (elem) {
        //             .value => _ = try expression(elem.value, env),
        //             .sequence => _ = try expression(elem.sequence, env),
        //         }
        //     }
        // },
        .literalString => ast.assignType(env.stringType.incRefR(), env.allocator),
        .literalVoid => ast.assignType(env.unitType.incRefR(), env.allocator),
        // .match => {
        //     _ = try expression(ast.kind.match.value, env);
        //     for (ast.kind.match.cases) |case| {
        //         _ = try pattern(case.pattern, env);
        //         _ = try expression(case.body, env);
        //     }
        // },
        .notOp => {
            const t = try expression(ast.kind.notOp.value, env);
            if (!t.isBool()) {
                try env.addConstraint(env.boolType, t, ast.kind.notOp.value.locationRange);
            }
            ast.assignType(env.boolType.incRefR(), env.allocator);
        },
        // .patternDeclaration => {
        //     _ = try pattern(ast.kind.patternDeclaration.pattern, env);
        //     _ = try expression(ast.kind.patternDeclaration.value, env);
        // },
        // .raise => {
        //     _ = try expression(ast.kind.raise.expr, env);
        // },
        // .whilee => {
        //     _ = try expression(ast.kind.whilee.condition, env);
        //     _ = try expression(ast.kind.whilee.body, env);
        // },
        else => unreachable,
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

const ApplyASTState = struct {
    subst: *Typing.Subst,
    env: *Env,
};

fn applyExpression(ast: *AST.Expression, state: *ApplyASTState) !void {
    if (ast.type) |t| {
        ast.assignType(try t.apply(state.subst), state.env.allocator);
    }

    switch (ast.kind) {
        .assignment => {
            try applyExpression(ast.kind.assignment.lhs, state);
            try applyExpression(ast.kind.assignment.rhs, state);
        },
        .binaryOp => {
            try applyExpression(ast.kind.binaryOp.lhs, state);
            try applyExpression(ast.kind.binaryOp.rhs, state);
        },
        .call => {
            try applyExpression(ast.kind.call.callee, state);
            for (ast.kind.call.args) |arg| {
                try applyExpression(arg, state);
            }
        },
        .catche => {
            try applyExpression(ast.kind.catche.value, state);
            for (ast.kind.catche.cases) |case| {
                try applyPattern(case.pattern, state);
                try applyExpression(case.body, state);
            }
        },
        .declarations => {
            for (ast.kind.declarations) |*declaration| {
                switch (declaration.*) {
                    .IdDeclaration => {
                        try applyExpression(declaration.IdDeclaration.value, state);

                        declaration.IdDeclaration.scheme = try ast.type.?.generalise(state.env.allocator, state.env.sp, &state.env.constraints);
                    },
                    .PatternDeclaration => {
                        try applyPattern(declaration.PatternDeclaration.pattern, state);
                        try applyExpression(declaration.PatternDeclaration.value, state);
                    },
                }
            }
        },
        .dot => _ = try applyExpression(ast.kind.dot.record, state),
        .exprs => for (ast.kind.exprs) |expr| {
            try applyExpression(expr, state);
        },
        .ifte => for (ast.kind.ifte) |case| {
            if (case.condition) |condition| {
                try applyExpression(condition, state);
            }
        },
        .indexRange => {
            try applyExpression(ast.kind.indexRange.expr, state);

            if (ast.kind.indexRange.start) |start| {
                try applyExpression(start, state);
            }
            if (ast.kind.indexRange.end) |end| {
                try applyExpression(end, state);
            }
        },
        .indexValue => {
            try applyExpression(ast.kind.indexValue.expr, state);
            try applyExpression(ast.kind.indexValue.index, state);
        },
        .literalFunction => {
            for (ast.kind.literalFunction.params) |param| {
                if (param.default) |d| {
                    try applyExpression(d, state);
                }
            }
            try applyExpression(ast.kind.literalFunction.body, state);
        },
        .literalRecord => for (ast.kind.literalRecord) |field| {
            switch (field) {
                .value => try applyExpression(field.value.value, state),
                .record => try applyExpression(field.record, state),
            }
        },
        .literalSequence => for (ast.kind.literalSequence) |elem| {
            switch (elem) {
                .value => try applyExpression(elem.value, state),
                .sequence => try applyExpression(elem.sequence, state),
            }
        },
        .match => {
            try applyExpression(ast.kind.match.value, state);
            for (ast.kind.match.cases) |case| {
                try applyPattern(case.pattern, state);
                try applyExpression(case.body, state);
            }
        },
        .notOp => try applyExpression(ast.kind.notOp.value, state),
        .raise => try applyExpression(ast.kind.raise.expr, state),
        .whilee => {
            try applyExpression(ast.kind.whilee.condition, state);
            try applyExpression(ast.kind.whilee.body, state);
        },
        else => {},
    }
}

fn applyPattern(ast: *AST.Pattern, state: *ApplyASTState) !void {
    switch (ast.kind) {
        .record => {
            for (ast.kind.record.entries) |field| {
                if (field.pattern) |p| {
                    try applyPattern(p, state);
                }
            }
        },
        .sequence => {
            for (ast.kind.sequence.patterns) |p| {
                try applyPattern(p, state);
            }
        },
        else => {},
    }
}

const TestState = @import("lib/test_state.zig");

test "!True" {
    try TestState.expectSchemeString("!True", "Bool");
}

test "!23" {
    var state = try TestState.TestState.init();
    defer state.deinit();

    const result = try state.parseAnalyse("!23");
    defer result.?.decRef(state.allocator);

    try std.testing.expect(state.errors.hasErrors());
    try std.testing.expect(result != null);
}

test "let inc(n) = n + 1" {
    try TestState.expectSchemeString("let inc(n) = n + 1", "(Int) -> Int");
}

test "let inc(n) = n + 1 ; inc(10)" {
    try TestState.expectSchemeString("let inc(n) = n + 1 ; inc(10)", "Int");
}

test "let add(n, m) = n + m" {
    try TestState.expectSchemeString("let add(n, m) = n + m", "[a: Char | Float | Int | String] (a, a) -> a");
}

test "let add(n, m) = n + m ; add(1, 2)" {
    try TestState.expectSchemeString("let add(n, m) = n + m ; add(1, 2)", "Int");
}

test "let factorial(n) = if n < 2 -> 1 | n * factorial(n - 1)" {
    try TestState.expectSchemeString("let factorial(n) = if n < 2 -> 1 | n * factorial(n - 1)", "(Int) -> Int");
}

// test "mutually recursive functions" {
//     try TestState.expectSchemeString("let x = 1 and y = 2", "Int; Int");
//     try TestState.expectSchemeString("let odd(n) = if n == 0 -> false | even(n - 1) and even(n) = if n == 0 -> true | odd(n - 1)", "(Int) -> Bool; (Int) -> Bool");
// }
