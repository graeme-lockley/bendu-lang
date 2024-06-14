const std = @import("std");

const AST = @import("../ast.zig");
const Pointer = @import("../runtime/pointer.zig");
const Runtime = @import("../runtime/runtime.zig").Runtime;
const SP = @import("../string_pool.zig");

pub fn eval(ast: *AST.Expression, runtime: *Runtime) !void {
    var env = Environment.init(runtime);
    defer env.deinit();

    try evalExpression(ast, &env);
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

fn evalExpression(ast: *AST.Expression, env: *Environment) !void {
    switch (ast.kind) {
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
        .literalBool => try env.runtime.push_bool(ast.kind.literalBool),
        .literalInt => try env.runtime.push_int(@intCast(ast.kind.literalInt)),
        .literalVoid => try env.runtime.push_int(0),
        else => {
            try std.io.getStdErr().writer().print("Internal Error: Unsupported expression kind\n", .{});
            std.process.exit(1);
        },
    }
}
