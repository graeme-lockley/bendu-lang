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

    pub fn deinit(self: *Value) void {
        switch (self.v) {
            .FrameKind => self.v.FrameKind.deinit(),
            .SequenceKind => self.v.SequenceKind.deinit(),
        }
    }
};

pub const ValueKind = enum {
    FrameKind,
    SequenceKind,

    pub fn toString(self: ValueKind) []const u8 {
        return switch (self) {
            ValueKind.FrameKind => "Frame",
            ValueKind.SequenceKind => "Sequence",
        };
    }
};

pub const ValueValue = union(ValueKind) {
    FrameKind: FrameValue,
    SequenceKind: SequenceValue,
};

pub const FrameValue = struct {
    enclosing: ?*FrameValue,
    values: std.ArrayList(Pointer.Pointer),

    pub fn init(allocator: std.mem.Allocator, enclosing: ?*FrameValue) !FrameValue {
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

    pub inline fn skip(self: *FrameValue, depth: usize) ?*FrameValue {
        var frame = self;
        var i = depth;

        while (i > 0) {
            frame = frame.enclosing.?;
            i -= 1;
        }

        return frame;
    }

    pub fn set(self: *FrameValue, depth: usize, offset: usize, value: Pointer.Pointer) !void {
        var frame = self.skip(depth).?;

        while (frame.values.items.len <= offset) {
            try frame.values.append(Pointer.fromInt(0));
        }

        // The string reference count is not incremented as this is only called from the interpreter
        // and the interpreter pop the value off of the stack without decrementing so the reference
        // count is maintained.
        //
        // if (Pointer.isString(value)) {
        //     Pointer.asString(value).incRef();
        // }

        frame.values.items[offset] = value;
    }

    pub fn get(self: *FrameValue, depth: usize, offset: usize) Pointer.Pointer {
        const frame = self.skip(depth).?;

        return frame.values.items[offset];
    }
};

pub const SequenceValue = struct {
    values: std.ArrayList(Pointer.Pointer),

    pub fn init(allocator: std.mem.Allocator) !SequenceValue {
        const result = SequenceValue{
            .values = std.ArrayList(Pointer.Pointer).init(allocator),
        };

        return result;
    }

    pub fn deinit(self: *SequenceValue) void {
        self.values.deinit();
    }

    pub fn appendItem(self: *SequenceValue, value: Pointer.Pointer) !void {
        try self.values.append(value);
    }

    pub fn prependItem(self: *SequenceValue, value: Pointer.Pointer) !void {
        try self.values.insert(0, value);
    }

    pub fn appendSlice(self: *SequenceValue, values: []const Pointer.Pointer) !void {
        try self.values.appendSlice(values);
    }

    pub fn replaceSlice(self: *SequenceValue, values: []Pointer.Pointer) !void {
        self.values.clearAndFree();
        try self.values.appendSlice(values);
    }

    pub fn replaceRange(self: *SequenceValue, start: usize, end: usize, values: []Pointer.Pointer) !void {
        try self.values.replaceRange(start, end - start, values);
    }

    pub fn removeRange(self: *SequenceValue, start: usize, end: usize) !void {
        const values = &[_]Pointer.Pointer{};

        try self.values.replaceRange(start, end - start, values);
    }

    pub fn len(self: *const SequenceValue) usize {
        return self.values.items.len;
    }

    pub fn items(self: *const SequenceValue) []Pointer.Pointer {
        return self.values.items;
    }

    pub fn at(self: *const SequenceValue, i: usize) Pointer.Pointer {
        return self.values.items[i];
    }

    pub fn set(self: *const SequenceValue, i: usize, v: Pointer.Pointer) void {
        self.values.items[i] = v;
    }
};
