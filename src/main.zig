const std = @import("std");

const AST = @import("ast.zig");
const Errors = @import("errors.zig");
const Parser = @import("parser.zig");
const Memory = @import("./runtime/memory.zig");
const Pointer = @import("./runtime/pointer.zig");
const Runtime = @import("./runtime/runtime.zig");
const SP = @import("lib/string_pool.zig");
const Static = @import("static.zig");
const Typing = @import("typing.zig");

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

    var errors = std.ArrayList(Errors.Error).init(allocator);

    const parseResult = try Parser.parse(&sp, "script.bendu", script, &errors);

    if (parseResult) |ast| {
        defer ast.decRef(allocator);
        const typ = try Static.analysis(ast, &sp, &errors);
        defer typ.decRef(allocator);

        if (errors.items.len == 0) {
            const typeString = try typ.toString(allocator);
            defer allocator.free(typeString);

            var runtime = Runtime.Runtime.init(&sp);
            defer runtime.deinit();

            if (std.mem.eql(u8, action, "--ast")) {
                try @import("./ast/interpreter.zig").eval(ast, &runtime);
            } else if (std.mem.eql(u8, action, "--bc")) {
                try @import("./bc/interpreter.zig").eval(ast, &runtime);
                // try stdout.print("{d}: {s}", .{ v, typeString });
                // } else if (std.mem.eql(u8, args[1], "--wasm")) {
                //     try stdout.print("WASM\n", .{});
                // } else if (std.mem.eql(u8, args[1], "--llvm")) {
                //     try stdout.print("executing LLVM\n", .{});
                //     try @import("./native/interpreter.zig").eval(program, allocator);
            } else {
                try stdout.print("Invalid argument: {s}\n", .{action});
                std.process.exit(1);
            }
            try printValue(runtime.peek().?, typ);
            try stdout.print(": {s}", .{typeString});
        }
    }

    if (errors.items.len != 0) {
        for (errors.items) |*err| {
            const msg = try err.toString(allocator);
            defer allocator.free(msg);

            try stdout.print("Error: {s}\n", .{msg});
        }
        std.process.exit(1);
    }
}

fn printValue(v: Pointer.Pointer, typ: *Typing.Type) !void {
    switch (typ.kind) {
        .Function => {
            try stdout.print("Function", .{});
        },
        .Tag => {
            const name = typ.kind.Tag.name.slice();

            if (std.mem.eql(u8, name, "Bool")) {
                try stdout.print("{s}", .{if (Pointer.asInt(v) == 0) "False" else "True"});
            } else if (std.mem.eql(u8, name, "Char")) {
                const c: u8 = @intCast(Pointer.asInt(v));
                switch (c) {
                    10 => try stdout.print("'\\n'", .{}),
                    39 => try stdout.print("'\\''", .{}),
                    92 => try stdout.print("'\\\\'", .{}),
                    0...9, 11...31 => try stdout.print("'\\x{d}'", .{c}),
                    else => try stdout.print("'{c}'", .{c}),
                }
            } else if (std.mem.eql(u8, name, "Float")) {
                const f = @as(*Memory.FloatValue, @ptrFromInt(v)).value;
                try stdout.print("{d}", .{f});
            } else if (std.mem.eql(u8, name, "Int")) {
                try stdout.print("{d}", .{Pointer.asInt(v)});
            } else if (std.mem.eql(u8, name, "String")) {
                const str = @as(*Memory.StringValue, @ptrFromInt(v)).value.slice();

                try stdout.print("\"", .{});
                for (str) |c| {
                    switch (c) {
                        10 => try stdout.print("\\n", .{}),
                        34 => try stdout.print("\\\"", .{}),
                        92 => try stdout.print("\\\\", .{}),
                        0...9, 11...31 => try stdout.print("\\x{d};", .{c}),
                        else => try stdout.print("{c}", .{c}),
                    }
                }
                try stdout.print("\"", .{});
            } else if (std.mem.eql(u8, name, "Unit")) {
                try stdout.print("()", .{});
            } else {
                try stdout.print("Tag", .{});
            }
        },
        .Variable => {
            try stdout.print("Variable", .{});
        },
        else => unreachable,
    }
}

test "All tests" {
    _ = @import("lexer.zig");
    _ = @import("parser.zig");
    _ = @import("runtime/memory.zig");
    _ = @import("runtime/pointer.zig");
    _ = @import("static.zig");
    _ = @import("typing.zig");
    _ = @import("typing/subst.zig");
}
