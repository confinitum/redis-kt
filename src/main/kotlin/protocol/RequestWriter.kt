package com.confinitum.common.redis.protocol

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import java.nio.charset.Charset
import kotlin.text.*

internal fun BytePacketBuilder.writeRedisValue(
    value: Any?,
    forceBulk: Boolean = true,
    charset: Charset = Charsets.UTF_8
): Unit = when {
    value is List<*> -> writeListValue(value, forceBulk, charset)
    value is Array<*> -> writeArrayValue(value, forceBulk, charset)
    value is ByteArray -> writeByteArray(value)
    value is Throwable -> writeThrowable(value)
    forceBulk -> writeBulk(value, charset)
    value is String -> writeString(value, charset)
    value is Int || value is Long -> writeIntegral(value)
    value == null -> writeNull()
    else -> error("Unsupported $value to write")
}

private fun <T : List<*>> BytePacketBuilder.writeListValue(
    value: T,
    forceBulk: Boolean = true, charset: Charset = Charsets.UTF_8
) {
    append(RedisType.ARRAY)
    append(value.size.toString())
    appendEOL()
    for (item in value) writeRedisValue(item, forceBulk, charset)
}

private fun BytePacketBuilder.writeArrayValue(
    value: Array<*>,
    forceBulk: Boolean = true, charset: Charset = Charsets.UTF_8
) {
    append(RedisType.ARRAY)
    append(value.size.toString())
    appendEOL()
    for (item in value) writeRedisValue(item, forceBulk, charset)
}

private fun BytePacketBuilder.writeBulk(value: Any?, charset: Charset) {
    val packet = buildPacket {
        writeStringEncoded(value.toString(), charset = charset)
    }
    append(RedisType.BULK)
    append(packet.remaining.toString())
    appendEOL()
    writePacket(packet)
    appendEOL()
}

private fun BytePacketBuilder.writeByteArray(value: ByteArray) {
    append(RedisType.BULK)
    append(value.size.toString())
    appendEOL()
    writeFully(value)
    appendEOL()
}

private fun BytePacketBuilder.writeString(value: String, charset: Charset) {
    if (value.contains('\n') || value.contains('\r')) {
        val packet = buildPacket { writeStringEncoded(value, charset) }
        append(RedisType.BULK)
        append(packet.remaining.toString())
        appendEOL()
        writePacket(packet)
        appendEOL()
        return
    }

    append('+')
    writeStringEncoded(value, charset)
    appendEOL()
}

private fun BytePacketBuilder.writeIntegral(value: Any) {
    append(RedisType.NUMBER)
    append(value.toString())
    appendEOL()
}

private fun BytePacketBuilder.writeNull() {
    append(RedisType.BULK)
    append("-1")
    appendEOL()
}

private fun BytePacketBuilder.writeThrowable(value: Throwable) {
    val message = (value.message ?: "Error")
        .replace("\r", "")
        .replace("\n", "")

    append(RedisType.ERROR)
    append(message)
    appendEOL()
}

private fun BytePacketBuilder.append(type: RedisType) {
    writeByte(type.code)
}

private fun BytePacketBuilder.appendEOL() {
    writeFully(EOL)
}

private fun BytePacketBuilder.writeStringEncoded(string: String, charset: Charset) {
    writeFully(charset.encode(string))
}


internal data class SimpleString(val value: String)
internal fun String.toSimple() = SimpleString(this)

/**
 * for testing
 */
internal fun BytePacketBuilder.writeRedisServerValue(
    value: Any?,
    forceBulk: Boolean = true,
    charset: Charset = Charsets.UTF_8
): Unit = when {
    value is List<*> -> writeListValue(value, forceBulk, charset)
    value is Array<*> -> writeArrayValue(value, forceBulk, charset)
    value is ByteArray -> writeByteArray(value)
    value is Throwable -> writeThrowable(value)
    value is String -> writeBulk(value, charset)
    value is SimpleString -> writeString(value.value, charset)
    value is Int || value is Long -> writeIntegral(value)
    value == null -> writeNull()
    else -> error("Unsupported $value to write")
}

