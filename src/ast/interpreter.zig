const std = @import("std");

const AST = @import("../ast.zig");
const Pointer = @import("../runtime/pointer.zig");
const Runtime = @import("../runtime/runtime.zig").Runtime;
const SP = @import("../lib/string_pool.zig");

pub fn eval(ast: *AST.Package, runtime: *Runtime) !void {
    var env = Environment.init(runtime);
    defer env.deinit();

    try evalPackage(ast, &env);
}

const Environment = struct {
    state: std.AutoHashMap(*SP.String, usize),
    runtime: *Runtime,

    pub fn init(runtime: *Runtime) Environment {
        return Environment{
            .state = std.AutoHashMap(*SP.String, usize).init(runtime.allocator),
            .runtime = runtime,
        };
    }

    pub fn deinit(self: *Environment) void {
        var iterator = self.state.keyIterator();

        while (iterator.next()) |key| {
            key.*.decRef();
        }

        self.state.deinit();
    }
};

fn evalPackage(ast: *AST.Package, env: *Environment) !void {
    try env.runtime.push_unit();
    for (ast.exprs) |expr| {
        env.runtime.discard();
        try evalExpression(expr, env);
    }
}

fn evalExpression(ast: *AST.Expression, env: *Environment) !void {
    switch (ast.kind) {
        .binaryOp => {
            if (ast.kind.binaryOp.op == .And or ast.kind.binaryOp.op == .Or) {
                try evalExpression(ast.kind.binaryOp.lhs, env);
                if (ast.kind.binaryOp.op == .And) {
                    if (Pointer.asBool(env.runtime.peek().?)) {
                        env.runtime.discard();
                        try evalExpression(ast.kind.binaryOp.rhs, env);
                    }
                } else if (!Pointer.asBool(env.runtime.peek().?)) {
                    env.runtime.discard();
                    try evalExpression(ast.kind.binaryOp.rhs, env);
                }
            } else {
                try evalExpression(ast.kind.binaryOp.lhs, env);
                try evalExpression(ast.kind.binaryOp.rhs, env);

                switch (ast.kind.binaryOp.op) {
                    .Divide => if (ast.type.?.isInt()) try env.runtime.divide_int() else if (ast.type.?.isChar()) try env.runtime.divide_char() else if (ast.type.?.isFloat()) try env.runtime.divide_float() else try env.runtime.divide(),
                    .Equal => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.equals_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.equals_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.equals_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.equals_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.equals_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(true);
                    } else {
                        try env.runtime.equals();
                    },
                    .GreaterEqual => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.greaterequals_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.greaterequals_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.greaterequals_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.greaterequals_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.greaterequals_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(true);
                    } else {
                        try env.runtime.greaterequals();
                    },
                    .GreaterThan => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.greaterthan_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.greaterthan_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.greaterthan_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.greaterthan_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.greaterthan_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(false);
                    } else {
                        try env.runtime.greaterthan();
                    },
                    .LessEqual => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.lessequals_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.lessequals_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.lessequals_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.lessequals_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.lessequals_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(true);
                    } else {
                        try env.runtime.lessequals();
                    },
                    .LessThan => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.lessthan_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.lessthan_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.lessthan_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.lessthan_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.lessthan_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(false);
                    } else {
                        try env.runtime.lessthan();
                    },
                    .Minus => if (ast.type.?.isInt()) try env.runtime.minus_int() else if (ast.type.?.isChar()) try env.runtime.minus_char() else if (ast.type.?.isFloat()) try env.runtime.minus_float() else try env.runtime.minus(),
                    .Modulo => try env.runtime.modulo_int(),
                    .NotEqual => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.notequals_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.notequals_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.notequals_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.notequals_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.notequals_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(false);
                    } else {
                        try env.runtime.notequals();
                    },
                    .Plus => if (ast.type.?.isInt()) try env.runtime.add_int() else if (ast.type.?.isChar()) try env.runtime.add_char() else if (ast.type.?.isFloat()) try env.runtime.add_float() else if (ast.type.?.isString()) try env.runtime.add_string() else try env.runtime.add(),
                    .Power => if (ast.type.?.isInt()) try env.runtime.power_int() else if (ast.type.?.isChar()) try env.runtime.power_char() else if (ast.type.?.isFloat()) try env.runtime.power_float() else try env.runtime.power(),
                    .Times => if (ast.type.?.isInt()) try env.runtime.times_int() else if (ast.type.?.isChar()) try env.runtime.times_char() else if (ast.type.?.isFloat()) try env.runtime.times_float() else try env.runtime.times(),
                    else => {
                        try std.io.getStdErr().writer().print("Internal Error: Unsupported binary operator: \"{s}\"\n", .{ast.kind.binaryOp.op.toString()});
                        std.process.exit(1);
                    },
                }
            }
        },
        .call => {
            if (ast.kind.call.callee.kind != .identifier) {
                try std.io.getStdErr().writer().print("Internal Error: Expected identifier\n", .{});
                std.process.exit(1);
            } else {
                const callee = ast.kind.call.callee.kind.identifier.slice();

                if (std.mem.eql(u8, callee, "println")) {
                    const writer = std.io.getStdOut().writer();

                    try env.runtime.push_int(0);

                    for (ast.kind.call.args) |expr| {
                        env.runtime.discard();
                        try evalExpression(expr, env);
                        try writer.print("{d} ", .{Pointer.asInt(env.runtime.peek().?)});
                    }
                    try writer.print("\n", .{});
                } else {
                    try std.io.getStdErr().writer().print("Internal Error: Unknown function {s}\n", .{callee});
                    std.process.exit(1);
                }
            }
        },
        .exprs => {
            try env.runtime.push_int(0);
            for (ast.kind.exprs) |expr| {
                env.runtime.discard();
                try evalExpression(expr, env);
            }
        },
        .idDeclaration => {
            try evalExpression(ast.kind.idDeclaration.value, env);

            if (env.state.get(ast.kind.idDeclaration.name) != null) {
                try std.io.getStdErr().writer().print("Internal Error: Attempt to redefine {s}\n", .{ast.kind.idDeclaration.name.slice()});
                std.process.exit(1);
            }

            try env.state.put(ast.kind.idDeclaration.name.incRefR(), env.runtime.stackPointer());
            try env.runtime.duplicate();
        },
        .identifier => if (env.state.get(ast.kind.identifier)) |value| {
            try env.runtime.push_pointer(env.runtime.stackItem(value));
        } else {
            try std.io.getStdErr().writer().print("Internal Error: Undefined variable {s}\n", .{ast.kind.identifier.slice()});
            std.process.exit(1);
        },
        .ifte => {
            for (ast.kind.ifte) |ifte| {
                if (ifte.condition) |condition| {
                    try evalExpression(condition, env);

                    if (Pointer.asBool(env.runtime.peek().?)) {
                        env.runtime.discard();
                        try evalExpression(ifte.then, env);
                        return;
                    }
                    env.runtime.discard();
                } else {
                    try evalExpression(ifte.then, env);
                    return;
                }
            }

            try std.io.getStdErr().writer().print("Runtime Error: no if branch executed: ", .{});
            try ast.locationRange.write(std.io.getStdErr().writer());
            try std.io.getStdErr().writer().print("\n", .{});
            std.process.exit(1);
        },
        .literalBool => try env.runtime.push_bool(ast.kind.literalBool),
        .literalChar => try env.runtime.push_int(@intCast(ast.kind.literalChar)),
        .literalInt => try env.runtime.push_int(@intCast(ast.kind.literalInt)),
        .literalFloat => try env.runtime.push_float(ast.kind.literalFloat),
        .literalString => try env.runtime.push_string(ast.kind.literalString),
        .literalVoid => try env.runtime.push_int(0),
        .notOp => {
            try evalExpression(ast.kind.notOp.value, env);
            try env.runtime.not();
        },
        else => {
            try std.io.getStdErr().writer().print("Internal Error: Unsupported expression kind\n", .{});
            std.process.exit(1);
        },
    }
}
