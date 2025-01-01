const std = @import("std");

const Pointer = @import("pointer.zig");
const SP = @import("string_pool.zig");

pub const Colour = enum(u2) {
    Black = 0,
    White = 1,
};

pub const Value = struct {
    colour: Colour,
    next: ?*Value,
    v: ValueValue,

    /// Create a new Value with the given type
    pub fn init(allocator: std.mem.Allocator, kind: ValueKind) !*Value {
        const self = try allocator.create(Value);
        self.* = .{
            .colour = .Black,
            .next = null,
            .v = switch (kind) {
                .ArrayKind => .{ .ArrayKind = try ArrayValue.init(allocator, 0) },
                .ClosureKind => .{ .ClosureKind = undefined },
                .CustomKind => .{ .CustomKind = undefined },
                .FrameKind => .{ .FrameKind = try FrameValue.init(allocator, null) },
                .TupleKind => .{ .TupleKind = try TupleValue.init(allocator, 0) },
            },
        };
        return self;
    }

    /// Safe accessor for value kind
    pub fn getKind(self: *const Value) ValueKind {
        return std.meta.activeTag(self.v);
    }

    pub fn deinit(self: *Value, allocator: std.mem.Allocator) void {
        switch (self.v) {
            .ArrayKind => self.v.ArrayKind.deinit(),
            .ClosureKind => self.v.ClosureKind.deinit(),
            .CustomKind => self.v.CustomKind.deinit(allocator),
            .FrameKind => self.v.FrameKind.deinit(),
            .TupleKind => self.v.TupleKind.deinit(allocator),
        }
    }
};

pub const ValueKind = enum {
    ArrayKind,
    ClosureKind,
    CustomKind,
    FrameKind,
    TupleKind,

    pub fn toString(self: ValueKind) []const u8 {
        return switch (self) {
            ValueKind.ArrayKind => "Array",
            ValueKind.ClosureKind => "Closure",
            ValueKind.CustomKind => "Custom",
            ValueKind.FrameKind => "Frame",
            ValueKind.TupleKind => "Tuple",
        };
    }
};

pub const ValueValue = union(ValueKind) {
    ArrayKind: ArrayValue,
    ClosureKind: ClosureValue,
    CustomKind: CustomValue,
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
            decRef(v);
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

    pub fn at(self: *const ArrayValue, index: usize) Pointer.Pointer {
        std.debug.assert(index < self.values.items.len);

        return self.values.items[index];
    }

    pub fn set(self: *ArrayValue, index: usize, value: Pointer.Pointer) void {
        std.debug.assert(index < self.values.items.len);

        const old_value = self.values.items[index];
        decRef(old_value);
        self.values.items[index] = value;
    }

    pub fn ensureCapacity(self: *ArrayValue, capacity: usize) !void {
        try self.values.ensureTotalCapacity(capacity);
    }
};

pub const ClosureValue = struct {
    packageID: usize,
    function: usize,
    frame: *Value,

    pub fn init(packageID: usize, function: usize, frame: *Value) ClosureValue {
        return ClosureValue{
            .packageID = packageID,
            .function = function,
            .frame = frame,
        };
    }

    pub fn deinit(self: *ClosureValue) void {
        _ = self;
    }
};

pub const CustomValue = struct {
    name: *SP.String,
    id: usize,
    values: []Pointer.Pointer,

    pub fn init(allocator: std.mem.Allocator, name: *SP.String, id: usize, size: usize) !CustomValue {
        const result = CustomValue{
            .name = name,
            .id = id,
            .values = try allocator.alloc(Pointer.Pointer, size),
        };

        return result;
    }

    pub fn deinit(self: *CustomValue, allocator: std.mem.Allocator) void {
        self.name.decRef();
        for (self.values) |v| {
            decRef(v);
        }

        allocator.free(self.values);
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
            decRef(v);
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

        if (offset >= frame.v.FrameKind.values.items.len) {
            try frame.v.FrameKind.values.resize(offset + 1);
            for (frame.v.FrameKind.values.items[offset..]) |*item| {
                item.* = Pointer.fromInt(0);
            }
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
            decRef(v);
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

pub inline fn incRef(v: Pointer.Pointer) void {
    _ = incRefR(v);
}

pub inline fn incRefR(v: Pointer.Pointer) Pointer.Pointer {
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
