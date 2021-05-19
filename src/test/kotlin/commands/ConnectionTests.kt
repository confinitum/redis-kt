package commands

import RedisTestServer
import com.confinitum.common.redis.RedisClient
import com.confinitum.common.redis.commands.echo
import com.confinitum.common.redis.commands.ping
import com.confinitum.common.redis.protocol.RedisException
import com.confinitum.common.redis.protocol.SimpleString
import com.confinitum.common.redis.protocol.toSimple
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.channels.ClosedReceiveChannelException

class ConnectionTests: StringSpec({
    "ping and echo" {
        val server = RedisTestServer(verbose = true)
        server.start()

        server.request("PING").answerWith("PONG".toSimple())
        server.request("PING", "hello").answerWith("hello")
        server.request("ECHO", "hello echo").answerWith("hello echo")

        val redis = RedisClient(server.host, server.port)

        redis.ping() shouldBe "PONG"
        redis.ping("hello") shouldBe "hello"
        redis.echo("hello echo") shouldBe "hello echo"

        redis.close()
        server.stop()
    }
    "authenticate before send" {
        val server = RedisTestServer()
        server.start()

        server.request("PING").answerWith(SimpleString("PONG"))
        server.request("AUTH", "secret").answerWith(SimpleString("OK"))

        val redis = RedisClient(server.host, server.port,password = "secret")

        redis.ping() shouldBe "PONG"

        redis.close()
        server.stop()
    }
    "wrong authentication throws exception" {
        val server = RedisTestServer()
        server.start()

        server.request("PING").answerWith(SimpleString("PONG"))
        server.request("AUTH", "secret").answerWith(RedisException("AUTH Error"))

        val redis = RedisClient(server.host, server.port, password = "secret")

        shouldThrow<RedisException> {
            redis.ping()
        }.message shouldStartWith "AUTH Error"

        shouldThrow<ClosedReceiveChannelException> {
            redis.ping()
        }

        redis.close()
        server.stop()
    }
})