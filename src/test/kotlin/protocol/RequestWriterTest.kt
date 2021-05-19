package protocol

import com.confinitum.common.redis.RedisClient
import com.confinitum.common.redis.commands.echo
import com.confinitum.common.redis.commands.ping
import com.confinitum.common.redis.protocol.toSimple
import com.confinitum.common.redis.protocol.writeRedisServerValue
import com.confinitum.common.redis.protocol.writeRedisValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.utils.io.core.*

class RequestWriterTest: StringSpec({
    "test basic types" {
        val packet = BytePacketBuilder()

        //bulk int
        packet.reset()
        packet.writeRedisValue(4711)
        packet.build().readText() shouldBe "$4\r\n4711\r\n"

        //bulk long
        packet.reset()
        packet.writeRedisValue(4711.toLong() )
        packet.build().readText() shouldBe "$4\r\n4711\r\n"

        //int
        packet.reset()
        packet.writeRedisValue(4711, false)
        packet.build().readText() shouldBe ":4711\r\n"

        //long
        packet.reset()
        packet.writeRedisValue(4711.toLong(), false)
        packet.build().readText() shouldBe ":4711\r\n"

    }
    "test bulk string" {
        val packet = BytePacketBuilder()

        //bulk string
        packet.writeRedisValue("some string")
        packet.build().readText() shouldBe "$11\r\nsome string\r\n"

        //bulk string with \n only
        packet.writeRedisValue("some string\nsecond line")
        packet.build().readText() shouldBe "$23\r\nsome string\nsecond line\r\n"

        //bulk string with nl
        val twoliner = """some string
            |second line"""
        packet.writeRedisValue(twoliner.trimMargin())
        packet.build().readText() shouldBe "$23\r\nsome string\nsecond line\r\n"
    }
    "test simple string" {
        val packet = BytePacketBuilder()

        //simple string
        packet.writeRedisValue("some",false)
        packet.build().readText() shouldBe "+some\r\n"

        //simple string fallback
        packet.writeRedisValue("some\nother",false)
        packet.build().readText() shouldBe "$10\r\nsome\nother\r\n"

    }
    "test null value" {
        val packet = BytePacketBuilder()

        //bulk null
        packet.reset()
        packet.writeRedisValue(null)
        packet.build().readText() shouldBe "$4\r\nnull\r\n"

        //null
        packet.reset()
        packet.writeRedisValue(null, false)
        packet.build().readText() shouldBe "$-1\r\n"

    }
    "test array" {
        val packet = BytePacketBuilder()

        packet.reset()
        packet.writeRedisValue(arrayOf("one", "two"))
        packet.build().readText() shouldBe "*2\r\n$3\r\none\r\n\$3\r\ntwo\r\n"

        packet.reset()
        packet.writeRedisValue(arrayOf("one", 4711))
        packet.build().readText() shouldBe "*2\r\n$3\r\none\r\n\$4\r\n4711\r\n"

        packet.reset()
        packet.writeRedisValue(arrayOf(123, 4711))
        packet.build().readText() shouldBe "*2\r\n$3\r\n123\r\n\$4\r\n4711\r\n"

    }
    "test list" {
        val packet = BytePacketBuilder()

        packet.reset()
        packet.writeRedisValue(listOf("one", "two"))
        packet.build().readText() shouldBe "*2\r\n$3\r\none\r\n\$3\r\ntwo\r\n"

        packet.reset()
        packet.writeRedisValue(listOf("one", 4711))
        packet.build().readText() shouldBe "*2\r\n$3\r\none\r\n\$4\r\n4711\r\n"

        packet.reset()
        packet.writeRedisValue(listOf(123, 4711))
        packet.build().readText() shouldBe "*2\r\n$3\r\n123\r\n\$4\r\n4711\r\n"

    }
    "test exception" {
        val packet = BytePacketBuilder()

        packet.reset()
        packet.writeRedisValue(IllegalStateException("state error"))
        packet.build().readText() shouldBe "-state error\r\n"

        packet.reset()
        packet.writeRedisValue(IllegalStateException())
        packet.build().readText() shouldBe "-Error\r\n"
    }

    "test server mock method" {
        val packet = BytePacketBuilder()

        //always expect non bulk output
        //int
        packet.reset()
        packet.writeRedisServerValue(4711)
        packet.build().readText() shouldBe ":4711\r\n"

        //long
        packet.reset()
        packet.writeRedisServerValue(4711.toLong())
        packet.build().readText() shouldBe ":4711\r\n"

        //null
        packet.reset()
        packet.writeRedisServerValue(null)
        packet.build().readText() shouldBe "$-1\r\n"

    }

})