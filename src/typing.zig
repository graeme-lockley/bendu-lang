const std = @import("std");

const Errors = @import("errors.zig");
const SP = @import("lib/string_pool.zig");

pub const Pump = @import("typing/pump.zig").Pump;
pub const Subst = @import("typing/subst.zig").Subst;

pub const SchemeBinding = struct {
    name: *SP.String,
    type: ?*Type,

    pub fn deinit(self: SchemeBinding, allocator: std.mem.Allocator) void {
        self.name.decRef();
        if (self.type) |typ| {
            typ.decRef(allocator);
        }
    }
};

pub const Scheme = struct {
    names: []SchemeBinding,
    type: *Type,

    pub fn deinit(self: Scheme, allocator: std.mem.Allocator) void {
        for (self.names) |name| {
            name.deinit(allocator);
        }
        self.type.decRef(allocator);
        if (self.names.len > 0) {
            allocator.free(self.names);
        }
    }

    pub fn instantiate(self: Scheme, pump: *Pump, allocator: std.mem.Allocator) !*Type {
        var s = Subst.init(allocator);
        defer s.deinit(allocator);

        for (self.names) |name| {
            const t = try pump.newBound(allocator);
            defer t.decRef(allocator);

            try s.put(@intFromPtr(name.name), t);
        }

        return try self.type.apply(&s);
    }
};

pub const Type = struct {
    kind: TypeKind,
    count: u32,

    pub fn create(allocator: std.mem.Allocator, kind: TypeKind) !*Type {
        const self = try allocator.create(Type);
        self.kind = kind;
        self.count = 1;

        return self;
    }

    pub fn decRef(self: *Type, allocator: std.mem.Allocator) void {
        if (self.count == 0) {
            return;
        }
        if (self.count == 1) {
            switch (self.kind) {
                .Bound => self.kind.Bound.deinit(),
                .Function => self.kind.Function.deinit(allocator),
                .OrEmpty => self.kind.OrEmpty.deinit(),
                .OrExtend => self.kind.OrExtend.deinit(allocator),
                .Tag => self.kind.Tag.deinit(),
                .Variable => self.kind.Variable.deinit(),
            }
            allocator.destroy(self);
        } else {
            self.count -= 1;
        }
    }

    pub fn incRef(this: *Type) void {
        if (this.count == std.math.maxInt(u32)) {
            this.count = 0;
        } else if (this.count > 0) {
            this.count += 1;
        }
    }

    pub fn incRefR(this: *Type) *Type {
        this.incRef();

        return this;
    }

    pub fn toString(self: *Type, allocator: std.mem.Allocator) std.mem.Allocator.Error![]u8 {
        var buffer = std.ArrayList(u8).init(allocator);

        try self.append(&buffer);

        return try buffer.toOwnedSlice();
    }

    pub fn print(self: *Type, allocator: std.mem.Allocator) !void {
        const s = try self.toString(allocator);
        defer allocator.free(s);

        std.debug.print("{s}", .{s});
    }

    pub fn append(self: *Type, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        switch (self.kind) {
            .Bound => try self.kind.Bound.append(buffer),
            .Function => try self.kind.Function.append(buffer),
            .OrEmpty => try self.kind.OrEmpty.append(buffer),
            .OrExtend => try self.kind.OrExtend.append(buffer),
            .Tag => try self.kind.Tag.append(buffer),
            .Variable => try self.kind.Variable.append(buffer),
        }
    }

    pub fn apply(self: *Type, s: *Subst) !*Type {
        switch (self.kind) {
            .Bound => return (s.get(self.kind.Bound.value) orelse self).incRefR(),
            .Function => return try FunctionType.new(
                s.items.allocator,
                try self.kind.Function.domain.apply(s),
                try self.kind.Function.range.apply(s),
            ),
            .OrExtend => return try OrExtendType.new(
                s.items.allocator,
                try self.kind.OrExtend.component.apply(s),
                try self.kind.OrExtend.rest.apply(s),
            ),
            else => return self.incRefR(),
        }
    }
};

pub const TypeKind = union(enum) {
    Bound: BoundType,
    Function: FunctionType,
    OrEmpty: OrEmptyType,
    OrExtend: OrExtendType,
    Tag: TagType,
    Variable: VariableType,
};

pub const BoundType = struct {
    value: u64,

    pub fn new(allocator: std.mem.Allocator, value: u64) !*Type {
        const self = try Type.create(allocator, TypeKind{ .Bound = BoundType{
            .value = value,
        } });

        return self;
    }

    pub fn deinit(self: *BoundType) void {
        _ = self;
    }

    pub fn append(self: *BoundType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try buffer.append('\'');
        try buffer.writer().print("{d}", .{self.value});
    }
};

pub const FunctionType = struct {
    domain: *Type,
    range: *Type,

    pub fn new(allocator: std.mem.Allocator, domain: *Type, range: *Type) !*Type {
        const self = try Type.create(allocator, TypeKind{ .Function = FunctionType{
            .domain = domain,
            .range = range,
        } });

        return self;
    }

    pub fn deinit(self: *FunctionType, allocator: std.mem.Allocator) void {
        self.domain.decRef(allocator);
        self.range.decRef(allocator);
    }

    pub fn append(self: *FunctionType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try self.domain.append(buffer);
        try buffer.appendSlice(" -> ");
        if (self.range.kind == TypeKind.Function) {
            try buffer.append('(');
            try self.range.append(buffer);
            try buffer.append(')');
        } else {
            try self.range.append(buffer);
        }
    }
};

pub const OrEmptyType = struct {
    pub fn new(allocator: std.mem.Allocator) !*Type {
        const self = try Type.create(allocator, TypeKind{ .OrEmpty = OrEmptyType{} });

        return self;
    }

    pub fn deinit(self: *OrEmptyType) void {
        _ = self;
    }

    pub fn append(self: *OrEmptyType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        _ = self;
        _ = buffer;

        unreachable;
    }
};

pub const OrExtendType = struct {
    component: *Type,
    rest: *Type,

    pub fn new(allocator: std.mem.Allocator, component: *Type, rest: *Type) !*Type {
        const self = try Type.create(allocator, TypeKind{ .OrExtend = OrExtendType{
            .component = component,
            .rest = rest,
        } });

        return self;
    }

    pub fn deinit(self: *OrExtendType, allocator: std.mem.Allocator) void {
        self.component.decRef(allocator);
        self.rest.decRef(allocator);
    }

    pub fn append(self: *OrExtendType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try self.component.append(buffer);

        if (self.rest.kind == TypeKind.OrExtend) {
            try buffer.appendSlice(" | ");
            try self.rest.append(buffer);
        } else if (self.rest.kind != TypeKind.OrEmpty) {
            unreachable;
        }
    }
};

pub const TagType = struct {
    name: *SP.String,

    pub fn new(allocator: std.mem.Allocator, name: *SP.String) !*Type {
        const self = try Type.create(allocator, TypeKind{ .Tag = TagType{
            .name = name,
        } });

        return self;
    }

    pub fn deinit(self: *TagType) void {
        self.name.decRef();
    }

    pub fn append(self: *TagType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try buffer.appendSlice(self.name.slice());
    }
};

pub const VariableType = struct {
    name: *SP.String,

    pub fn new(allocator: std.mem.Allocator, name: *SP.String) !*Type {
        const self = try Type.create(allocator, TypeKind{ .Variable = VariableType{
            .name = name,
        } });

        return self;
    }

    pub fn deinit(self: *VariableType) void {
        self.name.decRef();
    }

    pub fn append(self: *VariableType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try buffer.appendSlice(self.name.slice());
    }
};

pub const Constraint = struct {
    t1: *Type,
    t2: *Type,
    locationRange: Errors.LocationRange,

    pub fn init(t1: *Type, t2: *Type, locationRange: Errors.LocationRange) Constraint {
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
};

pub const Constraints = struct {
    items: std.ArrayList(Constraint),

    pub fn init(allocator: std.mem.Allocator) Constraints {
        return Constraints{
            .items = std.ArrayList(Constraint).init(allocator),
        };
    }

    pub fn deinit(self: *Constraints, allocator: std.mem.Allocator) void {
        for (self.items.items) |item| {
            item.deinit(allocator);
        }
        self.items.deinit();
    }

    pub fn add(self: *Constraints, t1: *Type, t2: *Type, locationRange: Errors.LocationRange) !void {
        const constraint = Constraint.init(t1, t2, locationRange);
        try self.items.append(constraint);
    }

    pub inline fn len(self: Constraints) usize {
        return self.items.items.len;
    }

    pub inline fn take(self: *Constraints) Constraint {
        return self.items.swapRemove(0);
    }

    pub inline fn apply(self: *Constraints, s: *Subst) !void {
        for (self.items.items) |*item| {
            var t1 = item.t1;
            defer t1.decRef(s.items.allocator);
            var t2 = item.t2;
            defer t2.decRef(s.items.allocator);

            item.t1 = try t1.apply(s);
            item.t2 = try t2.apply(s);
        }
    }
};

fn unify(t1: *Type, t2: *Type, locationRange: Errors.LocationRange, errors: *Errors.Errors, allocator: std.mem.Allocator) std.mem.Allocator.Error!Subst {
    // std.debug.print("unify: ", .{});
    // try t1.print(allocator);
    // std.debug.print(" with ", .{});
    // try t2.print(allocator);
    // std.debug.print("\n", .{});

    if (t1 == t2) return Subst.init(allocator);
    if (t1.kind == .Bound) {
        var s = Subst.init(allocator);
        try s.put(t1.kind.Bound.value, t2);
        return s;
    }
    if (t2.kind == .Bound) {
        var s = Subst.init(allocator);
        try s.put(t2.kind.Bound.value, t1);
        return s;
    }
    if (t1.kind == .Function and t2.kind == .Function) {
        const t1s = try allocator.alloc(*Type, 2);
        defer {
            for (t1s) |t1si| {
                t1si.decRef(allocator);
            }
            allocator.free(t1s);
        }

        const t2s = try allocator.alloc(*Type, 2);
        defer {
            for (t2s) |t2si| {
                t2si.decRef(allocator);
            }
            allocator.free(t2s);
        }

        t1s[0] = t1.kind.Function.domain.incRefR();
        t1s[1] = t1.kind.Function.range.incRefR();

        t2s[0] = t2.kind.Function.domain.incRefR();
        t2s[1] = t2.kind.Function.range.incRefR();

        return unifyMany(t1s, t2s, locationRange, errors, allocator);
    }

    try errors.append(try Errors.unificationError(allocator, locationRange, t1, t2));
    return Subst.init(allocator);
}

fn unifyMany(t1s: []*Type, t2s: []*Type, locationRange: Errors.LocationRange, errors: *Errors.Errors, allocator: std.mem.Allocator) std.mem.Allocator.Error!Subst {
    if (t1s.len == 0 and t2s.len == 0) return Subst.init(allocator);

    if (t1s.len != t2s.len) {
        try errors.append(try Errors.unificationError(allocator, locationRange, t1s[0], t2s[0]));
        return Subst.init(allocator);
    }

    var result = Subst.init(allocator);
    var i: usize = 0;
    while (i < t1s.len) {
        var s = try unify(t1s[i], t2s[i], locationRange, errors, allocator);
        defer s.deinit(allocator);

        var j = i + 1;
        while (j < t1s.len) {
            var s1o = t1s[j];
            defer s1o.decRef(allocator);
            var s2o = t2s[j];
            defer s2o.decRef(allocator);

            t1s[j] = try s1o.apply(&s);
            t2s[j] = try s2o.apply(&s);

            j += 1;
        }

        try result.compose(&s);

        i += 1;
    }

    return result;
}

pub fn solver(constraints: *Constraints, pump: *Pump, errors: *Errors.Errors, allocator: std.mem.Allocator) !Subst {
    _ = pump;

    var su = Subst.init(allocator);

    while (constraints.len() > 0) {
        const constraint = constraints.take();
        defer constraint.deinit(allocator);

        var s = try unify(constraint.t1, constraint.t2, constraint.locationRange, errors, allocator);
        defer s.deinit(allocator);

        try constraints.apply(&s);
        try su.compose(&s);
    }

    return su;
}

const TestState = struct {
    allocator: std.mem.Allocator,
    sp: SP.StringPool,

    pub fn init(allocator: std.mem.Allocator) TestState {
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
const expectEqualStrings = std.testing.expectEqualStrings;

test "Bound Substitution" {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer {
        const err = gpa.deinit();
        if (err == std.heap.Check.leak) {
            std.io.getStdErr().writeAll("Failed to deinit allocator\n") catch {};
            std.process.exit(1);
        }
    }

    var state = TestState.init(gpa.allocator());
    defer state.deinit();

    var constraints = Constraints.init(state.allocator);
    defer constraints.deinit(state.allocator);

    const boolType = try TagType.new(state.allocator, try state.sp.intern("Bool"));
    defer boolType.decRef(state.allocator);
    const boundType = try BoundType.new(state.allocator, 1);
    defer boundType.decRef(state.allocator);

    try constraints.add(
        boolType,
        boundType,
        Errors.LocationRange{
            .from = Errors.Location{ .offset = 0, .line = 0, .column = 0 },
            .to = Errors.Location{ .offset = 0, .line = 0, .column = 0 },
        },
    );

    var pump = Pump.init();
    var errors = try Errors.Errors.init(state.allocator);
    defer errors.deinit();

    var subst = try solver(&constraints, &pump, &errors, state.allocator);
    defer subst.deinit(state.allocator);

    var t = try boundType.apply(&subst);
    defer t.decRef(state.allocator);

    try expectEqual(subst.items.count(), 1);
    try expectEqual(subst.items.contains(1), true);
    try expectEqualStrings("Bool", t.kind.Tag.name.slice());
}
