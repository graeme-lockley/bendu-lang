const std = @import("std");

const SP = @import("string_pool.zig");

pub const Pointer = u64;

const boolMask = @as(u64, 0b000);
const charMask = @as(u64, 0b001);
const floatMask = @as(u64, 0b010);
const intMask = @as(u64, 0b011);
const pointerMask = @as(u64, 0b100);
const stringMask = @as(u64, 0b101);

const mask = @as(u64, 0b111);

pub inline fn isBool(value: Pointer) bool {
    return value & mask == boolMask;
}

pub inline fn isChar(value: Pointer) bool {
    return value & mask == charMask;
}

pub inline fn isInt(value: Pointer) bool {
    return value & mask == intMask;
}

pub inline fn isFloat(value: Pointer) bool {
    return value & mask == floatMask;
}

pub inline fn isPointer(value: Pointer) bool {
    return value & mask == pointerMask;
}

pub inline fn isString(value: Pointer) bool {
    return value & mask == stringMask;
}

pub inline fn as(t: type, value: Pointer) t {
    return @as(t, @ptrFromInt(@as(usize, value) & ~pointerMask));
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

pub inline fn asString(value: Pointer) *SP.String {
    return as(*SP.String, value & ~stringMask);
}

pub inline fn fromBool(value: bool) Pointer {
    const v: u32 = if (value) 1 else 0;
    return (@as(Pointer, @intCast(@as(u32, @bitCast(v)))) << 32) | boolMask;
}

pub inline fn fromChar(value: u8) Pointer {
    const v: u32 = @intCast(value);
    return (@as(Pointer, @intCast(v)) << 32) | charMask;
}

pub inline fn fromInt(value: i32) Pointer {
    return (@as(Pointer, @intCast(@as(u32, @bitCast(value)))) << 32) | intMask;
}

pub inline fn fromFloat(value: f32) Pointer {
    return (@as(Pointer, @intCast(@as(u32, @bitCast(value)))) << 32) | floatMask;
}

pub inline fn fromPointer(t: type, value: t) Pointer {
    return @as(Pointer, @intFromPtr(value) | pointerMask);
}

pub inline fn fromString(value: *SP.String) Pointer {
    return @as(Pointer, @intFromPtr(value) | stringMask);
}

const expect = std.testing.expect;
const expectEqual = std.testing.expectEqual;

test "Bool" {
    const p = fromBool(true);

    try expect(isBool(p));
    try expect(!isChar(p));
    try expect(!isFloat(p));
    try expect(!isInt(p));
    try expect(!isPointer(p));
    try expect(!isString(p));

    try expect(asBool(p));
}

test "Char" {
    const p = fromChar('A');

    try expect(!isBool(p));
    try expect(isChar(p));
    try expect(!isFloat(p));
    try expect(!isInt(p));
    try expect(!isPointer(p));
    try expect(!isString(p));

    try expectEqual(asChar(p), 'A');
}

test "Float" {
    const p = fromFloat(3.14);

    try expect(!isBool(p));
    try expect(!isChar(p));
    try expect(isFloat(p));
    try expect(!isInt(p));
    try expect(!isPointer(p));
    try expect(!isString(p));

    try expectEqual(asFloat(p), 3.14);
}

test "Int" {
    const p = fromInt(42);

    try expect(!isBool(p));
    try expect(!isChar(p));
    try expect(!isFloat(p));
    try expect(isInt(p));
    try expect(!isPointer(p));
    try expect(!isString(p));

    try expectEqual(asInt(p), 42);
}

test "Real Pointer" {
    const allocator = std.heap.page_allocator;
    const data = try allocator.dupe(u8, "Hello World");
    defer allocator.free(data);

    const p = fromPointer(*const []u8, &data);
    const pDeref = as(*[]u8, p);

    try expect(!isBool(p));
    try expect(!isChar(p));
    try expect(!isFloat(p));
    try expect(!isInt(p));
    try expect(isPointer(p));
    try expect(!isString(p));

    try expectEqual(pDeref, &data);
}

test "String" {
    var pool = SP.StringPool.init(std.heap.page_allocator);
    defer SP.StringPool.deinit(&pool);

    const s = try pool.intern("Hello, World!");
    defer s.deinit();

    const p = fromString(s);

    try expect(!isBool(p));
    try expect(!isChar(p));
    try expect(!isFloat(p));
    try expect(!isInt(p));
    try expect(!isPointer(p));
    try expect(isString(p));

    try expectEqual(asString(p), s);
}
