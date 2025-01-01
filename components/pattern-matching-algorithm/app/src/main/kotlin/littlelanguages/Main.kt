package littlelanguages

fun main(args: Array<String>) {
    println("Hello World!")

    println(partition(listOf(1, 3, 2, 4, 1)) { x -> x % 2 == 0 })
}
