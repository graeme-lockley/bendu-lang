const std = @import("std");

const SP = @import("string_pool.zig");

pub const Pointer = u64;

pub inline fn isInt(value: Pointer) bool {
    return value & 1 == 1;
}

pub inline fn isPointer(value: Pointer) bool {
    return value & 1 == 0;
}

pub inline fn isString(value: Pointer) bool {
    return value & 0b11 == 0b11;
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

pub inline fn asFloat(value: Pointer) f32 {
    return @as(f32, @bitCast(@as(u32, @intCast(value >> 32))));
}

pub inline fn asInt(value: Pointer) i32 {
    return @intCast(@as(i32, @bitCast(@as(u32, @intCast(value >> 32)))));
}

const stringMask = ~@as(u64, 0b11);

pub inline fn asString(value: Pointer) *SP.String {
    return as(*SP.String, value & stringMask);
}

pub inline fn fromBool(value: bool) Pointer {
    return if (value) fromInt(1) else fromInt(0);
}

pub inline fn fromChar(value: u8) Pointer {
    return fromInt(@intCast(value));
}

pub inline fn fromInt(value: i32) Pointer {
    return (@as(Pointer, @intCast(@as(u32, @bitCast(value)))) << 32) | 1;
}

pub inline fn fromFloat(value: f32) Pointer {
    return (@as(Pointer, @intCast(@as(u32, @bitCast(value)))) << 32) | 1;
}

pub inline fn fromString(value: *SP.String) Pointer {
    return @as(Pointer, @intFromPtr(value) | 0b11);
}

test "Pointer" {
    const p: Pointer = 18446744069414584321;
    const pptr: Pointer = @intFromPtr(&p);
    try std.testing.expect(!isPointer(p));
    try std.testing.expect(isInt(p));
    try std.testing.expect(isPointer(pptr));
    try std.testing.expect(!isInt(pptr));
    try std.testing.expectEqual(asInt(p), -1);
    try std.testing.expectEqual(p, as(*Pointer, pptr).*);

    try std.testing.expectEqual(fromBool(true), fromInt(1));
    try std.testing.expectEqual(fromBool(false), fromInt(0));
    try std.testing.expectEqual(asBool(fromBool(true)), true);
    try std.testing.expectEqual(asBool(fromBool(false)), false);

    try std.testing.expectEqual(asChar(fromChar(' ')), ' ');

    try std.testing.expectEqual(asFloat(fromFloat(0.0)), 0.0);

    try std.testing.expectEqual(fromInt(0), 1);
    try std.testing.expectEqual(fromInt(-1), p);
    try std.testing.expectEqual(asInt(fromInt(-1)) * asInt(fromInt(100)), -100);
}

test "String Pointer" {
    var pool = SP.StringPool.init(std.heap.page_allocator);
    defer SP.StringPool.deinit(&pool);

    const s = try pool.intern("Hello, World!");
    defer s.deinit();

    const p = fromString(s);
    try std.testing.expect(!isPointer(p));
    try std.testing.expect(isString(p));
    try std.testing.expectEqual(asString(p), s);
}

test "Real Pointer" {
    const allocator = std.heap.page_allocator;
    const data = try allocator.dupe(u8, "Hello World");
    defer allocator.free(data);

    const p = @as(Pointer, @intFromPtr(&data));
    const pDeref = as(*[]u8, p);

    try std.testing.expect(isPointer(p));
    try std.testing.expectEqual(pDeref, &data);
}
