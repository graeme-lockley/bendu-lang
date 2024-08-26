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

    pub fn destroy(self: *Scheme, allocator: std.mem.Allocator) void {
        self.deinit(allocator);
        allocator.destroy(self);
    }

    pub fn clone(self: *Scheme, allocator: std.mem.Allocator) !Scheme {
        var names = std.ArrayList(SchemeBinding).init(allocator);
        defer names.deinit();

        for (self.names) |name| {
            const t = name.type;

            if (t != null) {
                t.?.incRef();
            }

            try names.append(SchemeBinding{
                .name = name.name.incRefR(),
                .type = t,
            });
        }

        return Scheme{
            .names = try names.toOwnedSlice(),
            .type = self.type.incRefR(),
        };
    }

    pub fn instantiate(self: Scheme, pump: *Pump, allocator: std.mem.Allocator) !*Type {
        var s = Subst.init(allocator);
        defer s.deinit(allocator);

        for (self.names) |name| {
            const t = try pump.newBound(allocator);
            defer t.decRef(allocator);

            try s.add(@intFromPtr(name.name), t);
        }

        return try self.type.apply(&s);
    }

    pub fn append(self: *Scheme, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        if (self.names.len == 0) {
            try self.type.append(buffer);
        } else {
            try buffer.append('[');
            for (self.names, 0..) |n, i| {
                if (i > 0) {
                    try buffer.appendSlice(", ");
                }
                try buffer.appendSlice(n.name.slice());

                if (n.type) |t| {
                    try buffer.append(':');
                    try buffer.append(' ');
                    try t.append(buffer);
                }
            }
            try buffer.append(']');
            try buffer.append(' ');

            try self.type.append(buffer);
        }
    }

    pub fn toString(self: *Scheme, allocator: std.mem.Allocator) ![]u8 {
        var buffer = std.ArrayList(u8).init(allocator);
        defer buffer.deinit();

        try self.append(&buffer);

        return buffer.toOwnedSlice();
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

    pub fn generalise(self: *Type, allocator: std.mem.Allocator, sp: *SP.StringPool, constraints: *Constraints) !Scheme {
        var accState = AccumlativeState.init(allocator, sp, constraints);
        defer accState.deinit();

        const typ = try accumulateFreeVariableNames(self, &accState);
        return Scheme{ .names = try accState.names.toOwnedSlice(), .type = typ };
    }
};

const AccumlativeState = struct {
    allocator: std.mem.Allocator,
    sp: *SP.StringPool,
    names: std.ArrayList(SchemeBinding),
    boundBindings: std.AutoHashMap(u64, *Type),
    constraints: *Constraints,
    n: u32,

    fn init(allocator: std.mem.Allocator, sp: *SP.StringPool, constraints: *Constraints) AccumlativeState {
        return AccumlativeState{
            .allocator = allocator,
            .sp = sp,
            .names = std.ArrayList(SchemeBinding).init(allocator),
            .boundBindings = std.AutoHashMap(u64, *Type).init(allocator),
            .constraints = constraints,
            .n = 0,
        };
    }

    fn deinit(self: *AccumlativeState) void {
        self.names.deinit();
        self.boundBindings.deinit();
    }

    fn newBound(self: *AccumlativeState, v: u64) !*Type {
        const key = self.n;
        self.n += 1;

        var buffer = std.ArrayList(u8).init(self.allocator);
        defer buffer.deinit();

        try buffer.append(@intCast(key + @as(u32, @intCast('a'))));
        const keySP = try self.sp.internOwned(try buffer.toOwnedSlice());

        const typ = try VariableType.new(self.allocator, keySP);

        try self.boundBindings.put(v, typ);
        const typeDependency = self.constraints.findDependency(v);

        if (typeDependency) |d| {
            d.incRef();
        }

        // if (typeDependency == null) {
        //     std.debug.print("--- v: {d} ------------\n", .{v});

        //     try self.constraints.debugPrint();
        // }

        try self.names.append(SchemeBinding{
            .name = keySP.incRefR(),
            .type = typeDependency,
        });

        return typ;
    }

    fn getBound(self: *AccumlativeState, v: u64) ?*Type {
        return self.boundBindings.get(v);
    }
};

fn accumulateFreeVariableNames(typ: *Type, state: *AccumlativeState) !*Type {
    switch (typ.kind) {
        .Bound => {
            if (state.getBound(typ.kind.Bound.value)) |n| {
                return n.incRefR();
            } else {
                return try state.newBound(typ.kind.Bound.value);
            }
        },
        .Function => {
            const domain = try accumulateFreeVariableNames(typ.kind.Function.domain, state);
            errdefer domain.decRef(state.allocator);

            const range = try accumulateFreeVariableNames(typ.kind.Function.range, state);
            errdefer range.decRef(state.allocator);

            return try FunctionType.new(
                state.allocator,
                domain,
                range,
            );
        },
        .OrExtend => {
            const component = try accumulateFreeVariableNames(typ.kind.OrExtend.component, state);
            errdefer component.decRef(state.allocator);

            const rest = try accumulateFreeVariableNames(typ.kind.OrExtend.rest, state);
            errdefer rest.decRef(state.allocator);

            return try OrExtendType.new(
                state.allocator,
                component,
                rest,
            );
        },
        .Tuple => {
            var components = std.ArrayList(*Type).init(state.allocator);
            defer components.deinit();

            for (typ.kind.Tuple.components) |component| {
                try components.append(try accumulateFreeVariableNames(component, state));
            }

            return try TupleType.new(
                state.allocator,
                try components.toOwnedSlice(),
            );
        },
        // .Variable => return (s.get(@intFromPtr(self.kind.Variable.name)) orelse self).incRefR(),
        else => return typ.incRefR(),
    }
}

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
        try buffer.writer().print("'{d}", .{self.value});
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

fn unify(t1: *Type, t2: *Type, locationRange: Errors.LocationRange, errors: *Errors.Errors, subst: *Subst) std.mem.Allocator.Error!void {
    // std.debug.print("unify: ", .{});
    // try t1.print(allocator);
    // std.debug.print(" with ", .{});
    // try t2.print(allocator);
    // std.debug.print("\n", .{});

    if (t1 != t2) {
        if (t1.kind == .Bound) {
            try subst.add(t1.kind.Bound.value, t2);
        } else if (t2.kind == .Bound) {
            try subst.add(t2.kind.Bound.value, t1);
        } else if (t1.kind == .Function and t2.kind == .Function) {
            try unify(t1.kind.Function.domain, t2.kind.Function.domain, locationRange, errors, subst);

            const t1range = try t1.kind.Function.range.apply(subst);
            defer t1range.decRef(subst.allocator);
            const t2range = try t2.kind.Function.range.apply(subst);
            defer t2range.decRef(subst.allocator);

            try unify(t1range, t2range, locationRange, errors, subst);
        } else if (t1.kind == .Tuple and t2.kind == .Tuple) {
            if (t1.kind.Tuple.components.len != t2.kind.Tuple.components.len) {
                try errors.append(try Errors.unificationError(subst.allocator, locationRange, t1, t2));
            } else {
                try unifyMany(t1.kind.Tuple.components, t2.kind.Tuple.components, locationRange, errors, subst);
            }
        } else {
            try errors.append(try Errors.unificationError(subst.allocator, locationRange, t1, t2));
        }
    }
}

fn unifyMany(t1s: []*Type, t2s: []*Type, locationRange: Errors.LocationRange, errors: *Errors.Errors, subst: *Subst) std.mem.Allocator.Error!void {
    if (t1s.len != t2s.len) {
        try errors.append(try Errors.unificationError(subst.allocator, locationRange, t1s[0], t2s[0]));
    } else {
        var i: usize = 0;
        while (i < t1s.len) {
            try unify(t1s[i], t2s[i], locationRange, errors, subst);

            var j = i + 1;
            while (j < t1s.len) {
                var s1o = t1s[j];
                defer s1o.decRef(subst.allocator);
                var s2o = t2s[j];
                defer s2o.decRef(subst.allocator);

                t1s[j] = try s1o.apply(subst);
                t2s[j] = try s2o.apply(subst);

                j += 1;
            }

            i += 1;
        }
    }
}

pub fn solver(constraints: *Constraints, errors: *Errors.Errors, allocator: std.mem.Allocator) !Subst {
    var subst = Subst.init(allocator);

    while (constraints.len() > 0) {
        const constraint = constraints.take();
        defer constraint.deinit(allocator);

        try unify(constraint.t1, constraint.t2, constraint.locationRange, errors, &subst);

        try constraints.apply(&subst);
    }

    for (constraints.dependencies.items) |dependent| {
        if (!isDependent(dependent.t1, dependent.t2)) {
            try errors.append(try Errors.unificationError(allocator, dependent.locationRange, dependent.t1, dependent.t2));
        }
    }

    return subst;
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

    var errors = try Errors.Errors.init(state.allocator);
    defer errors.deinit();

    var subst = try solver(&constraints, &errors, state.allocator);
    defer subst.deinit(state.allocator);

    var t = try boundType.apply(&subst);
    defer t.decRef(state.allocator);

    try std.testing.expectEqual(subst.items.count(), 1);
    try std.testing.expectEqual(subst.items.contains(1), true);
    try std.testing.expectEqualStrings("Bool", t.kind.Tag.name.slice());
}
