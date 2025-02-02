const Pointer = @import("../../pointer.zig");
const Runtime = @import("../../runtime.zig");

pub fn do(runtime: *Runtime.Runtime) !void {
    const str = Pointer.asString(runtime.pop());

    const len = str.len();

    str.decRef();

    try runtime.push_i32_literal(@intCast(len));
}
