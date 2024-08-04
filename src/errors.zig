const std = @import("std");

const AST = @import("ast.zig");
const TokenKind = @import("token_kind.zig").TokenKind;
const Typing = @import("typing.zig");

pub const ParserErrors = error{ FunctionValueExpectedError, LexicalError, LiteralIntError, LiteralFloatError, SyntaxError, OutOfMemory, NotYetImplemented };

pub const Location = struct {
    offset: usize,
    line: usize,
    column: usize,
};

pub const LocationRange = struct {
    from: Location,
    to: Location,

    pub fn write(self: LocationRange, wr: anytype) !void {
        try wr.print("{d}:", .{self.from.line});
        if (self.from.line != self.to.line) {
            try wr.print("{d}-{d}:{d}", .{ self.from.column, self.to.line, self.to.column - 1 });
        } else if (self.from.column != self.to.column - 1) {
            try wr.print("{d}-{d}", .{ self.from.column, self.to.column - 1 });
        } else {
            try wr.print("{d}", .{self.from.column});
        }
    }
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

pub const NotImplementedError = struct {
    explanation: []const u8,

    pub fn deinit(self: NotImplementedError, allocator: std.mem.Allocator) void {
        allocator.free(self.explanation);
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

pub const UndefinedNameError = struct {
    name: []const u8,

    pub fn deinit(self: UndefinedNameError, allocator: std.mem.Allocator) void {
        allocator.free(self.name);
    }
};

pub const UndefinedOperatorError = struct {
    name: []const u8,

    pub fn deinit(self: UndefinedOperatorError, allocator: std.mem.Allocator) void {
        allocator.free(self.name);
    }
};

pub const UnificationError = struct {
    t1: *Typing.Type,
    t2: *Typing.Type,

    pub fn deinit(self: UnificationError, allocator: std.mem.Allocator) void {
        self.t1.decRef(allocator);
        self.t2.decRef(allocator);
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

        try self.locationRange.write(writer);
        try writer.print(": ", .{});

        switch (self.detail) {
            .DuplicateDeclarationKind => try writer.print("Duplicate declaration: {s}", .{self.detail.DuplicateDeclarationKind.name}),
            .FunctionValueExpectedKind => try buffer.appendSlice("Function value expected"),
            .LexicalKind => try writer.print("Lexical error: {s}", .{self.detail.LexicalKind.lexeme}),
            .LiteralFloatOverflowKind => try writer.print("Literal float overflow: {s}", .{self.detail.LiteralFloatOverflowKind.lexeme}),
            .LiteralIntOverflowKind => try writer.print("Literal int overflow: {s}", .{self.detail.LiteralIntOverflowKind.lexeme}),
            .NotImplementedKind => try writer.print("Not implemented: {s}", .{self.detail.NotImplementedKind.explanation}),
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
            .UndefinedOperatorKind => try writer.print("Unknown operator: {s}", .{self.detail.UndefinedOperatorKind.name}),
            .UnificationKind => {
                const s1 = try self.detail.UnificationKind.t1.toString(allocator);
                defer allocator.free(s1);
                const s2 = try self.detail.UnificationKind.t2.toString(allocator);
                defer allocator.free(s2);

                try writer.print("Unification error: Unable to unify {s} with {s}", .{ s1, s2 });
            },
        }

        return buffer.toOwnedSlice();
    }
};

pub const Errors = struct {
    items: std.ArrayList(Error),

    pub fn init(allocator: std.mem.Allocator) !Errors {
        return Errors{ .items = std.ArrayList(Error).init(allocator) };
    }

    pub fn deinit(self: Errors) void {
        for (self.items.items) |*item| {
            item.deinit();
        }
        self.items.deinit();
    }

    pub fn append(self: *Errors, err: Error) !void {
        try self.items.append(err);
    }

    pub fn hasErrors(self: *Errors) bool {
        return self.items.items.len > 0;
    }

    pub fn debugPrintErrors(self: *Errors) !void {
        if (self.hasErrors()) {
            std.debug.print("--- Errors ------------\n", .{});
            for (self.items.items) |*err| {
                const str = try err.toString(self.items.allocator);
                defer self.items.allocator.free(str);

                std.debug.print("{s}\n", .{str});
            }
            std.debug.print("-----------------------\n", .{});
        }
    }
};

pub const ErrorKind = enum {
    DuplicateDeclarationKind,
    FunctionValueExpectedKind,
    LexicalKind,
    LiteralFloatOverflowKind,
    LiteralIntOverflowKind,
    NotImplementedKind,
    ParserKind,
    UndefinedNameKind,
    UndefinedOperatorKind,
    UnificationKind,
};

pub const ErrorDetail = union(ErrorKind) {
    DuplicateDeclarationKind: DuplicateDeclarationError,
    FunctionValueExpectedKind: FunctionValueExpectedError,
    LexicalKind: LexicalError,
    LiteralFloatOverflowKind: LexicalError,
    LiteralIntOverflowKind: LexicalError,
    NotImplementedKind: NotImplementedError,
    ParserKind: ParserError,
    UndefinedNameKind: UndefinedNameError,
    UndefinedOperatorKind: UndefinedOperatorError,
    UnificationKind: UnificationError,

    pub fn deinit(self: ErrorDetail, allocator: std.mem.Allocator) void {
        switch (self) {
            .DuplicateDeclarationKind => self.DuplicateDeclarationKind.deinit(allocator),
            .FunctionValueExpectedKind => {},
            .LexicalKind => self.LexicalKind.deinit(allocator),
            .LiteralFloatOverflowKind => self.LiteralFloatOverflowKind.deinit(allocator),
            .LiteralIntOverflowKind => self.LiteralIntOverflowKind.deinit(allocator),
            .NotImplementedKind => self.NotImplementedKind.deinit(allocator),
            .ParserKind => self.ParserKind.deinit(allocator),
            .UndefinedNameKind => self.UndefinedNameKind.deinit(allocator),
            .UndefinedOperatorKind => self.UndefinedOperatorKind.deinit(allocator),
            .UnificationKind => self.UnificationKind.deinit(allocator),
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

pub fn notImplementedError(allocator: std.mem.Allocator, locationRange: LocationRange, explanation: []const u8) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .NotImplementedKind = .{
        .explanation = try allocator.dupe(u8, explanation),
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

pub fn undefinedOperatorError(allocator: std.mem.Allocator, locationRange: LocationRange, name: []const u8) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .UndefinedOperatorKind = .{
        .name = try allocator.dupe(u8, name),
    } });
}

pub fn unificationError(allocator: std.mem.Allocator, locationRange: LocationRange, t1: *Typing.Type, t2: *Typing.Type) !Error {
    return try Error.init(allocator, locationRange, ErrorDetail{ .UnificationKind = .{
        .t1 = t1.incRefR(),
        .t2 = t2.incRefR(),
    } });
}
