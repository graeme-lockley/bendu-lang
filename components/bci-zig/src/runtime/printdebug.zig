const std = @import("std");

const Memory = @import("../memory.zig");
const Pointer = @import("../pointer.zig");

pub fn print(value: Pointer.Pointer) void {
    if (Pointer.isBool(value)) {
        std.debug.print("{s}", .{if (Pointer.asBool(value)) "True" else "False"});
    } else if (Pointer.isChar(value)) {
        std.debug.print("{c}", .{Pointer.asChar(value)});
    } else if (Pointer.isInt(value)) {
        std.debug.print("{d}", .{Pointer.asInt(value)});
    } else if (Pointer.isFloat(value)) {
        std.debug.print("{d}", .{Pointer.asFloat(value)});
    } else if (Pointer.isPointer(value)) {
        const v = Pointer.asPointer(*Memory.Value, value);

        switch (v.v) {
            .ArrayKind => {
                std.debug.print("[", .{});
                for (v.v.ArrayKind.items(), 0..) |item, i| {
                    if (i > 0) {
                        std.debug.print(", ", .{});
                    }
                    print(item);
                }
                std.debug.print("]", .{});
            },
            .ClosureKind => std.debug.print("fn", .{}),
            .CustomKind => {
                std.debug.print("{s}", .{v.v.CustomKind.name.slice()});
                std.debug.print("(", .{});
                for (v.v.CustomKind.values, 0..) |item, i| {
                    if (i > 0) {
                        std.debug.print(", ", .{});
                    }
                    print(item);
                }
                std.debug.print(")", .{});
            },

            .FrameKind => {
                std.debug.print("<", .{});
                if (v.v.FrameKind.enclosing == null) {
                    std.debug.print("-", .{});
                } else {
                    print(Pointer.fromPointer(*Memory.Value, v.v.FrameKind.enclosing.?));
                }

                for (v.v.FrameKind.values.items, 0..) |item, i| {
                    if (i == 0) {
                        std.debug.print(" ", .{});
                    } else {
                        std.debug.print(", ", .{});
                    }
                    print(item);
                }
                std.debug.print(">", .{});
            },
            .TupleKind => {
                std.debug.print("(", .{});
                for (v.v.TupleKind.values, 0..) |item, i| {
                    if (i > 0) {
                        std.debug.print(", ", .{});
                    }
                    print(item);
                }
                std.debug.print(")", .{});
            },
        }
    } else if (Pointer.isString(value)) {
        const str = Pointer.asString(value).slice();
        std.debug.print("{s}", .{str});
    } else {
        std.debug.print("Unknown type", .{});
    }
}
