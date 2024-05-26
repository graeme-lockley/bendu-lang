const std = @import("std");

const AST = @import("../ast.zig");
const SP = @import("../string_pool.zig");

pub fn eval(ast: *AST.Expression, allocator: std.mem.Allocator) !void {
    var runtime = Runtime.init(allocator);
    defer runtime.deinit();

    _ = try evalExpression(ast, &runtime);
}

const Runtime = struct {
    allocator: std.mem.Allocator,
    state: std.AutoHashMap(*SP.String, i64),

    pub fn init(allocator: std.mem.Allocator) Runtime {
        return Runtime{
            .allocator = allocator,
            .state = std.AutoHashMap(*SP.String, i64).init(allocator),
        };
    }

    pub fn deinit(self: *Runtime) void {
        var iterator = self.state.keyIterator();

        while (iterator.next()) |key| {
            key.*.decRef();
        }

        self.state.deinit();
    }
};

fn evalExpression(ast: *AST.Expression, runtime: *Runtime) !i64 {
    switch (ast.kind) {
        .block => {
            var result: i64 = 0;

            for (ast.kind.block.exprs) |expr| {
                result = try evalExpression(expr, runtime);
            }

            return result;
        },
        .idDeclaration => {
            const result = try evalExpression(ast.kind.idDeclaration.value, runtime);

            if (runtime.state.get(ast.kind.idDeclaration.name) != null) {
                try std.io.getStdErr().writer().print("Error: Attempt to redefine {s}\n", .{ast.kind.idDeclaration.name.slice()});
                std.process.exit(1);
            }

            try runtime.state.put(ast.kind.idDeclaration.name.incRefR(), result);
            return result;
        },
        .identifier => if (runtime.state.get(ast.kind.identifier.name)) |value| {
            return value;
        } else {
            try std.io.getStdErr().writer().print("Error: Undefined variable {s}\n", .{ast.kind.identifier.name.slice()});
            std.process.exit(1);
        },
        .literalInt => return ast.kind.literalInt.value,
        .println => {
            const writer = std.io.getStdOut().writer();

            var result: i64 = 0;

            for (ast.kind.println.exprs) |expr| {
                result = try evalExpression(expr, runtime);
                try writer.print("{d} ", .{result});
            }
            try writer.print("\n", .{});

            return result;
        },
    }
}
