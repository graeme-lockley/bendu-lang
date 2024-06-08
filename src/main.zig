const std = @import("std");

const AST = @import("ast.zig");
const Parser = @import("parser.zig");
const SP = @import("string_pool.zig");
const Static = @import("static.zig");

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

    var action: []const u8 = undefined;
    var script: []const u8 = undefined;

    if (args.len == 2) {
        action = "--ast";
        script = args[1];
    } else if (args.len == 3) {
        action = args[1];
        script = args[2];
    } else {
        try stdout.print("Usage: {s} [--ast|--bc] script\n", .{args[0]});
        // try stdout.print("Usage: {s} [--ast|--bc|--wasm|--llvm]\n", .{args[0]});
        return;
    }

    var parseResult = try Parser.parse(&sp, "script.bendu", script);
    defer switch (parseResult) {
        .Ok => parseResult.Ok.decRef(allocator),
        .Err => parseResult.Err.deinit(),
    };

    switch (parseResult) {
        .Ok => {
            try Static.analysis(parseResult.Ok);

            if (std.mem.eql(u8, action, "--ast")) {
                const v = try @import("./ast/interpreter.zig").eval(parseResult.Ok, allocator);
                try stdout.print("{d}", .{v});
            } else if (std.mem.eql(u8, action, "--bc")) {
                const v = try @import("./bc/interpreter.zig").eval(parseResult.Ok, allocator);
                try stdout.print("{d}", .{v});
                // } else if (std.mem.eql(u8, args[1], "--wasm")) {
                //     try stdout.print("WASM\n", .{});
                // } else if (std.mem.eql(u8, args[1], "--llvm")) {
                //     try stdout.print("executing LLVM\n", .{});
                //     try @import("./native/interpreter.zig").eval(program, allocator);
            } else {
                try stdout.print("Invalid argument: {s}\n", .{action});
                std.process.exit(1);
            }
        },
        .Err => {
            const msg = try parseResult.Err.toString(allocator);
            defer allocator.free(msg);
            try stdout.print("Error: {s}\n", .{msg});
            std.process.exit(1);
        },
    }
}

test "All tests" {
    _ = @import("./lexer.zig");
    _ = @import("./parser.zig");
    _ = @import("./typing.zig");
}
