const std = @import("std");

const AST = @import("../ast.zig");
const Compiler = @import("compiler.zig");
const Machine = @import("machine.zig");
const Runtime = @import("../runtime/runtime.zig").Runtime;

pub fn eval(ast: *AST.Expression, runtime: *Runtime) !void {
    const bc = try Compiler.compile(ast, runtime.allocator);
    defer runtime.allocator.free(bc);

    try Machine.execute(bc, runtime);
}
