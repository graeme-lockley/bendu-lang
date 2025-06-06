package io.littlelanguages.minibendu

import io.littlelanguages.minibendu.parser.*
import java.io.StringReader

fun parse(scanner: Scanner, errors: Errors): Program {
    try {
        return Parser(scanner, ParserVisitor(errors)).program()
    } catch (e: ParsingException) {
        errors.addError(ParsingError(e.found, e.expected))
        return Program( emptyList())
    }
}

fun parse(input: String, errors: Errors): Program =
    parse(Scanner(StringReader(input)), errors)

fun parseType(input: String, errors: Errors): TypeExpr? {
    return try {
        val scanner = Scanner(StringReader(input))
        val parser = Parser(scanner, ParserVisitor(errors))
        parser.typeExpr()
    } catch (e: ParsingException) {
        errors.addError(ParsingError(e.found, e.expected))
        null
    } catch (_: Exception) {
        // Handle any other parsing errors gracefully
        null
    }
}
