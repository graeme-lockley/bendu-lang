const std = @import("std");

const AST = @import("../ast.zig");
const Op = @import("./op.zig").Op;
const Pointer = @import("../runtime/pointer.zig");
const Runtime = @import("../runtime/runtime.zig").Runtime;
const SP = @import("../lib/string_pool.zig");

pub fn compile(ast: *AST.Package, allocator: std.mem.Allocator) ![]u8 {
    var compileState = try CompileState.init(allocator);
    defer compileState.deinit();

    try compilePackage(ast, &compileState);
    try compileState.appendOp(Op.ret);

    return compileState.bc.toOwnedSlice();
}

const Scope = struct {
    parent: ?*Scope,
    bindings: std.AutoHashMap(*SP.String, BindingKind),
    nextBinding: usize,

    fn deinit(self: *Scope) void {
        self.parent = null;
        self.bindings.deinit();
    }
};

const CompileState = struct {
    allocator: std.mem.Allocator,
    bc: std.ArrayList(u8),
    scope: ?*Scope,

    fn init(allocator: std.mem.Allocator) !CompileState {
        var result = CompileState{
            .allocator = allocator,
            .bc = std.ArrayList(u8).init(allocator),
            .scope = null,
        };

        try result.open();

        return result;
    }

    fn deinit(self: *CompileState) void {
        self.bc.deinit();
        while (self.scope != null) {
            self.close();
        }
    }

    fn open(self: *CompileState) !void {
        const newScope = try self.allocator.create(Scope);
        newScope.* = Scope{
            .parent = self.scope,
            .bindings = std.AutoHashMap(*SP.String, BindingKind).init(self.allocator),
            .nextBinding = 0,
        };

        self.scope = newScope;
    }

    fn close(self: *CompileState) void {
        const parent = self.scope.?.parent;
        self.scope.?.deinit();
        self.allocator.destroy(self.scope.?);
        self.scope = parent;
    }

    fn findBinding(self: *CompileState, key: *SP.String) ?BindingKind {
        var currentScope: ?*Scope = self.scope;
        while (currentScope) |scope| {
            if (scope.bindings.get(key)) |binding| {
                return binding;
            }
            currentScope = scope.parent;
        }
        return null;
    }

    fn bindInScope(self: *CompileState, key: *SP.String, value: BindingKind) !void {
        if (self.scope.?.bindings.contains(key)) {
            try std.io.getStdErr().writer().print("Internal Error: Attempt to redefine {s}\n", .{key.slice()});
            std.process.exit(1);
        }

        try self.scope.?.bindings.put(key, value);
    }

    fn nextBindingOffset(self: *CompileState) usize {
        const result = self.scope.?.nextBinding;
        self.scope.?.nextBinding += 1;
        return result;
    }

    fn appendOp(self: *CompileState, op: Op) !void {
        try self.bc.append(@intFromEnum(op));
    }

    fn appendFloat(self: *CompileState, v: f64) !void {
        try self.appendInt(@bitCast(v));
    }

    fn appendIntAt(self: *CompileState, v: i64, offset: usize) !void {
        const v1: u8 = @intCast(v & 0xff);
        const v2: u8 = @intCast((@as(u64, @bitCast(v & 0xff00))) >> 8);
        const v3: u8 = @intCast((@as(u64, @bitCast(v & 0xff0000))) >> 16);
        const v4: u8 = @intCast((@as(u64, @bitCast(v & 0xff000000))) >> 24);
        const v5: u8 = @intCast((@as(u64, @bitCast(v & 0xff00000000))) >> 32);
        const v6: u8 = @intCast((@as(u64, @bitCast(v & 0xff0000000000))) >> 40);
        const v7: u8 = @intCast((@as(u64, @bitCast(v & 0xff000000000000))) >> 48);
        const v8: u8 = @intCast((@as(u64, @bitCast(v))) >> 56);

        self.bc.items[offset] = v1;
        self.bc.items[offset + 1] = v2;
        self.bc.items[offset + 2] = v3;
        self.bc.items[offset + 3] = v4;
        self.bc.items[offset + 4] = v5;
        self.bc.items[offset + 5] = v6;
        self.bc.items[offset + 6] = v7;
        self.bc.items[offset + 7] = v8;
    }

    fn appendInt(self: *CompileState, v: i64) !void {
        const v1: u8 = @intCast(v & 0xff);
        const v2: u8 = @intCast((@as(u64, @bitCast(v & 0xff00))) >> 8);
        const v3: u8 = @intCast((@as(u64, @bitCast(v & 0xff0000))) >> 16);
        const v4: u8 = @intCast((@as(u64, @bitCast(v & 0xff000000))) >> 24);
        const v5: u8 = @intCast((@as(u64, @bitCast(v & 0xff00000000))) >> 32);
        const v6: u8 = @intCast((@as(u64, @bitCast(v & 0xff0000000000))) >> 40);
        const v7: u8 = @intCast((@as(u64, @bitCast(v & 0xff000000000000))) >> 48);
        const v8: u8 = @intCast((@as(u64, @bitCast(v))) >> 56);

        try self.bc.append(v1);
        try self.bc.append(v2);
        try self.bc.append(v3);
        try self.bc.append(v4);
        try self.bc.append(v5);
        try self.bc.append(v6);
        try self.bc.append(v7);
        try self.bc.append(v8);
    }

    fn appendString(self: *CompileState, s: []const u8) !void {
        try self.appendInt(@intCast(s.len));
        for (s) |c| {
            try self.bc.append(c);
        }
    }

    fn bcOffset(self: *CompileState) usize {
        return self.bc.items.len;
    }
};

const BindingKind = union(enum) {
    PackageVariable: usize, // offset in stack
    PackageFunction: usize, // offset in bytecode
    LocalVariable: i64, // offset from lbp - positive is into stack and negative will be arguments
};

fn compilePackage(ast: *AST.Package, state: *CompileState) !void {
    const exprCount = ast.exprs.len;
    if (exprCount == 0) {
        try state.appendOp(Op.push_unit);
    } else {
        for (ast.exprs, 0..) |item, idx| {
            try compileExpr(item, state);
            if (idx != exprCount - 1) {
                try state.appendOp(Op.discard);
            }
        }
    }
}

fn compileExpr(ast: *AST.Expression, state: *CompileState) !void {
    switch (ast.kind) {
        .binaryOp => if (ast.kind.binaryOp.op == .And or ast.kind.binaryOp.op == .Or) {
            try compileExpr(ast.kind.binaryOp.lhs, state);
            if (ast.kind.binaryOp.op == .And) {
                try state.appendOp(Op.jmp_tos_false);
                const offset = state.bcOffset();
                try state.appendInt(0);
                try state.appendOp(Op.discard);
                try compileExpr(ast.kind.binaryOp.rhs, state);
                try state.appendIntAt(@intCast(state.bcOffset()), offset);
            } else {
                try state.appendOp(Op.jmp_tos_true);
                const offset = state.bcOffset();
                try state.appendInt(0);
                try state.appendOp(Op.discard);
                try compileExpr(ast.kind.binaryOp.rhs, state);
                try state.appendIntAt(@intCast(state.bcOffset()), offset);
            }
        } else {
            try compileExpr(ast.kind.binaryOp.lhs, state);
            try compileExpr(ast.kind.binaryOp.rhs, state);

            switch (ast.kind.binaryOp.op) {
                .Divide => {
                    if (ast.type.?.isInt()) {
                        try state.appendOp(Op.divide_int);
                    } else if (ast.type.?.isChar()) {
                        try state.appendOp(Op.divide_char);
                    } else if (ast.type.?.isFloat()) {
                        try state.appendOp(Op.divide_float);
                    } else {
                        try state.appendOp(Op.divide);
                    }
                },
                .Equal => {
                    if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try state.appendOp(Op.equals_bool);
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try state.appendOp(Op.equals_char);
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try state.appendOp(Op.equals_float);
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try state.appendOp(Op.equals_int);
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try state.appendOp(Op.equals_string);
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.push_true);
                    } else {
                        try state.appendOp(Op.equals);
                    }
                },
                .GreaterEqual => {
                    if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try state.appendOp(Op.greaterequals_bool);
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try state.appendOp(Op.greaterequals_char);
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try state.appendOp(Op.greaterequals_float);
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try state.appendOp(Op.greaterequals_int);
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try state.appendOp(Op.greaterequals_string);
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.push_true);
                    } else {
                        try state.appendOp(Op.greaterequals);
                    }
                },
                .GreaterThan => {
                    if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try state.appendOp(Op.greaterthan_bool);
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try state.appendOp(Op.greaterthan_char);
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try state.appendOp(Op.greaterthan_float);
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try state.appendOp(Op.greaterthan_int);
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try state.appendOp(Op.greaterthan_string);
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.push_false);
                    } else {
                        try state.appendOp(Op.greaterthan);
                    }
                },
                .LessEqual => {
                    if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try state.appendOp(Op.lessequals_bool);
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try state.appendOp(Op.lessequals_char);
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try state.appendOp(Op.lessequals_float);
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try state.appendOp(Op.lessequals_int);
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try state.appendOp(Op.lessequals_string);
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.push_true);
                    } else {
                        try state.appendOp(Op.lessequals);
                    }
                },
                .LessThan => {
                    if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try state.appendOp(Op.lessthan_bool);
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try state.appendOp(Op.lessthan_char);
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try state.appendOp(Op.lessthan_float);
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try state.appendOp(Op.lessthan_int);
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try state.appendOp(Op.lessthan_string);
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.push_false);
                    } else {
                        try state.appendOp(Op.lessthan);
                    }
                },
                .Minus => {
                    if (ast.type.?.isInt()) {
                        try state.appendOp(Op.minus_int);
                    } else if (ast.type.?.isChar()) {
                        try state.appendOp(Op.minus_char);
                    } else if (ast.type.?.isFloat()) {
                        try state.appendOp(Op.minus_float);
                    } else {
                        try state.appendOp(Op.minus);
                    }
                },
                .Modulo => try state.appendOp(Op.modulo_int),
                .NotEqual => {
                    if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try state.appendOp(Op.notequals_bool);
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try state.appendOp(Op.notequals_char);
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try state.appendOp(Op.notequals_float);
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try state.appendOp(Op.notequals_int);
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try state.appendOp(Op.notequals_string);
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.discard);
                        try state.appendOp(Op.push_false);
                    } else {
                        try state.appendOp(Op.notequals);
                    }
                },
                .Plus => {
                    if (ast.type.?.isInt()) {
                        try state.appendOp(Op.add_int);
                    } else if (ast.type.?.isChar()) {
                        try state.appendOp(Op.add_char);
                    } else if (ast.type.?.isFloat()) {
                        try state.appendOp(Op.add_float);
                    } else if (ast.type.?.isString()) {
                        try state.appendOp(Op.add_string);
                    } else {
                        try state.appendOp(Op.add);
                    }
                },
                .Power => {
                    if (ast.type.?.isInt()) {
                        try state.appendOp(Op.power_int);
                    } else if (ast.type.?.isChar()) {
                        try state.appendOp(Op.power_char);
                    } else if (ast.type.?.isFloat()) {
                        try state.appendOp(Op.power_float);
                    } else {
                        try state.appendOp(Op.power);
                    }
                },
                .Times => {
                    if (ast.type.?.isInt()) {
                        try state.appendOp(Op.times_int);
                    } else if (ast.type.?.isChar()) {
                        try state.appendOp(Op.times_char);
                    } else if (ast.type.?.isFloat()) {
                        try state.appendOp(Op.times_float);
                    } else {
                        try state.appendOp(Op.times);
                    }
                },
                else => {
                    try std.io.getStdErr().writer().print("Internal Error: Unsupported binary operator\n", .{});
                    std.process.exit(1);
                },
            }
        },
        .call => {
            if (ast.kind.call.callee.kind != .identifier) {
                try std.io.getStdErr().writer().print("Internal Error: Expected identifier\n", .{});
                std.process.exit(1);
            } else {
                const callee = ast.kind.call.callee.kind.identifier;

                if (std.mem.eql(u8, callee.slice(), "println")) {
                    const exprCount = ast.kind.call.args.len;
                    for (ast.kind.call.args, 0..) |expr, idx| {
                        try compileExpr(expr, state);
                        if (idx == exprCount - 1) {
                            try state.appendOp(Op.duplicate);
                        }
                        try state.appendOp(Op.print_int);
                    }
                    try state.appendOp(Op.print_ln);
                } else if (state.findBinding(callee)) |c| {
                    for (ast.kind.call.args) |expr| {
                        try compileExpr(expr, state);
                    }
                    switch (c) {
                        .PackageFunction => {
                            try state.appendOp(Op.call_local);
                            try state.appendInt(@intCast(c.PackageFunction));
                        },
                        else => unreachable,
                    }
                } else {
                    try std.io.getStdErr().writer().print("Internal Error: Unknown function {s}\n", .{callee.slice()});
                    std.process.exit(1);
                }
            }
        },
        .exprs => {
            const exprCount = ast.kind.exprs.len;
            for (ast.kind.exprs, 0..) |item, idx| {
                try compileExpr(item, state);
                if (idx != exprCount - 1) {
                    try state.appendOp(Op.discard);
                }
            }
        },
        .idDeclaration => {
            const name = ast.kind.idDeclaration.name;

            if (state.findBinding(name)) |_| {
                try std.io.getStdErr().writer().print("Internal Error: Attempt to redefine {s}\n", .{ast.kind.idDeclaration.name.slice()});
                std.process.exit(1);
            } else if (ast.kind.idDeclaration.value.kind == .literalFunction) {
                const value = ast.kind.idDeclaration.value;
                const numberOfParameters: i64 = @intCast(value.kind.literalFunction.params.len);

                try state.appendOp(Op.push_unit);
                try state.appendOp(Op.jmp);
                const patch = state.bcOffset();
                try state.appendInt(0);

                try state.bindInScope(
                    name,
                    BindingKind{ .PackageFunction = state.bcOffset() },
                );

                try state.open();
                defer state.close();

                for (value.kind.literalFunction.params, 0..) |param, i| {
                    try state.bindInScope(param.name, BindingKind{ .LocalVariable = @as(i64, @intCast(i)) - numberOfParameters });
                }
                try compileExpr(value.kind.literalFunction.body, state);
                try state.appendOp(Op.ret_local);
                try state.appendInt(numberOfParameters);
                try state.appendIntAt(@intCast(state.bcOffset()), patch);
            } else {
                try state.bindInScope(
                    name,
                    BindingKind{ .PackageVariable = state.nextBindingOffset() },
                );

                try compileExpr(ast.kind.idDeclaration.value, state);
                try state.appendOp(Op.duplicate);
            }
        },
        .identifier => {
            const name = ast.kind.identifier;

            if (state.findBinding(name)) |binding| {
                switch (binding) {
                    .PackageVariable => {
                        try state.appendOp(Op.push_global);
                        try state.appendInt(@intCast(binding.PackageVariable));
                    },
                    .LocalVariable => {
                        try state.appendOp(Op.push_local);
                        try state.appendInt(@intCast(binding.LocalVariable));
                    },
                    else => unreachable,
                }
            } else {
                try std.io.getStdErr().writer().print("Internal Error: Use of undeclared identifier {s}\n", .{name.slice()});
                std.process.exit(1);
            }
        },
        .ifte => {
            var restPatches = std.ArrayList(usize).init(state.allocator);
            defer restPatches.deinit();
            var nextPatch: ?usize = null;

            for (ast.kind.ifte) |ifte| {
                if (nextPatch) |p| {
                    try state.appendIntAt(@intCast(state.bcOffset()), p);
                    nextPatch = null;
                }
                if (ifte.condition) |condition| {
                    try compileExpr(condition, state);
                    try state.appendOp(Op.jmp_false);
                    nextPatch = state.bcOffset();
                    try state.appendInt(0);
                    try compileExpr(ifte.then, state);
                    try state.appendOp(Op.jmp);
                    try restPatches.append(state.bcOffset());
                    try state.appendInt(0);
                } else {
                    try compileExpr(ifte.then, state);

                    for (restPatches.items) |p| {
                        try state.appendIntAt(@intCast(state.bcOffset()), p);
                    }
                    return;
                }
            }

            try std.io.getStdErr().writer().print("Internal Error: ", .{});
            try ast.locationRange.write(std.io.getStdErr().writer());
            try std.io.getStdErr().writer().print(": if requires one branch without a guard\n", .{});
            std.process.exit(1);
        },
        .literalBool => try state.appendOp(if (ast.kind.literalBool) Op.push_true else Op.push_false),
        .literalChar => {
            try state.appendOp(Op.push_int);
            try state.appendInt(@intCast(ast.kind.literalChar));
        },
        .literalInt => {
            try state.appendOp(Op.push_int);
            try state.appendInt(ast.kind.literalInt);
        },
        .literalFloat => {
            try state.appendOp(Op.push_float);
            try state.appendFloat(ast.kind.literalFloat);
        },
        .literalString => {
            try state.appendOp(Op.push_string);
            try state.appendString(ast.kind.literalString.slice());
        },
        .literalVoid => try state.appendOp(Op.push_unit),
        .notOp => {
            try compileExpr(ast.kind.notOp.value, state);
            try state.appendOp(Op.not);
        },
        else => {
            try std.io.getStdErr().writer().print("Internal Error: Unsupported expression kind\n", .{});
            std.process.exit(1);
        },
    }
}
