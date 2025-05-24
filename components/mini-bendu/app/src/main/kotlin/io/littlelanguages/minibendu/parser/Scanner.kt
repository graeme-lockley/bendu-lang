package io.littlelanguages.minibendu.parser

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
            if (nextCh == 95) {
              markAndNextChar()
              state = 1
            } else if (nextCh == 93) {
              markAndNextChar()
              state = 2
            } else if (nextCh == 91) {
              markAndNextChar()
              state = 3
            } else if (nextCh == 38) {
              markAndNextChar()
              state = 4
            } else if (nextCh == 45) {
              markAndNextChar()
              state = 5
            } else if (nextCh == 46) {
              markAndNextChar()
              state = 6
            } else if (nextCh == 125) {
              markAndNextChar()
              state = 7
            } else if (nextCh == 123) {
              markAndNextChar()
              state = 8
            } else if (nextCh == 70) {
              markAndNextChar()
              state = 9
            } else if (nextCh == 84) {
              markAndNextChar()
              state = 10
            } else if (nextCh == 124) {
              markAndNextChar()
              state = 11
            } else if (nextCh == 119) {
              markAndNextChar()
              state = 12
            } else if (nextCh == 109) {
              markAndNextChar()
              state = 13
            } else if (nextCh == 101) {
              markAndNextChar()
              state = 14
            } else if (nextCh == 116) {
              markAndNextChar()
              state = 15
            } else if (nextCh == 105) {
              markAndNextChar()
              state = 16
            } else if (nextCh == 47) {
              markAndNextChar()
              state = 17
            } else if (nextCh == 42) {
              markAndNextChar()
              state = 18
            } else if (nextCh == 43) {
              markAndNextChar()
              state = 19
            } else if (nextCh == 33) {
              markAndNextChar()
              state = 20
            } else if (nextCh == 61) {
              markAndNextChar()
              state = 21
            } else if (nextCh == 92) {
              markAndNextChar()
              state = 22
            } else if (nextCh == 41) {
              markAndNextChar()
              state = 23
            } else if (nextCh == 44) {
              markAndNextChar()
              state = 24
            } else if (nextCh == 40) {
              markAndNextChar()
              state = 25
            } else if (nextCh == 58) {
              markAndNextChar()
              state = 26
            } else if (nextCh == 114) {
              markAndNextChar()
              state = 27
            } else if (nextCh == 108) {
              markAndNextChar()
              state = 28
            } else if (nextCh == 34) {
              markAndNextChar()
              state = 29
            } else if (nextCh in 97..100 || nextCh in 102..104 || nextCh == 106 || nextCh == 107 || nextCh in 110..113 || nextCh == 115 || nextCh == 117 || nextCh == 118 || nextCh in 120..122) {
              markAndNextChar()
              state = 30
            } else if (nextCh in 65..69 || nextCh in 71..83 || nextCh in 85..90) {
              markAndNextChar()
              state = 31
            } else if (nextCh == -1) {
              markAndNextChar()
              state = 32
            } else if (nextCh == 35) {
              markAndNextChar()
              state = 33
            } else if (nextCh in 48..57) {
              markAndNextChar()
              state = 34
            } else {
              markAndNextChar()
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          1 -> {
            setToken(TToken.TUnderscore)
            return
          }
          2 -> {
            setToken(TToken.TRBracket)
            return
          }
          3 -> {
            setToken(TToken.TLBracket)
            return
          }
          4 -> {
            if (nextCh == 38) {
              nextChar()
              state = 35
            } else {
              setToken(TToken.TAmpersand)
              return
            }
          }
          5 -> {
            if (nextCh == 62) {
              nextChar()
              state = 36
            } else if (nextCh in 48..57) {
              nextChar()
              state = 34
            } else {
              setToken(TToken.TDash)
              return
            }
          }
          6 -> {
            if (nextCh == 46) {
              markBacktrackPoint(TToken.TPeriod)
              nextChar()
              state = 37
            } else {
              setToken(TToken.TPeriod)
              return
            }
          }
          7 -> {
            setToken(TToken.TRCurly)
            return
          }
          8 -> {
            setToken(TToken.TLCurly)
            return
          }
          9 -> {
            if (nextCh == 97) {
              nextChar()
              state = 38
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 98..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TUpperID)
              return
            }
          }
          10 -> {
            if (nextCh == 114) {
              nextChar()
              state = 39
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..113 || nextCh in 115..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TUpperID)
              return
            }
          }
          11 -> {
            if (nextCh == 124) {
              nextChar()
              state = 40
            } else {
              setToken(TToken.TBar)
              return
            }
          }
          12 -> {
            if (nextCh == 105) {
              nextChar()
              state = 41
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..104 || nextCh in 106..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          13 -> {
            if (nextCh == 97) {
              nextChar()
              state = 42
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 98..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          14 -> {
            if (nextCh == 108) {
              nextChar()
              state = 43
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..107 || nextCh in 109..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          15 -> {
            if (nextCh == 104) {
              nextChar()
              state = 44
            } else if (nextCh == 121) {
              nextChar()
              state = 45
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..103 || nextCh in 105..120 || nextCh == 122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          16 -> {
            if (nextCh == 102) {
              nextChar()
              state = 46
            } else if (nextCh == 110) {
              nextChar()
              state = 47
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..101 || nextCh in 103..109 || nextCh in 111..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          17 -> {
            if (nextCh == 47) {
              nextChar()
              state = 48
            } else if (nextCh == 42) {
              nextChar()
              state = 49
            } else {
              setToken(TToken.TSlash)
              return
            }
          }
          18 -> {
            setToken(TToken.TStar)
            return
          }
          19 -> {
            setToken(TToken.TPlus)
            return
          }
          20 -> {
            if (nextCh == 61) {
              nextChar()
              state = 50
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          21 -> {
            if (nextCh == 61) {
              nextChar()
              state = 51
            } else if (nextCh == 62) {
              nextChar()
              state = 52
            } else {
              setToken(TToken.TEqual)
              return
            }
          }
          22 -> {
            setToken(TToken.TBackslash)
            return
          }
          23 -> {
            setToken(TToken.TRParen)
            return
          }
          24 -> {
            setToken(TToken.TComma)
            return
          }
          25 -> {
            setToken(TToken.TLParen)
            return
          }
          26 -> {
            setToken(TToken.TColon)
            return
          }
          27 -> {
            if (nextCh == 101) {
              nextChar()
              state = 53
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          28 -> {
            if (nextCh == 101) {
              nextChar()
              state = 54
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          29 -> {
            if (nextCh == 32 || nextCh == 33 || nextCh in 35..91 || nextCh in 93..255) {
              nextChar()
              state = 29
            } else if (nextCh == 92) {
              nextChar()
              state = 55
            } else if (nextCh == 34) {
              nextChar()
              state = 56
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          30 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          31 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TUpperID)
              return
            }
          }
          32 -> {
            setToken(TToken.TEOS)
            return
          }
          33 -> {
            if (nextCh in 0..9 || nextCh in 11..255) {
              nextChar()
              state = 33
            } else {
              next()
              return
            }
          }
          34 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 34
            } else {
              setToken(TToken.TLiteralInt)
              return
            }
          }
          35 -> {
            setToken(TToken.TAmpersandAmpersand)
            return
          }
          36 -> {
            setToken(TToken.TDashGreaterThan)
            return
          }
          37 -> {
            if (nextCh == 46) {
              nextChar()
              state = 57
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          38 -> {
            if (nextCh == 108) {
              nextChar()
              state = 58
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..107 || nextCh in 109..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TUpperID)
              return
            }
          }
          39 -> {
            if (nextCh == 117) {
              nextChar()
              state = 59
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..116 || nextCh in 118..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TUpperID)
              return
            }
          }
          40 -> {
            setToken(TToken.TBarBar)
            return
          }
          41 -> {
            if (nextCh == 116) {
              nextChar()
              state = 60
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          42 -> {
            if (nextCh == 116) {
              nextChar()
              state = 61
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          43 -> {
            if (nextCh == 115) {
              nextChar()
              state = 62
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..114 || nextCh in 116..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          44 -> {
            if (nextCh == 101) {
              nextChar()
              state = 63
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          45 -> {
            if (nextCh == 112) {
              nextChar()
              state = 64
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..111 || nextCh in 113..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          46 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIf)
              return
            }
          }
          47 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TIn)
              return
            }
          }
          48 -> {
            if (nextCh in 0..9 || nextCh in 11..255) {
              nextChar()
              state = 48
            } else {
              next()
              return
            }
          }
          49 -> {
            var nstate = 0
            var nesting = 1
            while (true) {
              when (nstate) {
                0 -> {
                  if (nextCh in 0..41 || nextCh in 43..46 || nextCh in 48..255) {
                    nextChar()
                    nstate = 1
                  } else if (nextCh == 42) {
                    nextChar()
                    nstate = 2
                  } else if (nextCh == 47) {
                    nextChar()
                    nstate = 3
                  } else {
                    attemptBacktrackOtherwise(TToken.TERROR)
                    return
                  }
                }
                1 -> {
                  nstate = 0
                }
                2 -> {
                  if (nextCh == 47) {
                    nextChar()
                    nstate = 4
                  } else {
                    nstate = 0
                  }
                }
                3 -> {
                  if (nextCh == 42) {
                    nextChar()
                    nstate = 5
                  } else {
                    nstate = 0
                  }
                }
                4 -> {
                  nesting -= 1
                  if (nesting == 0) {
                    next()
                    return
                  } else {
                    nstate = 0
                  }
                }
                5 -> {
                  nesting += 1
                  nstate = 0
                }
              }
            }
          }
          50 -> {
            setToken(TToken.TBangEqual)
            return
          }
          51 -> {
            setToken(TToken.TEqualEqual)
            return
          }
          52 -> {
            setToken(TToken.TEqualGreaterThan)
            return
          }
          53 -> {
            if (nextCh == 99) {
              nextChar()
              state = 65
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh == 97 || nextCh == 98 || nextCh in 100..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          54 -> {
            if (nextCh == 116) {
              nextChar()
              state = 66
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..115 || nextCh in 117..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          55 -> {
            if (nextCh == 34 || nextCh == 92 || nextCh == 110) {
              nextChar()
              state = 29
            } else if (nextCh == 120) {
              nextChar()
              state = 67
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          56 -> {
            setToken(TToken.TLiteralString)
            return
          }
          57 -> {
            setToken(TToken.TPeriodPeriodPeriod)
            return
          }
          58 -> {
            if (nextCh == 115) {
              nextChar()
              state = 68
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..114 || nextCh in 116..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TUpperID)
              return
            }
          }
          59 -> {
            if (nextCh == 101) {
              nextChar()
              state = 69
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TUpperID)
              return
            }
          }
          60 -> {
            if (nextCh == 104) {
              nextChar()
              state = 70
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..103 || nextCh in 105..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          61 -> {
            if (nextCh == 99) {
              nextChar()
              state = 71
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh == 97 || nextCh == 98 || nextCh in 100..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          62 -> {
            if (nextCh == 101) {
              nextChar()
              state = 72
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          63 -> {
            if (nextCh == 110) {
              nextChar()
              state = 73
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..109 || nextCh in 111..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          64 -> {
            if (nextCh == 101) {
              nextChar()
              state = 74
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          65 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TRec)
              return
            }
          }
          66 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLet)
              return
            }
          }
          67 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 75
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          68 -> {
            if (nextCh == 101) {
              nextChar()
              state = 76
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..100 || nextCh in 102..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TUpperID)
              return
            }
          }
          69 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TTrue)
              return
            }
          }
          70 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TWith)
              return
            }
          }
          71 -> {
            if (nextCh == 104) {
              nextChar()
              state = 77
            } else if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..103 || nextCh in 105..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TLowerID)
              return
            }
          }
          72 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TElse)
              return
            }
          }
          73 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TThen)
              return
            }
          }
          74 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TType)
              return
            }
          }
          75 -> {
            if (nextCh in 48..57) {
              nextChar()
              state = 75
            } else if (nextCh == 59) {
              nextChar()
              state = 29
            } else {
              attemptBacktrackOtherwise(TToken.TERROR)
              return
            }
          }
          76 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 31
            } else {
              setToken(TToken.TFalse)
              return
            }
          }
          77 -> {
            if (nextCh in 48..57 || nextCh in 65..90 || nextCh in 97..122) {
              nextChar()
              state = 30
            } else {
              setToken(TToken.TMatch)
              return
            }
          }
        }
      }
    }
  }
}

enum class TToken {
  TUnderscore,
  TRBracket,
  TLBracket,
  TAmpersand,
  TDashGreaterThan,
  TPeriodPeriodPeriod,
  TRCurly,
  TLCurly,
  TFalse,
  TTrue,
  TPeriod,
  TBar,
  TWith,
  TMatch,
  TElse,
  TThen,
  TIf,
  TSlash,
  TStar,
  TDash,
  TPlus,
  TBangEqual,
  TEqualEqual,
  TAmpersandAmpersand,
  TBarBar,
  TEqualGreaterThan,
  TBackslash,
  TRParen,
  TComma,
  TLParen,
  TIn,
  TColon,
  TRec,
  TLet,
  TEqual,
  TType,
  TLiteralInt,
  TLiteralString,
  TLowerID,
  TUpperID,
  TEOS,
  TERROR,
}

typealias Token = AbstractToken<TToken>
