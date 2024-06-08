const std = @import("std");

const AST = @import("./ast.zig");
const TokenKind = @import("./token_kind.zig").TokenKind;

pub const ParserErrors = error{ FunctionValueExpectedError, LexicalError, LiteralIntError, LiteralFloatError, SyntaxError, OutOfMemory, NotYetImplemented };

pub const Location = struct {
    offset: usize,
    line: usize,
    column: usize,
};

pub const LocationRange = struct {
    from: Location,
    to: Location,
};

pub const FunctionValueExpectedError = struct {};

pub const LexicalError = struct {
    lexeme: []u8,

    pub fn deinit(self: LexicalError, allocator: std.mem.Allocator) void {
        allocator.free(self.lexeme);
    }
};

pub const ParserError = struct {
    lexeme: []const u8,
    expected: []const TokenKind,

    pub fn deinit(self: ParserError, allocator: std.mem.Allocator) void {
        allocator.free(self.lexeme);
        allocator.free(self.expected);
    }
};

pub const Error = struct {
    allocator: std.mem.Allocator,
    locationRange: LocationRange,
    detail: ErrorDetail,

    pub fn init(allocator: std.mem.Allocator, locationRange: LocationRange, detail: ErrorDetail) !Error {
        return Error{ .allocator = allocator, .locationRange = locationRange, .detail = detail };
    }

    pub fn deinit(self: *Error) void {
        self.detail.deinit(self.allocator);
    }

    pub fn toString(self: *Error, allocator: std.mem.Allocator) ![]u8 {
        var buffer = std.ArrayList(u8).init(allocator);
        defer buffer.deinit();

        const writer = buffer.writer();

        try writer.print("{d}:", .{self.locationRange.from.line});
        if (self.locationRange.from.line != self.locationRange.to.line) {
            try writer.print("{d}-{d}:{d}: ", .{ self.locationRange.from.column, self.locationRange.to.line, self.locationRange.to.column - 1 });
        } else if (self.locationRange.from.column != self.locationRange.to.column - 1) {
            try writer.print("{d}-{d}: ", .{ self.locationRange.from.column, self.locationRange.to.column - 1 });
        } else {
            try writer.print("{d}: ", .{self.locationRange.from.column});
        }

        switch (self.detail) {
            .FunctionValueExpectedKind => try buffer.appendSlice("Function value expected"),
            .LexicalKind => try writer.print("Lexical error: {s}", .{self.detail.LexicalKind.lexeme}),
            .LiteralFloatOverflowKind => try writer.print("Literal float overflow: {s}", .{self.detail.LiteralFloatOverflowKind.lexeme}),
            .LiteralIntOverflowKind => try writer.print("Literal int overflow: {s}", .{self.detail.LiteralIntOverflowKind.lexeme}),
            .ParserKind => {
                try writer.print("Syntax error: Found \"{s}\", expected one of ", .{self.detail.ParserKind.lexeme});
                for (self.detail.ParserKind.expected, 0..) |expected, idx| {
                    if (idx > 0) {
                        try buffer.appendSlice(", ");
                    }
                    try buffer.appendSlice(expected.toString());
                }
            },
        }

        return buffer.toOwnedSlice();
    }
};

pub const ErrorKind = enum {
    FunctionValueExpectedKind,
    LexicalKind,
    LiteralFloatOverflowKind,
    LiteralIntOverflowKind,
    ParserKind,
};

pub const ErrorDetail = union(ErrorKind) {
    FunctionValueExpectedKind: FunctionValueExpectedError,
    LexicalKind: LexicalError,
    LiteralFloatOverflowKind: LexicalError,
    LiteralIntOverflowKind: LexicalError,
    ParserKind: ParserError,

    pub fn deinit(self: ErrorDetail, allocator: std.mem.Allocator) void {
        switch (self) {
            .FunctionValueExpectedKind => {},
            .LexicalKind => self.LexicalKind.deinit(allocator),
            .LiteralFloatOverflowKind => self.LiteralFloatOverflowKind.deinit(allocator),
            .LiteralIntOverflowKind => self.LiteralIntOverflowKind.deinit(allocator),
            .ParserKind => self.ParserKind.deinit(allocator),
        }
    }
};

pub fn functionValueExpectedError(allocator: std.mem.Allocator, locationRange: LocationRange) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .FunctionValueExpectedKind = .{} });
}

pub fn lexicalError(allocator: std.mem.Allocator, locationRange: LocationRange, lexeme: []const u8) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .LexicalKind = .{
        .lexeme = try allocator.dupe(u8, lexeme),
    } });
}

pub fn literalFloatOverflowError(allocator: std.mem.Allocator, locationRange: LocationRange, lexeme: []const u8) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .LiteralFloatOverflowKind = .{
        .lexeme = try allocator.dupe(u8, lexeme),
    } });
}

pub fn literalIntOverflowError(allocator: std.mem.Allocator, locationRange: LocationRange, lexeme: []const u8) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .LiteralIntOverflowKind = .{
        .lexeme = try allocator.dupe(u8, lexeme),
    } });
}

pub fn parserError(allocator: std.mem.Allocator, locationRange: LocationRange, lexeme: []const u8, expected: []const TokenKind) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .ParserKind = .{
        .lexeme = try allocator.dupe(u8, lexeme),
        .expected = try allocator.dupe(TokenKind, expected),
    } });
}
