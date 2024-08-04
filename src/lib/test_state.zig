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

            var env = try Static.Env.init(&self.sp, &self.errors);
            defer env.deinit();

            try Static.package(a, &env);

            return if (a.exprs.len == 0 or a.exprs[a.exprs.len - 1].type == null) null else a.exprs[a.exprs.len - 1].type.?.incRefR();
        } else {
            return null;
        }
    }

    fn expectSchemeString(self: *TestState, source: []const u8, schemeString: []const u8) !void {
        _ = self.setup();

        const ast = try Parser.parse(&self.sp, "script.bendu", source, &self.errors);

        try self.debugPrintErrors();
        try std.testing.expect(!self.errors.hasErrors());

        if (ast) |a| {
            defer a.destroy(self.allocator);

            var env = try Static.Env.init(&self.sp, &self.errors);
            defer env.deinit();

            try Static.package(a, &env);

            try self.debugPrintErrors();
            try std.testing.expect(!self.errors.hasErrors());

            const lastExpr = a.exprs[a.exprs.len - 1];

            switch (lastExpr.kind) {
                .declarations => {
                    const s = try lastExpr.kind.declarations[0].IdDeclaration.scheme.?.toString(self.allocator);
                    defer self.allocator.free(s);

                    try std.testing.expectEqualStrings(schemeString, s);
                },
                else => try self.expectTypeString(lastExpr.type.?, schemeString),
            }
        }
    }

    pub fn debugPrintErrors(self: *TestState) !void {
        _ = self.setup();

        try self.errors.debugPrintErrors();
    }

    fn expectTypeString(self: *TestState, typ: *Typing.Type, name: []const u8) !void {
        _ = self.setup();

        const typeString = try typ.toString(self.allocator);
        defer self.allocator.free(typeString);

        try std.testing.expectEqualStrings(name, typeString);
    }
};

pub fn expectSchemeString(source: []const u8, schemeString: []const u8) !void {
    var state = try TestState.init();
    defer state.deinit();

    try state.expectSchemeString(source, schemeString);
}
