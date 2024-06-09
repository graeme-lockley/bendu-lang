const std = @import("std");

const SP = @import("string_pool.zig");

pub const Scheme = struct {
    names: []const *SP.String,
    type: *Type,

    pub fn deinit(self: Scheme, allocator: std.mem.Allocator) void {
        self.type.decRef(allocator);
        for (self.names) |name| {
            name.decRef();
        }
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
                TypeKind.Function => self.kind.Function.deinit(allocator),
                TypeKind.Tag => self.kind.Tag.deinit(),
                TypeKind.Variable => self.kind.Variable.deinit(),
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

    pub fn append(self: *Type, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        switch (self.kind) {
            TypeKind.Function => try self.kind.Function.append(buffer),
            TypeKind.Tag => try self.kind.Tag.append(buffer),
            TypeKind.Variable => try self.kind.Variable.append(buffer),
        }
    }
};

pub const TypeKind = union(enum) {
    Function: FunctionType,
    Tag: TagType,
    Variable: VariableType,
};

pub const FunctionType = struct {
    domain: *Type,
    range: *Type,

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

pub const TagType = struct {
    name: *SP.String,

    pub fn deinit(self: *TagType) void {
        self.name.decRef();
    }

    pub fn append(self: *TagType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try buffer.appendSlice(self.name.slice());
    }
};

pub const VariableType = struct {
    name: *SP.String,

    pub fn deinit(self: *VariableType) void {
        self.name.decRef();
    }

    pub fn append(self: *VariableType, buffer: *std.ArrayList(u8)) std.mem.Allocator.Error!void {
        try buffer.appendSlice(self.name.slice());
    }
};
