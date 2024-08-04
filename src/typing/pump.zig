const std = @import("std");

const Typing = @import("../typing.zig");

pub const Pump = struct {
    count: u64,

    pub fn init() Pump {
        return Pump{
            .count = 1,
        };
    }

    pub fn pump(self: *Pump) u64 {
        const result = self.count;
        self.count += 2;

        return result;
    }

    pub fn newBound(self: *Pump, allocator: std.mem.Allocator) !*Typing.Type {
        return try Typing.BoundType.new(allocator, self.pump());
    }

    pub fn newBoundN(self: *Pump, allocator: std.mem.Allocator, n: u64) ![]*Typing.Type {
        var result = try allocator.alloc(*Typing.Type, n);
        errdefer allocator.free(result);

        for (result, 0..) |_, i| {
            result[i] = try Typing.BoundType.new(allocator, self.pump());
        }

        return result;
    }
};
