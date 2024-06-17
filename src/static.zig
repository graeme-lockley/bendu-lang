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

    names: std.ArrayList(std.AutoHashMap(*SP.String, Typing.Scheme)),
    schemes: std.AutoHashMap(*SP.String, Typing.Scheme),
    errors: std.ArrayList(Errors.Error),
    sp: *SP.StringPool,

    pub fn init(sp: *SP.StringPool) !Env {
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

        {
            const aType = try Typing.VariableType.new(allocator, try sp.intern("a"));
            defer aType.decRef(allocator);

            const fType = try Typing.FunctionType.new(allocator, aType.incRefR(), try Typing.FunctionType.new(allocator, aType.incRefR(), aType.incRefR()));
            errdefer fType.decRef(allocator);

            var nms = try allocator.alloc(Typing.SchemeBinding, 1);
            nms[0].name = try sp.intern("a");
            nms[0].type = try Typing.OrExtendType.new(allocator, intType.incRefR(), try Typing.OrExtendType.new(allocator, floatType.incRefR(), try Typing.OrExtendType.new(allocator, charType.incRefR(), try Typing.OrEmptyType.new(allocator))));
            errdefer {
                nms[0].deinit(allocator);
                allocator.free(nms);
            }

            try schemes.put(try sp.intern("+"), Typing.Scheme{ .names = nms, .type = fType });
        }

        return Env{
            .boolType = boolType,
            .charType = charType,
            .errorType = errorType,
            .floatType = floatType,
            .intType = intType,
            .stringType = stringType,
            .unitType = unitType,
            .names = names,
            .schemes = schemes,
            .errors = std.ArrayList(Errors.Error).init(allocator),
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
};

pub const AnalysisResult = struct {
    errors: []Errors.Error,
    type: *Typing.Type,

    pub fn deinit(self: AnalysisResult, allocator: std.mem.Allocator) void {
        for (self.errors) |*err| {
            err.deinit();
        }
        self.type.decRef(allocator);
    }
};

pub fn analysis(ast: *AST.Expression, sp: *SP.StringPool) !AnalysisResult {
    var env = try Env.init(sp);
    defer env.deinit(sp.allocator);

    const typ = try expression(ast, &env);

    return AnalysisResult{ .errors = try env.errors.toOwnedSlice(), .type = typ.incRefR() };
}

fn expression(ast: *AST.Expression, env: *Env) !*Typing.Type {
    switch (ast.kind) {
        .assignment => {
            _ = try expression(ast.kind.assignment.lhs, env);
            _ = try expression(ast.kind.assignment.rhs, env);
        },
        .binaryOp => {
            _ = try expression(ast.kind.binaryOp.lhs, env);
            _ = try expression(ast.kind.binaryOp.rhs, env);
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

            return last;
        },
        .idDeclaration => {
            const t = try expression(ast.kind.idDeclaration.value, env);

            if (env.findNameInScope(ast.kind.idDeclaration.name)) |_| {
                try env.appendError(try Errors.duplicateDeclarationError(env.sp.allocator, ast.locationRange, ast.kind.idDeclaration.name.slice()));
            } else {
                try env.newName(ast.kind.idDeclaration.name, Typing.Scheme{ .names = &[_]Typing.SchemeBinding{}, .type = t.incRefR() });
            }

            return t;
        },
        .identifier => {
            if (env.findName(ast.kind.identifier)) |scheme| {
                ast.type = scheme.type.incRefR();
                return scheme.type;
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
        .literalBool => {
            ast.type = env.boolType.incRefR();
            return env.boolType;
        },
        .literalChar => {
            ast.type = env.charType.incRefR();
            return env.charType;
        },
        .literalFloat => {
            ast.type = env.floatType.incRefR();
            return env.floatType;
        },
        .literalFunction => {
            for (ast.kind.literalFunction.params) |param| {
                if (param.default) |d| {
                    _ = try expression(d, env);
                }
            }
            _ = try expression(ast.kind.literalFunction.body, env);
        },
        .literalInt => {
            ast.type = env.intType.incRefR();
            return env.intType;
        },
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
        .literalString => {
            ast.type = env.stringType.incRefR();
            return env.stringType;
        },
        .literalVoid => {
            ast.type = env.unitType.incRefR();
            return env.unitType;
        },
        .match => {
            _ = try expression(ast.kind.match.value, env);
            for (ast.kind.match.cases) |case| {
                _ = try pattern(case.pattern, env);
                _ = try expression(case.body, env);
            }
        },
        .notOp => {
            _ = try expression(ast.kind.notOp.value, env);
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

    return env.errorType;
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
