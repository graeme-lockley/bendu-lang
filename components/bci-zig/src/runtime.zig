const std = @import("std");

const Pointer = @import("pointer.zig");

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
};
