package io.littlelanguages.bendu.typeinference

import io.littlelanguages.scanpiler.Location

data class Pump(private var counter: Int = 0) {
    fun next(location: Location? = null): TVar {
        counter += 1
        return TVar(counter, location)
    }

    fun nextN(size: Int): List<TVar> =
        (1..size).map { next() }
}