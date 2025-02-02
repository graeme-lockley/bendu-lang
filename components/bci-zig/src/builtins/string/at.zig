const Pointer = @import("../../pointer.zig");
const Runtime = @import("../../runtime.zig");

pub fn do(runtime: *Runtime.Runtime) !void {
    const idx = Pointer.asInt(runtime.pop());
    const str = Pointer.asString(runtime.pop());

    if (idx < 0 or idx >= str.len()) {
        try runtime.push_u8_literal(0);
    } else {
        try runtime.push_u8_literal(str.slice()[@intCast(idx)]);
    }

    str.decRef();
}
