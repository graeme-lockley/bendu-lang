const std = @import("std");

const SP = @import("./string_pool.zig");

const Scheme = struct {
    names: []*SP.String,
    type: *Type,
};

pub const Type = struct {
    kind: TypeKind,
    count: u32,

    pub fn decRef(self: *Type, allocator: std.mem.Allocator) void {
        if (self.count == 0) {
            return;
        }
        if (self.count == 1) {
            self.count = 0;
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
};

const TypeKind = union(enum) {
    Function: FunctionType,
    Tag: TagType,
    Variable: VariableType,
};

const FunctionType = struct {
    domain: *Type,
    range: *Type,

    pub fn deinit(self: *FunctionType, allocator: std.mem.Allocator) void {
        self.domain.decRef(allocator);
        self.range.decRef(allocator);
    }
};

const TagType = struct {
    name: *SP.String,

    pub fn deinit(self: *TagType) void {
        self.name.decRef();
    }
};

const VariableType = struct {
    name: *SP.String,

    pub fn deinit(self: *VariableType) void {
        self.name.decRef();
    }
};
