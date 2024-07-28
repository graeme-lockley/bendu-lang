const std = @import("std");

const AST = @import("../ast.zig");
const Pointer = @import("../runtime/pointer.zig");
const Runtime = @import("../runtime/runtime.zig").Runtime;
const SP = @import("../lib/string_pool.zig");

const DEBUG = false;

pub fn eval(ast: *AST.Package, runtime: *Runtime) !void {
    var env = try Environment.init(runtime);
    defer env.deinit();

    try evalPackage(ast, &env);
}

const Scope = struct {
    parent: ?*Scope,
    bindings: std.AutoHashMap(*SP.String, BindingKind),

    pub fn deinit(self: *Scope, allocator: std.mem.Allocator) void {
        var iterator = self.bindings.iterator();

        while (iterator.next()) |entry| {
            entry.key_ptr.*.decRef();
            if (entry.value_ptr.* == .PackageFunction) {
                entry.value_ptr.*.PackageFunction.decRef(allocator);
            }
        }

        self.bindings.deinit();
    }
};

const Environment = struct {
    allocator: std.mem.Allocator,
    scope: ?*Scope,
    runtime: *Runtime,
    lbp: usize,

    pub fn init(runtime: *Runtime) !Environment {
        var result = Environment{
            .allocator = runtime.allocator,
            .scope = null,
            .runtime = runtime,
            .lbp = 0,
        };

        try result.open();

        return result;
    }

    pub fn deinit(self: *Environment) void {
        while (self.scope != null) {
            self.close();
        }
    }

    fn open(self: *Environment) !void {
        const newScope = try self.allocator.create(Scope);
        newScope.* = Scope{
            .parent = self.scope,
            .bindings = std.AutoHashMap(*SP.String, BindingKind).init(self.allocator),
        };

        self.scope = newScope;
    }

    fn close(self: *Environment) void {
        const parent = self.scope.?.parent;

        self.scope.?.deinit(self.allocator);
        self.allocator.destroy(self.scope.?);
        self.scope = parent;
    }

    fn findBinding(self: *Environment, key: *SP.String) ?BindingKind {
        var currentScope: ?*Scope = self.scope;
        while (currentScope) |scope| {
            if (scope.bindings.get(key)) |binding| {
                return binding;
            }
            currentScope = scope.parent;
        }

        return null;
    }

    fn bind(self: *Environment, key: *SP.String, value: BindingKind) !void {
        if (self.scope.?.bindings.contains(key)) {
            try std.io.getStdErr().writer().print("Internal Error: Attempt to redefine {s}\n", .{key.slice()});
            std.process.exit(1);
        }

        try self.scope.?.bindings.put(key.incRefR(), value);
    }
};

const BindingKind = union(enum) {
    PackageVariable: usize, // offset in stack
    PackageFunction: *AST.Expression, // lambda expression associated with the function
    LocalVariable: i64, // offset from lbp - positive is into stack and negative will be arguments
};

fn evalPackage(ast: *AST.Package, env: *Environment) !void {
    try env.runtime.push_unit();
    for (ast.exprs) |expr| {
        env.runtime.discard();
        try evalExpression(expr, env);
    }
}

fn evalExpression(ast: *AST.Expression, env: *Environment) !void {
    switch (ast.kind) {
        .binaryOp => {
            if (ast.kind.binaryOp.op == .And or ast.kind.binaryOp.op == .Or) {
                try evalExpression(ast.kind.binaryOp.lhs, env);
                if (ast.kind.binaryOp.op == .And) {
                    if (Pointer.asBool(env.runtime.peek().?)) {
                        env.runtime.discard();
                        try evalExpression(ast.kind.binaryOp.rhs, env);
                    }
                } else if (!Pointer.asBool(env.runtime.peek().?)) {
                    env.runtime.discard();
                    try evalExpression(ast.kind.binaryOp.rhs, env);
                }
            } else {
                try evalExpression(ast.kind.binaryOp.lhs, env);
                try evalExpression(ast.kind.binaryOp.rhs, env);

                switch (ast.kind.binaryOp.op) {
                    .Divide => if (ast.type.?.isInt()) try env.runtime.divide_int() else if (ast.type.?.isChar()) try env.runtime.divide_char() else if (ast.type.?.isFloat()) try env.runtime.divide_float() else try env.runtime.divide(),
                    .Equal => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.equals_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.equals_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.equals_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.equals_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.equals_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(true);
                    } else {
                        try env.runtime.equals();
                    },
                    .GreaterEqual => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.greaterequals_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.greaterequals_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.greaterequals_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.greaterequals_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.greaterequals_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(true);
                    } else {
                        try env.runtime.greaterequals();
                    },
                    .GreaterThan => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.greaterthan_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.greaterthan_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.greaterthan_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.greaterthan_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.greaterthan_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(false);
                    } else {
                        try env.runtime.greaterthan();
                    },
                    .LessEqual => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.lessequals_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.lessequals_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.lessequals_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.lessequals_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.lessequals_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(true);
                    } else {
                        try env.runtime.lessequals();
                    },
                    .LessThan => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.lessthan_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.lessthan_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.lessthan_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.lessthan_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.lessthan_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(false);
                    } else {
                        try env.runtime.lessthan();
                    },
                    .Minus => if (ast.type.?.isInt()) try env.runtime.minus_int() else if (ast.type.?.isChar()) try env.runtime.minus_char() else if (ast.type.?.isFloat()) try env.runtime.minus_float() else try env.runtime.minus(),
                    .Modulo => try env.runtime.modulo_int(),
                    .NotEqual => if (ast.kind.binaryOp.lhs.type.?.isBool()) {
                        try env.runtime.notequals_bool();
                    } else if (ast.kind.binaryOp.lhs.type.?.isChar()) {
                        try env.runtime.notequals_char();
                    } else if (ast.kind.binaryOp.lhs.type.?.isFloat()) {
                        try env.runtime.notequals_float();
                    } else if (ast.kind.binaryOp.lhs.type.?.isInt()) {
                        try env.runtime.notequals_int();
                    } else if (ast.kind.binaryOp.lhs.type.?.isString()) {
                        try env.runtime.notequals_string();
                    } else if (ast.kind.binaryOp.lhs.type.?.isUnit()) {
                        env.runtime.discard();
                        env.runtime.discard();
                        try env.runtime.push_bool(false);
                    } else {
                        try env.runtime.notequals();
                    },
                    .Plus => if (ast.type.?.isInt()) try env.runtime.add_int() else if (ast.type.?.isChar()) try env.runtime.add_char() else if (ast.type.?.isFloat()) try env.runtime.add_float() else if (ast.type.?.isString()) try env.runtime.add_string() else try env.runtime.add(),
                    .Power => if (ast.type.?.isInt()) try env.runtime.power_int() else if (ast.type.?.isChar()) try env.runtime.power_char() else if (ast.type.?.isFloat()) try env.runtime.power_float() else try env.runtime.power(),
                    .Times => if (ast.type.?.isInt()) try env.runtime.times_int() else if (ast.type.?.isChar()) try env.runtime.times_char() else if (ast.type.?.isFloat()) try env.runtime.times_float() else try env.runtime.times(),
                    else => {
                        try std.io.getStdErr().writer().print("Internal Error: Unsupported binary operator: \"{s}\"\n", .{ast.kind.binaryOp.op.toString()});
                        std.process.exit(1);
                    },
                }
            }
        },
        .call => {
            if (ast.kind.call.callee.kind != .identifier) {
                try std.io.getStdErr().writer().print("Internal Error: Expected identifier\n", .{});
                std.process.exit(1);
            } else {
                const callee = ast.kind.call.callee.kind.identifier;

                if (env.findBinding(callee)) |binding| {
                    switch (binding) {
                        .PackageFunction => {
                            const function = binding.PackageFunction;
                            const args = ast.kind.call.args;
                            const n = args.len;

                            if (n != function.kind.literalFunction.params.len) {
                                try std.io.getStdErr().writer().print("Internal Error: Expected {d} arguments, got {d}\n", .{ function.kind.literalFunction.params.len, n });
                                std.process.exit(1);
                            }

                            try env.open();
                            defer env.close();

                            const oldLBP = env.lbp;
                            const stackHeight = env.runtime.stack.items.len;

                            for (function.kind.literalFunction.params, 0..) |arg, i| {
                                try env.bind(arg.name, BindingKind{ .LocalVariable = @as(i64, @intCast(i)) - @as(i64, @intCast(n)) });
                                try evalExpression(args[i], env);
                            }

                            env.lbp = env.runtime.stack.items.len;

                            try evalExpression(function.kind.literalFunction.body, env);

                            env.lbp = oldLBP;

                            const r = env.runtime.pop();
                            env.runtime.stack.items.len = stackHeight;
                            try env.runtime.push_pointer(r);
                        },
                        else => {
                            try std.io.getStdErr().writer().print("Internal Error: Unsupported binding kind: {}\n", .{binding});
                            std.process.exit(1);
                        },
                    }
                } else {
                    try std.io.getStdErr().writer().print("Internal Error: Unknown function {s}\n", .{callee.slice()});
                    std.process.exit(1);
                }
            }
        },
        .declarations => {
            const declaration = ast.kind.declarations[0].IdDeclaration;

            if (env.findBinding(declaration.name) != null) {
                try std.io.getStdErr().writer().print("Internal Error: Attempt to redefine {s}\n", .{declaration.name.slice()});
                std.process.exit(1);
            }

            if (declaration.value.kind == .literalFunction) {
                try env.bind(declaration.name, BindingKind{ .PackageFunction = declaration.value.incRefR() });
                try env.runtime.push_unit();
            } else {
                try evalExpression(declaration.value, env);

                try env.bind(declaration.name, BindingKind{ .PackageVariable = env.runtime.stackPointer() });
                try env.runtime.duplicate();
            }
        },

        .exprs => {
            try env.runtime.push_int(0);
            for (ast.kind.exprs) |expr| {
                env.runtime.discard();
                try evalExpression(expr, env);
            }
        },
        .identifier => if (env.findBinding(ast.kind.identifier)) |value| {
            switch (value) {
                .PackageVariable => try env.runtime.push_pointer(env.runtime.stackItem(value.PackageVariable)),
                .LocalVariable => {
                    if (DEBUG) {
                        std.debug.print(".identifier: lbp: {d}, offset: {d}\n", .{ env.lbp, value.LocalVariable });
                    }

                    const lbp: i64 = @intCast(env.lbp);
                    const offset: usize = @intCast(lbp + value.LocalVariable);

                    try env.runtime.push_pointer(env.runtime.stackItem(offset));
                },
                else => {
                    try std.io.getStdErr().writer().print("Internal Error: Unsupported binding kind: {}\n", .{value});
                    std.process.exit(1);
                },
            }
        } else {
            try std.io.getStdErr().writer().print("Internal Error: Undefined variable {s}\n", .{ast.kind.identifier.slice()});
            std.process.exit(1);
        },
        .ifte => {
            for (ast.kind.ifte) |ifte| {
                if (ifte.condition) |condition| {
                    try evalExpression(condition, env);

                    const guard = Pointer.asBool(env.runtime.peek().?);
                    env.runtime.discard();

                    if (guard) {
                        try evalExpression(ifte.then, env);
                        return;
                    }
                } else {
                    try evalExpression(ifte.then, env);
                    return;
                }
            }

            try std.io.getStdErr().writer().print("Runtime Error: no if branch executed: ", .{});
            try ast.locationRange.write(std.io.getStdErr().writer());
            try std.io.getStdErr().writer().print("\n", .{});
            std.process.exit(1);
        },
        .literalBool => try env.runtime.push_bool(ast.kind.literalBool),
        .literalChar => try env.runtime.push_int(@intCast(ast.kind.literalChar)),
        .literalInt => try env.runtime.push_int(@intCast(ast.kind.literalInt)),
        .literalFloat => try env.runtime.push_float(ast.kind.literalFloat),
        .literalString => try env.runtime.push_string(ast.kind.literalString),
        .literalVoid => try env.runtime.push_int(0),
        .notOp => {
            try evalExpression(ast.kind.notOp.value, env);
            try env.runtime.not();
        },
        else => {
            try std.io.getStdErr().writer().print("Internal Error: Unsupported expression kind: {}\n", .{ast.kind});
            std.process.exit(1);
        },
    }
}

fn run(script: []const u8, result: []const u8) !void {
    const Errors = @import("../errors.zig");
    const Main = @import("../main.zig");
    const Parser = @import("../parser.zig");
    const Static = @import("../static.zig");

    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    const allocator = gpa.allocator();
    defer _ = gpa.deinit();

    var sp = SP.StringPool.init(allocator);
    defer sp.deinit();

    var errors = try Errors.Errors.init(allocator);
    defer errors.deinit();

    const parseResult = try Parser.parse(&sp, "script.bendu", script, &errors);

    try std.testing.expect(parseResult != null);

    if (parseResult) |ast| {
        defer ast.destroy(allocator);

        var env = try Static.Env.init(&sp, &errors);
        defer env.deinit();

        try Static.package(ast, &env);

        try std.testing.expect(!errors.hasErrors());

        var runtime = Runtime.init(&sp);
        defer runtime.deinit();

        try eval(ast, &runtime);

        const typ = ast.exprs[ast.exprs.len - 1].type.?;

        var sb = std.ArrayList(u8).init(allocator);
        defer sb.deinit();

        try Main.valueTypeToString(runtime.peek().?, typ, &sb);
        try std.testing.expectEqualStrings(result, sb.items);
    }
}

test "ackermann" {
    try run("let ackermann(m, n) = if m == 0 -> n + 1 | n == 0 -> ackermann(m - 1, 1) | ackermann(m - 1, ackermann(m, n - 1))", "fn: (Int, Int) -> Int");
    try run("let ackermann(m, n) { if m == 0 -> n + 1 | n == 0 -> ackermann(m - 1, 1) | ackermann(m - 1, ackermann(m, n - 1)) } ; ackermann(1, 1)", "3: Int");
}
