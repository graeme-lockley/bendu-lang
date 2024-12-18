const std = @import("std");

const Memory = @import("memory.zig");
const Pointer = @import("pointer.zig");
const SP = @import("string_pool.zig");

const stdout = std.io.getStdOut().writer();

const MAINTAIN_FREE_CHAIN = true;
const INITIAL_HEAP_SIZE = 1;
const HEAP_GROW_THRESHOLD = 0.25;

pub const Runtime = struct {
    allocator: std.mem.Allocator,
    sp: SP.StringPool,
    stack: std.ArrayList(Pointer.Pointer),
    colour: Memory.Colour,
    root: ?*Memory.Value,
    free: ?*Memory.Value,
    memory_size: u32,
    memory_capacity: u32,
    allocations: u32,

    pub fn init(allocator: std.mem.Allocator) !Runtime {
        var result = Runtime{
            .allocator = allocator,
            .sp = SP.StringPool.init(allocator),
            .stack = std.ArrayList(Pointer.Pointer).init(allocator),
            .colour = Memory.Colour.White,
            .root = null,
            .free = null,
            .memory_size = 0,
            .memory_capacity = INITIAL_HEAP_SIZE,
            .allocations = 0,
        };

        _ = try result.push_frame(null);

        return result;
    }

    pub fn deinit(self: *Runtime) void {
        while (self.stack.items.len > 0) {
            self.discard();
        }

        var number_of_values: u32 = 0;
        {
            var runner: ?*Memory.Value = self.root;
            while (runner != null) {
                const next = runner.?.next;
                number_of_values += 1;

                runner.?.deinit(self.allocator);
                self.allocator.destroy(runner.?);

                runner = next;
            }
        }
        // std.log.info("gc: memory state stack length: values: {d} vs {d}", .{ self.memory_size, number_of_values });

        if (MAINTAIN_FREE_CHAIN) {
            self.destroyFreeList();
        }

        self.stack.deinit();
        self.sp.deinit();
    }

    fn destroyFreeList(self: *Runtime) void {
        var runner: ?*Memory.Value = self.free;
        while (runner != null) {
            const next = runner.?.next;
            self.allocator.destroy(runner.?);
            runner = next;
        }
        self.free = null;
    }

    fn pushNewValue(self: *Runtime, vv: Memory.ValueValue) !*Memory.Value {
        const v = if (self.free == null) try self.allocator.create(Memory.Value) else self.nextFreeValue();
        self.memory_size += 1;
        self.allocations += 1;

        v.colour = self.colour;
        v.v = vv;
        v.next = self.root;

        self.root = v;

        // TODO - FIX THIS
        try self.stack.append(@as(Pointer.Pointer, @intFromPtr(v)));

        return v;
    }

    inline fn nextFreeValue(self: *Runtime) *Memory.Value {
        const v: *Memory.Value = self.free.?;
        self.free = v.next;

        return v;
    }

    pub inline fn pop(self: *Runtime) Pointer.Pointer {
        return self.stack.pop();
    }

    pub inline fn popN(self: *Runtime, n: usize) void {
        for (0..n) |_| {
            _ = self.pop();
        }
    }

    pub inline fn peekN(self: *Runtime, n: usize) Pointer.Pointer {
        return self.stack.items[self.stack.items.len - 1 - n];
    }

    pub inline fn peek(self: *Runtime) Pointer.Pointer {
        return self.peekN(0);
    }

    pub inline fn push(self: *Runtime, value: Pointer.Pointer) !void {
        try self.stack.append(value);
    }

    pub inline fn push_array(self: *Runtime, arity: usize) !void {
        const array = try self.pushNewValue(Memory.ValueValue{ .ArrayKind = try Memory.ArrayValue.init(self.allocator, arity) });

        self.discard();

        for (0..arity) |i| {
            try array.v.ArrayKind.values.append(self.peekN(arity - i - 1));
        }
        self.popN(arity);

        try self.stack.append(Pointer.fromPointer(*Memory.Value, array));
    }

    pub inline fn push_array_element(self: *Runtime) !void {
        const index = Pointer.asInt(self.pop());
        const array = Pointer.as(*Memory.Value, self.pop());

        if (index < 0 or index >= array.v.ArrayKind.len()) {
            try stdout.print("Error: Index out of bounds: index: {d}, length: {d}\n", .{ index, array.v.ArrayKind.len() });
            std.posix.exit(1);
        }

        const element = array.v.ArrayKind.at(@intCast(index));
        if (Pointer.isString(element)) {
            Pointer.asString(element).incRef();
        }

        try self.stack.append(element);
    }

    pub inline fn push_array_range(self: *Runtime) !void {
        const end = Pointer.asInt(self.peek());
        const start = Pointer.asInt(self.peekN(1));
        const array = Pointer.as(*Memory.Value, self.peekN(2));

        const arrayLen = array.v.ArrayKind.len();

        const aStart: usize = if (start < 0) 0 else if (start >= arrayLen) arrayLen else @intCast(start);
        const aEnd: usize = if (end < aStart) aStart else if (end >= arrayLen) arrayLen else @intCast(end);

        const range = try self.pushNewValue(Memory.ValueValue{ .ArrayKind = try Memory.ArrayValue.init(self.allocator, aEnd - aStart + 1) });

        for (aStart..aEnd) |i| {
            const v = array.v.ArrayKind.at(i);
            if (Pointer.isString(v)) {
                Pointer.asString(v).incRef();
            }
            try range.v.ArrayKind.values.append(v);
        }

        self.popN(4);
        try self.push(Pointer.fromPointer(*Memory.Value, range));
    }

    pub inline fn push_array_range_from(self: *Runtime) !void {
        const start = Pointer.asInt(self.peek());
        const array = Pointer.as(*Memory.Value, self.peekN(1));

        const arrayLen = array.v.ArrayKind.len();

        const aStart: usize = if (start < 0) 0 else if (start >= arrayLen) arrayLen else @intCast(start);
        const aEnd: usize = arrayLen;

        const range = try self.pushNewValue(Memory.ValueValue{ .ArrayKind = try Memory.ArrayValue.init(self.allocator, aEnd - aStart + 1) });

        for (aStart..aEnd) |i| {
            const v = array.v.ArrayKind.at(i);
            if (Pointer.isString(v)) {
                Pointer.asString(v).incRef();
            }
            try range.v.ArrayKind.values.append(v);
        }

        self.popN(3);
        try self.push(Pointer.fromPointer(*Memory.Value, range));
    }

    pub inline fn push_array_range_to(self: *Runtime) !void {
        const end = Pointer.asInt(self.peek());
        const array = Pointer.as(*Memory.Value, self.peekN(1));

        const arrayLen = array.v.ArrayKind.len();

        const aStart: usize = 0;
        const aEnd: usize = if (end < aStart) aStart else if (end >= arrayLen) arrayLen else @intCast(end);

        const range = try self.pushNewValue(Memory.ValueValue{ .ArrayKind = try Memory.ArrayValue.init(self.allocator, aEnd - aStart + 1) });

        for (aStart..aEnd) |i| {
            const v = array.v.ArrayKind.at(i);
            if (Pointer.isString(v)) {
                Pointer.asString(v).incRef();
            }
            try range.v.ArrayKind.values.append(v);
        }

        self.popN(3);
        try self.push(Pointer.fromPointer(*Memory.Value, range));
    }

    pub inline fn push_bool_true(self: *Runtime) !void {
        try self.stack.append(Pointer.fromInt(1));
    }

    pub inline fn push_bool_false(self: *Runtime) !void {
        try self.stack.append(Pointer.fromInt(0));
    }

    pub inline fn push_closure(self: *Runtime, function: usize, frame: *Memory.Value) !void {
        _ = try self.pushNewValue(Memory.ValueValue{ .ClosureKind = Memory.ClosureValue.init(function, frame) });
    }

    pub inline fn push_f32_literal(self: *Runtime, value: f32) !void {
        try self.stack.append(Pointer.fromFloat(value));
    }

    pub inline fn push_frame(self: *Runtime, previousFrame: ?*Memory.Value) !*Memory.Value {
        return try self.pushNewValue(Memory.ValueValue{ .FrameKind = try Memory.FrameValue.init(self.allocator, previousFrame) });
    }

    pub inline fn push_i32_literal(self: *Runtime, value: i32) !void {
        try self.stack.append(Pointer.fromInt(value));
    }

    pub inline fn push_u8_literal(self: *Runtime, value: u8) !void {
        try self.stack.append(Pointer.fromChar(value));
    }

    pub inline fn push_string_literal(self: *Runtime, value: []const u8) !void {
        try self.stack.append(Pointer.fromString(try self.sp.intern(value)));
    }

    pub inline fn push_tuple(self: *Runtime, arity: usize) !void {
        const tuple = try self.pushNewValue(Memory.ValueValue{ .TupleKind = try Memory.TupleValue.init(self.allocator, arity) });

        self.discard();

        for (0..arity) |i| {
            tuple.v.TupleKind.values[arity - i - 1] = self.pop();
        }

        try self.stack.append(Pointer.fromPointer(*Memory.Value, tuple));
    }

    pub inline fn push_tuple_component(self: *Runtime, index: usize) !void {
        const tuple = Pointer.as(*Memory.Value, self.peek());
        const component = tuple.v.TupleKind.values[index];

        if (Pointer.isString(component)) {
            Pointer.asString(component).incRef();
        }

        self.discard();
        try self.stack.append(component);
    }

    pub inline fn push_unit_literal(self: *Runtime) !void {
        try self.stack.append(Pointer.fromInt(0));
    }

    pub inline fn push_stack(self: *Runtime, index: i32) !void {
        const value = self.stack.items[@intCast(index)];

        if (Pointer.isString(value)) {
            Pointer.asString(value).incRef();
        }

        try self.stack.append(value);
    }

    pub inline fn load(self: *Runtime, fp: usize, frame: usize, offset: usize) !void {
        const f = @as(*Memory.Value, @ptrFromInt(self.stack.items[fp]));
        const v = Memory.FrameValue.get(f, frame, offset);

        if (Pointer.isString(v)) {
            Pointer.asString(v).incRef();
        }

        try self.stack.append(v);
    }

    pub inline fn store(self: *Runtime, fp: usize, frame: usize, offset: usize) !void {
        const f = @as(*Memory.Value, @ptrFromInt(self.stack.items[fp]));
        const v = self.pop();

        try Memory.FrameValue.set(f, frame, offset, v);
    }

    pub inline fn duplicate(self: *Runtime) !void {
        const value = self.stack.items[self.stack.items.len - 1];

        if (Pointer.isString(value)) {
            Pointer.asString(value).incRef();
        }

        try self.stack.append(value);
    }

    pub inline fn discard(self: *Runtime) void {
        const value = self.stack.pop();

        if (Pointer.isString(value)) {
            Pointer.asString(value).decRef();
        }
    }

    pub inline fn not_bool(self: *Runtime) !void {
        const value = Pointer.asBool(self.pop());
        try self.stack.append(Pointer.fromBool(!value));
    }

    pub inline fn add_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromFloat(a + b));
    }
    pub inline fn add_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a + b));
    }
    pub inline fn add_string(self: *Runtime) !void {
        const b = Pointer.asString(self.pop());
        const a = Pointer.asString(self.pop());
        defer b.decRef();
        defer a.decRef();

        try self.stack.append(Pointer.fromString(try self.sp.concat(a, b)));
    }
    pub inline fn add_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromChar(a + b));
    }

    pub inline fn sub_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromFloat(a - b));
    }
    pub inline fn sub_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a - b));
    }
    pub inline fn sub_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromChar(a - b));
    }

    pub inline fn mul_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromFloat(a * b));
    }
    pub inline fn mul_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(a * b));
    }
    pub inline fn mul_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromChar(a * b));
    }

    pub inline fn div_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());

        if (b == 0.0) {
            try stdout.print("Error: Attempt to divide by zero\n", .{});
            std.posix.exit(1);
        }

        try self.stack.append(Pointer.fromFloat(a / b));
    }
    pub inline fn div_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());

        if (b == 0) {
            try stdout.print("Error: Attempt to divide by zero\n", .{});
            std.posix.exit(1);
        }

        try self.stack.append(Pointer.fromInt(@divTrunc(a, b)));
    }
    pub inline fn div_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());

        if (b == 0) {
            try stdout.print("Error: Attempt to divide by zero\n", .{});
            std.posix.exit(1);
        }

        try self.stack.append(Pointer.fromChar(a / b));
    }

    pub inline fn mod_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());

        if (b == 0) {
            try stdout.print("Error: Attempt to divide by zero\n", .{});
            std.posix.exit(1);
        }

        try self.stack.append(Pointer.fromInt(@mod(a, b)));
    }

    pub inline fn pow_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromFloat(std.math.pow(f32, a, b)));
    }
    pub inline fn pow_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromInt(std.math.pow(i32, a, b)));
    }

    pub inline fn eq(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();
        try self.stack.append(Pointer.fromBool(@import("./runtime/eq.zig").eq(a, b)));

        if (Pointer.isString(b)) {
            Pointer.asString(b).decRef();
        }
        if (Pointer.isString(a)) {
            Pointer.asString(a).decRef();
        }
    }
    pub inline fn eq_bool(self: *Runtime) !void {
        const b = Pointer.asBool(self.pop());
        const a = Pointer.asBool(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }
    pub inline fn eq_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }
    pub inline fn eq_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }
    pub inline fn eq_string(self: *Runtime) !void {
        const b = Pointer.asString(self.pop());
        const a = Pointer.asString(self.pop());
        defer b.decRef();
        defer a.decRef();

        try self.stack.append(Pointer.fromBool(a == b));
    }
    pub inline fn eq_unit(self: *Runtime) !void {
        self.discard();
        self.discard();
        try self.stack.append(Pointer.fromBool(true));
    }
    pub inline fn eq_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a == b));
    }

    pub inline fn neq(self: *Runtime) !void {
        const b = self.pop();
        const a = self.pop();
        try self.stack.append(Pointer.fromBool(@import("./runtime/neq.zig").neq(a, b)));

        if (Pointer.isString(b)) {
            Pointer.asString(b).decRef();
        }
        if (Pointer.isString(a)) {
            Pointer.asString(a).decRef();
        }
    }
    pub inline fn neq_bool(self: *Runtime) !void {
        const b = Pointer.asBool(self.pop());
        const a = Pointer.asBool(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_string(self: *Runtime) !void {
        const b = Pointer.asString(self.pop());
        const a = Pointer.asString(self.pop());
        defer b.decRef();
        defer a.decRef();

        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a != b));
    }
    pub inline fn neq_unit(self: *Runtime) !void {
        self.discard();
        self.discard();
        try self.stack.append(Pointer.fromBool(false));
    }

    pub inline fn lt_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a < b));
    }
    pub inline fn lt_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a < b));
    }
    pub inline fn lt_string(self: *Runtime) !void {
        const b = Pointer.asString(self.pop());
        const a = Pointer.asString(self.pop());
        defer b.decRef();
        defer a.decRef();

        try self.stack.append(Pointer.fromBool(std.mem.lessThan(u8, a.slice(), b.slice())));
    }
    pub inline fn lt_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a < b));
    }

    pub inline fn le_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a <= b));
    }
    pub inline fn le_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a <= b));
    }
    pub inline fn le_string(self: *Runtime) !void {
        const b = Pointer.asString(self.pop());
        const a = Pointer.asString(self.pop());
        defer b.decRef();
        defer a.decRef();

        if (a == b) {
            try self.stack.append(Pointer.fromBool(true));
        } else {
            try self.stack.append(Pointer.fromBool(std.mem.lessThan(u8, a.slice(), b.slice())));
        }
    }
    pub inline fn le_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a <= b));
    }

    pub inline fn gt_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a > b));
    }
    pub inline fn gt_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a > b));
    }
    pub inline fn gt_string(self: *Runtime) !void {
        const b = Pointer.asString(self.pop());
        const a = Pointer.asString(self.pop());
        defer b.decRef();
        defer a.decRef();

        try self.stack.append(Pointer.fromBool(std.mem.lessThan(u8, b.slice(), a.slice())));
    }
    pub inline fn gt_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a > b));
    }

    pub inline fn ge_f32(self: *Runtime) !void {
        const b = Pointer.asFloat(self.pop());
        const a = Pointer.asFloat(self.pop());
        try self.stack.append(Pointer.fromBool(a >= b));
    }
    pub inline fn ge_i32(self: *Runtime) !void {
        const b = Pointer.asInt(self.pop());
        const a = Pointer.asInt(self.pop());
        try self.stack.append(Pointer.fromBool(a >= b));
    }
    pub inline fn ge_string(self: *Runtime) !void {
        const b = Pointer.asString(self.pop());
        const a = Pointer.asString(self.pop());
        defer b.decRef();
        defer a.decRef();

        if (a == b) {
            try self.stack.append(Pointer.fromBool(true));
        } else {
            try self.stack.append(Pointer.fromBool(std.mem.lessThan(u8, b.slice(), a.slice())));
        }
    }
    pub inline fn ge_u8(self: *Runtime) !void {
        const b = Pointer.asChar(self.pop());
        const a = Pointer.asChar(self.pop());
        try self.stack.append(Pointer.fromBool(a >= b));
    }

    pub inline fn print(self: *Runtime) !void {
        const value = self.peek();

        try @import("./runtime/print.zig").print(value);

        self.discard();
    }

    pub inline fn println(self: *Runtime) !void {
        _ = self;

        try stdout.print("\n", .{});
    }

    pub inline fn print_bool(self: *Runtime) !void {
        const value = Pointer.asBool(self.pop());

        try stdout.print("{s}", .{if (value) "True" else "False"});
    }

    pub inline fn print_f32(self: *Runtime) !void {
        const value = Pointer.asFloat(self.pop());

        try stdout.print("{d}", .{value});
    }

    pub inline fn print_i32(self: *Runtime) !void {
        const value = Pointer.asInt(self.pop());

        try stdout.print("{d}", .{value});
    }

    pub inline fn print_u8(self: *Runtime) !void {
        const value = Pointer.asChar(self.pop());

        try stdout.print("{c}", .{value});
    }

    pub inline fn print_string(self: *Runtime) !void {
        const value = Pointer.asString(self.pop());
        defer value.decRef();

        try stdout.print("{s}", .{value.slice()});
    }

    pub inline fn print_unit(self: *Runtime) !void {
        _ = self;

        try stdout.print("()", .{});
    }
};
