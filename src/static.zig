const std = @import("std");

const AST = @import("ast.zig");
const SP = @import("string_pool.zig");
const Typing = @import("typing.zig");

const Env = struct {
    names: std.AutoHashMap(*SP.String, Typing.Scheme),
    schemes: std.AutoHashMap(*SP.String, Typing.Scheme),

    pub fn init(allocator: std.mem.Allocator) Env {
        return Env{
            .names = std.AutoHashMap(*SP.String, Typing.Scheme).init(allocator),
            .schemes = std.AutoHashMap(*SP.String, Typing.Scheme).init(allocator),
        };
    }

    pub fn deinit(self: *Env, allocator: std.mem.Allocator) void {
        var iterator1 = self.names.iterator();
        while (iterator1.next()) |*entry| {
            entry.key_ptr.*.decRef();
            entry.value_ptr.*.deinit(allocator);
        }
        self.names.deinit();

        var iterator2 = self.schemes.iterator();
        while (iterator2.next()) |entry| {
            entry.key_ptr.*.decRef();
            entry.value_ptr.*.deinit(allocator);
        }
        self.schemes.deinit();
    }
};

pub fn analysis(ast: *AST.Expression, allocator: std.mem.Allocator) !void {
    var env = Env.init(allocator);
    defer env.deinit(allocator);

    try expression(ast, &env);
}

fn expression(ast: *AST.Expression, env: *Env) !void {
    _ = ast;
    _ = env;
}
