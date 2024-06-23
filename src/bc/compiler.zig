const std = @import("std");

const AST = @import("../ast.zig");
const Op = @import("./op.zig").Op;
const Pointer = @import("../runtime/pointer.zig");
const Runtime = @import("../runtime/runtime.zig").Runtime;
const SP = @import("../lib/string_pool.zig");

pub fn compile(ast: *AST.Expression, allocator: std.mem.Allocator) ![]u8 {
    var compileState = try CompileState.init(allocator);
    defer compileState.deinit();

    try compileExpr(ast, &compileState);
    try compileState.appendOp(Op.ret);

    return compileState.bc.toOwnedSlice();
}

const CompileState = struct {
    allocator: std.mem.Allocator,
    bc: std.ArrayList(u8),
    bindings: std.AutoHashMap(*SP.String, usize),
    nextGlobalBinding: usize,

    fn init(allocator: std.mem.Allocator) !CompileState {
        return CompileState{
            .allocator = allocator,
            .bc = std.ArrayList(u8).init(allocator),
            .bindings = std.AutoHashMap(*SP.String, usize).init(allocator),
            .nextGlobalBinding = 0,
        };
    }

    fn deinit(self: *CompileState) void {
        self.bc.deinit();
        self.bindings.deinit();
    }

    fn appendOp(self: *CompileState, op: Op) !void {
        try self.bc.append(@intFromEnum(op));
    }

    fn appendFloat(self: *CompileState, v: f64) !void {
        try self.appendInt(@bitCast(v));
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
};

fn compileExpr(ast: *AST.Expression, state: *CompileState) !void {
    switch (ast.kind) {
        .binaryOp => {
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
                .Minus => {
                    if (ast.type.?.isInt()) {
                        try state.appendOp(Op.minus_int);
                    } else if (ast.type.?.isChar()) {
                        try state.appendOp(Op.minus_char);
                    } else if (ast.type.?.isFloat()) {
                        try state.appendOp(Op.minus_float);
                    } else {
                        try state.appendOp(Op.add);
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
                const callee = ast.kind.call.callee.kind.identifier.slice();

                if (std.mem.eql(u8, callee, "println")) {
                    const exprCount = ast.kind.call.args.len;
                    for (ast.kind.call.args, 0..) |expr, idx| {
                        try compileExpr(expr, state);
                        if (idx == exprCount - 1) {
                            try state.appendOp(Op.duplicate);
                        }
                        try state.appendOp(Op.print_int);
                    }
                    try state.appendOp(Op.print_ln);
                } else {
                    try std.io.getStdErr().writer().print("Internal Error: Unknown function {s}\n", .{callee});
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

            if (state.bindings.get(name)) |_| {
                try std.io.getStdErr().writer().print("Internal Error: Attempt to redefine {s}\n", .{ast.kind.idDeclaration.name.slice()});
                std.process.exit(1);
            } else {
                try state.bindings.put(name, state.nextGlobalBinding);
                state.nextGlobalBinding += 1;

                try compileExpr(ast.kind.idDeclaration.value, state);
                try state.appendOp(Op.duplicate);
            }
        },
        .identifier => {
            const name = ast.kind.identifier;

            if (state.bindings.get(name)) |binding| {
                try state.appendOp(Op.push_global);
                try state.appendInt(@intCast(binding));
            } else {
                try std.io.getStdErr().writer().print("Internal Error: Use of undeclared identifier {s}\n", .{name.slice()});
                std.process.exit(1);
            }
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
