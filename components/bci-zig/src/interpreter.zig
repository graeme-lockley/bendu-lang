const std = @import("std");

const Op = @import("op.zig").Op;
const Pointer = @import("pointer.zig");
const Runtime = @import("runtime.zig");

const DEBUG = false;

const stdout = std.io.getStdOut().writer();

pub fn run(bc: []const u8, runtime: *Runtime.Runtime) !void {
    var ip: usize = 0;
    while (ip < bc.len) {
        const op = @as(Op, @enumFromInt(bc[ip]));

        ip += 1;
        switch (op) {
            .push_i32_literal => {
                const value = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d}: push_i32_literal: value={d}\n", .{ ip - 1, value });
                }

                try runtime.push_i32_literal(value);
                ip += 4;
            },
            .push_i32_stack => {
                const index = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d}: push_i32_stack: offset={d}, value={d}\n", .{ ip - 1, index, Pointer.asInt(runtime.stack.items[@intCast(index)]) });
                }

                try runtime.push_i32_stack(index);
                ip += 4;
            },
            .print_i32 => {
                const value = runtime.pop();

                if (DEBUG) {
                    std.debug.print("{d}: print_i32: value={d}\n", .{ ip - 1, Pointer.asInt(value) });
                }

                try stdout.print("{d}", .{Pointer.asInt(value)});
            },
            .println => {
                if (DEBUG) {
                    std.debug.print("{d}: println\n", .{ip - 1});
                }
                try stdout.print("\n", .{});
            },
            // else => std.debug.panic("unknown op code: {d}\n", .{op}),
        }
    }
}

fn readi32(bc: []const u8, ip: usize) i32 {
    // std.io.getStdOut().writer().print("readInt: bc.len={d}, ip={d}\n", .{ bc.len, ip }) catch {};

    const v: i32 = @bitCast(@as(u32, (bc[ip + 3])) |
        (@as(u32, bc[ip + 2]) << 8) |
        (@as(u32, bc[ip + 1]) << 16) |
        (@as(u32, bc[ip]) << 24));

    return v;
}

test "push_i32_literal" {
    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42 };
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try run(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 1);
    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 42);
}

test "push_i32_stack" {
    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_i32_stack), 0, 0, 0, 0 };
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runtime.push_i32_literal(100);
    try run(&bc, &runtime);
    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 100);
}
