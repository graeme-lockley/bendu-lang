package io.littlelanguages.minibendu.parser

interface Visitor<T_Program, T_TopLevel, T_TypeAliasDecl, T_ExprStmt, T_Expr, T_LetExpr, T_Parameters, T_ParameterType, T_LambdaExpr, T_LogicalOrExpr, T_LogicalAndExpr, T_EqualityExpr, T_EqualityOp, T_AdditiveExpr, T_AdditiveOp, T_MultiplicativeExpr, T_MultiplicativeOp, T_ApplicationExpr, T_IfExpr, T_MatchExpr, T_MatchCase, T_SimpleExpr, T_PrimaryExpr, T_Variable, T_Record, T_SpreadOrField, T_TypeExpr, T_MergeType, T_UnionType, T_PrimaryType, T_BaseType, T_GenericArgs, T_RecordType, T_TypeField, T_TupleType, T_TypeParams, T_TypeParam, T_Pattern, T_RecordPattern, T_FieldPattern, T_VarPattern, T_Wildcard, T_LiteralPattern> {
  fun visitProgram(a: List<T_TopLevel>): T_Program
  fun visitTopLevel1(a: T_TypeAliasDecl): T_TopLevel
  fun visitTopLevel2(a: T_ExprStmt): T_TopLevel
  fun visitTypeAliasDecl(a1: Token, a2: Token, a3: T_TypeParams?, a4: Token, a5: T_TypeExpr): T_TypeAliasDecl
  fun visitExprStmt(a: T_Expr): T_ExprStmt
  fun visitExpr1(a: T_LetExpr): T_Expr
  fun visitExpr2(a: T_LambdaExpr): T_Expr
  fun visitExpr3(a: T_LogicalOrExpr): T_Expr
  fun visitExpr4(a: T_IfExpr): T_Expr
  fun visitExpr5(a: T_MatchExpr): T_Expr
  fun visitLetExpr(a1: Token, a2: Token?, a3: Token, a4: T_TypeParams?, a5: T_Parameters?, a6: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>?, a7: Token, a8: T_Expr, a9: io.littlelanguages.data.Tuple2<Token, T_Expr>?): T_LetExpr
  fun visitParameters(a1: Token, a2: io.littlelanguages.data.Tuple2<T_ParameterType, List<io.littlelanguages.data.Tuple2<Token, T_ParameterType>>>?, a3: Token): T_Parameters
  fun visitParameterType(a1: Token, a2: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>?): T_ParameterType
  fun visitLambdaExpr(a1: Token, a2: T_TypeParams?, a3: Token, a4: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>?, a5: Token, a6: T_Expr): T_LambdaExpr
  fun visitLogicalOrExpr(a1: T_LogicalAndExpr, a2: List<io.littlelanguages.data.Tuple2<Token, T_LogicalAndExpr>>): T_LogicalOrExpr
  fun visitLogicalAndExpr(a1: T_EqualityExpr, a2: List<io.littlelanguages.data.Tuple2<Token, T_EqualityExpr>>): T_LogicalAndExpr
  fun visitEqualityExpr(a1: T_AdditiveExpr, a2: List<io.littlelanguages.data.Tuple2<T_EqualityOp, T_AdditiveExpr>>): T_EqualityExpr
  fun visitEqualityOp1(a: Token): T_EqualityOp
  fun visitEqualityOp2(a: Token): T_EqualityOp
  fun visitAdditiveExpr(a1: T_MultiplicativeExpr, a2: List<io.littlelanguages.data.Tuple2<T_AdditiveOp, T_MultiplicativeExpr>>): T_AdditiveExpr
  fun visitAdditiveOp1(a: Token): T_AdditiveOp
  fun visitAdditiveOp2(a: Token): T_AdditiveOp
  fun visitMultiplicativeExpr(a1: T_ApplicationExpr, a2: List<io.littlelanguages.data.Tuple2<T_MultiplicativeOp, T_ApplicationExpr>>): T_MultiplicativeExpr
  fun visitMultiplicativeOp1(a: Token): T_MultiplicativeOp
  fun visitMultiplicativeOp2(a: Token): T_MultiplicativeOp
  fun visitApplicationExpr(a1: T_SimpleExpr, a2: List<io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expr, List<io.littlelanguages.data.Tuple2<Token, T_Expr>>>?, Token>>): T_ApplicationExpr
  fun visitIfExpr(a1: Token, a2: T_Expr, a3: Token, a4: T_Expr, a5: Token, a6: T_Expr): T_IfExpr
  fun visitMatchExpr(a1: Token, a2: T_Expr, a3: Token, a4: T_MatchCase, a5: List<io.littlelanguages.data.Tuple2<Token, T_MatchCase>>): T_MatchExpr
  fun visitMatchCase(a1: T_Pattern, a2: Token, a3: T_Expr): T_MatchCase
  fun visitSimpleExpr(a1: T_PrimaryExpr, a2: List<io.littlelanguages.data.Tuple2<Token, Token>>): T_SimpleExpr
  fun visitPrimaryExpr1(a: Token): T_PrimaryExpr
  fun visitPrimaryExpr2(a: Token): T_PrimaryExpr
  fun visitPrimaryExpr3(a: Token): T_PrimaryExpr
  fun visitPrimaryExpr4(a: Token): T_PrimaryExpr
  fun visitPrimaryExpr5(a: T_Variable): T_PrimaryExpr
  fun visitPrimaryExpr6(a: T_Record): T_PrimaryExpr
  fun visitPrimaryExpr7(a1: Token, a2: T_Expr, a3: List<io.littlelanguages.data.Tuple2<Token, T_Expr>>, a4: Token): T_PrimaryExpr
  fun visitVariable(a: Token): T_Variable
  fun visitRecord(a1: Token, a2: io.littlelanguages.data.Tuple2<T_SpreadOrField, List<io.littlelanguages.data.Tuple2<Token, T_SpreadOrField>>>?, a3: Token): T_Record
  fun visitSpreadOrField1(a1: Token, a2: Token, a3: T_Expr): T_SpreadOrField
  fun visitSpreadOrField2(a1: Token, a2: T_Expr): T_SpreadOrField
  fun visitTypeExpr(a1: T_MergeType, a2: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>?): T_TypeExpr
  fun visitMergeType(a1: T_UnionType, a2: List<io.littlelanguages.data.Tuple2<Token, T_UnionType>>): T_MergeType
  fun visitUnionType(a1: T_PrimaryType, a2: List<io.littlelanguages.data.Tuple2<Token, T_PrimaryType>>): T_UnionType
  fun visitPrimaryType1(a: T_BaseType): T_PrimaryType
  fun visitPrimaryType2(a: T_RecordType): T_PrimaryType
  fun visitPrimaryType3(a: T_TupleType): T_PrimaryType
  fun visitPrimaryType4(a: Token): T_PrimaryType
  fun visitBaseType(a1: Token, a2: T_GenericArgs?): T_BaseType
  fun visitGenericArgs(a1: Token, a2: T_TypeExpr, a3: List<io.littlelanguages.data.Tuple2<Token, T_TypeExpr>>, a4: Token): T_GenericArgs
  fun visitRecordType(a1: Token, a2: io.littlelanguages.data.Tuple2<T_TypeField, List<io.littlelanguages.data.Tuple2<Token, T_TypeField>>>?, a3: io.littlelanguages.data.Tuple2<Token, Token>?, a4: Token): T_RecordType
  fun visitTypeField(a1: Token, a2: Token, a3: T_TypeExpr): T_TypeField
  fun visitTupleType(a1: Token, a2: T_TypeExpr, a3: List<io.littlelanguages.data.Tuple2<Token, T_TypeExpr>>, a4: Token): T_TupleType
  fun visitTypeParams(a1: Token, a2: T_TypeParam, a3: List<io.littlelanguages.data.Tuple2<Token, T_TypeParam>>, a4: Token): T_TypeParams
  fun visitTypeParam(a1: Token, a2: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>?): T_TypeParam
  fun visitPattern1(a: T_RecordPattern): T_Pattern
  fun visitPattern2(a: T_VarPattern): T_Pattern
  fun visitPattern3(a: T_Wildcard): T_Pattern
  fun visitPattern4(a: T_LiteralPattern): T_Pattern
  fun visitPattern5(a1: Token, a2: T_Pattern, a3: List<io.littlelanguages.data.Tuple2<Token, T_Pattern>>, a4: Token): T_Pattern
  fun visitRecordPattern(a1: Token, a2: io.littlelanguages.data.Tuple2<T_FieldPattern, List<io.littlelanguages.data.Tuple2<Token, T_FieldPattern>>>?, a3: Token): T_RecordPattern
  fun visitFieldPattern(a1: Token, a2: Token, a3: T_Pattern): T_FieldPattern
  fun visitVarPattern(a: Token): T_VarPattern
  fun visitWildcard(a: Token): T_Wildcard
  fun visitLiteralPattern1(a: Token): T_LiteralPattern
  fun visitLiteralPattern2(a: Token): T_LiteralPattern
  fun visitLiteralPattern3(a: Token): T_LiteralPattern
  fun visitLiteralPattern4(a: Token): T_LiteralPattern
}

class Parser<T_Program, T_TopLevel, T_TypeAliasDecl, T_ExprStmt, T_Expr, T_LetExpr, T_Parameters, T_ParameterType, T_LambdaExpr, T_LogicalOrExpr, T_LogicalAndExpr, T_EqualityExpr, T_EqualityOp, T_AdditiveExpr, T_AdditiveOp, T_MultiplicativeExpr, T_MultiplicativeOp, T_ApplicationExpr, T_IfExpr, T_MatchExpr, T_MatchCase, T_SimpleExpr, T_PrimaryExpr, T_Variable, T_Record, T_SpreadOrField, T_TypeExpr, T_MergeType, T_UnionType, T_PrimaryType, T_BaseType, T_GenericArgs, T_RecordType, T_TypeField, T_TupleType, T_TypeParams, T_TypeParam, T_Pattern, T_RecordPattern, T_FieldPattern, T_VarPattern, T_Wildcard, T_LiteralPattern>(
    private val scanner: Scanner,
    private val visitor: Visitor<T_Program, T_TopLevel, T_TypeAliasDecl, T_ExprStmt, T_Expr, T_LetExpr, T_Parameters, T_ParameterType, T_LambdaExpr, T_LogicalOrExpr, T_LogicalAndExpr, T_EqualityExpr, T_EqualityOp, T_AdditiveExpr, T_AdditiveOp, T_MultiplicativeExpr, T_MultiplicativeOp, T_ApplicationExpr, T_IfExpr, T_MatchExpr, T_MatchCase, T_SimpleExpr, T_PrimaryExpr, T_Variable, T_Record, T_SpreadOrField, T_TypeExpr, T_MergeType, T_UnionType, T_PrimaryType, T_BaseType, T_GenericArgs, T_RecordType, T_TypeField, T_TupleType, T_TypeParams, T_TypeParam, T_Pattern, T_RecordPattern, T_FieldPattern, T_VarPattern, T_Wildcard, T_LiteralPattern>) {
  fun program(): T_Program {
    val a = mutableListOf<T_TopLevel>()
    
    while (isTokens(set1)) {
      val at: T_TopLevel = topLevel()
      a.add(at)
    }
    return visitor.visitProgram(a)
  }
  
  fun topLevel(): T_TopLevel {
    when {
      isToken(TToken.TType) -> {
        return visitor.visitTopLevel1(typeAliasDecl())
      }
      isTokens(set2) -> {
        return visitor.visitTopLevel2(exprStmt())
      }
      else -> {
        throw ParsingException(peek(), set1)
      }
    }
  }
  
  fun typeAliasDecl(): T_TypeAliasDecl {
    val a1: Token = matchToken(TToken.TType)
    val a2: Token = matchToken(TToken.TUpperID)
    var a3: T_TypeParams? = null
    
    if (isToken(TToken.TLBracket)) {
      val a3t: T_TypeParams = typeParams()
      a3 = a3t
    }
    val a4: Token = matchToken(TToken.TEqual)
    val a5: T_TypeExpr = typeExpr()
    return visitor.visitTypeAliasDecl(a1, a2, a3, a4, a5)
  }
  
  fun exprStmt(): T_ExprStmt {
    return visitor.visitExprStmt(expr())
  }
  
  fun expr(): T_Expr {
    when {
      isToken(TToken.TLet) -> {
        return visitor.visitExpr1(letExpr())
      }
      isToken(TToken.TBackslash) -> {
        return visitor.visitExpr2(lambdaExpr())
      }
      isTokens(set3) -> {
        return visitor.visitExpr3(logicalOrExpr())
      }
      isToken(TToken.TIf) -> {
        return visitor.visitExpr4(ifExpr())
      }
      isToken(TToken.TMatch) -> {
        return visitor.visitExpr5(matchExpr())
      }
      else -> {
        throw ParsingException(peek(), set2)
      }
    }
  }
  
  fun letExpr(): T_LetExpr {
    val a1: Token = matchToken(TToken.TLet)
    var a2: Token? = null
    
    if (isToken(TToken.TRec)) {
      val a2t: Token = matchToken(TToken.TRec)
      a2 = a2t
    }
    val a3: Token = matchToken(TToken.TLowerID)
    var a4: T_TypeParams? = null
    
    if (isToken(TToken.TLBracket)) {
      val a4t: T_TypeParams = typeParams()
      a4 = a4t
    }
    var a5: T_Parameters? = null
    
    if (isToken(TToken.TLParen)) {
      val a5t: T_Parameters = parameters()
      a5 = a5t
    }
    var a6: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>? = null
    
    if (isToken(TToken.TColon)) {
      val a6t1: Token = matchToken(TToken.TColon)
      val a6t2: T_TypeExpr = typeExpr()
      val a6t: io.littlelanguages.data.Tuple2<Token, T_TypeExpr> = io.littlelanguages.data.Tuple2(a6t1, a6t2)
      a6 = a6t
    }
    val a7: Token = matchToken(TToken.TEqual)
    val a8: T_Expr = expr()
    var a9: io.littlelanguages.data.Tuple2<Token, T_Expr>? = null
    
    if (isToken(TToken.TIn)) {
      val a9t1: Token = matchToken(TToken.TIn)
      val a9t2: T_Expr = expr()
      val a9t: io.littlelanguages.data.Tuple2<Token, T_Expr> = io.littlelanguages.data.Tuple2(a9t1, a9t2)
      a9 = a9t
    }
    return visitor.visitLetExpr(a1, a2, a3, a4, a5, a6, a7, a8, a9)
  }
  
  fun parameters(): T_Parameters {
    val a1: Token = matchToken(TToken.TLParen)
    var a2: io.littlelanguages.data.Tuple2<T_ParameterType, List<io.littlelanguages.data.Tuple2<Token, T_ParameterType>>>? = null
    
    if (isToken(TToken.TLowerID)) {
      val a2t1: T_ParameterType = parameterType()
      val a2t2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_ParameterType>>()
      
      while (isToken(TToken.TComma)) {
        val a2t2t1: Token = matchToken(TToken.TComma)
        val a2t2t2: T_ParameterType = parameterType()
        val a2t2t: io.littlelanguages.data.Tuple2<Token, T_ParameterType> = io.littlelanguages.data.Tuple2(a2t2t1, a2t2t2)
        a2t2.add(a2t2t)
      }
      val a2t: io.littlelanguages.data.Tuple2<T_ParameterType, List<io.littlelanguages.data.Tuple2<Token, T_ParameterType>>> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2 = a2t
    }
    val a3: Token = matchToken(TToken.TRParen)
    return visitor.visitParameters(a1, a2, a3)
  }
  
  fun parameterType(): T_ParameterType {
    val a1: Token = matchToken(TToken.TLowerID)
    var a2: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>? = null
    
    if (isToken(TToken.TColon)) {
      val a2t1: Token = matchToken(TToken.TColon)
      val a2t2: T_TypeExpr = typeExpr()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_TypeExpr> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2 = a2t
    }
    return visitor.visitParameterType(a1, a2)
  }
  
  fun lambdaExpr(): T_LambdaExpr {
    val a1: Token = matchToken(TToken.TBackslash)
    var a2: T_TypeParams? = null
    
    if (isToken(TToken.TLBracket)) {
      val a2t: T_TypeParams = typeParams()
      a2 = a2t
    }
    val a3: Token = matchToken(TToken.TLowerID)
    var a4: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>? = null
    
    if (isToken(TToken.TColon)) {
      val a4t1: Token = matchToken(TToken.TColon)
      val a4t2: T_TypeExpr = typeExpr()
      val a4t: io.littlelanguages.data.Tuple2<Token, T_TypeExpr> = io.littlelanguages.data.Tuple2(a4t1, a4t2)
      a4 = a4t
    }
    val a5: Token = matchToken(TToken.TEqualGreaterThan)
    val a6: T_Expr = expr()
    return visitor.visitLambdaExpr(a1, a2, a3, a4, a5, a6)
  }
  
  fun logicalOrExpr(): T_LogicalOrExpr {
    val a1: T_LogicalAndExpr = logicalAndExpr()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_LogicalAndExpr>>()
    
    while (isToken(TToken.TBarBar)) {
      val a2t1: Token = matchToken(TToken.TBarBar)
      val a2t2: T_LogicalAndExpr = logicalAndExpr()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_LogicalAndExpr> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitLogicalOrExpr(a1, a2)
  }
  
  fun logicalAndExpr(): T_LogicalAndExpr {
    val a1: T_EqualityExpr = equalityExpr()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_EqualityExpr>>()
    
    while (isToken(TToken.TAmpersandAmpersand)) {
      val a2t1: Token = matchToken(TToken.TAmpersandAmpersand)
      val a2t2: T_EqualityExpr = equalityExpr()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_EqualityExpr> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitLogicalAndExpr(a1, a2)
  }
  
  fun equalityExpr(): T_EqualityExpr {
    val a1: T_AdditiveExpr = additiveExpr()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<T_EqualityOp, T_AdditiveExpr>>()
    
    while (isTokens(set4)) {
      val a2t1: T_EqualityOp = equalityOp()
      val a2t2: T_AdditiveExpr = additiveExpr()
      val a2t: io.littlelanguages.data.Tuple2<T_EqualityOp, T_AdditiveExpr> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitEqualityExpr(a1, a2)
  }
  
  fun equalityOp(): T_EqualityOp {
    when {
      isToken(TToken.TEqualEqual) -> {
        return visitor.visitEqualityOp1(matchToken(TToken.TEqualEqual))
      }
      isToken(TToken.TBangEqual) -> {
        return visitor.visitEqualityOp2(matchToken(TToken.TBangEqual))
      }
      else -> {
        throw ParsingException(peek(), set4)
      }
    }
  }
  
  fun additiveExpr(): T_AdditiveExpr {
    val a1: T_MultiplicativeExpr = multiplicativeExpr()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<T_AdditiveOp, T_MultiplicativeExpr>>()
    
    while (isTokens(set5)) {
      val a2t1: T_AdditiveOp = additiveOp()
      val a2t2: T_MultiplicativeExpr = multiplicativeExpr()
      val a2t: io.littlelanguages.data.Tuple2<T_AdditiveOp, T_MultiplicativeExpr> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitAdditiveExpr(a1, a2)
  }
  
  fun additiveOp(): T_AdditiveOp {
    when {
      isToken(TToken.TPlus) -> {
        return visitor.visitAdditiveOp1(matchToken(TToken.TPlus))
      }
      isToken(TToken.TDash) -> {
        return visitor.visitAdditiveOp2(matchToken(TToken.TDash))
      }
      else -> {
        throw ParsingException(peek(), set5)
      }
    }
  }
  
  fun multiplicativeExpr(): T_MultiplicativeExpr {
    val a1: T_ApplicationExpr = applicationExpr()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<T_MultiplicativeOp, T_ApplicationExpr>>()
    
    while (isTokens(set6)) {
      val a2t1: T_MultiplicativeOp = multiplicativeOp()
      val a2t2: T_ApplicationExpr = applicationExpr()
      val a2t: io.littlelanguages.data.Tuple2<T_MultiplicativeOp, T_ApplicationExpr> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitMultiplicativeExpr(a1, a2)
  }
  
  fun multiplicativeOp(): T_MultiplicativeOp {
    when {
      isToken(TToken.TStar) -> {
        return visitor.visitMultiplicativeOp1(matchToken(TToken.TStar))
      }
      isToken(TToken.TSlash) -> {
        return visitor.visitMultiplicativeOp2(matchToken(TToken.TSlash))
      }
      else -> {
        throw ParsingException(peek(), set6)
      }
    }
  }
  
  fun applicationExpr(): T_ApplicationExpr {
    val a1: T_SimpleExpr = simpleExpr()
    val a2= mutableListOf<io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expr, List<io.littlelanguages.data.Tuple2<Token, T_Expr>>>?, Token>>()
    
    while (isToken(TToken.TLParen)) {
      val a2t1: Token = matchToken(TToken.TLParen)
      var a2t2: io.littlelanguages.data.Tuple2<T_Expr, List<io.littlelanguages.data.Tuple2<Token, T_Expr>>>? = null
      
      if (isTokens(set2)) {
        val a2t2t1: T_Expr = expr()
        val a2t2t2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_Expr>>()
        
        while (isToken(TToken.TComma)) {
          val a2t2t2t1: Token = matchToken(TToken.TComma)
          val a2t2t2t2: T_Expr = expr()
          val a2t2t2t: io.littlelanguages.data.Tuple2<Token, T_Expr> = io.littlelanguages.data.Tuple2(a2t2t2t1, a2t2t2t2)
          a2t2t2.add(a2t2t2t)
        }
        val a2t2t: io.littlelanguages.data.Tuple2<T_Expr, List<io.littlelanguages.data.Tuple2<Token, T_Expr>>> = io.littlelanguages.data.Tuple2(a2t2t1, a2t2t2)
        a2t2 = a2t2t
      }
      val a2t3: Token = matchToken(TToken.TRParen)
      val a2t: io.littlelanguages.data.Tuple3<Token, io.littlelanguages.data.Tuple2<T_Expr, List<io.littlelanguages.data.Tuple2<Token, T_Expr>>>?, Token> = io.littlelanguages.data.Tuple3(a2t1, a2t2, a2t3)
      a2.add(a2t)
    }
    return visitor.visitApplicationExpr(a1, a2)
  }
  
  fun ifExpr(): T_IfExpr {
    val a1: Token = matchToken(TToken.TIf)
    val a2: T_Expr = expr()
    val a3: Token = matchToken(TToken.TThen)
    val a4: T_Expr = expr()
    val a5: Token = matchToken(TToken.TElse)
    val a6: T_Expr = expr()
    return visitor.visitIfExpr(a1, a2, a3, a4, a5, a6)
  }
  
  fun matchExpr(): T_MatchExpr {
    val a1: Token = matchToken(TToken.TMatch)
    val a2: T_Expr = expr()
    val a3: Token = matchToken(TToken.TWith)
    val a4: T_MatchCase = matchCase()
    val a5= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_MatchCase>>()
    
    while (isToken(TToken.TBar)) {
      val a5t1: Token = matchToken(TToken.TBar)
      val a5t2: T_MatchCase = matchCase()
      val a5t: io.littlelanguages.data.Tuple2<Token, T_MatchCase> = io.littlelanguages.data.Tuple2(a5t1, a5t2)
      a5.add(a5t)
    }
    return visitor.visitMatchExpr(a1, a2, a3, a4, a5)
  }
  
  fun matchCase(): T_MatchCase {
    val a1: T_Pattern = pattern()
    val a2: Token = matchToken(TToken.TEqualGreaterThan)
    val a3: T_Expr = expr()
    return visitor.visitMatchCase(a1, a2, a3)
  }
  
  fun simpleExpr(): T_SimpleExpr {
    val a1: T_PrimaryExpr = primaryExpr()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<Token, Token>>()
    
    while (isToken(TToken.TPeriod)) {
      val a2t1: Token = matchToken(TToken.TPeriod)
      val a2t2: Token = matchToken(TToken.TLowerID)
      val a2t: io.littlelanguages.data.Tuple2<Token, Token> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitSimpleExpr(a1, a2)
  }
  
  fun primaryExpr(): T_PrimaryExpr {
    when {
      isToken(TToken.TLiteralInt) -> {
        return visitor.visitPrimaryExpr1(matchToken(TToken.TLiteralInt))
      }
      isToken(TToken.TLiteralString) -> {
        return visitor.visitPrimaryExpr2(matchToken(TToken.TLiteralString))
      }
      isToken(TToken.TTrue) -> {
        return visitor.visitPrimaryExpr3(matchToken(TToken.TTrue))
      }
      isToken(TToken.TFalse) -> {
        return visitor.visitPrimaryExpr4(matchToken(TToken.TFalse))
      }
      isToken(TToken.TLowerID) -> {
        return visitor.visitPrimaryExpr5(variable())
      }
      isToken(TToken.TLCurly) -> {
        return visitor.visitPrimaryExpr6(record())
      }
      isToken(TToken.TLParen) -> {
        val a1: Token = matchToken(TToken.TLParen)
        val a2: T_Expr = expr()
        val a3= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_Expr>>()
        
        while (isToken(TToken.TComma)) {
          val a3t1: Token = matchToken(TToken.TComma)
          val a3t2: T_Expr = expr()
          val a3t: io.littlelanguages.data.Tuple2<Token, T_Expr> = io.littlelanguages.data.Tuple2(a3t1, a3t2)
          a3.add(a3t)
        }
        val a4: Token = matchToken(TToken.TRParen)
        return visitor.visitPrimaryExpr7(a1, a2, a3, a4)
      }
      else -> {
        throw ParsingException(peek(), set3)
      }
    }
  }
  
  fun variable(): T_Variable {
    return visitor.visitVariable(matchToken(TToken.TLowerID))
  }
  
  fun record(): T_Record {
    val a1: Token = matchToken(TToken.TLCurly)
    var a2: io.littlelanguages.data.Tuple2<T_SpreadOrField, List<io.littlelanguages.data.Tuple2<Token, T_SpreadOrField>>>? = null
    
    if (isTokens(set7)) {
      val a2t1: T_SpreadOrField = spreadOrField()
      val a2t2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_SpreadOrField>>()
      
      while (isToken(TToken.TComma)) {
        val a2t2t1: Token = matchToken(TToken.TComma)
        val a2t2t2: T_SpreadOrField = spreadOrField()
        val a2t2t: io.littlelanguages.data.Tuple2<Token, T_SpreadOrField> = io.littlelanguages.data.Tuple2(a2t2t1, a2t2t2)
        a2t2.add(a2t2t)
      }
      val a2t: io.littlelanguages.data.Tuple2<T_SpreadOrField, List<io.littlelanguages.data.Tuple2<Token, T_SpreadOrField>>> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2 = a2t
    }
    val a3: Token = matchToken(TToken.TRCurly)
    return visitor.visitRecord(a1, a2, a3)
  }
  
  fun spreadOrField(): T_SpreadOrField {
    when {
      isToken(TToken.TLowerID) -> {
        val a1: Token = matchToken(TToken.TLowerID)
        val a2: Token = matchToken(TToken.TEqual)
        val a3: T_Expr = expr()
        return visitor.visitSpreadOrField1(a1, a2, a3)
      }
      isToken(TToken.TPeriodPeriodPeriod) -> {
        val a1: Token = matchToken(TToken.TPeriodPeriodPeriod)
        val a2: T_Expr = expr()
        return visitor.visitSpreadOrField2(a1, a2)
      }
      else -> {
        throw ParsingException(peek(), set7)
      }
    }
  }
  
  fun typeExpr(): T_TypeExpr {
    val a1: T_MergeType = mergeType()
    var a2: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>? = null
    
    if (isToken(TToken.TDashGreaterThan)) {
      val a2t1: Token = matchToken(TToken.TDashGreaterThan)
      val a2t2: T_TypeExpr = typeExpr()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_TypeExpr> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2 = a2t
    }
    return visitor.visitTypeExpr(a1, a2)
  }
  
  fun mergeType(): T_MergeType {
    val a1: T_UnionType = unionType()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_UnionType>>()
    
    while (isToken(TToken.TAmpersand)) {
      val a2t1: Token = matchToken(TToken.TAmpersand)
      val a2t2: T_UnionType = unionType()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_UnionType> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitMergeType(a1, a2)
  }
  
  fun unionType(): T_UnionType {
    val a1: T_PrimaryType = primaryType()
    val a2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_PrimaryType>>()
    
    while (isToken(TToken.TBar)) {
      val a2t1: Token = matchToken(TToken.TBar)
      val a2t2: T_PrimaryType = primaryType()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_PrimaryType> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2.add(a2t)
    }
    return visitor.visitUnionType(a1, a2)
  }
  
  fun primaryType(): T_PrimaryType {
    when {
      isToken(TToken.TUpperID) -> {
        return visitor.visitPrimaryType1(baseType())
      }
      isToken(TToken.TLCurly) -> {
        return visitor.visitPrimaryType2(recordType())
      }
      isToken(TToken.TLParen) -> {
        return visitor.visitPrimaryType3(tupleType())
      }
      isToken(TToken.TLiteralString) -> {
        return visitor.visitPrimaryType4(matchToken(TToken.TLiteralString))
      }
      else -> {
        throw ParsingException(peek(), set8)
      }
    }
  }
  
  fun baseType(): T_BaseType {
    val a1: Token = matchToken(TToken.TUpperID)
    var a2: T_GenericArgs? = null
    
    if (isToken(TToken.TLBracket)) {
      val a2t: T_GenericArgs = genericArgs()
      a2 = a2t
    }
    return visitor.visitBaseType(a1, a2)
  }
  
  fun genericArgs(): T_GenericArgs {
    val a1: Token = matchToken(TToken.TLBracket)
    val a2: T_TypeExpr = typeExpr()
    val a3= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_TypeExpr>>()
    
    while (isToken(TToken.TComma)) {
      val a3t1: Token = matchToken(TToken.TComma)
      val a3t2: T_TypeExpr = typeExpr()
      val a3t: io.littlelanguages.data.Tuple2<Token, T_TypeExpr> = io.littlelanguages.data.Tuple2(a3t1, a3t2)
      a3.add(a3t)
    }
    val a4: Token = matchToken(TToken.TRBracket)
    return visitor.visitGenericArgs(a1, a2, a3, a4)
  }
  
  fun recordType(): T_RecordType {
    val a1: Token = matchToken(TToken.TLCurly)
    var a2: io.littlelanguages.data.Tuple2<T_TypeField, List<io.littlelanguages.data.Tuple2<Token, T_TypeField>>>? = null
    
    if (isToken(TToken.TLowerID)) {
      val a2t1: T_TypeField = typeField()
      val a2t2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_TypeField>>()
      
      while (isToken(TToken.TComma)) {
        val a2t2t1: Token = matchToken(TToken.TComma)
        val a2t2t2: T_TypeField = typeField()
        val a2t2t: io.littlelanguages.data.Tuple2<Token, T_TypeField> = io.littlelanguages.data.Tuple2(a2t2t1, a2t2t2)
        a2t2.add(a2t2t)
      }
      val a2t: io.littlelanguages.data.Tuple2<T_TypeField, List<io.littlelanguages.data.Tuple2<Token, T_TypeField>>> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2 = a2t
    }
    var a3: io.littlelanguages.data.Tuple2<Token, Token>? = null
    
    if (isToken(TToken.TBar)) {
      val a3t1: Token = matchToken(TToken.TBar)
      val a3t2: Token = matchToken(TToken.TUpperID)
      val a3t: io.littlelanguages.data.Tuple2<Token, Token> = io.littlelanguages.data.Tuple2(a3t1, a3t2)
      a3 = a3t
    }
    val a4: Token = matchToken(TToken.TRCurly)
    return visitor.visitRecordType(a1, a2, a3, a4)
  }
  
  fun typeField(): T_TypeField {
    val a1: Token = matchToken(TToken.TLowerID)
    val a2: Token = matchToken(TToken.TColon)
    val a3: T_TypeExpr = typeExpr()
    return visitor.visitTypeField(a1, a2, a3)
  }
  
  fun tupleType(): T_TupleType {
    val a1: Token = matchToken(TToken.TLParen)
    val a2: T_TypeExpr = typeExpr()
    val a3= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_TypeExpr>>()
    
    while (isToken(TToken.TComma)) {
      val a3t1: Token = matchToken(TToken.TComma)
      val a3t2: T_TypeExpr = typeExpr()
      val a3t: io.littlelanguages.data.Tuple2<Token, T_TypeExpr> = io.littlelanguages.data.Tuple2(a3t1, a3t2)
      a3.add(a3t)
    }
    val a4: Token = matchToken(TToken.TRParen)
    return visitor.visitTupleType(a1, a2, a3, a4)
  }
  
  fun typeParams(): T_TypeParams {
    val a1: Token = matchToken(TToken.TLBracket)
    val a2: T_TypeParam = typeParam()
    val a3= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_TypeParam>>()
    
    while (isToken(TToken.TComma)) {
      val a3t1: Token = matchToken(TToken.TComma)
      val a3t2: T_TypeParam = typeParam()
      val a3t: io.littlelanguages.data.Tuple2<Token, T_TypeParam> = io.littlelanguages.data.Tuple2(a3t1, a3t2)
      a3.add(a3t)
    }
    val a4: Token = matchToken(TToken.TRBracket)
    return visitor.visitTypeParams(a1, a2, a3, a4)
  }
  
  fun typeParam(): T_TypeParam {
    val a1: Token = matchToken(TToken.TUpperID)
    var a2: io.littlelanguages.data.Tuple2<Token, T_TypeExpr>? = null
    
    if (isToken(TToken.TColon)) {
      val a2t1: Token = matchToken(TToken.TColon)
      val a2t2: T_TypeExpr = typeExpr()
      val a2t: io.littlelanguages.data.Tuple2<Token, T_TypeExpr> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2 = a2t
    }
    return visitor.visitTypeParam(a1, a2)
  }
  
  fun pattern(): T_Pattern {
    when {
      isToken(TToken.TLCurly) -> {
        return visitor.visitPattern1(recordPattern())
      }
      isToken(TToken.TLowerID) -> {
        return visitor.visitPattern2(varPattern())
      }
      isToken(TToken.TUnderscore) -> {
        return visitor.visitPattern3(wildcard())
      }
      isTokens(set9) -> {
        return visitor.visitPattern4(literalPattern())
      }
      isToken(TToken.TLParen) -> {
        val a1: Token = matchToken(TToken.TLParen)
        val a2: T_Pattern = pattern()
        val a3= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_Pattern>>()
        
        while (isToken(TToken.TComma)) {
          val a3t1: Token = matchToken(TToken.TComma)
          val a3t2: T_Pattern = pattern()
          val a3t: io.littlelanguages.data.Tuple2<Token, T_Pattern> = io.littlelanguages.data.Tuple2(a3t1, a3t2)
          a3.add(a3t)
        }
        val a4: Token = matchToken(TToken.TRParen)
        return visitor.visitPattern5(a1, a2, a3, a4)
      }
      else -> {
        throw ParsingException(peek(), set10)
      }
    }
  }
  
  fun recordPattern(): T_RecordPattern {
    val a1: Token = matchToken(TToken.TLCurly)
    var a2: io.littlelanguages.data.Tuple2<T_FieldPattern, List<io.littlelanguages.data.Tuple2<Token, T_FieldPattern>>>? = null
    
    if (isToken(TToken.TLowerID)) {
      val a2t1: T_FieldPattern = fieldPattern()
      val a2t2= mutableListOf<io.littlelanguages.data.Tuple2<Token, T_FieldPattern>>()
      
      while (isToken(TToken.TComma)) {
        val a2t2t1: Token = matchToken(TToken.TComma)
        val a2t2t2: T_FieldPattern = fieldPattern()
        val a2t2t: io.littlelanguages.data.Tuple2<Token, T_FieldPattern> = io.littlelanguages.data.Tuple2(a2t2t1, a2t2t2)
        a2t2.add(a2t2t)
      }
      val a2t: io.littlelanguages.data.Tuple2<T_FieldPattern, List<io.littlelanguages.data.Tuple2<Token, T_FieldPattern>>> = io.littlelanguages.data.Tuple2(a2t1, a2t2)
      a2 = a2t
    }
    val a3: Token = matchToken(TToken.TRCurly)
    return visitor.visitRecordPattern(a1, a2, a3)
  }
  
  fun fieldPattern(): T_FieldPattern {
    val a1: Token = matchToken(TToken.TLowerID)
    val a2: Token = matchToken(TToken.TEqual)
    val a3: T_Pattern = pattern()
    return visitor.visitFieldPattern(a1, a2, a3)
  }
  
  fun varPattern(): T_VarPattern {
    return visitor.visitVarPattern(matchToken(TToken.TLowerID))
  }
  
  fun wildcard(): T_Wildcard {
    return visitor.visitWildcard(matchToken(TToken.TUnderscore))
  }
  
  fun literalPattern(): T_LiteralPattern {
    when {
      isToken(TToken.TLiteralInt) -> {
        return visitor.visitLiteralPattern1(matchToken(TToken.TLiteralInt))
      }
      isToken(TToken.TLiteralString) -> {
        return visitor.visitLiteralPattern2(matchToken(TToken.TLiteralString))
      }
      isToken(TToken.TTrue) -> {
        return visitor.visitLiteralPattern3(matchToken(TToken.TTrue))
      }
      isToken(TToken.TFalse) -> {
        return visitor.visitLiteralPattern4(matchToken(TToken.TFalse))
      }
      else -> {
        throw ParsingException(peek(), set9)
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

private val set1 = setOf(TToken.TType, TToken.TLet, TToken.TBackslash, TToken.TIf, TToken.TMatch, TToken.TLiteralInt, TToken.TLiteralString, TToken.TTrue, TToken.TFalse, TToken.TLParen, TToken.TLowerID, TToken.TLCurly)

private val set2 = setOf(TToken.TLet, TToken.TBackslash, TToken.TIf, TToken.TMatch, TToken.TLiteralInt, TToken.TLiteralString, TToken.TTrue, TToken.TFalse, TToken.TLParen, TToken.TLowerID, TToken.TLCurly)

private val set3 = setOf(TToken.TLiteralInt, TToken.TLiteralString, TToken.TTrue, TToken.TFalse, TToken.TLParen, TToken.TLowerID, TToken.TLCurly)

private val set4 = setOf(TToken.TEqualEqual, TToken.TBangEqual)

private val set5 = setOf(TToken.TPlus, TToken.TDash)

private val set6 = setOf(TToken.TStar, TToken.TSlash)

private val set7 = setOf(TToken.TLowerID, TToken.TPeriodPeriodPeriod)

private val set8 = setOf(TToken.TUpperID, TToken.TLCurly, TToken.TLParen, TToken.TLiteralString)

private val set9 = setOf(TToken.TLiteralInt, TToken.TLiteralString, TToken.TTrue, TToken.TFalse)

private val set10 = setOf(TToken.TLCurly, TToken.TLowerID, TToken.TUnderscore, TToken.TLiteralInt, TToken.TLiteralString, TToken.TTrue, TToken.TFalse, TToken.TLParen)

class ParsingException(
  val found: Token,
  val expected: Set<TToken>) : Exception()
