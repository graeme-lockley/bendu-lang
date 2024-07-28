pub const TokenKind = enum {
    EOS,
    Invalid,

    LiteralBoolFalse,
    LiteralBoolTrue,
    LiteralChar,
    LiteralFloat,
    LiteralInt,
    LiteralString,

    Identifier,

    And,
    Catch,
    Fn,
    If,
    Let,
    Match,
    Raise,
    While,

    AmpersandAmpersand,
    At,
    Bang,
    BangEqual,
    Bar,
    BarBar,
    BarGreater,
    Colon,
    ColonEqual,
    Comma,
    Dot,
    DotDotDot,
    Equal,
    EqualEqual,
    GreaterBang,
    GreaterEqual,
    GreaterGreater,
    GreaterThan,
    Hook,
    LBracket,
    LCurly,
    LessBang,
    LessBar,
    LessEqual,
    LessLess,
    LessThan,
    LParen,
    Minus,
    MinusGreater,
    Percentage,
    Plus,
    RBracket,
    RCurly,
    RParen,
    Semicolon,
    Slash,
    Star,
    StarStar,

    pub fn toString(self: TokenKind) []const u8 {
        switch (self) {
            TokenKind.EOS => return "end-of-stream",
            TokenKind.Invalid => return "invalid-token",
            TokenKind.LiteralBoolFalse => return "false",
            TokenKind.LiteralBoolTrue => return "true",
            TokenKind.LiteralChar => return "literal char",
            TokenKind.LiteralFloat => return "literal float",
            TokenKind.LiteralInt => return "literal int",
            TokenKind.LiteralString => return "literal string",

            TokenKind.Identifier => return "identifier",

            TokenKind.And => return "and",
            TokenKind.Catch => return "catch",
            TokenKind.Fn => return "fn",
            TokenKind.If => return "if",
            TokenKind.Let => return "let",
            TokenKind.Match => return "match",
            TokenKind.Raise => return "raise",
            TokenKind.While => return "while",

            TokenKind.AmpersandAmpersand => return "'&&'",
            TokenKind.At => return "'@'",
            TokenKind.Bang => return "'!'",
            TokenKind.BangEqual => return "'!='",
            TokenKind.Bar => return "'|'",
            TokenKind.BarBar => return "'||'",
            TokenKind.BarGreater => return "'|>'",
            TokenKind.Colon => return "':'",
            TokenKind.ColonEqual => return "':='",
            TokenKind.Comma => return "','",
            TokenKind.Dot => return "'.'",
            TokenKind.DotDotDot => return "'...'",
            TokenKind.Equal => return "'='",
            TokenKind.EqualEqual => return "'=='",
            TokenKind.GreaterBang => return "'>!'",
            TokenKind.GreaterEqual => return "'>='",
            TokenKind.GreaterGreater => return "'>>'",
            TokenKind.GreaterThan => return "'>'",
            TokenKind.Hook => return "'?'",
            TokenKind.LBracket => return "'['",
            TokenKind.LessBang => return "'<!'",
            TokenKind.LessBar => return "'<|'",
            TokenKind.LessEqual => return "'<='",
            TokenKind.LessLess => return "'<<'",
            TokenKind.LessThan => return "'<'",
            TokenKind.LCurly => return "'{'",
            TokenKind.LParen => return "'('",
            TokenKind.Minus => return "'-'",
            TokenKind.MinusGreater => return "'->'",
            TokenKind.Percentage => return "'%'",
            TokenKind.Plus => return "'+'",
            TokenKind.RBracket => return "']'",
            TokenKind.RCurly => return "'}'",
            TokenKind.RParen => return "')'",
            TokenKind.Semicolon => return "';'",
            TokenKind.Slash => return "'/'",
            TokenKind.Star => return "'*'",
            TokenKind.StarStar => return "'**'",
        }
    }
};
