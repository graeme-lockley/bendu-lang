const std = @import("std");

const Errors = @import("./errors.zig");
pub const TokenKind = @import("./token_kind.zig").TokenKind;

pub const Token = struct {
    kind: TokenKind,
    locationRange: Errors.LocationRange,
};

const keywords = std.StaticStringMap(TokenKind).initComptime(.{
    .{ "catch", TokenKind.Catch },
    .{ "false", TokenKind.LiteralBoolFalse },
    .{ "fn", TokenKind.Fn },
    .{ "if", TokenKind.If },
    .{ "let", TokenKind.Let },
    .{ "match", TokenKind.Match },
    .{ "raise", TokenKind.Raise },
    .{ "true", TokenKind.LiteralBoolTrue },
    .{ "while", TokenKind.While },
});

fn isAlpha(c: u8) bool {
    return (c >= 'a' and c <= 'z') or (c >= 'A' and c <= 'Z') or c == '_';
}

fn isDigit(c: u8) bool {
    return c >= '0' and c <= '9';
}

fn isAlphaDigit(c: u8) bool {
    return isAlpha(c) or isDigit(c);
}

const START_OFFSET = 0;
const START_LINE = 1;
const START_COLUMN = 1;

pub const Lexer = struct {
    allocator: std.mem.Allocator,
    name: []const u8,
    source: []const u8,
    sourceLength: u32,
    location: Errors.Location,

    current: Token,
    err: ?Errors.Error,

    pub fn init(allocator: std.mem.Allocator) Lexer {
        return Lexer{
            .allocator = allocator,
            .name = "undefined",
            .source = "",
            .sourceLength = 0,
            .location = Errors.Location{ .offset = START_OFFSET, .line = START_LINE, .column = START_COLUMN },
            .current = Token{
                .kind = TokenKind.EOS,
                .locationRange = Errors.LocationRange{
                    .from = Errors.Location{ .offset = START_OFFSET, .line = START_LINE, .column = START_OFFSET },
                    .to = Errors.Location{ .offset = START_OFFSET, .line = START_LINE, .column = START_COLUMN },
                },
            },
            .err = null,
        };
    }

    pub fn initBuffer(self: *Lexer, name: []const u8, source: []const u8) Errors.ParserErrors!void {
        self.name = name;
        self.source = source;
        self.sourceLength = @intCast(source.len);
        self.location = Errors.Location{ .offset = START_OFFSET, .line = START_LINE, .column = START_COLUMN };

        try self.next();
    }

    pub fn lexeme(self: *const Lexer, token: Token) []const u8 {
        return self.source[token.locationRange.from.offset..token.locationRange.to.offset];
    }

    fn atEnd(self: *const Lexer) bool {
        return self.location.offset >= self.sourceLength;
    }

    fn currentCharacter(self: *const Lexer) u8 {
        if (self.atEnd()) {
            return 0;
        }
        return self.source[self.location.offset];
    }

    inline fn skipCharacter(self: *Lexer) void {
        if (!self.atEnd()) {
            if (self.source[self.location.offset] == '\n') {
                self.location.line += 1;
                self.location.column = START_COLUMN;
            } else {
                self.location.column += 1;
            }
            self.location.offset += 1;
        }
    }

    fn reportLexicalError(self: *Lexer, tokenStart: Errors.Location) Errors.ParserErrors!void {
        self.assignToken(TokenKind.Invalid, tokenStart);
        self.replaceErr(try Errors.lexicalError(self.allocator, self.current.locationRange, self.lexeme(self.current)));

        return error.LexicalError;
    }

    pub fn peekNext(self: *Lexer) Errors.ParserErrors!TokenKind {
        const location = self.location;
        const token = self.current;

        try self.next();
        const kind = self.current.kind;
        self.location = location;
        self.current = token;

        return kind;
    }

    pub fn next(self: *Lexer) Errors.ParserErrors!void {
        while (!self.atEnd() and self.currentCharacter() <= ' ') {
            self.skipCharacter();
        }

        while (self.currentCharacter() == '#') {
            self.skipCharacter();
            while (!self.atEnd() and self.currentCharacter() != '\n') {
                self.skipCharacter();
            }
            while (!self.atEnd() and self.currentCharacter() <= ' ') {
                self.skipCharacter();
            }
        }

        if (self.atEnd()) {
            self.assignToken(TokenKind.EOS, self.location);
            return;
        }

        const tokenStart = self.location;
        switch (self.currentCharacter()) {
            'a'...'z', 'A'...'Z', '_' => {
                self.skipCharacter();
                while (isAlphaDigit(self.currentCharacter())) {
                    self.skipCharacter();
                }
                if (self.currentCharacter() == '?' or self.currentCharacter() == '!') {
                    self.skipCharacter();
                }
                while (self.currentCharacter() == '\'') {
                    self.skipCharacter();
                }

                const text = self.source[tokenStart.offset..self.location.offset];

                self.assignToken(keywords.get(text) orelse TokenKind.Identifier, tokenStart);
            },
            '!' => {
                self.skipCharacter();
                if (self.currentCharacter() == '=') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.BangEqual, tokenStart);
                } else {
                    self.assignToken(TokenKind.Bang, tokenStart);
                }
            },
            '@' => self.setSymbolToken(TokenKind.At, tokenStart),
            '?' => self.setSymbolToken(TokenKind.Hook, tokenStart),
            '[' => self.setSymbolToken(TokenKind.LBracket, tokenStart),
            '{' => self.setSymbolToken(TokenKind.LCurly, tokenStart),
            '(' => self.setSymbolToken(TokenKind.LParen, tokenStart),
            ']' => self.setSymbolToken(TokenKind.RBracket, tokenStart),
            '}' => self.setSymbolToken(TokenKind.RCurly, tokenStart),
            ')' => self.setSymbolToken(TokenKind.RParen, tokenStart),
            ',' => self.setSymbolToken(TokenKind.Comma, tokenStart),
            '.' => {
                self.skipCharacter();
                if (self.currentCharacter() == '.') {
                    self.skipCharacter();
                    if (self.currentCharacter() == '.') {
                        self.skipCharacter();
                        self.assignToken(TokenKind.DotDotDot, tokenStart);
                    } else {
                        try self.reportLexicalError(tokenStart);
                    }
                } else {
                    self.assignToken(TokenKind.Dot, tokenStart);
                }
            },
            ':' => {
                self.skipCharacter();
                if (self.currentCharacter() == '=') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.ColonEqual, tokenStart);
                } else {
                    self.assignToken(TokenKind.Colon, tokenStart);
                }
            },
            ';' => self.setSymbolToken(TokenKind.Semicolon, tokenStart),
            '|' => {
                self.skipCharacter();
                if (self.currentCharacter() == '|') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.BarBar, tokenStart);
                } else if (self.currentCharacter() == '>') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.BarGreater, tokenStart);
                } else {
                    self.assignToken(TokenKind.Bar, tokenStart);
                }
            },
            '+' => self.setSymbolToken(TokenKind.Plus, tokenStart),
            '*' => {
                self.skipCharacter();
                if (self.currentCharacter() == '*') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.StarStar, tokenStart);
                } else {
                    self.assignToken(TokenKind.Star, tokenStart);
                }
            },
            '/' => self.setSymbolToken(TokenKind.Slash, tokenStart),
            '%' => self.setSymbolToken(TokenKind.Percentage, tokenStart),
            '=' => {
                self.skipCharacter();
                if (self.currentCharacter() == '=') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.EqualEqual, tokenStart);
                } else {
                    self.assignToken(TokenKind.Equal, tokenStart);
                }
            },
            '<' => {
                self.skipCharacter();
                if (self.currentCharacter() == '=') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.LessEqual, tokenStart);
                } else if (self.currentCharacter() == '|') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.LessBar, tokenStart);
                } else if (self.currentCharacter() == '!') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.LessBang, tokenStart);
                } else if (self.currentCharacter() == '<') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.LessLess, tokenStart);
                } else {
                    self.assignToken(TokenKind.LessThan, tokenStart);
                }
            },
            '>' => {
                self.skipCharacter();
                if (self.currentCharacter() == '=') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.GreaterEqual, tokenStart);
                } else if (self.currentCharacter() == '!') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.GreaterBang, tokenStart);
                } else if (self.currentCharacter() == '>') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.GreaterGreater, tokenStart);
                } else {
                    self.assignToken(TokenKind.GreaterThan, tokenStart);
                }
            },
            '&' => {
                self.skipCharacter();
                if (self.currentCharacter() == '&') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.AmpersandAmpersand, tokenStart);
                } else {
                    try self.reportLexicalError(tokenStart);
                }
            },
            '-' => {
                self.skipCharacter();
                if (self.currentCharacter() == '>') {
                    self.skipCharacter();
                    self.assignToken(TokenKind.MinusGreater, tokenStart);
                } else if (isDigit(self.currentCharacter())) {
                    self.skipCharacter();
                    while (isDigit(self.currentCharacter())) {
                        self.skipCharacter();
                    }

                    if (self.currentCharacter() == '.') {
                        self.skipCharacter();
                        while (isDigit(self.currentCharacter())) {
                            self.skipCharacter();
                        }
                        if (self.currentCharacter() == 'e' or self.currentCharacter() == 'E') {
                            self.skipCharacter();
                            if (self.currentCharacter() == '+' or self.currentCharacter() == '-') {
                                self.skipCharacter();
                            }
                            while (isDigit(self.currentCharacter())) {
                                self.skipCharacter();
                            }
                        }
                        self.assignToken(TokenKind.LiteralFloat, tokenStart);
                    } else {
                        self.assignToken(TokenKind.LiteralInt, tokenStart);
                    }
                } else {
                    self.assignToken(TokenKind.Minus, tokenStart);
                }
            },
            '0'...'9' => {
                self.skipCharacter();
                while (isDigit(self.currentCharacter())) {
                    self.skipCharacter();
                }

                if (self.currentCharacter() == '.') {
                    self.skipCharacter();
                    while (isDigit(self.currentCharacter())) {
                        self.skipCharacter();
                    }
                    if (self.currentCharacter() == 'e' or self.currentCharacter() == 'E') {
                        self.skipCharacter();
                        if (self.currentCharacter() == '+' or self.currentCharacter() == '-') {
                            self.skipCharacter();
                        }
                        while (isDigit(self.currentCharacter())) {
                            self.skipCharacter();
                        }
                    }
                    self.assignToken(TokenKind.LiteralFloat, tokenStart);
                } else {
                    self.assignToken(TokenKind.LiteralInt, tokenStart);
                }
            },
            '\'' => {
                self.skipCharacter();
                if (self.currentCharacter() == '\\') {
                    self.skipCharacter();
                    if (self.currentCharacter() == '\'' or self.currentCharacter() == '\\' or self.currentCharacter() == 'n') {
                        self.skipCharacter();
                        if (self.currentCharacter() == '\'') {
                            self.skipCharacter();
                            self.assignToken(TokenKind.LiteralChar, tokenStart);

                            return;
                        }
                    } else if (self.currentCharacter() == 'x') {
                        self.skipCharacter();
                        if (isDigit(self.currentCharacter())) {
                            self.skipCharacter();
                            while (isDigit(self.currentCharacter())) {
                                self.skipCharacter();
                            }
                            if (self.currentCharacter() == '\'') {
                                self.skipCharacter();
                                self.assignToken(TokenKind.LiteralChar, tokenStart);

                                return;
                            }
                        }
                    }
                } else if (self.currentCharacter() != '\'') {
                    self.skipCharacter();
                    if (self.currentCharacter() == '\'') {
                        self.skipCharacter();
                        self.assignToken(TokenKind.LiteralChar, tokenStart);

                        return;
                    }
                }

                try self.reportLexicalError(tokenStart);
            },
            '"' => {
                self.skipCharacter();
                while (self.currentCharacter() != '"') {
                    if (self.currentCharacter() == 0) {
                        try self.reportLexicalError(tokenStart);
                    }

                    if (self.currentCharacter() == '\\') {
                        self.skipCharacter();

                        if (self.currentCharacter() == 'n' or self.currentCharacter() == '\\' or self.currentCharacter() == '"') {
                            self.skipCharacter();
                        } else if (self.currentCharacter() == 'x') {
                            self.skipCharacter();
                            if (isDigit(self.currentCharacter())) {
                                self.skipCharacter();
                                while (isDigit(self.currentCharacter())) {
                                    self.skipCharacter();
                                }
                                if (self.currentCharacter() == ';') {
                                    self.skipCharacter();
                                } else {
                                    try self.reportLexicalError(tokenStart);
                                }
                            } else {
                                try self.reportLexicalError(tokenStart);
                            }
                        } else {
                            try self.reportLexicalError(tokenStart);
                        }
                    } else {
                        self.skipCharacter();
                    }
                }
                self.skipCharacter();

                self.assignToken(TokenKind.LiteralString, tokenStart);
            },
            else => {
                try self.reportLexicalError(tokenStart);
            },
        }
    }

    inline fn assignToken(self: *Lexer, kind: TokenKind, fromLocation: Errors.Location) void {
        self.current = Token{ .kind = kind, .locationRange = Errors.LocationRange{ .from = fromLocation, .to = self.location } };
    }

    inline fn setSymbolToken(self: *Lexer, kind: TokenKind, fromLocation: Errors.Location) void {
        self.skipCharacter();
        self.assignToken(kind, fromLocation);
    }

    fn replaceErr(self: *Lexer, err: Errors.Error) void {
        self.eraseErr();
        self.err = err;
    }

    pub fn eraseErr(self: *Lexer) void {
        if (self.err != null) {
            self.err.?.deinit();
            self.err = null;
        }
    }

    pub fn grabErr(self: *Lexer) ?Errors.Error {
        const err = self.err;
        self.err = null;

        return err;
    }

    pub fn currentLexeme(self: *Lexer) []const u8 {
        return self.lexeme(self.current);
    }
};

const expectEqual = std.testing.expectEqual;
const expectEqualStrings = std.testing.expectEqualStrings;

test "identifier and keywords" {
    var lexer = Lexer.init(std.heap.page_allocator);
    try lexer.initBuffer("test.bendu", " foo fn if let while ");

    try expectEqual(lexer.current.kind, TokenKind.Identifier);
    try expectEqualStrings(lexer.lexeme(lexer.current), "foo");
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Fn);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.If);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Let);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.While);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.EOS);
}

test "literal bool" {
    var lexer = Lexer.init(std.heap.page_allocator);
    try lexer.initBuffer("test.bendu", "true   false");

    try expectEqual(lexer.current.kind, TokenKind.LiteralBoolTrue);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LiteralBoolFalse);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.EOS);
}

fn expectTokenEqual(lexer: *Lexer, kind: TokenKind, lexeme: []const u8) !void {
    try expectEqual(
        kind,
        lexer.current.kind,
    );
    try expectEqualStrings(lexeme, lexer.lexeme(lexer.current));

    try lexer.next();
}

test "literal char" {
    var lexer = Lexer.init(std.heap.page_allocator);
    try lexer.initBuffer("test.bendu", "'x' '\\n' '\\\\' '\\'' '\\x31'");

    try expectTokenEqual(&lexer, TokenKind.LiteralChar, "'x'");
    try expectTokenEqual(&lexer, TokenKind.LiteralChar, "'\\n'");
    try expectTokenEqual(&lexer, TokenKind.LiteralChar, "'\\\\'");
    try expectTokenEqual(&lexer, TokenKind.LiteralChar, "'\\''");
    try expectTokenEqual(&lexer, TokenKind.LiteralChar, "'\\x31'");
    try expectEqual(lexer.current.kind, TokenKind.EOS);
}

test "literal float" {
    var lexer = Lexer.init(std.heap.page_allocator);
    try lexer.initBuffer("test.bendu", "1.0 -1.0 1.0e1 -1.0e1 1.0e+1 -1.0e+1 1.0e-1 -1.0e-1");

    try expectTokenEqual(&lexer, TokenKind.LiteralFloat, "1.0");
    try expectTokenEqual(&lexer, TokenKind.LiteralFloat, "-1.0");
    try expectTokenEqual(&lexer, TokenKind.LiteralFloat, "1.0e1");
    try expectTokenEqual(&lexer, TokenKind.LiteralFloat, "-1.0e1");
    try expectTokenEqual(&lexer, TokenKind.LiteralFloat, "1.0e+1");
    try expectTokenEqual(&lexer, TokenKind.LiteralFloat, "-1.0e+1");
    try expectTokenEqual(&lexer, TokenKind.LiteralFloat, "1.0e-1");
    try expectTokenEqual(&lexer, TokenKind.LiteralFloat, "-1.0e-1");

    try expectEqual(lexer.current.kind, TokenKind.EOS);
}

test "literal int" {
    var lexer = Lexer.init(std.heap.page_allocator);
    try lexer.initBuffer("test.bendu", "0 123 -1 -0 -123");

    try expectTokenEqual(&lexer, TokenKind.LiteralInt, "0");
    try expectTokenEqual(&lexer, TokenKind.LiteralInt, "123");
    try expectTokenEqual(&lexer, TokenKind.LiteralInt, "-1");
    try expectTokenEqual(&lexer, TokenKind.LiteralInt, "-0");
    try expectTokenEqual(&lexer, TokenKind.LiteralInt, "-123");

    try expectEqual(lexer.current.kind, TokenKind.EOS);
}

test "literal string" {
    var lexer = Lexer.init(std.heap.page_allocator);
    try lexer.initBuffer("test.bendu", "\"\" \"hello world\" \"\\n\\\\\\\" \\x123;x\"");

    try expectTokenEqual(&lexer, TokenKind.LiteralString, "\"\"");
    try expectTokenEqual(&lexer, TokenKind.LiteralString, "\"hello world\"");
    try expectTokenEqual(&lexer, TokenKind.LiteralString, "\"\\n\\\\\\\" \\x123;x\"");

    try expectEqual(lexer.current.kind, TokenKind.EOS);
}

test "@ ? + - * ** / % = == ! != <! <| < <= << >! > >= >> && || [ { ( , . ... : := ; -> | |> ] } )" {
    var lexer = Lexer.init(std.heap.page_allocator);
    try lexer.initBuffer("test.bendu", " @ ? + - * ** / % = == ! != <! <| < <= << >! > >= >> && || [ { ( , . ... : := ; -> | |> ] } ) ");

    try expectEqual(lexer.current.kind, TokenKind.At);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Hook);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Plus);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Minus);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Star);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.StarStar);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Slash);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Percentage);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Equal);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.EqualEqual);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Bang);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.BangEqual);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LessBang);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LessBar);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LessThan);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LessEqual);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LessLess);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.GreaterBang);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.GreaterThan);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.GreaterEqual);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.GreaterGreater);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.AmpersandAmpersand);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.BarBar);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LBracket);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LCurly);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.LParen);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Comma);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Dot);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.DotDotDot);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Colon);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.ColonEqual);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Semicolon);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.MinusGreater);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.Bar);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.BarGreater);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.RBracket);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.RCurly);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.RParen);
    try lexer.next();
    try expectEqual(lexer.current.kind, TokenKind.EOS);
}
