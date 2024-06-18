const std = @import("std");

const SP = @import("../lib/string_pool.zig");
const Typing = @import("../typing.zig");

pub const Subst = struct {
    items: std.AutoHashMap(u64, *Typing.Type),

    pub fn init(allocator: std.mem.Allocator) Subst {
        return Subst{
            .items = std.AutoHashMap(u64, *Typing.Type).init(allocator),
        };
    }

    pub fn deinit(self: *Subst, allocator: std.mem.Allocator) void {
        var iterator = self.items.iterator();
        while (iterator.next()) |item| {
            item.value_ptr.*.decRef(allocator);
        }
        self.items.deinit();
    }

    pub fn clone(self: Subst) !Subst {
        const result = Subst.init(self.items.allocator);

        var iterator = self.items.iterator();

        while (iterator.next()) |item| {
            try result.items.put(item.key_ptr.?, item.value_ptr.?.incRefR());
        }

        return result;
    }

    pub fn get(self: *Subst, key: u64) ?*Typing.Type {
        return self.items.get(key);
    }

    fn contains(self: *Subst, key: u64) bool {
        return self.items.contains(key);
    }

    pub fn put(self: *Subst, key: u64, value: *Typing.Type) !void {
        if (self.items.get(key)) |v| {
            v.decRef(self.items.allocator);
        }

        try self.items.put(key, value.incRefR());
    }

    pub fn compose(self: *Subst, other: *Subst) !void {

        // const compose(s: Subst): Subst {
        //   return new Subst(
        //     Maps.union(Maps.map(s.items, (v) => v.apply(this)), this.items),
        //   );
        // }

        var iterator1 = other.items.iterator();
        while (iterator1.next()) |item| {
            const substValue = try item.value_ptr.*.apply(self);
            defer substValue.decRef(self.items.allocator);

            if (!self.contains(item.key_ptr.*)) {
                try self.put(item.key_ptr.*, substValue);
            }
        }
    }
};

const TestState = struct {
    allocator: std.mem.Allocator,
    sp: SP.StringPool,

    pub fn init(allocator: std.mem.Allocator) !TestState {
        return TestState{
            .allocator = allocator,
            .sp = SP.StringPool.init(allocator),
        };
    }

    pub fn deinit(self: *TestState) void {
        self.sp.deinit();
    }
};

const expectEqual = std.testing.expectEqual;
const expect = std.testing.expect;

test "Subst scenarios" {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer {
        const err = gpa.deinit();
        if (err == std.heap.Check.leak) {
            std.io.getStdErr().writeAll("Failed to deinit allocator\n") catch {};
            std.process.exit(1);
        }
    }

    var state = try TestState.init(gpa.allocator());
    defer state.deinit();

    var boolType = try Typing.TagType.new(state.allocator, try state.sp.intern("Bool"));
    defer boolType.decRef(state.allocator);

    var subst = Subst.init(state.allocator);
    defer subst.deinit(state.allocator);

    try expectEqual(1, boolType.count);
    var r = try boolType.apply(&subst);
    defer r.decRef(state.allocator);
    try expectEqual(2, boolType.count);

    var boundType = try Typing.BoundType.new(state.allocator, 0);
    defer boundType.decRef(state.allocator);
    try expectEqual(2, boolType.count);
    try expectEqual(1, boundType.count);

    try subst.put(0, boolType);
    try expectEqual(3, boolType.count);
    try expectEqual(1, boundType.count);

    var r2 = try boundType.apply(&subst);
    defer r2.decRef(state.allocator);
    try expectEqual(4, boolType.count);
    try expectEqual(1, boundType.count);
    try expectEqual(4, r2.count);

    try expectEqual(boolType, r2);
    try expect(boolType != boundType);
}
