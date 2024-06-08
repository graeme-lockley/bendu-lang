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

    var program = try mkProgram(&sp);
    defer program.decRef(allocator);

    if (args.len == 1) {
        try stdout.print("executing AST\n", .{});
        try @import("./ast/interpreter.zig").eval(program, allocator);
        return;
    }

    if (args.len != 2) {
        try stdout.print("Usage: {s} [--ast|--bc]\n", .{args[0]});
        // try stdout.print("Usage: {s} [--ast|--bc|--wasm|--llvm]\n", .{args[0]});
        return;
    }

    if (std.mem.eql(u8, args[1], "--ast")) {
        try stdout.print("executing AST\n", .{});
        try @import("./ast/interpreter.zig").eval(program, allocator);
    } else if (std.mem.eql(u8, args[1], "--bc")) {
        try stdout.print("executing BC\n", .{});
        try @import("./bc/interpreter.zig").eval(program, allocator);
        // } else if (std.mem.eql(u8, args[1], "--wasm")) {
        //     try stdout.print("WASM\n", .{});
        // } else if (std.mem.eql(u8, args[1], "--llvm")) {
        //     try stdout.print("executing LLVM\n", .{});
        //     try @import("./native/interpreter.zig").eval(program, allocator);
    } else {
        try stdout.print("Invalid argument: {s}\n", .{args[1]});
    }
}

fn mkProgram(sp: *SP.StringPool) !*AST.Expression {
    const Parser = @import("./parser.zig");

    return try Parser.parse(sp, "test.bendu", "let x = 42; println(x)");
}

test "All tests" {
    _ = @import("./lexer.zig");
    _ = @import("./parser.zig");
}
