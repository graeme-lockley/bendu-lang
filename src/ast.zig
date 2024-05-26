const std = @import("std");
const SP = @import("./string_pool.zig");

pub const ExpressionKind = union(enum) {
    block: BlockExpression,
    idDeclaration: IdDeclarationExpression,
    identifier: IdentifierExpression,
    literalInt: LiteralIntExpression,
    println: PrintlnExpression,
};

pub const Expression = struct {
    kind: ExpressionKind,
    count: u32,

    fn init(kind: ExpressionKind) Expression {
        return Expression{
            .kind = kind,
            .count = 1,
        };
    }

    pub fn create(allocator: std.mem.Allocator, kind: ExpressionKind) !*Expression {
        const expr = try allocator.create(Expression);
        expr.* = Expression.init(kind);

        return expr;
    }

    pub fn decRef(self: *Expression, allocator: std.mem.Allocator) void {
        if (self.count == 0) {
            return;
        }

        if (self.count == 1) {
            switch (self.kind) {
                .block => self.kind.block.deinit(allocator),
                .idDeclaration => self.kind.idDeclaration.deinit(allocator),
                .identifier => self.kind.identifier.deinit(),
                .literalInt => self.kind.literalInt.deinit(),
                .println => self.kind.println.deinit(allocator),
            }

            allocator.destroy(self);
            return;
        }

        self.count -= 1;
    }

    pub fn incRef(self: *Expression) void {
        if (self.count == std.math.maxInt(u32)) {
            self.count = 0;
        } else if (self.count > 0) {
            self.count += 1;
        }
    }

    pub fn incRefR(self: *Expression) *Expression {
        self.incRef();

        return self;
    }
};

pub const BlockExpression = struct {
    exprs: []*Expression,

    pub fn deinit(self: *BlockExpression, allocator: std.mem.Allocator) void {
        for (self.exprs) |expr| {
            expr.decRef(allocator);
        }

        allocator.free(self.exprs);
    }
};

pub const IdDeclarationExpression = struct {
    name: *SP.String,
    value: *Expression,

    pub fn deinit(self: *IdDeclarationExpression, allocator: std.mem.Allocator) void {
        self.name.decRef();
        self.value.decRef(allocator);
    }
};

pub const IdentifierExpression = struct {
    name: *SP.String,

    pub fn deinit(self: *IdentifierExpression) void {
        self.name.decRef();
    }
};

pub const LiteralIntExpression = struct {
    value: i64,

    pub fn deinit(self: *LiteralIntExpression) void {
        _ = self;
    }
};

pub const PrintlnExpression = struct {
    exprs: []*Expression,

    pub fn deinit(self: *PrintlnExpression, allocator: std.mem.Allocator) void {
        for (self.exprs) |expr| {
            expr.decRef(allocator);
        }

        allocator.free(self.exprs);
    }
};
