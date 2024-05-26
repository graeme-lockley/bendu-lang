const std = @import("std");

const AST = @import("ast.zig");
const SP = @import("string_pool.zig");

const stdout = std.io.getStdOut().writer();

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    const allocator = gpa.allocator();
    defer {
        const err = gpa.deinit();
        if (err == std.heap.Check.leak) {
            stdout.print("Failed to deinit allocator\n", .{}) catch {};
            std.process.exit(1);
        }
    }
    var sp = SP.StringPool.init(allocator);
    defer sp.deinit();

    const args = try std.process.argsAlloc(allocator);
    defer std.process.argsFree(allocator, args);

    var program = try mkProgram(allocator, &sp);
    defer program.decRef(allocator);

    if (args.len == 1) {
        try stdout.print("executing AST\n", .{});
        try @import("./ast/interpreter.zig").eval(program, allocator);
        return;
    }

    if (args.len != 2) {
        try stdout.print("Usage: {s} [--ast|--bc|--wasm|--llvm]\n", .{args[0]});
        return;
    }

    if (std.mem.eql(u8, args[1], "--ast")) {
        try stdout.print("executing AST\n", .{});
        try @import("./ast/interpreter.zig").eval(program, allocator);
    } else if (std.mem.eql(u8, args[1], "--bc")) {
        try stdout.print("BC\n", .{});
    } else if (std.mem.eql(u8, args[1], "--wasm")) {
        try stdout.print("WASM\n", .{});
    } else if (std.mem.eql(u8, args[1], "--llvm")) {
        try stdout.print("LLVM\n", .{});
    } else {
        try stdout.print("Invalid argument: {s}\n", .{args[1]});
    }
}

fn mkProgram(allocator: std.mem.Allocator, sp: *SP.StringPool) !*AST.Expression {
    const e1 = try AST.Expression.create(allocator, AST.ExpressionKind{ .idDeclaration = AST.IdDeclarationExpression{ .name = try sp.intern("x"), .value = try AST.Expression.create(allocator, AST.ExpressionKind{ .literalInt = AST.LiteralIntExpression{ .value = 42 } }) } });

    var printlnExprs = std.ArrayList(*AST.Expression).init(allocator);
    defer printlnExprs.deinit();

    try printlnExprs.append(try AST.Expression.create(allocator, AST.ExpressionKind{ .identifier = AST.IdentifierExpression{ .name = try sp.intern("x") } }));

    const e2 = try AST.Expression.create(allocator, AST.ExpressionKind{ .println = AST.PrintlnExpression{ .exprs = try printlnExprs.toOwnedSlice() } });

    var exprs = std.ArrayList(*AST.Expression).init(allocator);
    defer exprs.deinit();

    try exprs.append(e1);
    try exprs.append(e2);

    return try AST.Expression.create(allocator, AST.ExpressionKind{ .block = AST.BlockExpression{ .exprs = try exprs.toOwnedSlice() } });
}
