const std = @import("std");

const Errors = @import("errors.zig");
const SP = @import("lib/string_pool.zig");

pub const Constraint = @import("typing/constraints.zig").Constraint;
pub const Constraints = @import("typing/constraints.zig").Constraints;
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
                .Tuple => self.kind.Tuple.deinit(allocator),
                .Variable => self.kind.Variable.deinit(),
            }
            allocator.destroy(self);
        } else {
            self.count -= 1;
        }
    }

    pub fn incRef(self: *Type) void {
        if (self.count == std.math.maxInt(u32)) {
            self.count = 0;
        } else if (self.count > 0) {
            self.count += 1;
        }
    }

    pub fn incRefR(this: *Type) *Type {
        this.incRef();

        return this;
    }

    pub inline fn isBool(self: *Type) bool {
        return self.kind == TypeKind.Tag and std.mem.eql(u8, self.kind.Tag.name.slice(), "Bool");
    }

    pub inline fn isChar(self: *Type) bool {
        return self.kind == TypeKind.Tag and std.mem.eql(u8, self.kind.Tag.name.slice(), "Char");
    }

    pub inline fn isFloat(self: *Type) bool {
        return self.kind == TypeKind.Tag and std.mem.eql(u8, self.kind.Tag.name.slice(), "Float");
    }

    pub inline fn isInt(self: *Type) bool {
        return self.kind == TypeKind.Tag and std.mem.eql(u8, self.kind.Tag.name.slice(), "Int");
    }

    pub inline fn isString(self: *Type) bool {
        return self.kind == TypeKind.Tag and std.mem.eql(u8, self.kind.Tag.name.slice(), "String");
    }

    pub inline fn isUnit(self: *Type) bool {
        return self.kind == TypeKind.Tag and std.mem.eql(u8, self.kind.Tag.name.slice(), "Unit");
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
            .Tuple => try self.kind.Tuple.append(buffer),
            .Variable => try self.kind.Variable.append(buffer),
        }
    }

    pub fn apply(self: *Type, s: *Subst) !*Type {
        switch (self.kind) {
            .Bound => return (s.get(self.kind.Bound.value) orelse self).incRefR(),
            .Function => {
                const allocator = s.items.allocator;

                const domain = try self.kind.Function.domain.apply(s);
                errdefer domain.decRef(allocator);

                const range = try self.kind.Function.range.apply(s);
                errdefer range.decRef(allocator);

                return try FunctionType.new(
                    allocator,
                    domain,
                    range,
                );
            },
            .OrExtend => {
                const allocator = s.items.allocator;

                const component = try self.kind.OrExtend.component.apply(s);
                errdefer component.decRef(allocator);

                const rest = try self.kind.OrExtend.rest.apply(s);
                errdefer rest.decRef(allocator);

                return try OrExtendType.new(
                    allocator,
                    component,
                    rest,
                );
            },
            .Tuple => {
                const allocator = s.items.allocator;

                var components = std.ArrayList(*Type).init(allocator);
                defer components.deinit();

                for (self.kind.Tuple.components) |component| {
                    try components.append(try component.apply(s));
                }

                return try TupleType.new(
                    allocator,
                    try components.toOwnedSlice(),
                );
            },
            .Variable => return (s.get(@intFromPtr(self.kind.Variable.name)) orelse self).incRefR(),
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
    Tuple: TupleType,
    Variable: VariableType,
};

pub const BoundType = struct {
    value: u64,

    pub fn new(allocator: std.mem.Allocator, value: u64) !*Type {
        return try Type.create(allocator, TypeKind{ .Bound = BoundType{
            .value = value,
        } });
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
        return try Type.create(allocator, TypeKind{ .Function = FunctionType{
            .domain = domain,
            .range = range,
        } });
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
        return try Type.create(allocator, TypeKind{ .OrEmpty = OrEmptyType{} });
    }

    pub fn deinit(self: *OrEmptyType) void {
        _ = self;
    }

    pub fn append(self: *OrEmptyType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        _ = self;

        try buffer.appendSlice("...");
    }
};

pub const OrExtendType = struct {
    component: *Type,
    rest: *Type,

    pub fn new(allocator: std.mem.Allocator, component: *Type, rest: *Type) !*Type {
        return try Type.create(allocator, TypeKind{ .OrExtend = OrExtendType{
            .component = component,
            .rest = rest,
        } });
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
        return try Type.create(allocator, TypeKind{ .Tag = TagType{
            .name = name,
        } });
    }

    pub fn deinit(self: *TagType) void {
        self.name.decRef();
    }

    pub fn append(self: *TagType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try buffer.appendSlice(self.name.slice());
    }
};

pub const TupleType = struct {
    components: []*Type,

    pub fn new(allocator: std.mem.Allocator, components: []*Type) !*Type {
        return try Type.create(allocator, TypeKind{ .Tuple = TupleType{
            .components = components,
        } });
    }

    pub fn deinit(self: *TupleType, allocator: std.mem.Allocator) void {
        for (self.components) |component| {
            component.decRef(allocator);
        }
        allocator.free(self.components);
    }

    pub fn append(self: *TupleType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try buffer.append('(');

        try self.components[0].append(buffer);
        for (self.components[1..]) |component| {
            try buffer.appendSlice(", ");
            try component.append(buffer);
        }

        try buffer.append(')');
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

    for (constraints.dependencies.items) |dependent| {
        if (!isDependent(dependent.t1, dependent.t2)) {
            try errors.append(try Errors.unificationError(allocator, dependent.locationRange, dependent.t1, dependent.t2));
        }
    }

    return su;
}

fn isDependent(t1: *Type, t2: *Type) bool {
    if (t1.kind == .Bound) return true;
    if (t2.kind == .Bound) return true;

    if (t1.kind == .Variable and t2.kind == .Variable) {
        return t1.kind.Variable.name == t2.kind.Variable.name;
    }

    if (t1.kind == .Tag and t2.kind == .Tag) {
        return t1.kind.Tag.name == t2.kind.Tag.name;
    }
    if (t1.kind == .Function and t2.kind == .Function) {
        return isDependent(t1.kind.Function.domain, t2.kind.Function.domain) and isDependent(t1.kind.Function.range, t2.kind.Function.range);
    }

    if (t2.kind == .OrExtend) {
        return isDependent(t1, t2.kind.OrExtend.component) or isDependent(t1, t2.kind.OrExtend.rest);
    }
    return false;
}

test "Bound Substitution" {
    var state = try @import("lib/test_state.zig").TestState.init();
    defer state.deinit();

    var constraints = Constraints.init(state.setup().allocator);
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

    try std.testing.expectEqual(subst.items.count(), 1);
    try std.testing.expectEqual(subst.items.contains(1), true);
    try std.testing.expectEqualStrings("Bool", t.kind.Tag.name.slice());
}
