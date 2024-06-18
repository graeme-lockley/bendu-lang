const std = @import("std");

const Typing = @import("../typing.zig");

pub const Pump = struct {
    count: u64,

    pub fn init() Pump {
        return Pump{
            .count = 0,
        };
    }

    pub fn pump(self: *Pump) u64 {
        const result = self.count;
        self.count += 1;

        return result;
    }

    pub fn newBound(self: *Pump, allocator: std.mem.Allocator) !*Typing.Type {
        return try Typing.BoundType.new(allocator, self.pump());
    }
};
