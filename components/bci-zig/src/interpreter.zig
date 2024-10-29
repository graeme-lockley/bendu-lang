const std = @import("std");

const Op = @import("op.zig").Op;
const Pointer = @import("pointer.zig");
const Runtime = @import("runtime.zig");

const DEBUG = false;

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
            .add_i32 => {
                if (DEBUG) {
                    std.debug.print("{d}: add_i32\n", .{ip - 1});
                }

                try runtime.add_i32();
            },
            .sub_i32 => {
                if (DEBUG) {
                    std.debug.print("{d}: sub_i32\n", .{ip - 1});
                }

                try runtime.sub_i32();
            },
            .mul_i32 => {
                if (DEBUG) {
                    std.debug.print("{d}: mul_i32\n", .{ip - 1});
                }

                try runtime.mul_i32();
            },
            .div_i32 => {
                if (DEBUG) {
                    std.debug.print("{d}: div_i32\n", .{ip - 1});
                }

                try runtime.div_i32();
            },
            .mod_i32 => {
                if (DEBUG) {
                    std.debug.print("{d}: mod_i32\n", .{ip - 1});
                }

                try runtime.mod_i32();
            },
            .pow_i32 => {
                if (DEBUG) {
                    std.debug.print("{d}: pow_i32\n", .{ip - 1});
                }

                try runtime.pow_i32();
            },
            .print_i32 => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d}: print_i32: value={d}\n", .{ ip - 1, Pointer.asInt(value) });
                }

                try runtime.print_i32();
            },
            .println => {
                if (DEBUG) {
                    std.debug.print("{d}: println\n", .{ip - 1});
                }

                try runtime.println();
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

test "add_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.add_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 142);
}

test "sub_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.sub_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 58);
}

test "mul_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.mul_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 4200);
}

test "div_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.div_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 2);
}

test "mod_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.mod_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 16);
}

test "pow_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.pow_i32)};
    try runtime.push_i32_literal(2);
    try runtime.push_i32_literal(16);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 65536);
}
