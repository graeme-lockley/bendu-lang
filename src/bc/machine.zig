const std = @import("std");

const AST = @import("../ast.zig");
const Op = @import("./op.zig").Op;
const Pointer = @import("../runtime/pointer.zig");
const Runtime = @import("../runtime/runtime.zig").Runtime;
const SP = @import("../lib/string_pool.zig");

pub fn execute(bc: []u8, runtime: *Runtime) !void {
    const writer = std.io.getStdOut().writer();
    var ip: usize = 0;

    while (true) {
        const op = @as(Op, @enumFromInt(bc[ip]));

        // std.io.getStdOut().writer().print("instruction: ip={d}, op={}, stack={}\n", .{ ip, op, stack }) catch {};

        switch (op) {
            .ret => return,
            .discard => {
                runtime.discard();
                ip += 1;
            },
            .duplicate => {
                try runtime.duplicate();
                ip += 1;
            },
            .push_false => {
                try runtime.push_bool(false);
                ip += 1;
            },
            .push_float => {
                try runtime.push_float(@bitCast(readInt(bc, ip + 1)));
                ip += 9;
            },
            .push_int => {
                try runtime.push_int(@intCast(readInt(bc, ip + 1)));
                ip += 9;
            },
            .push_string => {
                const len: usize = @intCast(readInt(bc, ip + 1));
                const s = try runtime.sp.intern(bc[ip + 9 .. ip + 9 + len]);

                try runtime.push_string_owned(s);
                ip += 9 + len;
            },
            .push_true => {
                try runtime.push_bool(true);
                ip += 1;
            },
            .push_unit => {
                try runtime.push_unit();
                ip += 1;
            },
            .push_global => {
                try runtime.push_pointer(runtime.stackItem(@intCast(readInt(bc, ip + 1))));
                ip += 9;
            },

            .add => {
                try runtime.add();
                ip += 1;
            },
            .add_char => {
                try runtime.add_char();
                ip += 1;
            },
            .add_float => {
                try runtime.add_float();
                ip += 1;
            },
            .add_int => {
                try runtime.add_int();
                ip += 1;
            },
            .add_string => {
                try runtime.add_string();
                ip += 1;
            },
            .divide => {
                try runtime.divide();
                ip += 1;
            },
            .divide_char => {
                try runtime.divide_char();
                ip += 1;
            },
            .divide_float => {
                try runtime.divide_float();
                ip += 1;
            },
            .divide_int => {
                try runtime.divide_int();
                ip += 1;
            },
            .minus => {
                try runtime.minus();
                ip += 1;
            },
            .minus_char => {
                try runtime.minus_char();
                ip += 1;
            },
            .minus_float => {
                try runtime.minus_float();
                ip += 1;
            },
            .minus_int => {
                try runtime.minus_int();
                ip += 1;
            },
            .power => {
                try runtime.power();
                ip += 1;
            },
            .power_char => {
                try runtime.power_char();
                ip += 1;
            },
            .power_float => {
                try runtime.power_float();
                ip += 1;
            },
            .power_int => {
                try runtime.power_int();
                ip += 1;
            },
            .times => {
                try runtime.times();
                ip += 1;
            },
            .times_char => {
                try runtime.times_char();
                ip += 1;
            },
            .times_float => {
                try runtime.times_float();
                ip += 1;
            },
            .times_int => {
                try runtime.times_int();
                ip += 1;
            },

            .not => {
                try runtime.not();
                ip += 1;
            },
            .print_int => {
                const v = runtime.pop();
                try writer.print("{d}", .{v});
                ip += 1;
            },
            .print_ln => {
                try writer.print("\n", .{});
                ip += 1;
            },
        }
    }
}

fn readInt(bc: []const u8, ip: usize) i64 {
    // std.io.getStdOut().writer().print("readInt: bc.len={d}, ip={d}\n", .{ bc.len, ip }) catch {};

    const v: i64 = @bitCast(@as(u64, (bc[ip])) |
        (@as(u64, bc[ip + 1]) << 8) |
        (@as(u64, bc[ip + 2]) << 16) |
        (@as(u64, bc[ip + 3]) << 24) |
        (@as(u64, bc[ip + 4]) << 32) |
        (@as(u64, bc[ip + 5]) << 40) |
        (@as(u64, bc[ip + 6]) << 48) |
        (@as(u64, bc[ip + 7]) << 56));

    return v;
}
