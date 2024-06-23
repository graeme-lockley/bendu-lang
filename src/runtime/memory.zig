const std = @import("std");

const SP = @import("../lib/string_pool.zig");

// The header block has 64 bits of data which is used to store control
// information and then details about the remainder of the block.
//
// 0-3: The GC colour
// 4-7: The type of the block

const Colour = enum(u3) {
    Black = 0, // swap colour
    White = 1, // swap colour
    Grey = 2, // confirmed garbage
};

pub const Type = enum(u3) {
    Bool = 0,
    Char = 1,
    Float = 2,
    Int = 3,
    String = 4,
    Unit = 5,
};

pub const Memory = struct {
    allocator: std.mem.Allocator,
    colour: Colour,
    pages: ?*Page,

    pub fn init(allocator: std.mem.Allocator) Memory {
        return Memory{
            .allocator = allocator,
            .colour = Colour.White,
            .pages = null,
        };
    }

    pub fn deinit(self: *Memory) void {
        var last = self.pages;
        while (last) |ptr| {
            const next = ptr.next;
            ptr.deinit();
            self.allocator.destroy(ptr);
            last = next;
        }
    }

    pub fn allocateFloat(self: *Memory, v: f64) !*FloatValue {
        const item = try self.allocate();

        item.setType(Type.Float);

        const result: *FloatValue = @ptrCast(item);
        result.value = v;

        return result;
    }

    pub fn allocateString(self: *Memory, str: *SP.String) !*StringValue {
        str.incRef();
        errdefer str.decRef();

        return self.allocateStringOwned(str);
    }

    pub fn allocateStringOwned(self: *Memory, str: *SP.String) !*StringValue {
        const item = try self.allocate();

        item.setType(Type.String);

        const result: *StringValue = @ptrCast(item);
        result.value = str;

        return result;
    }

    fn allocate(self: *Memory) !*PageItem {
        var last = &self.pages;

        while (last.* != null) {
            if (last.*.?.item(self.colour)) |potential| {
                return potential;
            }

            last = &last.*.?.next;
        }

        const page = try Page.new(self.allocator);
        last.* = page;

        return page.item(self.colour).?;
    }

    fn setColour(self: *Memory, page: *PageItem) void {
        page.ctrl = page.ctrl & 0b11100 | self.colour;
    }
};

const PAGE_SIZE = 1024;

pub const PageItem = struct {
    ctrl: usize,
    data: usize,

    pub fn isFloat(self: *PageItem) bool {
        return self.typeOf() == Type.Float;
    }

    pub fn isString(self: *PageItem) bool {
        return self.typeOf() == Type.String;
    }

    pub fn isFree(self: *PageItem) bool {
        return self.colourOf() == Colour.Grey;
    }

    pub fn isInUse(self: *PageItem) bool {
        return self.colourOf() != Colour.Grey;
    }

    pub fn setType(self: *PageItem, t: Type) void {
        const v1: usize = @intFromEnum(t);
        const v2: usize = v1 << 2;
        // std.debug.print("- setType: before: {d}: {d} << 2 = {d}\n", .{ self.ctrl, v1, v2 });
        self.ctrl = (self.ctrl & 0b00011) | v2;
        // std.debug.print("- setType: after: {d}\n", .{self.ctrl});
    }

    pub fn typeOf(self: *PageItem) Type {
        return @as(Type, @enumFromInt(self.ctrl >> 2));
    }

    pub fn setColour(self: *PageItem, c: Colour) void {
        self.ctrl = self.ctrl & 0b11100 | @intFromEnum(c);
    }

    pub fn colourOf(self: *PageItem) Colour {
        return @as(Colour, @enumFromInt(self.ctrl & 0b11));
    }

    pub fn markFree(self: *PageItem) void {
        if (self.isInUse()) {
            self.setColour(Colour.Grey);
            switch (self.typeOf()) {
                .String => {
                    const str: *StringValue = @ptrCast(self);
                    str.deinit();
                },
                else => {},
            }
        }
    }

    pub fn markInUse(self: *PageItem, colour: Colour) void {
        self.setColour(colour);
    }
};

const Page = struct {
    next: ?*Page,
    data: [PAGE_SIZE]PageItem,

    pub fn deinit(self: *Page) void {
        self.next = null;

        var lp: usize = 0;
        while (lp < PAGE_SIZE) {
            self.data[lp].markFree();
            lp += 1;
        }
    }

    pub fn new(allocator: std.mem.Allocator) !*Page {
        const page = try allocator.create(Page);

        page.next = null;
        var lp: usize = 0;
        while (lp < PAGE_SIZE) {
            page.data[lp].ctrl = @intFromEnum(Colour.Grey);
            lp += 1;
        }

        return page;
    }

    pub fn item(self: *Page, colour: Colour) ?*PageItem {
        var lp: usize = 0;
        while (lp < PAGE_SIZE) {
            const potential = &self.data[lp];
            if (potential.isFree()) {
                potential.markInUse(colour);
                return potential;
            }
            lp += 1;
        }

        return null;
    }
};

pub const FloatValue = struct {
    ctrl: usize,
    value: f64,
};

pub const StringValue = struct {
    ctrl: usize,
    value: *SP.String,

    pub fn deinit(self: *StringValue) void {
        self.value.decRef();
    }
};

test "String pool memory" {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    const allocator = gpa.allocator();
    defer _ = gpa.deinit();

    var sp = SP.StringPool.init(allocator);
    defer sp.deinit();

    var memory = Memory.init(allocator);
    defer memory.deinit();

    const str = try sp.intern("Hello, world!");
    defer str.decRef();

    const value = try memory.allocateString(str);

    try std.testing.expectEqualStrings(value.value.slice(), str.slice());
}
