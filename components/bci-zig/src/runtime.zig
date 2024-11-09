const std = @import("std");

const Pointer = @import("pointer.zig");
const SP = @import("string_pool.zig");

const stdout = std.io.getStdOut().writer();

pub const Runtime = struct {
    allocator: std.mem.Allocator,
    sp: SP.StringPool,
    stack: std.ArrayList(Pointer.Pointer),

    pub fn init(allocator: std.mem.Allocator) Runtime {
        return Runtime{
            .allocator = allocator,
            .sp = SP.StringPool.init(allocator),
            .stack = std.ArrayList(Pointer.Pointer).init(allocator),
        };
    }

    pub fn deinit(self: *Runtime) void {
        while (self.stack.items.len > 0) {
            self.discard();
        }
        self.stack.deinit();
        self.sp.deinit();
    }

    pub inline fn pop(self: *Runtime) Pointer.Pointer {
        return self.stack.pop();
    }
    pub inline fn peek(self: *Runtime) Pointer.Pointer {
        return self.stack.items[self.stack.items.len - 1];
    }

    pub inline fn push_bool_true(self: *Runtime) !void {
        try self.stack.append(Pointer.fromInt(1));
    }

    pub inline fn push_bool_false(self: *Runtime) !void {
        try self.stack.append(Pointer.fromInt(0));
    }

    pub inline fn push_f32_literal(self: *Runtime, value: f32) !void {
        try self.stack.append(Pointer.fromFloat(value));
    }

    pub inline fn push_i32_literal(self: *Runtime, value: i32) !void {
        try self.stack.append(Pointer.fromInt(value));
    }

    pub inline fn push_u8_literal(self: *Runtime, value: u8) !void {
        try self.stack.append(Pointer.fromChar(value));
    }

    pub inline fn push_string_literal(self: *Runtime, value: []const u8) !void {
        try self.stack.append(Pointer.fromString(try self.sp.intern(value)));
    }

    pub inline fn push_stack(self: *Runtime, index: i32) !void {
        const value = self.stack.items[@intCast(index)];

        if (Pointer.isString(value)) {
            Pointer.asString(value).incRef();
        }

        try self.stack.append(value);
    }

    pub inline fn discard(self: *Runtime) void {
        const value = self.stack.pop();

        if (Pointer.isString(value)) {
            Pointer.asString(value).decRef();
        }
    }

    pub inline fn not_bool(self: *Runtime) !void {
        const value = Pointer.asBool(self.pop());
        try self.stack.append(Pointer.fromBool(!value));
    }

    pub inline fn add_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromFloat(a + b));
    }
    pub inline fn add_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a + b));
    }
    pub inline fn add_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromChar(a + b));
    }

    pub inline fn sub_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromFloat(a - b));
    }
    pub inline fn sub_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a - b));
    }
    pub inline fn sub_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromChar(a - b));
    }

    pub inline fn mul_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromFloat(a * b));
    }
    pub inline fn mul_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a * b));
    }
    pub inline fn mul_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromChar(a * b));
    }

    pub inline fn div_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());

        if (b == 0.0) {
            try stdout.print("Error: Attempt to divide by zero\n", .{});
            std.posix.exit(1);
        }

        try self.stack.append(Pointer.fromFloat(a / b));
    }
    pub inline fn div_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());

        if (b == 0) {
            try stdout.print("Error: Attempt to divide by zero\n", .{});
            std.posix.exit(1);
        }

        try self.stack.append(Pointer.fromInt(@divTrunc(a, b)));
    }
    pub inline fn div_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());

        if (b == 0) {
            try stdout.print("Error: Attempt to divide by zero\n", .{});
            std.posix.exit(1);
        }

        try self.stack.append(Pointer.fromChar(a / b));
    }

    pub inline fn mod_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());

        if (b == 0) {
            try stdout.print("Error: Attempt to divide by zero\n", .{});
            std.posix.exit(1);
        }

        try self.stack.append(Pointer.fromInt(@mod(a, b)));
    }

    pub inline fn pow_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromFloat(std.math.pow(f32, a, b)));
    }
    pub inline fn pow_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(std.math.pow(i32, a, b)));
    }

    pub inline fn eq_bool(self: *Runtime) !void {
        const b = Pointer.asBool(self.pop());
        const a = Pointer.asBool(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }
    pub inline fn eq_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }
    pub inline fn eq_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }
    pub inline fn eq_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }

    pub inline fn neq_bool(self: *Runtime) !void {
        const b = Pointer.asBool(self.pop());
        const a = Pointer.asBool(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }

    pub inline fn lt_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a < b));
    }
    pub inline fn lt_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a < b));
    }
    pub inline fn lt_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a < b));
    }

    pub inline fn le_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a <= b));
    }
    pub inline fn le_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a <= b));
    }
    pub inline fn le_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a <= b));
    }

    pub inline fn gt_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a > b));
    }
    pub inline fn gt_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a > b));
    }
    pub inline fn gt_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a > b));
    }

    pub inline fn ge_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a >= b));
    }
    pub inline fn ge_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a >= b));
    }
    pub inline fn ge_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a >= b));
    }

    pub inline fn println(self: *Runtime) !void {
        _ = self;

        try stdout.print("\n", .{});
    }

    pub inline fn print_bool(self: *Runtime) !void {
        const value = Pointer.asBool(self.pop());

        try stdout.print("{s}", .{if (value) "True" else "False"});
    }

    pub inline fn print_f32(self: *Runtime) !void {
        const value = Pointer.asFloat(self.pop());

        try stdout.print("{d}", .{value});
    }

    pub inline fn print_i32(self: *Runtime) !void {
        const value = Pointer.asInt(self.pop());

        try stdout.print("{d}", .{value});
    }

    pub inline fn print_u8(self: *Runtime) !void {
        const value = Pointer.asChar(self.pop());

        try stdout.print("{c}", .{value});
    }

    pub inline fn print_string(self: *Runtime) !void {
        const value = Pointer.asString(self.pop());
        defer value.decRef();

        try stdout.print("{s}", .{value.slice()});
    }
};
