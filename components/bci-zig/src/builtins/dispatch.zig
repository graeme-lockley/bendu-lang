// Note - this is generated code

const std = @import("std");
const Runtime = @import("../runtime.zig");

pub fn dispatch(runtime: *Runtime.Runtime, id: usize) !void {
    switch (id) {
        0 => try @import("./string/length.zig").do(runtime),
        1 => try @import("./string/at.zig").do(runtime),
        else => std.debug.panic("unknown builtin op code: {d}\n", .{id}),
    }
}
