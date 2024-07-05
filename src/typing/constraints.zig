const std = @import("std");

const Errors = @import("../errors.zig");
const Typing = @import("../typing.zig");

pub const Constraint = struct {
    t1: *Typing.Type,
    t2: *Typing.Type,
    locationRange: Errors.LocationRange,

    pub fn init(t1: *Typing.Type, t2: *Typing.Type, locationRange: Errors.LocationRange) Constraint {
        return Constraint{
            .t1 = t1.incRefR(),
            .t2 = t2.incRefR(),
            .locationRange = locationRange,
        };
    }

    pub fn deinit(self: Constraint, allocator: std.mem.Allocator) void {
        self.t1.decRef(allocator);
        self.t2.decRef(allocator);
    }

    pub fn apply(self: *Constraint, s: *Typing.Subst) !void {
        var t1 = self.t1;
        defer t1.decRef(s.items.allocator);
        var t2 = self.t2;
        defer t2.decRef(s.items.allocator);

        self.t1 = try t1.apply(s);
        self.t2 = try t2.apply(s);
    }

    pub fn debugPrint(self: *Constraint, allocator: std.mem.Allocator) !void {
        const s1 = try self.t1.toString(allocator);
        defer allocator.free(s1);
        const s2 = try self.t2.toString(allocator);
        defer allocator.free(s2);

        std.debug.print("- {s} ~ {s}\n", .{ s1, s2 });
    }
};

pub const Constraints = struct {
    items: std.ArrayList(Constraint),
    dependencies: std.ArrayList(Constraint),

    pub fn init(allocator: std.mem.Allocator) Constraints {
        return Constraints{
            .items = std.ArrayList(Constraint).init(allocator),
            .dependencies = std.ArrayList(Constraint).init(allocator),
        };
    }

    pub fn deinit(self: *Constraints, allocator: std.mem.Allocator) void {
        self.deinitState(allocator);

        self.items.deinit();
        self.dependencies.deinit();
    }

    pub fn reset(self: *Constraints, allocator: std.mem.Allocator) void {
        self.deinitState(allocator);

        // The following is the most conservative implememation. It could
        // probably be simpler and more efficient.
        self.items.deinit();
        self.dependencies.deinit();

        self.items = std.ArrayList(Constraint).init(allocator);
        self.dependencies = std.ArrayList(Constraint).init(allocator);
    }

    fn deinitState(self: *Constraints, allocator: std.mem.Allocator) void {
        for (self.items.items) |item| {
            item.deinit(allocator);
        }
        for (self.dependencies.items) |dependency| {
            dependency.deinit(allocator);
        }
    }

    pub fn add(self: *Constraints, t1: *Typing.Type, t2: *Typing.Type, locationRange: Errors.LocationRange) !void {
        const constraint = Constraint.init(t1, t2, locationRange);
        try self.items.append(constraint);
    }

    pub fn addDependency(self: *Constraints, t1: *Typing.Type, t2: *Typing.Type, locationRange: Errors.LocationRange) !void {
        const constraint = Constraint.init(t1, t2, locationRange);
        try self.dependencies.append(constraint);
    }

    pub inline fn len(self: Constraints) usize {
        return self.items.items.len;
    }

    pub inline fn take(self: *Constraints) Constraint {
        return self.items.swapRemove(0);
    }

    pub fn apply(self: *Constraints, s: *Typing.Subst) !void {
        try applyArrayList(&self.items, s);
        try applyArrayList(&self.dependencies, s);
    }

    pub fn debugPrint(self: *Constraints) !void {
        std.debug.print("--- Constraints ------------\n", .{});
        for (self.items.items) |*err| {
            try err.debugPrint(self.items.allocator);
        }
        std.debug.print("----------------------------\n", .{});
    }
};

fn applyArrayList(items: *std.ArrayList(Constraint), s: *Typing.Subst) !void {
    for (items.items) |*item| {
        try item.apply(s);
    }
}
