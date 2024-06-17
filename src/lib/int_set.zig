const std = @import("std");

pub const IntSet = struct {
    items: std.ArrayList(u64),

    pub fn init(allocator: std.mem.Allocator) IntSet {
        return IntSet{ .items = std.ArrayList(u64).init(allocator) };
    }

    pub fn deinit(self: *IntSet) void {
        self.items.deinit();
    }

    pub fn contains(self: *IntSet, value: u64) bool {
        for (self.items) |item| {
            if (item == value) {
                return true;
            }
        }
        return false;
    }

    pub inline fn insert(self: *IntSet, value: u64) void {
        if (self.contains(value)) {
            return;
        }
        self.items.append(value);
    }

    pub inline fn intersection(self: *IntSet, other: IntSet) IntSet {
        const result = IntSet.init(self.items.allocator);
        for (self.items) |item| {
            if (other.contains(item)) {
                result.insert(item);
            }
        }
        return result;
    }

    pub inline fn combine(self: *IntSet, other: IntSet) IntSet {
        const result = self.clone();
        for (other.items) |item| {
            result.insert(item);
        }
        return result;
    }

    pub inline fn clone(self: *IntSet) IntSet {
        return IntSet{ .items = self.items.clone() };
    }
};
