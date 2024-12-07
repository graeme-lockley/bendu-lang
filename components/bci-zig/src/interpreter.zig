const std = @import("std");

const Op = @import("op.zig").Op;
const Pointer = @import("pointer.zig");
const Runtime = @import("runtime.zig");

const DEBUG = false;

pub fn run(bc: []const u8, runtime: *Runtime.Runtime) !void {
    var ip: usize = 0;
    var fp: usize = 0;

    while (ip < bc.len) {
        const op = @as(Op, @enumFromInt(bc[ip]));

        ip += 1;
        switch (op) {
            .abort => {
                const code = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: abort, code={d}\n", .{ ip - 1, fp, code });
                }

                std.posix.exit(@intCast(code));
            },

            .push_bool_true => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: push_bool_true\n", .{ ip - 1, fp });
                }

                try runtime.push_bool_true();
            },

            .push_bool_false => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: push_bool_false\n", .{ ip - 1, fp });
                }

                try runtime.push_bool_false();
            },
            .push_f32_literal => {
                const value = readf32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: push_f32_literal: value={d}\n", .{ ip - 1, fp, value });
                }

                try runtime.push_f32_literal(value);
                ip += 4;
            },
            .push_i32_literal => {
                const value = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: push_i32_literal: value={d}\n", .{ ip - 1, fp, value });
                }

                try runtime.push_i32_literal(value);
                ip += 4;
            },
            .push_u8_literal => {
                const value = bc[ip];

                if (DEBUG) {
                    std.debug.print("{d} {d}: push_u8_literal: value={d}\n", .{ ip - 1, fp, value });
                }

                try runtime.push_u8_literal(value);
                ip += 1;
            },
            .push_unit_literal => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: push_unit_literal\n", .{ ip - 1, fp });
                }

                try runtime.push_unit_literal();
            },
            .push_string_literal => {
                const len = readi32(bc, ip);
                const data = bc[ip + 4 .. ip + 4 + @as(usize, @intCast(len))];

                if (DEBUG) {
                    std.debug.print("{d} {d}: push_string_literal: len={d}, data={s}\n", .{ ip - 1, fp, len, data });
                }

                try runtime.push_string_literal(data);
                ip += 4 + @as(usize, @intCast(len));
            },
            .push_stack => {
                const index = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: push_stack: offset={d}\n", .{ ip - 1, fp, index });
                }

                try runtime.push_stack(index);
                ip += 4;
            },
            .push_parameter => {
                const index = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: push_parameter: offset={d}\n", .{ ip - 1, fp, index });
                }

                try runtime.push_stack(@intCast(@as(i32, @intCast(fp)) + index));
                ip += 4;
            },
            .discard => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: discard\n", .{ ip - 1, fp });
                }

                runtime.discard();
            },

            .jmp => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: jmp: offset={d}\n", .{ ip - 1, fp, offset });
                }

                ip = @intCast(offset);
            },
            .jmp_false => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: jmp_false: offset={d}\n", .{ ip - 1, fp, offset });
                }

                const value = runtime.pop();
                if (Pointer.asBool(value)) {
                    ip += 4;
                } else {
                    ip = @intCast(offset);
                }
            },
            .jmp_dup_false => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: jmp_dup_false: offset={d}\n", .{ ip - 1, fp, offset });
                }

                const value = runtime.peek();
                if (Pointer.asBool(value)) {
                    ip += 4;
                } else {
                    ip = @intCast(offset);
                }
            },
            .jmp_dup_true => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: jmp_dup_true: offset={d}\n", .{ ip - 1, fp, offset });
                }

                const value = runtime.peek();
                if (Pointer.asBool(value)) {
                    ip = @intCast(offset);
                } else {
                    ip += 4;
                }
            },
            .call_local => {
                const offset = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: call_local: offset={d}\n", .{ ip - 1, fp, offset });
                }

                const newFP = runtime.stack.items.len - 1;

                try runtime.push_i32_literal(@intCast(ip + 4));
                try runtime.push_i32_literal(@intCast(fp));

                fp = newFP;
                ip = @intCast(offset);
            },
            .ret => {
                const items = readi32(bc, ip);

                if (DEBUG) {
                    std.debug.print("{d} {d}: ret: items={d}\n", .{ ip - 1, fp, items });
                }

                const result = runtime.pop();

                ip = @intCast(Pointer.asInt(runtime.stack.items[fp + 1]));
                fp = @intCast(Pointer.asInt(runtime.stack.items[fp + 2]));

                for (@as(usize, @intCast(items + 2))) |_| {
                    runtime.discard();
                }

                try runtime.stack.append(result);
            },

            .not_bool => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: not_bool\n", .{ ip - 1, fp });
                }

                try runtime.not_bool();
            },

            .add_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: add_f32\n", .{ ip - 1, fp });
                }

                try runtime.add_f32();
            },
            .add_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: add_i32\n", .{ ip - 1, fp });
                }

                try runtime.add_i32();
            },
            .add_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: add_string\n", .{ ip - 1, fp });
                }

                try runtime.add_string();
            },
            .add_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: add_u8\n", .{ ip - 1, fp });
                }

                try runtime.add_u8();
            },
            .sub_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: sub_f32\n", .{ ip - 1, fp });
                }

                try runtime.sub_f32();
            },
            .sub_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: sub_i32\n", .{ ip - 1, fp });
                }

                try runtime.sub_i32();
            },
            .sub_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: sub_u8\n", .{ ip - 1, fp });
                }

                try runtime.sub_u8();
            },
            .mul_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: mul_f32\n", .{ ip - 1, fp });
                }

                try runtime.mul_f32();
            },
            .mul_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: mul_i32\n", .{ ip - 1, fp });
                }

                try runtime.mul_i32();
            },
            .mul_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: mul_u8\n", .{ ip - 1, fp });
                }

                try runtime.mul_u8();
            },
            .div_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: div_f32\n", .{ ip - 1, fp });
                }

                try runtime.div_f32();
            },
            .div_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: div_i32\n", .{ ip - 1, fp });
                }

                try runtime.div_i32();
            },
            .div_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: div_u8\n", .{ ip - 1, fp });
                }

                try runtime.div_u8();
            },
            .mod_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: mod_i32\n", .{ ip - 1, fp });
                }

                try runtime.mod_i32();
            },
            .pow_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: pow_f32\n", .{ ip - 1, fp });
                }

                try runtime.pow_f32();
            },
            .pow_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: pow_i32\n", .{ ip - 1, fp });
                }

                try runtime.pow_i32();
            },

            .eq_bool => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: eq_bool\n", .{ ip - 1, fp });
                }

                try runtime.eq_bool();
            },
            .eq_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: eq_f32\n", .{ ip - 1, fp });
                }

                try runtime.eq_f32();
            },
            .eq_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: eq_i32\n", .{ ip - 1, fp });
                }

                try runtime.eq_i32();
            },
            .eq_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: eq_string\n", .{ ip - 1, fp });
                }

                try runtime.eq_string();
            },
            .eq_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: eq_u8\n", .{ ip - 1, fp });
                }

                try runtime.eq_u8();
            },
            .eq_unit => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: eq_unit\n", .{ ip - 1, fp });
                }

                try runtime.eq_unit();
            },
            .neq_bool => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: neq_bool\n", .{ ip - 1, fp });
                }

                try runtime.neq_bool();
            },
            .neq_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: neq_f32\n", .{ ip - 1, fp });
                }

                try runtime.neq_f32();
            },
            .neq_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: neq_i32\n", .{ ip - 1, fp });
                }

                try runtime.neq_i32();
            },
            .neq_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: neq_string\n", .{ ip - 1, fp });
                }

                try runtime.neq_string();
            },
            .neq_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: neq_u8\n", .{ ip - 1, fp });
                }

                try runtime.neq_u8();
            },
            .neq_unit => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: neq_unit\n", .{ ip - 1, fp });
                }

                try runtime.neq_unit();
            },
            .lt_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: lt_f32\n", .{ ip - 1, fp });
                }

                try runtime.lt_f32();
            },
            .lt_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: lt_i32\n", .{ ip - 1, fp });
                }

                try runtime.lt_i32();
            },
            .lt_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: lt_string\n", .{ ip - 1, fp });
                }

                try runtime.lt_string();
            },
            .lt_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: lt_u8\n", .{ ip - 1, fp });
                }

                try runtime.lt_u8();
            },
            .le_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: le_f32\n", .{ ip - 1, fp });
                }

                try runtime.le_f32();
            },
            .le_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: le_i32\n", .{ ip - 1, fp });
                }

                try runtime.le_i32();
            },
            .le_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: le_string\n", .{ ip - 1, fp });
                }

                try runtime.le_string();
            },
            .le_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: le_u8\n", .{ ip - 1, fp });
                }

                try runtime.le_u8();
            },
            .gt_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: gt_f32\n", .{ ip - 1, fp });
                }

                try runtime.gt_f32();
            },
            .gt_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: gt_i32\n", .{ ip - 1, fp });
                }

                try runtime.gt_i32();
            },
            .gt_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: gt_string\n", .{ ip - 1, fp });
                }

                try runtime.gt_string();
            },
            .gt_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: gt_u8\n", .{ ip - 1, fp });
                }

                try runtime.gt_u8();
            },
            .ge_f32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: ge_f32\n", .{ ip - 1, fp });
                }

                try runtime.ge_f32();
            },
            .ge_i32 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: ge_i32\n", .{ ip - 1, fp });
                }

                try runtime.ge_i32();
            },
            .ge_string => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: ge_string\n", .{ ip - 1, fp });
                }

                try runtime.ge_string();
            },
            .ge_u8 => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: ge_u8\n", .{ ip - 1, fp });
                }

                try runtime.ge_u8();
            },

            .println => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: println\n", .{ ip - 1, fp });
                }

                try runtime.println();
            },
            .print_bool => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: print_bool\n", .{ ip - 1, fp });
                }

                try runtime.print_bool();
            },
            .print_f32 => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d} {d}: print_f32: value={d}\n", .{ ip - 1, fp, Pointer.asInt(value) });
                }

                try runtime.print_f32();
            },
            .print_i32 => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d} {d}: print_i32: value={d}\n", .{ ip - 1, fp, Pointer.asInt(value) });
                }

                try runtime.print_i32();
            },
            .print_u8 => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d} {d}: print_u8: value={c}\n", .{ ip - 1, fp, Pointer.asChar(value) });
                }

                try runtime.print_u8();
            },
            .print_string => {
                if (DEBUG) {
                    const value = runtime.peek();
                    std.debug.print("{d} {d}: print_string: value={s}\n", .{ ip - 1, fp, Pointer.asString(value).data });
                }

                try runtime.print_string();
            },
            .print_unit => {
                if (DEBUG) {
                    std.debug.print("{d} {d}: print_unit\n", .{ ip - 1, fp });
                }

                try runtime.print_unit();
            },

            else => std.debug.panic("unknown op code\n", .{}),
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

fn readf32(bc: []const u8, ip: usize) f32 {
    const v: f32 = @bitCast(@as(u32, (bc[ip + 3])) |
        (@as(u32, bc[ip + 2]) << 8) |
        (@as(u32, bc[ip + 1]) << 16) |
        (@as(u32, bc[ip]) << 24));

    return v;
}

test "push_bool_true" {
    const bc: [1]u8 = [_]u8{@intFromEnum(Op.push_bool_true)};
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try run(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 1);
    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "push_bool_false" {
    const bc: [1]u8 = [_]u8{@intFromEnum(Op.push_bool_false)};
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try run(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 1);
    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), false);
}

test "push_f32_literal" {
    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_f32_literal), 0, 1, 0, 0 };
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try run(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 1);
    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 9.1835e-41);
}

test "push_i32_literal" {
    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_i32_literal), 0, 0, 0, 42 };
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try run(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 1);
    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 42);
}

test "push_u8_literal" {
    const bc: [2]u8 = [_]u8{ @intFromEnum(Op.push_u8_literal), 42 };
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try run(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 1);
    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 42);
}

test "push_stack" {
    const bc: [5]u8 = [_]u8{ @intFromEnum(Op.push_stack), 0, 0, 0, 0 };
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runtime.push_i32_literal(100);
    try run(&bc, &runtime);
    try std.testing.expectEqual(Pointer.asInt(runtime.pop()), 100);
}

test "not_bool" {
    const bc: [1]u8 = [_]u8{@intFromEnum(Op.not_bool)};
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();
    try runtime.push_bool_true();
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), false);
}

test "add_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.add_f32)};
    try runtime.push_f32_literal(100.0);
    try runtime.push_f32_literal(42.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 142.0);
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
test "add_string" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.add_string)};
    try runtime.push_string_literal("hello");
    try runtime.push_string_literal(" world");
    try run(&bc, &runtime);

    try std.testing.expectEqual(runtime.stack.items.len, 1);
    try std.testing.expect(std.mem.eql(u8, Pointer.asString(runtime.peek()).slice(), "hello world"));
}
test "add_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.add_u8)};
    try runtime.push_u8_literal(100);
    try runtime.push_u8_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 142);
}

test "sub_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.sub_f32)};
    try runtime.push_f32_literal(100.0);
    try runtime.push_f32_literal(42.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 58.0);
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
test "sub_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.sub_u8)};
    try runtime.push_u8_literal(100);
    try runtime.push_u8_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 58);
}

test "mul_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.mul_f32)};
    try runtime.push_f32_literal(100.0);
    try runtime.push_f32_literal(42.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 4200.0);
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
test "mul_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.mul_u8)};
    try runtime.push_u8_literal(80);
    try runtime.push_u8_literal(2);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 160);
}

test "div_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.div_f32)};
    try runtime.push_f32_literal(100.0);
    try runtime.push_f32_literal(42.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 2.3809524);
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

test "div_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.div_u8)};
    try runtime.push_u8_literal(100);
    try runtime.push_u8_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asChar(runtime.pop()), 2);
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

test "pow_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.pow_f32)};
    try runtime.push_f32_literal(2.0);
    try runtime.push_f32_literal(16.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asFloat(runtime.pop()), 65536.0);
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

test "eq_bool" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.eq_bool)};
    try runtime.push_bool_true();
    try runtime.push_bool_false();
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), false);
}

test "eq_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.eq_f32)};
    try runtime.push_f32_literal(100.0);
    try runtime.push_f32_literal(100.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "eq_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.eq_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(100);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "eq_string" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.eq_string)};
    try runtime.push_string_literal("hello");
    try runtime.push_string_literal("hello");
    try run(&bc, &runtime);

    try std.testing.expect(Pointer.asBool(runtime.peek()));
}

test "eq_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.eq_u8)};
    try runtime.push_u8_literal(100);
    try runtime.push_u8_literal(100);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "neq_bool" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.neq_bool)};
    try runtime.push_bool_true();
    try runtime.push_bool_false();
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "neq_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.neq_f32)};
    try runtime.push_f32_literal(100.0);
    try runtime.push_f32_literal(42.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "neq_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.neq_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "neq_string" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.neq_string)};
    try runtime.push_string_literal("hello");
    try runtime.push_string_literal("world");
    try run(&bc, &runtime);

    try std.testing.expect(Pointer.asBool(runtime.peek()));
}

test "neq_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.neq_u8)};
    try runtime.push_u8_literal(100);
    try runtime.push_u8_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "lt_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.lt_f32)};
    try runtime.push_f32_literal(42.0);
    try runtime.push_f32_literal(100.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "lt_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.lt_i32)};
    try runtime.push_i32_literal(42);
    try runtime.push_i32_literal(100);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "lt_string" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.lt_string)};
    try runtime.push_string_literal("hello");
    try runtime.push_string_literal("world");
    try run(&bc, &runtime);

    try std.testing.expect(Pointer.asBool(runtime.peek()));
}

test "lt_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.lt_u8)};
    try runtime.push_u8_literal(42);
    try runtime.push_u8_literal(100);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "le_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.le_f32)};
    try runtime.push_f32_literal(42.0);
    try runtime.push_f32_literal(100.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "le_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.le_i32)};
    try runtime.push_i32_literal(42);
    try runtime.push_i32_literal(100);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "le_string" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.le_string)};
    try runtime.push_string_literal("hello");
    try runtime.push_string_literal("world");
    try run(&bc, &runtime);

    try std.testing.expect(Pointer.asBool(runtime.peek()));
}

test "le_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.le_u8)};
    try runtime.push_u8_literal(42);
    try runtime.push_u8_literal(100);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "gt_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.gt_f32)};
    try runtime.push_f32_literal(100.0);
    try runtime.push_f32_literal(42.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "gt_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.gt_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "gt_string" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.gt_string)};
    try runtime.push_string_literal("hello");
    try runtime.push_string_literal("world");
    try run(&bc, &runtime);

    try std.testing.expect(!Pointer.asBool(runtime.peek()));
}

test "gt_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.gt_u8)};
    try runtime.push_u8_literal(100);
    try runtime.push_u8_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "ge_f32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.ge_f32)};
    try runtime.push_f32_literal(100.0);
    try runtime.push_f32_literal(42.0);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "ge_i32" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.ge_i32)};
    try runtime.push_i32_literal(100);
    try runtime.push_i32_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}

test "ge_string" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.ge_string)};
    try runtime.push_string_literal("hello");
    try runtime.push_string_literal("world");
    try run(&bc, &runtime);

    try std.testing.expect(!Pointer.asBool(runtime.peek()));
}

test "ge_u8" {
    var runtime = Runtime.Runtime.init(std.testing.allocator);
    defer runtime.deinit();

    const bc: [1]u8 = [_]u8{@intFromEnum(Op.ge_u8)};
    try runtime.push_u8_literal(100);
    try runtime.push_u8_literal(42);
    try run(&bc, &runtime);

    try std.testing.expectEqual(Pointer.asBool(runtime.pop()), true);
}
