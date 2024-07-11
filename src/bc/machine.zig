const std = @import("std");

const AST = @import("../ast.zig");
const Op = @import("./op.zig").Op;
const Pointer = @import("../runtime/pointer.zig");
const Runtime = @import("../runtime/runtime.zig").Runtime;
const SP = @import("../lib/string_pool.zig");

pub fn execute(bc: []u8, runtime: *Runtime) !void {
    const writer = std.io.getStdOut().writer();
    var ip: usize = 0;
    var lbp: usize = 0;

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

            .call_local => {
                const newLBP = runtime.stack.items.len;

                try runtime.push_unit();
                try runtime.push_int(@as(i63, @intCast(ip)) + 5);
                try runtime.push_int(@intCast(lbp));

                lbp = newLBP;

                ip = @intCast(readInt(bc, ip + 1));
            },
            .ret_local => {
                const n: usize = @intCast(readInt(bc, ip + 1));

                const oldLBP = lbp;
                const r = runtime.stack.items[lbp];
                ip = @intCast(runtime.stack.items[lbp + 1]);
                lbp = @intCast(runtime.stack.items[lbp + 2]);
                runtime.stack.items.len = oldLBP - n;

                try runtime.push_pointer(r);
            },

            .jmp => {
                ip = @intCast(readInt(bc, ip + 1));
            },
            .jmp_false => {
                const v = Pointer.asBool(runtime.pop());
                if (!v) {
                    ip = @intCast(readInt(bc, ip + 1));
                } else {
                    ip += 9;
                }
            },
            .jmp_tos_false => {
                const v = Pointer.asBool(runtime.peek().?);
                if (!v) {
                    ip = @intCast(readInt(bc, ip + 1));
                } else {
                    ip += 9;
                }
            },
            .jmp_tos_true => {
                const v = Pointer.asBool(runtime.peek().?);
                if (v) {
                    ip = @intCast(readInt(bc, ip + 1));
                } else {
                    ip += 9;
                }
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
            .equals => {
                try runtime.equals();
                ip += 1;
            },
            .equals_bool => {
                try runtime.equals_bool();
                ip += 1;
            },
            .equals_char => {
                try runtime.equals_char();
                ip += 1;
            },
            .equals_float => {
                try runtime.equals_float();
                ip += 1;
            },
            .equals_int => {
                try runtime.equals_int();
                ip += 1;
            },
            .equals_string => {
                try runtime.equals_string();
                ip += 1;
            },
            .greaterequals => {
                try runtime.greaterequals();
                ip += 1;
            },
            .greaterequals_bool => {
                try runtime.greaterequals_bool();
                ip += 1;
            },
            .greaterequals_char => {
                try runtime.greaterequals_char();
                ip += 1;
            },
            .greaterequals_float => {
                try runtime.greaterequals_float();
                ip += 1;
            },
            .greaterequals_int => {
                try runtime.greaterequals_int();
                ip += 1;
            },
            .greaterequals_string => {
                try runtime.greaterequals_string();
                ip += 1;
            },
            .greaterthan => {
                try runtime.greaterthan();
                ip += 1;
            },
            .greaterthan_bool => {
                try runtime.greaterthan_bool();
                ip += 1;
            },
            .greaterthan_char => {
                try runtime.greaterthan_char();
                ip += 1;
            },
            .greaterthan_float => {
                try runtime.greaterthan_float();
                ip += 1;
            },
            .greaterthan_int => {
                try runtime.greaterthan_int();
                ip += 1;
            },
            .greaterthan_string => {
                try runtime.greaterthan_string();
                ip += 1;
            },
            .lessequals => {
                try runtime.lessequals();
                ip += 1;
            },
            .lessequals_bool => {
                try runtime.lessequals_bool();
                ip += 1;
            },
            .lessequals_char => {
                try runtime.lessequals_char();
                ip += 1;
            },
            .lessequals_float => {
                try runtime.lessequals_float();
                ip += 1;
            },
            .lessequals_int => {
                try runtime.lessequals_int();
                ip += 1;
            },
            .lessequals_string => {
                try runtime.lessequals_string();
                ip += 1;
            },
            .lessthan => {
                try runtime.lessthan();
                ip += 1;
            },
            .lessthan_bool => {
                try runtime.lessthan_bool();
                ip += 1;
            },
            .lessthan_char => {
                try runtime.lessthan_char();
                ip += 1;
            },
            .lessthan_float => {
                try runtime.lessthan_float();
                ip += 1;
            },
            .lessthan_int => {
                try runtime.lessthan_int();
                ip += 1;
            },
            .lessthan_string => {
                try runtime.lessthan_string();
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
            .modulo_int => {
                try runtime.modulo_int();
                ip += 1;
            },
            .notequals => {
                try runtime.notequals();
                ip += 1;
            },
            .notequals_bool => {
                try runtime.notequals_bool();
                ip += 1;
            },
            .notequals_char => {
                try runtime.notequals_char();
                ip += 1;
            },
            .notequals_float => {
                try runtime.notequals_float();
                ip += 1;
            },
            .notequals_int => {
                try runtime.notequals_int();
                ip += 1;
            },
            .notequals_string => {
                try runtime.notequals_string();
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
