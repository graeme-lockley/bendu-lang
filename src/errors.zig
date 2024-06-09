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

pub const DuplicateDeclarationError = struct {
    name: []const u8,

    pub fn deinit(self: DuplicateDeclarationError, allocator: std.mem.Allocator) void {
        allocator.free(self.name);
    }
};

pub const FunctionValueExpectedError = struct {};

pub const LexicalError = struct {
    lexeme: []const u8,

    pub fn deinit(self: LexicalError, allocator: std.mem.Allocator) void {
        allocator.free(self.lexeme);
    }
};

pub const UndefinedNameError = struct {
    name: []const u8,

    pub fn deinit(self: UndefinedNameError, allocator: std.mem.Allocator) void {
        allocator.free(self.name);
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
            .DuplicateDeclarationKind => try writer.print("Duplicate declaration attempt: {s}", .{self.detail.DuplicateDeclarationKind.name}),
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
            .UndefinedNameKind => try writer.print("Unknown name: {s}", .{self.detail.UndefinedNameKind.name}),
        }

        return buffer.toOwnedSlice();
    }
};

pub const ErrorKind = enum {
    DuplicateDeclarationKind,
    FunctionValueExpectedKind,
    LexicalKind,
    LiteralFloatOverflowKind,
    LiteralIntOverflowKind,
    ParserKind,
    UndefinedNameKind,
};

pub const ErrorDetail = union(ErrorKind) {
    DuplicateDeclarationKind: DuplicateDeclarationError,
    FunctionValueExpectedKind: FunctionValueExpectedError,
    LexicalKind: LexicalError,
    LiteralFloatOverflowKind: LexicalError,
    LiteralIntOverflowKind: LexicalError,
    ParserKind: ParserError,
    UndefinedNameKind: UndefinedNameError,

    pub fn deinit(self: ErrorDetail, allocator: std.mem.Allocator) void {
        switch (self) {
            .DuplicateDeclarationKind => self.DuplicateDeclarationKind.deinit(allocator),
            .FunctionValueExpectedKind => {},
            .LexicalKind => self.LexicalKind.deinit(allocator),
            .LiteralFloatOverflowKind => self.LiteralFloatOverflowKind.deinit(allocator),
            .LiteralIntOverflowKind => self.LiteralIntOverflowKind.deinit(allocator),
            .ParserKind => self.ParserKind.deinit(allocator),
            .UndefinedNameKind => self.UndefinedNameKind.deinit(allocator),
        }
    }
};

pub fn duplicateDeclarationError(allocator: std.mem.Allocator, locationRange: LocationRange, name: []const u8) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .DuplicateDeclarationKind = .{
        .name = try allocator.dupe(u8, name),
    } });
}

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

pub fn undefinedNameError(allocator: std.mem.Allocator, locationRange: LocationRange, name: []const u8) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .UndefinedNameKind = .{
        .name = try allocator.dupe(u8, name),
    } });
}
