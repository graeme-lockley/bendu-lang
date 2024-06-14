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

    pub inline fn push_bool(self: *Runtime, value: bool) !void {
        if (value) {
            try self.stack.append(Pointer.fromInt(1));
        } else {
            try self.stack.append(Pointer.fromInt(0));
        }
    }

    pub inline fn push_int(self: *Runtime, value: i63) !void {
        try self.stack.append(Pointer.fromInt(value));
    }

    pub inline fn push_pointer(self: *Runtime, value: Pointer.Pointer) !void {
        try self.stack.append(value);
    }

    pub inline fn push_unit(self: *Runtime) !void {
        try self.stack.append(Pointer.fromInt(0));
    }

    pub inline fn discard(self: *Runtime) void {
        self.stack.items.len -= 1;
    }

    pub inline fn duplicate(self: *Runtime) !void {
        const last = self.peek().?;
        try self.stack.append(last);
    }

    pub inline fn peek(self: *Runtime) ?Pointer.Pointer {
        return self.stack.getLastOrNull();
    }

    pub inline fn stackPointer(self: *Runtime) usize {
        return self.stack.items.len - 1;
    }

    pub inline fn stackItem(self: *Runtime, index: usize) Pointer.Pointer {
        return self.stack.items[index];
    }
};
