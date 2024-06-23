const std = @import("std");

const Memory = @import("memory.zig");
const Pointer = @import("pointer.zig");
const SP = @import("../lib/string_pool.zig");

pub const Runtime = struct {
    allocator: std.mem.Allocator,
    memory: Memory.Memory,
    sp: *SP.StringPool,
    stack: std.ArrayList(Pointer.Pointer),

    pub fn init(sp: *SP.StringPool) Runtime {
        const allocator = sp.allocator;

        return Runtime{
            .allocator = allocator,
            .memory = Memory.Memory.init(allocator),
            .sp = sp,
            .stack = std.ArrayList(Pointer.Pointer).init(allocator),
        };
    }

    pub fn deinit(self: *Runtime) void {
        self.stack.deinit();
        self.memory.deinit();
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

    pub inline fn push_float(self: *Runtime, value: f64) !void {
        const pointer: Pointer.Pointer = @intFromPtr(try self.memory.allocateFloat(value));
        try self.stack.append(pointer);
    }

    pub inline fn push_int(self: *Runtime, value: i63) !void {
        try self.stack.append(Pointer.fromInt(value));
    }

    pub inline fn push_pointer(self: *Runtime, value: Pointer.Pointer) !void {
        try self.stack.append(value);
    }

    pub inline fn push_string(self: *Runtime, value: *SP.String) !void {
        const pointer: Pointer.Pointer = @intFromPtr(try self.memory.allocateString(value));
        try self.stack.append(pointer);
    }

    pub inline fn push_string_owned(self: *Runtime, value: *SP.String) !void {
        const pointer: Pointer.Pointer = @intFromPtr(try self.memory.allocateStringOwned(value));
        try self.stack.append(pointer);
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

    // Operators

    pub fn add(self: *Runtime) !void {
        const tos = self.peek().?;

        if (Pointer.isPointer(tos)) {
            const value = Pointer.as(*Memory.PageItem, tos);

            if (value.isFloat()) {
                try self.add_float();
            } else {
                try self.add_string();
            }
        } else {
            try self.add_int();
        }
    }

    pub inline fn add_char(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();

        try self.push_int(@mod(@as(i63, @intCast(Pointer.asInt(a) + Pointer.asInt(b))), 256));
    }

    pub inline fn add_float(self: *Runtime) !void {
        const b = self.pop();
        const valueB = Pointer.as(*Memory.FloatValue, b).value;

        const a = self.pop();
        const valueA = Pointer.as(*Memory.FloatValue, a).value;

        try self.push_float(valueA + valueB);
    }

    pub inline fn add_int(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();

        try self.push_int(@intCast(Pointer.asInt(a) + Pointer.asInt(b)));
    }

    pub inline fn add_string(self: *Runtime) !void {
        const b = self.pop();
        const valueB = Pointer.as(*Memory.StringValue, b).value;

        if (valueB.len() != 0) {
            const a = self.pop();
            const valueA = Pointer.as(*Memory.StringValue, a).value;

            if (valueA.len() == 0) {
                try self.push_string(valueB);
            } else {
                const slices = [_][]const u8{ valueA.slice(), valueB.slice() };
                const result = try std.mem.concat(self.allocator, u8, &slices);

                try self.push_string_owned(try self.sp.internOwned(result));
            }
        }
    }

    pub fn minus(self: *Runtime) !void {
        const tos = self.peek().?;

        if (Pointer.isPointer(tos)) {
            try self.minus_float();
        } else {
            try self.minus_int();
        }
    }

    pub inline fn minus_char(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();

        var v: i64 = Pointer.asInt(a) - Pointer.asInt(b);
        while (v < 0) {
            v += 256;
        }

        try self.push_int(@mod(@as(i63, @intCast(v)), 256));
    }

    pub inline fn minus_float(self: *Runtime) !void {
        const b = self.pop();
        const valueB = Pointer.as(*Memory.FloatValue, b).value;

        const a = self.pop();
        const valueA = Pointer.as(*Memory.FloatValue, a).value;

        try self.push_float(valueA - valueB);
    }

    pub inline fn minus_int(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();

        try self.push_int(@intCast(Pointer.asInt(a) - Pointer.asInt(b)));
    }

    pub fn power(self: *Runtime) !void {
        const tos = self.peek().?;

        if (Pointer.isPointer(tos)) {
            try self.power_float();
        } else {
            try self.power_int();
        }
    }

    pub inline fn power_float(self: *Runtime) !void {
        const b = self.pop();
        const valueB = Pointer.as(*Memory.FloatValue, b).value;

        const a = self.pop();
        const valueA = Pointer.as(*Memory.FloatValue, a).value;

        try self.push_float(std.math.pow(f64, valueA, valueB));
    }

    pub inline fn power_char(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();

        const v: i64 = std.math.pow(i64, Pointer.asInt(a), Pointer.asInt(b));

        try self.push_int(@mod(@as(i63, @intCast(v)), 256));
    }

    pub inline fn power_int(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();

        try self.push_int(@intCast(std.math.pow(i64, Pointer.asInt(a), Pointer.asInt(b))));
    }

    pub fn times(self: *Runtime) !void {
        const tos = self.peek().?;

        if (Pointer.isPointer(tos)) {
            try self.times_float();
        } else {
            try self.times_int();
        }
    }

    pub inline fn times_char(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();

        const v: i64 = Pointer.asInt(a) * Pointer.asInt(b);

        try self.push_int(@mod(@as(i63, @intCast(v)), 256));
    }

    pub inline fn times_float(self: *Runtime) !void {
        const b = self.pop();
        const valueB = Pointer.as(*Memory.FloatValue, b).value;

        const a = self.pop();
        const valueA = Pointer.as(*Memory.FloatValue, a).value;

        try self.push_float(valueA * valueB);
    }

    pub inline fn times_int(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();

        try self.push_int(@intCast(Pointer.asInt(a) * Pointer.asInt(b)));
    }

    pub inline fn not(self: *Runtime) !void {
        const v = self.pop();
        if (Pointer.asInt(v) == 0) {
            try self.stack.append(Pointer.fromInt(1));
        } else {
            try self.stack.append(Pointer.fromInt(0));
        }
    }
};

test "push float and pop float" {
    var sp = SP.StringPool.init(std.testing.allocator);
    var runtime = Runtime.init(&sp);
    defer runtime.deinit();

    try runtime.push_float(1.0);
    try runtime.push_float(2.0);

    const v1 = runtime.pop();
    const v1actual = Pointer.as(*Memory.FloatValue, v1).value;
    try std.testing.expectEqual(2.0, v1actual);

    const v2 = runtime.pop();
    const v2actual = Pointer.as(*Memory.FloatValue, v2).value;
    try std.testing.expectEqual(1.0, v2actual);
}

test "add_float" {
    var sp = SP.StringPool.init(std.testing.allocator);
    var runtime = Runtime.init(&sp);
    defer runtime.deinit();

    try runtime.push_float(1.0);
    try runtime.push_float(2.0);

    try runtime.add_float();

    const result = runtime.pop();
    const value = Pointer.as(*Memory.FloatValue, result).value;

    try std.testing.expectEqual(3.0, value);
}

test "add_string" {
    var sp = SP.StringPool.init(std.testing.allocator);
    defer sp.deinit();

    var runtime = Runtime.init(&sp);
    defer runtime.deinit();

    const s1 = try sp.intern("hello");
    defer s1.decRef();

    try runtime.push_string(s1);
    try runtime.push_string_owned(try sp.intern("world"));

    try runtime.add_string();

    const result = runtime.pop();
    const value = Pointer.as(*Memory.StringValue, result).value;
    try std.testing.expectEqualStrings("helloworld", value.slice());
}

test "minus_char" {
    var sp = SP.StringPool.init(std.testing.allocator);
    defer sp.deinit();

    var runtime = Runtime.init(&sp);
    defer runtime.deinit();

    try runtime.push_int(0);
    try runtime.push_int(1);

    try runtime.minus_char();

    const result = runtime.pop();
    try std.testing.expectEqual(255, Pointer.asChar(result));
}
