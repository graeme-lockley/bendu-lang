const std = @import("std");
const SP = @import("string_pool.zig");

/// Tagged union pointer implementation using the lower 3 bits as type tags
pub const Pointer = u64;

/// Type tags for different pointer types
pub const TypeTag = enum(u3) {
    Bool = 0b000,
    Char = 0b001,
    Float = 0b010,
    Int = 0b011,
    Pointer = 0b100,
    String = 0b101,
    // Reserved = 0b110,
    // Reserved = 0b111,

    pub fn getMask(self: TypeTag) u64 {
        return @as(u64, @intFromEnum(self));
    }
};

const mask = @as(u64, 0b111);

/// Maximum values that can be stored in the upper bits
const MAX_CHAR_VALUE = 255;
const MAX_INT_VALUE = (1 << 31) - 1;
const MIN_INT_VALUE = -(1 << 31);

pub inline fn isBool(value: Pointer) bool {
    return (value & mask) == TypeTag.Bool.getMask();
}

pub inline fn isChar(value: Pointer) bool {
    return (value & mask) == TypeTag.Char.getMask();
}

pub inline fn isInt(value: Pointer) bool {
    return (value & mask) == TypeTag.Int.getMask();
}

pub inline fn isFloat(value: Pointer) bool {
    return (value & mask) == TypeTag.Float.getMask();
}

pub inline fn isPointer(value: Pointer) bool {
    return (value & mask) == TypeTag.Pointer.getMask();
}

pub inline fn isString(value: Pointer) bool {
    return (value & mask) == TypeTag.String.getMask();
}

inline fn as(t: type, value: Pointer) t {
    return @as(t, @ptrFromInt(@as(usize, value) & ~TypeTag.Pointer.getMask()));
}

pub inline fn asPointer(t: type, value: Pointer) t {
    std.debug.assert((value & mask) == TypeTag.Pointer.getMask());
    return as(t, value);
}

pub inline fn asBool(value: Pointer) bool {
    std.debug.assert((value & mask) == TypeTag.Bool.getMask());
    return @as(i32, @bitCast(@as(u32, @intCast(value >> 32)))) == 1;
}

pub inline fn asChar(value: Pointer) u8 {
    std.debug.assert((value & mask) == TypeTag.Char.getMask());
    const v: i64 = @mod(@as(i32, @bitCast(@as(u32, @intCast(value >> 32)))), 256);
    return @intCast(v);
}

pub inline fn asFloat(value: Pointer) f32 {
    std.debug.assert((value & mask) == TypeTag.Float.getMask());
    return @as(f32, @bitCast(@as(u32, @intCast(value >> 32))));
}

pub inline fn asInt(value: Pointer) i32 {
    std.debug.assert((value & mask) == TypeTag.Int.getMask());
    return @intCast(@as(i32, @bitCast(@as(u32, @intCast(value >> 32)))));
}

pub inline fn asString(value: Pointer) *SP.String {
    std.debug.assert((value & mask) == TypeTag.String.getMask());
    return as(*SP.String, value & ~TypeTag.String.getMask());
}

pub inline fn fromBool(value: bool) Pointer {
    const v: u32 = if (value) 1 else 0;
    return (@as(Pointer, @intCast(@as(u32, @bitCast(v)))) << 32) | TypeTag.Bool.getMask();
}

pub inline fn fromChar(value: u8) Pointer {
    std.debug.assert(value <= MAX_CHAR_VALUE);
    const v: u32 = @intCast(value);
    return (@as(Pointer, @intCast(v)) << 32) | TypeTag.Char.getMask();
}

pub inline fn fromInt(value: i32) Pointer {
    std.debug.assert(value >= MIN_INT_VALUE and value <= MAX_INT_VALUE);
    return (@as(Pointer, @intCast(@as(u32, @bitCast(value)))) << 32) | TypeTag.Int.getMask();
}

pub inline fn fromFloat(value: f32) Pointer {
    return (@as(Pointer, @intCast(@as(u32, @bitCast(value)))) << 32) | TypeTag.Float.getMask();
}

pub inline fn fromPointer(t: type, value: t) Pointer {
    return @as(Pointer, @intFromPtr(value) | TypeTag.Pointer.getMask());
}

pub inline fn fromString(value: *SP.String) Pointer {
    return @as(Pointer, @intFromPtr(value) | TypeTag.String.getMask());
}

/// Get the type tag of a pointer value
pub inline fn getTypeTag(value: Pointer) TypeTag {
    return @enumFromInt(value & mask);
}

/// Get a string representation of the pointer's type
pub inline fn getType(value: Pointer) []const u8 {
    return switch (getTypeTag(value)) {
        .Bool => "bool",
        .Char => "char",
        .Float => "float",
        .Int => "int",
        .Pointer => "pointer",
        .String => "string",
    };
}

const expect = std.testing.expect;
const expectEqual = std.testing.expectEqual;
const expectError = std.testing.expectError;

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
    defer s.decRef();

    const p = fromString(s);

    try expect(!isBool(p));
    try expect(!isChar(p));
    try expect(!isFloat(p));
    try expect(!isInt(p));
    try expect(!isPointer(p));
    try expect(isString(p));

    try expectEqual(asString(p), s);
}

test "Type Tag Conversions" {
    const boolPtr = fromBool(true);
    const charPtr = fromChar('A');
    const intPtr = fromInt(42);
    const floatPtr = fromFloat(3.14);

    try expectEqual(getTypeTag(boolPtr), TypeTag.Bool);
    try expectEqual(getTypeTag(charPtr), TypeTag.Char);
    try expectEqual(getTypeTag(intPtr), TypeTag.Int);
    try expectEqual(getTypeTag(floatPtr), TypeTag.Float);
}

test "Type String Representations" {
    const boolPtr = fromBool(true);
    const charPtr = fromChar('A');
    const intPtr = fromInt(42);
    const floatPtr = fromFloat(3.14);

    try expectEqual(getType(boolPtr), "bool");
    try expectEqual(getType(charPtr), "char");
    try expectEqual(getType(intPtr), "int");
    try expectEqual(getType(floatPtr), "float");
}

test "Bool Conversions" {
    const truePtr = fromBool(true);
    const falsePtr = fromBool(false);

    try expect(asBool(truePtr));
    try expect(!asBool(falsePtr));
    try expectEqual(getType(truePtr), "bool");
}

test "Char Edge Cases" {
    const nullChar = fromChar(0);
    const maxChar = fromChar(MAX_CHAR_VALUE);

    try expectEqual(asChar(nullChar), 0);
    try expectEqual(asChar(maxChar), MAX_CHAR_VALUE);
}

test "Float Special Values" {
    const zero = fromFloat(0.0);
    const inf = fromFloat(std.math.inf(f32));
    const nan = fromFloat(std.math.nan(f32));

    try expectEqual(asFloat(zero), 0.0);
    try expect(std.math.isInf(asFloat(inf)));
    try expect(std.math.isNan(asFloat(nan)));
}

test "Pointer Alignment" {
    const allocator = std.heap.page_allocator;
    const data = try allocator.alloc(u8, 8);
    defer allocator.free(data);

    const ptr = fromPointer([*]u8, data.ptr);
    try expect(isPointer(ptr));

    // Verify that the pointer alignment is preserved
    const recovered = asPointer([*]u8, ptr);
    try expectEqual(@intFromPtr(data.ptr), @intFromPtr(recovered));
}

test "String Pool Integration" {
    var pool = SP.StringPool.init(std.heap.page_allocator);
    defer SP.StringPool.deinit(&pool);

    // Test empty string
    const emptyStr = try pool.intern("");
    defer emptyStr.decRef();
    const emptyPtr = fromString(emptyStr);
    try expectEqual(asString(emptyPtr).len(), 0);

    // Test long string
    const longStr = try pool.intern("This is a somewhat longer string to test with");
    defer longStr.decRef();
    const longPtr = fromString(longStr);
    try expectEqual(asString(longPtr).len(), 45);

    // Test string with special characters
    const specialStr = try pool.intern("Hello\n\t\r世界");
    defer specialStr.decRef();
    const specialPtr = fromString(specialStr);
    try expect(isString(specialPtr));
}
