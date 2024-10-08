const std = @import("std");

const Errors = @import("errors.zig");
const SP = @import("lib/string_pool.zig");
const Typing = @import("typing.zig");

pub const Operator = enum {
    Plus,
    Minus,
    Times,
    Divide,
    Modulo,
    Power,
    Equal,
    NotEqual,
    LessThan,
    LessEqual,
    GreaterThan,
    GreaterEqual,
    And,
    Or,
    Append,
    AppendUpdate,
    Prepend,
    PrependUpdate,
    Hook,

    pub fn toString(self: Operator) []const u8 {
        return switch (self) {
            Operator.Plus => "+",
            Operator.Minus => "-",
            Operator.Times => "*",
            Operator.Divide => "/",
            Operator.Power => "**",
            Operator.Modulo => "%",
            Operator.Equal => "==",
            Operator.NotEqual => "!=",
            Operator.LessThan => "<",
            Operator.LessEqual => "<=",
            Operator.GreaterThan => ">",
            Operator.GreaterEqual => ">=",
            Operator.And => "&&",
            Operator.Or => "||",
            Operator.Append => "<<",
            Operator.AppendUpdate => "<!",
            Operator.Prepend => ">>",
            Operator.PrependUpdate => ">!",
            Operator.Hook => "?",
        };
    }
};

pub const Package = struct {
    exprs: []*Expression,

    pub fn init(exprs: []*Expression) Package {
        return Package{
            .exprs = exprs,
        };
    }

    pub fn deinit(self: *Package, allocator: std.mem.Allocator) void {
        for (self.exprs) |expr| {
            destroyExpr(allocator, expr);
        }
        allocator.free(self.exprs);
    }

    pub fn create(allocator: std.mem.Allocator, exprs: []*Expression) !*Package {
        const package = try allocator.create(Package);
        package.* = Package.init(exprs);

        return package;
    }

    pub fn destroy(self: *Package, allocator: std.mem.Allocator) void {
        self.deinit(allocator);
        allocator.destroy(self);
    }
};

pub const Expression = struct {
    kind: ExpressionKind,
    locationRange: Errors.LocationRange,
    type: ?*Typing.Type,
    count: u32,

    fn init(kind: ExpressionKind, locationRange: Errors.LocationRange) Expression {
        return Expression{
            .kind = kind,
            .locationRange = locationRange,
            .type = null,
            .count = 1,
        };
    }

    pub fn create(allocator: std.mem.Allocator, kind: ExpressionKind, locationRange: Errors.LocationRange) !*Expression {
        const expr = try allocator.create(Expression);
        expr.* = Expression.init(kind, locationRange);

        return expr;
    }

    pub inline fn assignType(self: *Expression, typ: *Typing.Type, allocator: std.mem.Allocator) void {
        const old = self.type;
        defer if (old) |p| p.decRef(allocator);

        self.type = typ;
    }

    pub fn decRef(self: *Expression, allocator: std.mem.Allocator) void {
        destroyExpr(allocator, self);
    }

    pub fn incRef(this: *Expression) void {
        if (this.count == std.math.maxInt(u32)) {
            this.count = 0;
        } else if (this.count > 0) {
            this.count += 1;
        }
    }

    pub fn incRefR(this: *Expression) *Expression {
        this.incRef();

        return this;
    }

    pub fn schemeString(self: *Expression, allocator: std.mem.Allocator) ![]const u8 {
        switch (self.kind) {
            .declarations => {
                var buffer = std.ArrayList(u8).init(allocator);
                defer buffer.deinit();

                for (self.kind.declarations, 0..) |*d, idx| {
                    if (idx > 0) {
                        try buffer.appendSlice("; ");
                    }

                    try d.IdDeclaration.scheme.?.append(&buffer);
                }

                return try buffer.toOwnedSlice();
            },
            else => return try self.type.?.toString(allocator),
        }
    }
};

pub const ExpressionKind = union(enum) {
    assignment: AssignmentExpression,
    binaryOp: BinaryOpExpression,
    call: CallExpression,
    catche: CatchExpression,
    declarations: []DeclarationExpression,
    dot: DotExpression,
    exprs: []*Expression,
    identifier: *SP.String,
    ifte: []IfCouple,
    indexRange: IndexRangeExpression,
    indexValue: IndexValueExpression,
    literalBool: bool,
    literalChar: u8,
    literalFunction: LiteralFunction,
    literalInt: i64,
    literalFloat: f64,
    literalRecord: []RecordEntry,
    literalSequence: []LiteralSequenceValue,
    literalString: *SP.String,
    literalTuple: []*Expression,
    literalVoid: void,
    match: MatchExpression,
    notOp: NotOpExpression,
    raise: RaiseExpression,
    whilee: WhileExpression,
};

pub const AssignmentExpression = struct {
    lhs: *Expression,
    rhs: *Expression,
};

pub const BinaryOpExpression = struct {
    lhs: *Expression,
    op: Operator,
    rhs: *Expression,
};

pub const CallExpression = struct {
    callee: *Expression,
    args: []*Expression,
};

pub const CatchExpression = struct {
    value: *Expression,
    cases: []MatchCase,
};

pub const DeclarationExpression = union(enum) {
    IdDeclaration: IdDeclarationExpression,
    PatternDeclaration: PatternDeclarationExpression,
};

pub const DotExpression = struct {
    record: *Expression,
    field: *SP.String,

    pub fn deinit(self: *DotExpression, allocator: std.mem.Allocator) void {
        destroyExpr(allocator, self.record);
        self.field.decRef();
    }
};

pub const IdDeclarationExpression = struct {
    name: *SP.String,
    value: *Expression,
    scheme: ?Typing.Scheme,

    pub fn deinit(self: *IdDeclarationExpression, allocator: std.mem.Allocator) void {
        self.name.decRef();
        destroyExpr(allocator, self.value);

        if (self.scheme) |s| {
            s.deinit(allocator);
        }
    }
};

pub const IfCouple = struct {
    condition: ?*Expression,
    then: *Expression,
};

pub const IndexRangeExpression = struct {
    expr: *Expression,
    start: ?*Expression,
    end: ?*Expression,
};

pub const IndexValueExpression = struct {
    expr: *Expression,
    index: *Expression,
};

pub const LiteralFunction = struct {
    params: []FunctionParam,
    restOfParams: ?*SP.String,
    body: *Expression,

    pub fn deinit(self: *LiteralFunction, allocator: std.mem.Allocator) void {
        for (self.params) |*param| {
            param.deinit(allocator);
        }
        allocator.free(self.params);
        if (self.restOfParams != null) {
            self.restOfParams.?.decRef();
        }
        destroyExpr(allocator, self.body);
    }
};

pub const FunctionParam = struct {
    name: *SP.String,
    default: ?*Expression,

    pub fn deinit(self: *FunctionParam, allocator: std.mem.Allocator) void {
        self.name.decRef();

        if (self.default != null) {
            destroyExpr(allocator, self.default.?);
        }
    }
};

pub const LiteralSequenceValue = union(enum) {
    value: *Expression,
    sequence: *Expression,
};

pub const MatchExpression = struct {
    value: *Expression,
    cases: []MatchCase,
};

pub const MatchCase = struct {
    pattern: *Pattern,
    body: *Expression,

    pub fn deinit(self: *MatchCase, allocator: std.mem.Allocator) void {
        destroyPattern(allocator, self.pattern);
        destroyExpr(allocator, self.body);
    }
};

pub const NotOpExpression = struct {
    value: *Expression,
};

pub const RaiseExpression = struct {
    expr: *Expression,
};

pub const RecordEntry = union(enum) {
    value: struct {
        key: *SP.String,
        value: *Expression,
    },
    record: *Expression,
};

pub const PatternDeclarationExpression = struct {
    pattern: *Pattern,
    value: *Expression,

    pub fn deinit(self: *PatternDeclarationExpression, allocator: std.mem.Allocator) void {
        destroyPattern(allocator, self.pattern);
        destroyExpr(allocator, self.value);
    }
};

pub const WhileExpression = struct {
    condition: *Expression,
    body: *Expression,
};

fn destroyExpr(allocator: std.mem.Allocator, expr: *Expression) void {
    if (expr.count == 0) {
        return;
    }

    if (expr.count == 1) {
        switch (expr.kind) {
            .assignment => {
                destroyExpr(allocator, expr.kind.assignment.lhs);
                destroyExpr(allocator, expr.kind.assignment.rhs);
            },
            .binaryOp => {
                destroyExpr(allocator, expr.kind.binaryOp.lhs);
                destroyExpr(allocator, expr.kind.binaryOp.rhs);
            },
            .call => {
                destroyExpr(allocator, expr.kind.call.callee);
                for (expr.kind.call.args) |arg| {
                    destroyExpr(allocator, arg);
                }
                allocator.free(expr.kind.call.args);
            },
            .catche => {
                destroyExpr(allocator, expr.kind.catche.value);
                for (expr.kind.catche.cases) |*c| {
                    c.deinit(allocator);
                }
                allocator.free(expr.kind.catche.cases);
            },
            .declarations => {
                for (expr.kind.declarations) |*declaration| {
                    switch (declaration.*) {
                        .IdDeclaration => declaration.IdDeclaration.deinit(allocator),
                        .PatternDeclaration => declaration.PatternDeclaration.deinit(allocator),
                    }
                }

                allocator.free(expr.kind.declarations);
            },
            .dot => expr.kind.dot.deinit(allocator),
            .exprs => {
                for (expr.kind.exprs) |v| {
                    destroyExpr(allocator, v);
                }
                allocator.free(expr.kind.exprs);
            },
            .identifier => expr.kind.identifier.decRef(),
            .ifte => {
                for (expr.kind.ifte) |v| {
                    if (v.condition != null) {
                        destroyExpr(allocator, v.condition.?);
                    }
                    destroyExpr(allocator, v.then);
                }
                allocator.free(expr.kind.ifte);
            },
            .indexRange => {
                destroyExpr(allocator, expr.kind.indexRange.expr);
                if (expr.kind.indexRange.start != null) {
                    destroyExpr(allocator, expr.kind.indexRange.start.?);
                }
                if (expr.kind.indexRange.end != null) {
                    destroyExpr(allocator, expr.kind.indexRange.end.?);
                }
            },
            .indexValue => {
                destroyExpr(allocator, expr.kind.indexValue.expr);
                destroyExpr(allocator, expr.kind.indexValue.index);
            },
            .literalBool, .literalChar, .literalFloat, .literalInt, .literalVoid => {},
            .literalFunction => expr.kind.literalFunction.deinit(allocator),
            .literalRecord => {
                for (expr.kind.literalRecord) |v| {
                    switch (v) {
                        .value => {
                            v.value.key.decRef();
                            destroyExpr(allocator, v.value.value);
                        },
                        .record => destroyExpr(allocator, v.record),
                    }
                }
                allocator.free(expr.kind.literalRecord);
            },
            .literalSequence => {
                for (expr.kind.literalSequence) |v| {
                    switch (v) {
                        .value => destroyExpr(allocator, v.value),
                        .sequence => destroyExpr(allocator, v.sequence),
                    }
                }
                allocator.free(expr.kind.literalSequence);
            },
            .literalString => expr.kind.literalString.decRef(),
            .literalTuple => {
                for (expr.kind.literalTuple) |v| {
                    destroyExpr(allocator, v);
                }
                allocator.free(expr.kind.literalTuple);
            },
            .match => {
                destroyExpr(allocator, expr.kind.match.value);
                for (expr.kind.match.cases) |*c| {
                    c.deinit(allocator);
                }
                allocator.free(expr.kind.match.cases);
            },
            .notOp => destroyExpr(allocator, expr.kind.notOp.value),
            .raise => destroyExpr(allocator, expr.kind.raise.expr),
            .whilee => {
                destroyExpr(allocator, expr.kind.whilee.condition);
                destroyExpr(allocator, expr.kind.whilee.body);
            },
        }

        if (expr.type) |*typ| {
            typ.*.decRef(allocator);
        }

        allocator.destroy(expr);

        return;
    }

    expr.count -= 1;
}

pub const Pattern = struct {
    kind: PatternKind,
    locationRange: Errors.LocationRange,

    fn init(kind: PatternKind, locationRange: Errors.LocationRange) Pattern {
        return Pattern{
            .kind = kind,
            .locationRange = locationRange,
        };
    }

    pub fn create(allocator: std.mem.Allocator, kind: PatternKind, locationRange: Errors.LocationRange) !*Pattern {
        const expr = try allocator.create(Pattern);
        expr.* = Pattern.init(kind, locationRange);

        return expr;
    }

    pub fn destroy(self: *Pattern, allocator: std.mem.Allocator) void {
        destroyPattern(allocator, self);
    }
};

pub const PatternKind = union(enum) {
    identifier: *SP.String,
    literalBool: bool,
    literalChar: u8,
    literalFloat: f64,
    literalInt: i64,
    literalString: *SP.String,
    literalUnit: void,
    record: RecordPattern,
    sequence: SequencePattern,
};

pub const SequencePattern = struct {
    patterns: []*Pattern,
    restOfPatterns: ?*SP.String,
    id: ?*SP.String,

    pub fn deinit(self: *SequencePattern, allocator: std.mem.Allocator) void {
        for (self.patterns) |p| {
            destroyPattern(allocator, p);
        }
        allocator.free(self.patterns);
        if (self.restOfPatterns != null) {
            self.restOfPatterns.?.decRef();
        }
        if (self.id != null) {
            self.id.?.decRef();
        }
    }
};

pub const RecordPattern = struct {
    entries: []RecordPatternEntry,
    id: ?*SP.String,

    pub fn deinit(self: *RecordPattern, allocator: std.mem.Allocator) void {
        for (self.entries) |*e| {
            e.deinit(allocator);
        }
        allocator.free(self.entries);

        if (self.id != null) {
            self.id.?.decRef();
        }
    }
};

pub const RecordPatternEntry = struct {
    key: *SP.String,
    pattern: ?*Pattern,
    id: ?*SP.String,

    pub fn deinit(self: *RecordPatternEntry, allocator: std.mem.Allocator) void {
        self.key.decRef();
        if (self.pattern != null) {
            destroyPattern(allocator, self.pattern.?);
        }
        if (self.id != null) {
            self.id.?.decRef();
        }
    }
};

fn destroyPattern(allocator: std.mem.Allocator, pattern: *Pattern) void {
    switch (pattern.kind) {
        .identifier => pattern.kind.identifier.decRef(),
        .literalChar, .literalFloat, .literalInt, .literalBool, .literalUnit => {},
        .literalString => pattern.kind.literalString.decRef(),
        .record => pattern.kind.record.deinit(allocator),
        .sequence => pattern.kind.sequence.deinit(allocator),
    }

    allocator.destroy(pattern);
}
