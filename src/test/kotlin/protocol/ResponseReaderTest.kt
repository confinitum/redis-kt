package protocol

import com.confinitum.common.redis.protocol.readRedisMessage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.utils.io.*

class ResponseReaderTest: StringSpec({
    "test basic types" {

        "+OK\r\n".toBRC().readRedisMessage() shouldBe "OK"

        ":4711\r\n".toBRC().readRedisMessage() shouldBe 4711L

        shouldThrow<NumberFormatException> {
            ":471a\r\n".toBRC().readRedisMessage() shouldBe 471L
        }


    }
    "test bulk input" {

        "$9\r\nsome text\r\n".toBRC().readRedisMessage() shouldBe "some text".toByteArray()

        val twoliner = """some
            |text
        """.trimIndent().trimMargin()
        "\$${twoliner.length}\r\n$twoliner\r\n".toBRC().readRedisMessage() shouldBe "some\ntext".toByteArray()

    }

    "test array" {
        val sut = "*2\r\n$3\r\none\r\n$3\r\ntwo\r\n".toBRC().readRedisMessage()

        sut.shouldNotBeNull()
        sut.shouldBeInstanceOf<List<Any>>()
        val aSut = (sut as List<Any>)
        aSut  shouldHaveSize 2
        aSut[0] shouldBe "one".toByteArray()
        aSut shouldBe listOf("one".toByteArray(), "two".toByteArray())

    }

})

private fun String.toBRC() = ByteReadChannel(this.toByteArray())