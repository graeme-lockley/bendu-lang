package io.littlelanguages.bendu.compiler

import java.io.ByteArrayOutputStream

class ByteBuilder {
    private val outputStream = ByteArrayOutputStream()

    private fun append(byte: Byte): ByteBuilder {
        outputStream.write(byte.toInt())
        return this
    }

    fun appendInstruction(op: Instructions): ByteBuilder =
        append(op.op)

    fun appendChar(c: Int) =
        outputStream.write(c)

    fun appendFloat(value: Float): ByteBuilder {
        val bytes = ByteArray(4)
        val intBits = value.toRawBits()
        bytes[0] = (intBits shr 24).toByte()
        bytes[1] = (intBits shr 16).toByte()
        bytes[2] = (intBits shr 8).toByte()
        bytes[3] = intBits.toByte()
        outputStream.write(bytes)
        return this
    }

    fun appendInt(value: Int): ByteBuilder {
        val bytes = ByteArray(4)
        bytes[0] = (value shr 24).toByte()
        bytes[1] = (value shr 16).toByte()
        bytes[2] = (value shr 8).toByte()
        bytes[3] = value.toByte()
        outputStream.write(bytes)
        return this
    }

    fun appendLong(value: Long): ByteBuilder {
        val bytes = ByteArray(8)
        bytes[0] = (value shr 56).toByte()
        bytes[1] = (value shr 48).toByte()
        bytes[2] = (value shr 40).toByte()
        bytes[3] = (value shr 32).toByte()
        bytes[4] = (value shr 24).toByte()
        bytes[5] = (value shr 16).toByte()
        bytes[6] = (value shr 8).toByte()
        bytes[7] = value.toByte()
        outputStream.write(bytes)
        return this
    }

    fun appendString(value: String): ByteBuilder {
        appendInt(value.length)
        value.forEach { appendChar(it.code) }

        return this
    }

    fun append(bytes: ByteArray): ByteBuilder {
        outputStream.write(bytes)
        return this
    }

    fun toByteArray(): ByteArray =
        outputStream.toByteArray()

    fun size(): Int =
        outputStream.size()

    fun writeIntAtPosition(position: Int, value: Int): ByteBuilder {
        val byteArray = outputStream.toByteArray()
        if (position < 0 || position + 4 > byteArray.size) {
            throw IndexOutOfBoundsException("Position $position is out of bounds")
        }
        byteArray[position] = (value shr 24).toByte()
        byteArray[position + 1] = (value shr 16).toByte()
        byteArray[position + 2] = (value shr 8).toByte()
        byteArray[position + 3] = value.toByte()
        outputStream.reset()
        outputStream.write(byteArray)
        return this
    }
}