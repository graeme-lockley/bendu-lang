const std = @import("std");

const AST = @import("../ast.zig");
const SP = @import("../string_pool.zig");

pub fn eval(ast: *AST.Expression, allocator: std.mem.Allocator) !void {
    const bc = try compile(ast, allocator);
    defer allocator.free(bc);

    try execute(bc, allocator);
}

const Op = enum(u8) {
    ret,
    discard,
    duplicate,

    push_int,
    push_global,

    print_int,
    print_ln,
};

fn compile(ast: *AST.Expression, allocator: std.mem.Allocator) ![]u8 {
    var compileState = try CompileState.init(allocator);
    defer compileState.deinit();

    try compileExpr(ast, &compileState);
    try compileState.appendOp(Op.ret);

    return compileState.bc.toOwnedSlice();
}

const CompileState = struct {
    bc: std.ArrayList(u8),
    bindings: std.AutoHashMap(*SP.String, usize),
    nextGlobalBinding: usize,

    fn init(allocator: std.mem.Allocator) !CompileState {
        return CompileState{
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

    fn append(self: *CompileState, v: i64) !void {
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
};

fn compileExpr(ast: *AST.Expression, state: *CompileState) !void {
    switch (ast.kind) {
        .call => {
            if (ast.kind.call.callee.kind != .identifier) {
                try std.io.getStdErr().writer().print("Error: Expected identifier\n", .{});
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
                    try std.io.getStdErr().writer().print("Error: Unknown function {s}\n", .{callee});
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
                try std.io.getStdErr().writer().print("Error: Attempt to redefine {s}\n", .{ast.kind.idDeclaration.name.slice()});
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
                try state.append(@intCast(binding));
            } else {
                try std.io.getStdErr().writer().print("Error: Use of undeclared identifier {s}\n", .{name.slice()});
                std.process.exit(1);
            }
        },
        .literalInt => {
            try state.appendOp(Op.push_int);
            try state.append(ast.kind.literalInt);
        },
        else => {
            try std.io.getStdErr().writer().print("Error: Unsupported expression kind\n", .{});
            std.process.exit(1);
        },
    }
}

fn execute(bc: []u8, allocator: std.mem.Allocator) !void {
    const writer = std.io.getStdOut().writer();
    var ip: usize = 0;
    var stack = std.ArrayList(i64).init(allocator);
    defer stack.deinit();

    while (true) {
        const op = @as(Op, @enumFromInt(bc[ip]));

        // std.io.getStdOut().writer().print("instruction: ip={d}, op={}, stack={}\n", .{ ip, op, stack }) catch {};

        switch (op) {
            .ret => break,
            .discard => {
                _ = stack.pop();
                ip += 1;
            },
            .duplicate => {
                const v = stack.pop();
                try stack.append(v);
                try stack.append(v);
                ip += 1;
            },
            .push_int => {
                try stack.append(readInt(bc, ip + 1));
                ip += 9;
            },
            .push_global => {
                try stack.append(stack.items[@intCast(readInt(bc, ip + 1))]);
                ip += 9;
            },
            .print_int => {
                const v = stack.pop();
                try writer.print("{d}", .{v});
                ip += 1;
            },
            .print_ln => {
                try writer.print("\n", .{});
                ip += 1;
            },
        }
    }
}

fn readInt(bc: []const u8, ip: usize) i64 {
    // std.io.getStdOut().writer().print("readInt: bc.len={d}, ip={d}\n", .{ bc.len, ip }) catch {};

    const v: i64 = @bitCast(@as(u64, (bc[ip])) |
        (@as(u64, bc[ip + 1]) << 8) |
        (@as(u64, bc[ip + 2]) << 16) |
        (@as(u64, bc[ip + 3]) << 24) |
        (@as(u64, bc[ip + 4]) << 32) |
        (@as(u64, bc[ip + 5]) << 40) |
        (@as(u64, bc[ip + 6]) << 48) |
        (@as(u64, bc[ip + 7]) << 56));

    return v;
}
