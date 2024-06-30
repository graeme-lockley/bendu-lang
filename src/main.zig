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

    var errors = try Errors.Errors.init(allocator);

    const parseResult = try Parser.parse(&sp, "script.bendu", script, &errors);

    if (parseResult) |ast| {
        defer ast.destroy(allocator);
        const typ = try Static.package(ast, &sp, &errors);
        defer typ.decRef(allocator);

        if (!errors.hasErrors()) {
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
            } else if (std.mem.eql(u8, action, "--test")) {
                try @import("./ast/interpreter.zig").eval(ast, &runtime);
                var runtime2 = Runtime.Runtime.init(&sp);
                defer runtime2.deinit();
                try @import("./bc/interpreter.zig").eval(ast, &runtime2);

                var buffer1 = std.ArrayList(u8).init(allocator);
                defer buffer1.deinit();
                var buffer2 = std.ArrayList(u8).init(allocator);
                defer buffer2.deinit();

                try valueToString(runtime.peek().?, typ, &buffer1);
                try valueToString(runtime2.peek().?, typ, &buffer2);

                if (std.mem.eql(u8, buffer1.items, buffer2.items)) {
                    try stdout.print("{s}: {s}", .{ buffer1.items, typeString });
                } else {
                    try stdout.print("{s}\n{s}\n", .{ buffer1.items, buffer2.items });
                }
                return;
            } else {
                try stdout.print("Invalid argument: {s}\n", .{action});
                std.process.exit(1);
            }
            try printValue(runtime.peek().?, typ);
            try stdout.print(": {s}", .{typeString});

            // if (runtime.stack.items.len > 1) {
            //     try stdout.print(" (stack has {d} items)", .{runtime.stack.items.len});
            // }
        }
    }

    if (errors.hasErrors()) {
        for (errors.items.items) |*err| {
            const msg = try err.toString(allocator);
            defer allocator.free(msg);

            try stdout.print("Error: {s}\n", .{msg});
        }
        std.process.exit(1);
    }
}

fn printValue(v: Pointer.Pointer, typ: *Typing.Type) !void {
    var dbuffer: [1024]u8 = undefined;
    var fba = std.heap.FixedBufferAllocator.init(&dbuffer);
    const allocator = fba.allocator();
    var buffer = std.ArrayList(u8).init(allocator);

    defer buffer.deinit();

    try valueToString(v, typ, &buffer);
    try stdout.print("{s}", .{buffer.items});
}

fn valueToString(v: Pointer.Pointer, typ: *Typing.Type, buffer: *std.ArrayList(u8)) !void {
    switch (typ.kind) {
        .Function => try buffer.appendSlice("Function"),
        .Tag => {
            const name = typ.kind.Tag.name.slice();

            if (std.mem.eql(u8, name, "Bool")) {
                try buffer.appendSlice(if (Pointer.asInt(v) == 0) "False" else "True");
            } else if (std.mem.eql(u8, name, "Char")) {
                const c: u8 = Pointer.asChar(v);
                switch (c) {
                    10 => try buffer.appendSlice("'\\n'"),
                    39 => try buffer.appendSlice("'\\''"),
                    92 => try buffer.appendSlice("'\\\\'"),
                    0...9, 11...31, 128...255 => try std.fmt.format(buffer.writer(), "'\\x{d}'", .{c}),
                    else => try std.fmt.format(buffer.writer(), "'{c}'", .{c}),
                }
            } else if (std.mem.eql(u8, name, "Float")) {
                const f = @as(*Memory.FloatValue, @ptrFromInt(v)).value;
                try std.fmt.format(buffer.writer(), "{d}", .{f});
            } else if (std.mem.eql(u8, name, "Int")) {
                try std.fmt.format(buffer.writer(), "{d}", .{Pointer.asInt(v)});
            } else if (std.mem.eql(u8, name, "String")) {
                const str = @as(*Memory.StringValue, @ptrFromInt(v)).value.slice();

                try buffer.appendSlice("\"");
                for (str) |c| {
                    switch (c) {
                        10 => try buffer.appendSlice("\\n"),
                        34 => try buffer.appendSlice("\\\""),
                        92 => try buffer.appendSlice("\\\\"),
                        0...9, 11...31 => try std.fmt.format(buffer.writer(), "\\x{d};", .{c}),
                        else => try buffer.append(c),
                    }
                }
                try buffer.appendSlice("\"");
            } else if (std.mem.eql(u8, name, "Unit")) {
                try buffer.appendSlice("()");
            } else {
                try buffer.appendSlice("Tag");
            }
        },
        .Variable => try buffer.appendSlice("Variable"),
        else => unreachable,
    }
}

test "All tests" {
    _ = @import("lexer.zig");
    _ = @import("parser.zig");
    _ = @import("runtime/memory.zig");
    _ = @import("runtime/pointer.zig");
    _ = @import("runtime/runtime.zig");
    _ = @import("static.zig");
    _ = @import("typing.zig");
    _ = @import("typing/subst.zig");
}
