package io.littlelanguages.bendu.parser

interface Visitor<T_Program, T_Statement, T_Expression, T_Factor> {
  fun visitProgram(a: List<io.littlelanguages.data.Tuple2<T_Statement, Token?>>): T_Program
  fun visitStatement(a1: Token, a2: Token, a3: Token, a4: T_Expression): T_Statement
  fun visitExpression(a: T_Factor): T_Expression
  fun visitFactor1(a1: Token, a2: T_Expression, a3: Token): T_Factor
  fun visitFactor2(a: Token): T_Factor
  fun visitFactor3(a: Token): T_Factor
}

class Parser<T_Program, T_Statement, T_Expression, T_Factor>(
    private val scanner: Scanner,
    private val visitor: Visitor<T_Program, T_Statement, T_Expression, T_Factor>) {
  fun program(): T_Program {
    val a = mutableListOf<io.littlelanguages.data.Tuple2<T_Statement, Token?>>()
    
    while (isToken(TToken.TLet)) {
      val at1: T_Statement = statement()
      var at2: Token? = null
      
      if (isToken(TToken.TSemicolon)) {
        val at2t: Token = matchToken(TToken.TSemicolon)
        at2 = at2t
      }
      val at: io.littlelanguages.data.Tuple2<T_Statement, Token?> = io.littlelanguages.data.Tuple2(at1, at2)
      a.add(at)
    }
    return visitor.visitProgram(a)
  }
  
  fun statement(): T_Statement {
    val a1: Token = matchToken(TToken.TLet)
    val a2: Token = matchToken(TToken.TLowerID)
    val a3: Token = matchToken(TToken.TEqual)
    val a4: T_Expression = expression()
    return visitor.visitStatement(a1, a2, a3, a4)
  }
  
  fun expression(): T_Expression {
    return visitor.visitExpression(factor())
  }
  
  fun factor(): T_Factor {
    when {
      isToken(TToken.TLParen) -> {
        val a1: Token = matchToken(TToken.TLParen)
        val a2: T_Expression = expression()
        val a3: Token = matchToken(TToken.TRParen)
        return visitor.visitFactor1(a1, a2, a3)
      }
      isToken(TToken.TLiteralInt) -> {
        return visitor.visitFactor2(matchToken(TToken.TLiteralInt))
      }
      isToken(TToken.TLowerID) -> {
        return visitor.visitFactor3(matchToken(TToken.TLowerID))
      }
      else -> {
        throw ParsingException(peek(), set1)
      }
    }
  }
  
  
  private fun matchToken(tToken: TToken): Token =
    when (peek().tToken) {
      tToken -> nextToken()
      else -> throw ParsingException(peek(), setOf(tToken))
    }
  
  private fun nextToken(): Token {
    val result =
      peek()
    
    skipToken()
    
    return result
  }
  
  private fun skipToken() {
    scanner.next()
  }
  
  private fun isToken(tToken: TToken): Boolean =
    peek().tToken == tToken
  
  private fun isTokens(tTokens: Set<TToken>): Boolean =
    tTokens.contains(peek().tToken)
  
  private fun peek(): Token =
    scanner.current()
}

private val set1 = setOf(TToken.TLParen, TToken.TLiteralInt, TToken.TLowerID)

class ParsingException(
  val found: Token,
  val expected: Set<TToken>) : Exception()