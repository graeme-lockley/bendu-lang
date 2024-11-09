package io.littlelanguages.bendu.compiler

import java.io.ByteArrayOutputStream

class ByteBuilder {
    private val outputStream = ByteArrayOutputStream()

    fun append(byte: Byte): ByteBuilder {
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

    fun append(bytes: ByteArray): ByteBuilder {
        outputStream.write(bytes)
        return this
    }

    fun toByteArray(): ByteArray =
        outputStream.toByteArray()

    fun size(): Int =
        outputStream.size()

    fun reset() {
        outputStream.reset()
    }

    fun writeAtPosition(position: Int, byte: Byte): ByteBuilder {
        val byteArray = outputStream.toByteArray()
        if (position < 0 || position >= byteArray.size) {
            throw IndexOutOfBoundsException("Position $position is out of bounds")
        }
        byteArray[position] = byte
        outputStream.reset()
        outputStream.write(byteArray)
        return this
    }

    fun writeIntAtPosition(position: Int, value: Int): ByteBuilder {
        val byteArray = outputStream.toByteArray()
        if (position < 0 || position + 4 >= byteArray.size) {
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