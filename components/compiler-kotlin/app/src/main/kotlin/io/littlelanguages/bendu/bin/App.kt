package io.littlelanguages.bendu.bin

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

class App {
    val greeting: String
        get() {
            return "Hello World!"
        }
}

fun main(args: Array<String>) {
    val parser = ArgParser("app")
    val verbose by parser.option(ArgType.Boolean, description = "Enable verbose mode").default(false)

    parser.parse(args)

    println("Verbose: $verbose")
    println(App().greeting)
}
