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

    pub inline fn push_i32_literal(self: *Runtime, value: i32) !void {
        try self.stack.append(Pointer.fromInt(value));
    }

    pub inline fn push_i32_stack(self: *Runtime, index: i32) !void {
        const value = self.stack.items[@intCast(index)];
        try self.stack.append(value);
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

    pub inline fn print_i32(self: *Runtime) !void {
        const value = Pointer.asInt(self.pop());

        try stdout.print("{d}", .{value});
    }

    pub inline fn println(self: *Runtime) !void {
        _ = self;

        try stdout.print("\n", .{});
    }
};
