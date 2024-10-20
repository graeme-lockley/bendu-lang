package io.littlelanguages.bendu.parser

import java.io.Reader
import io.littlelanguages.scanpiler.AbstractScanner
import io.littlelanguages.scanpiler.AbstractToken
import io.littlelanguages.scanpiler.Location

class Scanner(input: Reader): AbstractScanner<TToken>(input, TToken.TERROR) {
  override fun newToken(ttoken: TToken, location: Location, lexeme: String): AbstractToken<TToken> =
    Token(ttoken, location, lexeme)
  
  override fun next() {
    if (currentToken.tToken != TToken.TEOS) {
      while (nextCh in 0..32) {
        nextChar()
      }
      
      var state = 0
      while (true) {
        when (state) {
          0 -> {
            if (nextCh == 41) {
              markAndNextChar()
              state = 1
            } else if (nextCh == 40) {
              markAndNextChar()
              state = 2
            } else if (nextCh == 61) {
              markAndNextChar()
              state = 3
            } else if (nextCh == 108) {
              markAndNextChar()
              state = 4
            } else if (nextCh == 59) {
              markAndNextChar()
              state = 5
            } else if (nextCh == 45) {
              markAndNextChar()
              state = 6
            } else if (nextCh in 65..90 || nextCh in 97..107 || nextCh in 109..122) {
              markAndNextChar()
              state = 7
            } else if (nextCh == -1) {
              markAndNextChar()
              state = 8
            } else if (nextCh in 48..57) {
              markAndNextChar()
              state = 9
            } else {
              markAndNextChar()
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          1 -> {
            setToken(TToken.TRParen)
            return
          }
          2 -> {
            setToken(TToken.TLParen)
            return
          }
          3 -> {
            setToken(TToken.TEqual)
            return
          }
          4 -> {
            if (nextCh == 101) {
              nextChar()
              state = 10
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 7
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          5 -> {
            setToken(TToken.TSemicolon)
            return
          }
          6 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 9
            } else if (nextCh == 45) {
              nextChar()
              state = 11
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          7 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 7
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          8 -> {
            setToken(TToken.TEOS)
            return
          }
          9 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 9
            } else {
              setToken(TToken.TLiteralInt)
              return
            }
          }
          10 -> {
            if (nextCh == 116) {
              nextChar()
              state = 12
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 7
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          11 -> {
            if (nextCh in 0..9 || nextCh in 11..255) {
              nextChar()
              state = 11
            } else {
              next()
              return
            }
          }
          12 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 7
            } else {
              setToken(TToken.TLet)
              return
            }
          }
        }
      }
    }
  }
}

enum class TToken {
  TRParen,
  TLParen,
  TEqual,
  TLet,
  TSemicolon,
  TLiteralInt,
  TLowerID,
  TEOS,
  TERROR,
}

typealias Token = AbstractToken<TToken>