const std = @import("std");

const Memory = @import("../memory.zig");
const Pointer = @import("../pointer.zig");

const stdout = std.io.getStdOut().writer();

pub fn print(value: Pointer.Pointer) !void {
    if (Pointer.isBool(value)) {
        try stdout.print("{s}", .{if (Pointer.asBool(value)) "True" else "False"});
    } else if (Pointer.isChar(value)) {
        try stdout.print("{c}", .{Pointer.asChar(value)});
    } else if (Pointer.isInt(value)) {
        try stdout.print("{d}", .{Pointer.asInt(value)});
    } else if (Pointer.isFloat(value)) {
        try stdout.print("{d}", .{Pointer.asFloat(value)});
    } else if (Pointer.isPointer(value)) {
        const v = Pointer.as(*Memory.Value, value);

        switch (v.v) {
            .ClosureKind => try stdout.print("fn", .{}),
            .FrameKind => try stdout.print("frame", .{}),
            .SequenceKind => {
                try stdout.print("[", .{});
                for (v.v.SequenceKind.items(), 0..) |item, i| {
                    if (i > 0) {
                        try stdout.print(", ", .{});
                    }
                    try print(item);
                }
                try stdout.print("]", .{});
            },
            .TupleKind => {
                try stdout.print("(", .{});
                for (v.v.TupleKind.values, 0..) |item, i| {
                    if (i > 0) {
                        try stdout.print(", ", .{});
                    }
                    try print(item);
                }
                try stdout.print(")", .{});
            },
        }
    } else if (Pointer.isString(value)) {
        const str = Pointer.asString(value).slice();
        try stdout.print("{s}", .{str});
    } else {
        try stdout.print("Unknown type", .{});
    }
}
