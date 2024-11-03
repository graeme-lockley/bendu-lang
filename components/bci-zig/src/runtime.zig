const std = @import("std");

const Pointer = @import("pointer.zig");

const stdout = std.io.getStdOut().writer();

pub const Runtime = struct {
    allocator: std.mem.Allocator,
    stack: std.ArrayList(Pointer.Pointer),

    pub fn init(allocator: std.mem.Allocator) Runtime {
        return Runtime{
            .allocator = allocator,
            .stack = std.ArrayList(Pointer.Pointer).init(allocator),
        };
    }

    pub fn deinit(self: *Runtime) void {
        self.stack.deinit();
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

    pub inline fn push_i32_literal(self: *Runtime, value: i32) !void {
        try self.stack.append(Pointer.fromInt(value));
    }

    pub inline fn push_i32_stack(self: *Runtime, index: i32) !void {
        const value = self.stack.items[@intCast(index)];
        try self.stack.append(value);
    }

    pub inline fn not_bool(self: *Runtime) !void {
        const value = Pointer.asBool(self.pop());
        try self.stack.append(Pointer.fromBool(!value));
    }

    pub inline fn add_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a + b));
    }

    pub inline fn sub_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a - b));
    }

    pub inline fn mul_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a * b));
    }

    pub inline fn div_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(@divTrunc(a, b)));
    }

    pub inline fn mod_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(@mod(a, b)));
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
    pub inline fn eq_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }
    pub inline fn neq_bool(self: *Runtime) !void {
        const b = Pointer.asBool(self.pop());
        const a = Pointer.asBool(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn lt_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a < b));
    }
    pub inline fn le_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a <= b));
    }
    pub inline fn gt_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a > b));
    }
    pub inline fn ge_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
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

    pub inline fn print_i32(self: *Runtime) !void {
        const value = Pointer.asInt(self.pop());

        try stdout.print("{d}", .{value});
    }
};
