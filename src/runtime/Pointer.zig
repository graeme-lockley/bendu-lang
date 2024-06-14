const std = @import("std");

pub const Pointer = u64;

pub inline fn isPointer(value: Pointer) bool {
    return value & 1 == 0;
}

pub inline fn isInt(value: Pointer) bool {
    return value & 1 == 1;
}

pub inline fn as(t: type, value: Pointer) t {
    return @as(t, @ptrFromInt(@as(usize, value)));
}

pub inline fn asInt(value: Pointer) i64 {
    return @intCast(@as(i63, @bitCast(@as(u63, @intCast(value >> 1)))));
}

pub inline fn fromInt(value: i63) Pointer {
    return (@as(Pointer, @intCast(@as(u63, @bitCast(value)))) << 1) | 1;
}

test "Pointer" {
    const p: Pointer = 18446744073709551615;
    const pptr: Pointer = @intFromPtr(&p);
    try std.testing.expect(!isPointer(p));
    try std.testing.expect(isInt(p));
    try std.testing.expect(isPointer(pptr));
    try std.testing.expect(!isInt(pptr));
    try std.testing.expectEqual(asInt(p), -1);
    try std.testing.expectEqual(p, as(*Pointer, pptr).*);

    try std.testing.expectEqual(fromInt(0), 1);
    try std.testing.expectEqual(fromInt(-1), p);

    try std.testing.expectEqual(asInt(fromInt(-1)) * asInt(fromInt(100)), -100);
}
