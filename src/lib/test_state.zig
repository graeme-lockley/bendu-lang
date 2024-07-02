const std = @import("std");

const AST = @import("../ast.zig");
const Errors = @import("../errors.zig");
const Parser = @import("../parser.zig");
const SP = @import("../lib/string_pool.zig");
const Static = @import("../static.zig");
const Typing = @import("../typing.zig");

pub const TestState = struct {
    gpa: std.heap.GeneralPurposeAllocator(.{}),
    allocator: std.mem.Allocator,
    sp: SP.StringPool,
    errors: Errors.Errors,
    isSetup: bool,

    pub fn init() !TestState {
        var buffer: [100]u8 = undefined;
        var fba = std.heap.FixedBufferAllocator.init(&buffer);
        const tempAllocator = fba.allocator();

        return TestState{
            .gpa = std.heap.GeneralPurposeAllocator(.{}){},
            .allocator = tempAllocator,
            .sp = SP.StringPool.init(tempAllocator),
            .errors = try Errors.Errors.init(tempAllocator),
            .isSetup = false,
        };
    }

    pub fn setup(self: *TestState) *TestState {
        if (self.isSetup) {
            return self;
        }

        self.gpa = std.heap.GeneralPurposeAllocator(.{}){};
        self.allocator = self.gpa.allocator();
        self.sp = SP.StringPool.init(self.allocator);
        self.errors = try Errors.Errors.init(self.allocator);
        self.isSetup = true;

        return self;
    }

    pub fn deinit(self: *TestState) void {
        _ = self.setup();

        self.errors.deinit();
        self.sp.deinit();

        _ = self.gpa.deinit();
    }

    pub fn parseAnalyse(self: *TestState, source: []const u8) !?*Typing.Type {
        _ = self.setup();

        const ast = try Parser.parse(&self.sp, "script.bendu", source, &self.errors);

        if (ast) |a| {
            defer a.destroy(self.allocator);

            return try Static.package(a, &self.sp, &self.errors);
        } else {
            return null;
        }
    }

    pub fn debugPrintErrors(self: *TestState) !void {
        _ = self.setup();

        if (self.errors.hasErrors()) {
            std.debug.print("--- Errors ------------\n", .{});
            for (self.errors.items.items) |*err| {
                const str = try err.toString(self.allocator);
                defer self.allocator.free(str);

                std.debug.print("{s}\n", .{str});
            }
            std.debug.print("-----------------------\n", .{});
        }
    }

    pub fn expectTypeString(self: *TestState, typ: *Typing.Type, name: []const u8) !void {
        _ = self.setup();

        const typeString = try typ.toString(self.allocator);
        defer self.allocator.free(typeString);

        try std.testing.expectEqualSlices(u8, name, typeString);
    }
};
