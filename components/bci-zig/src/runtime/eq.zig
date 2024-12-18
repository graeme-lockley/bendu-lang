const std = @import("std");

const Memory = @import("../memory.zig");
const Pointer = @import("../pointer.zig");

pub fn eq(p1: Pointer.Pointer, p2: Pointer.Pointer) bool {
    if (Pointer.isPointer(p1)) {
        const v1 = Pointer.as(*Memory.Value, p1);

        switch (v1.v) {
            .ArrayKind => {
                const v2 = Pointer.as(*Memory.Value, p2);

                if (v1.v.ArrayKind.len() != v2.v.ArrayKind.len()) {
                    return false;
                }

                for (0..v1.v.ArrayKind.len()) |i| {
                    if (!eq(v1.v.ArrayKind.at(i), v2.v.ArrayKind.at(i))) {
                        return false;
                    }
                }
                return true;
            },
            .ClosureKind => return p1 == p2,
            .FrameKind => return p1 == p2,
            .TupleKind => {
                const v2 = Pointer.as(*Memory.Value, p2);
                for (0..v1.v.TupleKind.values.len) |i| {
                    if (!eq(v1.v.TupleKind.values[i], v2.v.TupleKind.values[i])) {
                        return false;
                    }
                }
                return true;
            },
        }
    } else {
        return p1 == p2;
    }
}
