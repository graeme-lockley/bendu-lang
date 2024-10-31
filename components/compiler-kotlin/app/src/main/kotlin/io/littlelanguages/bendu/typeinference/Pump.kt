package io.littlelanguages.bendu.typeinference

data class Pump(private var counter: Int = 0) {
    fun next(): TVar {
        counter += 1
        return TVar(counter)
    }

    fun nextN(size: Int): List<TVar> =
        (1..size).map { next() }
}