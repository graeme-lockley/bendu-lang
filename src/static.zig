const std = @import("std");

const AST = @import("ast.zig");
const Errors = @import("errors.zig");
const SP = @import("string_pool.zig");
const Typing = @import("typing.zig");

const Env = struct {
    names: std.ArrayList(std.AutoHashMap(*SP.String, Typing.Scheme)),
    schemes: std.AutoHashMap(*SP.String, Typing.Scheme),
    errors: std.ArrayList(Errors.Error),

    pub fn init(allocator: std.mem.Allocator) !Env {
        var names = std.ArrayList(std.AutoHashMap(*SP.String, Typing.Scheme)).init(allocator);
        try names.append(std.AutoHashMap(*SP.String, Typing.Scheme).init(allocator));

        return Env{
            .names = names,
            .schemes = std.AutoHashMap(*SP.String, Typing.Scheme).init(allocator),
            .errors = std.ArrayList(Errors.Error).init(allocator),
        };
    }

    pub fn deinit(self: *Env, allocator: std.mem.Allocator) void {
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
};

pub fn analysis(ast: *AST.Expression, allocator: std.mem.Allocator) !void {
    var env = try Env.init(allocator);
    defer env.deinit(allocator);

    try expression(ast, &env);
}

fn expression(ast: *AST.Expression, env: *Env) !void {
    switch (ast.kind) {
        .assignment => {
            try expression(ast.kind.assignment.lhs, env);
            try expression(ast.kind.assignment.rhs, env);
        },
        .binaryOp => {
            try expression(ast.kind.binaryOp.lhs, env);
            try expression(ast.kind.binaryOp.rhs, env);
        },
        .call => {
            try expression(ast.kind.call.callee, env);
            for (ast.kind.call.args) |arg| {
                try expression(arg, env);
            }
        },
        .catche => {
            try expression(ast.kind.catche.value, env);
            for (ast.kind.catche.cases) |case| {
                try pattern(case.pattern, env);
                try expression(case.body, env);
            }
        },
        .dot => {
            try expression(ast.kind.dot.record, env);
        },
        .idDeclaration => {
            try expression(ast.kind.idDeclaration.value, env);
        },
        .ifte => {
            for (ast.kind.ifte) |case| {
                if (case.condition) |condition| {
                    try expression(condition, env);
                }
                try expression(case.then, env);
            }
        },
        .indexRange => {
            try expression(ast.kind.indexRange.expr, env);

            if (ast.kind.indexRange.start) |start| {
                try expression(start, env);
            }
            if (ast.kind.indexRange.end) |end| {
                try expression(end, env);
            }
        },
        .indexValue => {
            try expression(ast.kind.indexValue.expr, env);
            try expression(ast.kind.indexValue.index, env);
        },
        .literalFunction => {
            for (ast.kind.literalFunction.params) |param| {
                if (param.default) |d| {
                    try expression(d, env);
                }
            }
            try expression(ast.kind.literalFunction.body, env);
        },
        .literalRecord => {
            for (ast.kind.literalRecord) |field| {
                switch (field) {
                    .value => try expression(field.value.value, env),
                    .record => try expression(field.record, env),
                }
            }
        },
        .literalSequence => {
            for (ast.kind.literalSequence) |elem| {
                switch (elem) {
                    .value => try expression(elem.value, env),
                    .sequence => try expression(elem.sequence, env),
                }
            }
        },
        .match => {
            try expression(ast.kind.match.value, env);
            for (ast.kind.match.cases) |case| {
                try pattern(case.pattern, env);
                try expression(case.body, env);
            }
        },
        .notOp => {
            try expression(ast.kind.notOp.value, env);
        },
        .patternDeclaration => {
            try pattern(ast.kind.patternDeclaration.pattern, env);
            try expression(ast.kind.patternDeclaration.value, env);
        },
        .raise => {
            try expression(ast.kind.raise.expr, env);
        },
        .whilee => {
            try expression(ast.kind.whilee.condition, env);
            try expression(ast.kind.whilee.body, env);
        },
        else => {},
    }
}

fn pattern(ast: *AST.Pattern, env: *Env) !void {
    switch (ast.kind) {
        .record => {
            for (ast.kind.record.entries) |field| {
                if (field.pattern) |p| {
                    try pattern(p, env);
                }
            }
        },
        .sequence => {
            for (ast.kind.sequence.patterns) |p| {
                try pattern(p, env);
            }
        },
        else => {},
    }
}
