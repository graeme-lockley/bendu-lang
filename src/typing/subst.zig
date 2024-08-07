const std = @import("std");

const SP = @import("../lib/string_pool.zig");
const Typing = @import("../typing.zig");

pub const Subst = struct {
    allocator: std.mem.Allocator,
    items: std.AutoHashMap(u64, *Typing.Type),

    pub fn init(allocator: std.mem.Allocator) Subst {
        return Subst{
            .allocator = allocator,
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
        var result = self.items.get(key);

        while (result != null and result.?.kind == .Bound and self.contains(result.?.kind.Bound.value)) {
            const bound = result.?.kind.Bound.value;
            result = self.items.get(bound);
        }

        return result;
    }

    fn contains(self: *Subst, key: u64) bool {
        return self.items.contains(key);
    }

    fn put(self: *Subst, key: u64, value: *Typing.Type) !void {
        if (self.items.get(key)) |v| {
            v.decRef(self.items.allocator);
        }

        try self.items.put(key, value.incRefR());
    }

    pub fn add(self: *Subst, key: u64, value: *Typing.Type) !void {
        var s = Subst.init(self.items.allocator);
        defer s.deinit(self.items.allocator);

        try s.put(key, value);

        try self.applySubst(&s);

        if (self.items.get(key)) |v| {
            v.decRef(self.items.allocator);
        }

        try self.items.put(key, value.incRefR());
    }

    pub fn debugPrint(self: *Subst) !void {
        std.debug.print("--- Subst ------------\n", .{});
        var iterator = self.items.iterator();
        while (iterator.next()) |item| {
            const key = item.key_ptr.*;
            const value = item.value_ptr.*;
            const valueStr = try value.toString(self.items.allocator);
            defer self.items.allocator.free(valueStr);
            std.debug.print("'{d} -> {s}\n", .{ key, valueStr });
        }
        std.debug.print("----------------------\n", .{});
    }

    fn applySubst(self: *Subst, other: *Subst) !void {
        var iterator1 = self.items.iterator();
        while (iterator1.next()) |item| {
            const substValue = try item.value_ptr.*.apply(other);
            defer substValue.decRef(self.items.allocator);

            try self.put(item.key_ptr.*, substValue);
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
