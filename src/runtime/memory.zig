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
                switch (@as(Type, @enumFromInt(item.ctrl >> 2))) {
                    Type.String => {
                        const str: *StringValue = @ptrCast(item);
                        str.deinit();
                    },
                    else => {},
                }
                lp += 1;
            }
            self.allocator.destroy(last.?);
            last = next;
        }
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
            var lp: usize = 0;
            while (lp < PAGE_SIZE) {
                const item = &last.*.?.data[lp];
                if (item.ctrl & 0b11 == @intFromEnum(Colour.Grey)) {
                    item.ctrl = item.ctrl & 0b11100 | @intFromEnum(self.colour);
                    return item;
                }
                lp += 1;
            }

            last = &last.*.?.next;
        }

        const page = try self.allocator.create(Page);
        last.* = page;

        page.next = null;
        var lp: usize = 0;
        while (lp < PAGE_SIZE) {
            page.data[lp].ctrl = @intFromEnum(Colour.Grey);
            lp += 1;
        }
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
};

const Page = struct {
    next: ?*Page,
    data: [PAGE_SIZE]PageItem,
};

pub const StringValue = struct {
    ctrl: usize,
    value: *SP.String,

    pub fn deinit(self: *StringValue) void {
        self.value.decRef();
    }
};

const expectEqual = std.testing.expectEqual;
const expectEqualStrings = std.testing.expectEqualStrings;

test "This thing" {
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
