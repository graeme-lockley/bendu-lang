const std = @import("std");

pub const Pointer = u64;

pub inline fn isInt(value: Pointer) bool {
    return value & 1 == 1;
}

pub inline fn isPointer(value: Pointer) bool {
    return value & 1 == 0;
}

pub inline fn as(t: type, value: Pointer) t {
    return @as(t, @ptrFromInt(@as(usize, value)));
}

pub inline fn asBool(value: Pointer) bool {
    return asInt(value) == 1;
}

pub inline fn asChar(value: Pointer) u8 {
    const v: i64 = @mod(asInt(value), 256);

    return @intCast(v);
}

pub inline fn asInt(value: Pointer) i32 {
    return @intCast(@as(i32, @bitCast(@as(u32, @intCast(value >> 1)))));
}

pub inline fn fromBool(value: bool) Pointer {
    return if (value) fromInt(1) else fromInt(0);
}

pub inline fn fromInt(value: i32) Pointer {
    return (@as(Pointer, @intCast(@as(u32, @bitCast(value)))) << 1) | 1;
}

test "Pointer" {
    const p: Pointer = 8589934591;
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

    try std.testing.expectEqual(fromBool(true), fromInt(1));
    try std.testing.expectEqual(fromBool(false), fromInt(0));

    try std.testing.expectEqual(asBool(fromBool(true)), true);
    try std.testing.expectEqual(asBool(fromBool(false)), false);
}
