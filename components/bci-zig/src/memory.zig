const std = @import("std");

const Pointer = @import("pointer.zig");

pub const Colour = enum(u2) {
    Black = 0,
    White = 1,
};

pub const Value = struct {
    colour: Colour,
    next: ?*Value,

    v: ValueValue,

    pub fn deinit(self: *Value, allocator: std.mem.Allocator) void {
        switch (self.v) {
            .ArrayKind => self.v.ArrayKind.deinit(),
            .ClosureKind => self.v.ClosureKind.deinit(),
            .FrameKind => self.v.FrameKind.deinit(),
            .TupleKind => self.v.TupleKind.deinit(allocator),
        }
    }
};

pub const ValueKind = enum {
    ArrayKind,
    ClosureKind,
    FrameKind,
    TupleKind,

    pub fn toString(self: ValueKind) []const u8 {
        return switch (self) {
            ValueKind.ArrayKind => "Array",
            ValueKind.ClosureKind => "Closure",
            ValueKind.FrameKind => "Frame",
            ValueKind.TupleKind => "Tuple",
        };
    }
};

pub const ValueValue = union(ValueKind) {
    ArrayKind: ArrayValue,
    ClosureKind: ClosureValue,
    FrameKind: FrameValue,
    TupleKind: TupleValue,
};

pub const ArrayValue = struct {
    values: std.ArrayList(Pointer.Pointer),

    pub fn init(allocator: std.mem.Allocator, size: usize) !ArrayValue {
        const result = ArrayValue{
            .values = try std.ArrayList(Pointer.Pointer).initCapacity(allocator, size),
        };

        return result;
    }

    pub fn deinit(self: *ArrayValue) void {
        for (self.values.items) |v| {
            if (Pointer.isString(v)) {
                Pointer.asString(v).decRef();
            }
        }

        self.values.deinit();
    }

    pub fn appendItem(self: *ArrayValue, value: Pointer.Pointer) !void {
        try self.values.append(value);
    }

    pub fn prependItem(self: *ArrayValue, value: Pointer.Pointer) !void {
        try self.values.insert(0, value);
    }

    pub fn appendSlice(self: *ArrayValue, values: []const Pointer.Pointer) !void {
        try self.values.appendSlice(values);
    }

    pub fn replaceSlice(self: *ArrayValue, values: []Pointer.Pointer) !void {
        self.values.clearAndFree();
        try self.values.appendSlice(values);
    }

    pub fn replaceRange(self: *ArrayValue, start: usize, end: usize, values: []Pointer.Pointer) !void {
        try self.values.replaceRange(start, end - start, values);
    }

    pub fn removeRange(self: *ArrayValue, start: usize, end: usize) !void {
        const values = &[_]Pointer.Pointer{};

        try self.values.replaceRange(start, end - start, values);
    }

    pub fn len(self: *const ArrayValue) usize {
        return self.values.items.len;
    }

    pub fn items(self: *const ArrayValue) []Pointer.Pointer {
        return self.values.items;
    }

    pub fn at(self: *const ArrayValue, i: usize) Pointer.Pointer {
        return self.values.items[i];
    }

    pub fn set(self: *const ArrayValue, i: usize, v: Pointer.Pointer) void {
        self.values.items[i] = v;
    }
};

pub const ClosureValue = struct {
    function: usize,
    frame: *Value,

    pub fn init(function: usize, frame: *Value) ClosureValue {
        return ClosureValue{
            .function = function,
            .frame = frame,
        };
    }

    pub fn deinit(self: *ClosureValue) void {
        _ = self;
    }
};

pub const FrameValue = struct {
    enclosing: ?*Value,
    values: std.ArrayList(Pointer.Pointer),

    pub fn init(allocator: std.mem.Allocator, enclosing: ?*Value) !FrameValue {
        const result = FrameValue{
            .enclosing = enclosing,
            .values = std.ArrayList(Pointer.Pointer).init(allocator),
        };

        return result;
    }

    pub fn deinit(self: *FrameValue) void {
        for (self.values.items) |v| {
            if (Pointer.isString(v)) {
                Pointer.asString(v).decRef();
            }
        }
        self.values.deinit();
    }

    pub inline fn skip(self: *Value, depth: usize) ?*Value {
        var frame = self;
        var i = depth;

        while (i > 0) {
            frame = frame.v.FrameKind.enclosing.?;
            i -= 1;
        }

        return frame;
    }

    pub fn set(self: *Value, depth: usize, offset: usize, value: Pointer.Pointer) !void {
        var frame = skip(self, depth).?;

        while (frame.v.FrameKind.values.items.len <= offset) {
            try frame.v.FrameKind.values.append(Pointer.fromInt(0));
        }

        // The string reference count is not incremented as this is only called from the interpreter
        // and the interpreter pop the value off of the stack without decrementing so the reference
        // count is maintained.
        //
        // if (Pointer.isString(value)) {
        //     Pointer.asString(value).incRef();
        // }

        frame.v.FrameKind.values.items[offset] = value;
    }

    pub fn get(self: *Value, depth: usize, offset: usize) Pointer.Pointer {
        const frame = skip(self, depth).?;

        return frame.v.FrameKind.values.items[offset];
    }
};

pub const TupleValue = struct {
    values: []Pointer.Pointer,

    pub fn init(allocator: std.mem.Allocator, size: usize) !TupleValue {
        const result = TupleValue{
            .values = try allocator.alloc(Pointer.Pointer, size),
        };

        return result;
    }

    pub fn deinit(self: *TupleValue, allocator: std.mem.Allocator) void {
        for (self.values) |v| {
            if (Pointer.isString(v)) {
                Pointer.asString(v).decRef();
            }
        }

        allocator.free(self.values);
    }

    pub inline fn len(self: *const TupleValue) usize {
        return self.values.len;
    }

    pub inline fn at(self: *const TupleValue, i: usize) Pointer.Pointer {
        return self.values[i];
    }
};

pub inline fn incRef(v: Pointer.Pointer) Pointer.Pointer {
    if (Pointer.isString(v)) {
        Pointer.asString(v).incRef();
    }

    return v;
}

pub inline fn decRef(v: Pointer.Pointer) void {
    if (Pointer.isString(v)) {
        Pointer.asString(v).decRef();
    }
}
