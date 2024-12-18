const std = @import("std");

const Memory = @import("../memory.zig");
const Pointer = @import("../pointer.zig");

pub fn neq(p1: Pointer.Pointer, p2: Pointer.Pointer) bool {
    if (Pointer.isPointer(p1)) {
        const v1 = Pointer.as(*Memory.Value, p1);

        switch (v1.v) {
            .ArrayKind => {
                const v2 = Pointer.as(*Memory.Value, p2);

                if (v1.v.ArrayKind.len() != v2.v.ArrayKind.len()) {
                    return true;
                }

                for (0..v1.v.ArrayKind.len()) |i| {
                    if (neq(v1.v.ArrayKind.at(i), v2.v.ArrayKind.at(i))) {
                        return true;
                    }
                }
                return false;
            },
            .ClosureKind => return p1 != p2,
            .FrameKind => return p1 != p2,
            .TupleKind => {
                const v2 = Pointer.as(*Memory.Value, p2);
                for (0..v1.v.TupleKind.values.len) |i| {
                    if (neq(v1.v.TupleKind.values[i], v2.v.TupleKind.values[i])) {
                        return true;
                    }
                }
                return false;
            },
        }
    } else {
        return p1 != p2;
    }
}
