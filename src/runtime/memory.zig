const std = @import("std");

const SP = @import("../string_pool.zig");

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
            .colour = Colour.Black,
            .pages = null,
        };
    }

    pub fn deinit(self: *Memory) void {
        var last = self.pages;
        while (last != null) {
            const next = last.?.next;

            var lp: usize = 0;
            while (lp < PAGE_SIZE) {
                const item = &last.?.data[lp];

                if (!item.isFree()) {
                    switch (@as(Type, @enumFromInt(item.ctrl >> 2))) {
                        Type.String => {
                            const str: *StringValue = @ptrCast(item);
                            str.deinit();
                        },
                        else => {},
                    }
                    item.markFree();
                }
                lp += 1;
            }
            self.allocator.destroy(last.?);
            last = next;
        }
    }

    pub fn allocateFloat(self: *Memory, v: f64) !*FloatValue {
        const item = try self.allocate();

        item.ctrl = item.ctrl & 0b00011 | (@intFromEnum(Type.Float) << 2);

        const result: *FloatValue = @ptrCast(item);
        result.value = v;

        return result;
    }

    pub fn allocateString(self: *Memory, str: *SP.String) !*StringValue {
        const item = try self.allocate();

        item.ctrl = item.ctrl & 0b00011 | (@intFromEnum(Type.String) << 2);

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

        return &page.data[0];
    }

    fn setColour(self: *Memory, page: *PageItem) void {
        page.ctrl = page.ctrl & 0b11100 | self.colour;
    }
};

const PAGE_SIZE = 1024;

const PageItem = struct {
    ctrl: usize,
    data: usize,

    pub fn isFree(self: *PageItem) bool {
        return self.ctrl & 0b11 == @intFromEnum(Colour.Grey);
    }

    pub fn markFree(self: *PageItem) void {
        self.ctrl = self.ctrl & 0b11100 | @intFromEnum(Colour.Grey);
    }
};

const Page = struct {
    next: ?*Page,
    data: [PAGE_SIZE]PageItem,

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
            if (potential.ctrl & 0b11 == @intFromEnum(Colour.Grey)) {
                potential.ctrl = potential.ctrl & 0b11100 | @intFromEnum(colour);
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

test "This thing" {
    const expectEqualStrings = std.testing.expectEqualStrings;

    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    const allocator = gpa.allocator();
    defer {
        const err = gpa.deinit();
        if (err == std.heap.Check.leak) {
            std.io.getStdErr().writeAll("Failed to deinit allocator\n") catch {};
            std.process.exit(1);
        }
    }
    var sp = SP.StringPool.init(allocator);
    defer sp.deinit();

    var memory = Memory.init(allocator);
    defer memory.deinit();

    const str = try sp.intern("Hello, world!");
    defer str.decRef();
    const value = try memory.allocateString(str);

    try expectEqualStrings(value.value.slice(), str.slice());
}
