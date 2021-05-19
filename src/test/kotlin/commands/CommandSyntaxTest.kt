import com.confinitum.common.redis.commands.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class CommandSyntaxTest: StringSpec({
    "connection commands" {
        val client = RedisClientMock()

        client.onExec {
            it shouldBe arrayOf("AUTH", "secret")
            "OK"
        }.auth("secret")

        client.onExec {
            it shouldBe arrayOf("ECHO", "foobar")
            "foobar"
        }.echo("foobar")

        client.onExec {
            it shouldBe arrayOf("PING")
            "PONG"
        }.ping()

        client.onExec {
            it shouldBe arrayOf("PING", "foobar")
            "foobar"
        }.ping("foobar")

        client.onExec {
            it shouldBe arrayOf("SELECT", 42)
            this
        }.select(42)

        client.onExec {
            it shouldBe arrayOf("SWAPDB", 41, 42)
            "foobar"
        }.swapdb(41, 42)

        client.onExec {
            it shouldBe arrayOf("QUIT")
            Unit
        }.quit()

    }

    "string commands" {
        val client = RedisClientMock()

        client.onExec {
            it shouldBe arrayOf("APPEND", "foo", "bar")
            3
        }.append("foo", "bar")

        client.onExec {
            it shouldBe arrayOf("BITCOUNT", "foo")
            0
        }.bitcount("foo")

        client.onExec {
            it shouldBe arrayOf("BITCOUNT", "foo", 1, 2)
            0
        }.bitcount("foo",1,2)

        client.onExec {
            it shouldBe arrayOf("BITCOUNT", "foo", 2, 3)
            0
        }.bitcount("foo",2L .. 3L)

        client.onExec {
            it shouldBe arrayOf("BITFIELD", "foo", "INCRBY","i5", 100, 1,
                "GET", "u4", 0,
                "SET", "u8", 8, 255)
            listOf<Any>()
        }.bitfield("foo") {
            incrby(i(5), 100, 1)
            get(u(4), 0)
            set(u(8), 8, 255)
        }

        client.onExec {
            it shouldBe arrayOf("BITOP", "AND", "foo", "bar")
            3
        }.bitop(RedisBitop.AND,"foo", "bar")
        client.onExec {
            it shouldBe arrayOf("BITOP", "AND", "foo", "bar")
            3
        }.bitopAnd("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("BITOP", "OR", "foo", "bar")
            3
        }.bitopOr("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("BITOP", "XOR", "foo", "bar")
            3
        }.bitopXor("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("BITOP", "NOT", "foo", "bar")
            3
        }.bitopNot("foo", "bar")

        client.onExec {
            it shouldBe arrayOf("BITPOS", "foo", 1)
            3
        }.bitpos("foo", 1, null)
        client.onExec {
            it shouldBe arrayOf("BITPOS", "foo", 0, 0, 2)
            3
        }.bitpos("foo", 0, 0L .. 2L)

        client.onExec {
            it shouldBe arrayOf("DECR", "foo")
            0
        }.decr("foo")
        client.onExec {
            it shouldBe arrayOf("DECRBY", "foo", 5)
            0
        }.decrby("foo", 5)
        client.onExec {
            it shouldBe arrayOf("INCR", "foo")
            0
        }.incr("foo")
        client.onExec {
            it shouldBe arrayOf("INCRBY", "foo", 5)
            0
        }.incrby("foo", 5)
        client.onExec {
            it shouldBe arrayOf("INCRBYFLOAT", "foo", 5.5)
            0
        }.incrbyfloat("foo", 5.5)

        client.onExec {
            it shouldBe arrayOf("GET", "foo")
            ""
        }.get("foo")
        client.onExec {
            it shouldBe arrayOf("GETBIT", "foo", 8)
            0
        }.getbit("foo", 8)
        client.onExec {
            it shouldBe arrayOf("GETRANGE", "foo", 0, 3)
            ""
        }.getrange("foo", 0,3)

        client.onExec {
            it shouldBe arrayOf("SET", "foo", "bar")
            ""
        }.set("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("GETSET", "foo", "bar")
            ""
        }.getset("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("SETNX", "foo", "bar")
            0
        }.setnx("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("SETEX", "foo", 1, "bar")
            ""
        }.setex("foo", 1, "bar")
        client.onExec {
            it shouldBe arrayOf("PSETEX", "foo", 100, "bar")
            ""
        }.psetex("foo", 100, "bar")

        client.onExec {
            it shouldBe arrayOf("SETBIT", "foo", 8, 1)
            0
        }.setbit("foo", 8, true)

        client.onExec {
            it shouldBe arrayOf("SETRANGE", "foo", 2, "bar")
            0
        }.setrange("foo", 2, "bar")

        client.onExec {
            it shouldBe arrayOf("STRLEN", "foo")
            0
        }.strlen("foo")

    }

    " commands" {
        val client = RedisClientMock()

        client.onExec {
            it shouldBe arrayOf("PING")
            "PONG"
        }.ping()
    }

})

class PermutationTests: FunSpec({
    val client = RedisClientMock()

    context("String commands") {
        context("set with expiration") {
            withData(
                RedisSetMode.SET_ALWAYS,
                RedisSetMode.SET_ONLY_IF_EXISTS,
                RedisSetMode.SET_ONLY_IF_NOT_EXISTS
            ) { m ->
                client.onExec {
                    it shouldBe arrayOf("SET", "foo", "bar", "PX", 100, m.v).filterNotNull()
                    ""
                }.set("foo", "bar", 100, m)
            }
        }
        context("mget") {
            withData(
                arrayOf("foo"),
                arrayOf("foo", "bar"),
                arrayOf("foo", "bar", "foobar")
            ) { m ->
                client.onExec {
                    it shouldBe arrayOf("MGET", *m)
                    emptyList<String>()
                }.mget(*m)
            }
        }
        context("mset") {
            withData(
                arrayOf("foo" to "bar"),
                arrayOf("foo" to "bar", "one" to "two")
            ) { m ->
                val flat = m.flatMap { (k, v) -> listOf(k, v) }.toTypedArray()
                client.onExec {
                    it shouldBe arrayOf("MSET", *flat)
                    emptyList<String>()
                }.mset(*m)
            }
        }
        context("msetnx") {
            withData(
                arrayOf("foo" to "bar"),
                arrayOf("foo" to "bar", "one" to "two")
            ) { m ->
                val flat = m.flatMap { (k, v) -> listOf(k, v) }.toTypedArray()
                client.onExec {
                    it shouldBe arrayOf("MSETNX", *flat)
                    emptyList<String>()
                }.msetnx(*m)
            }
        }
        context("mset with map") {
            withData(
                mapOf("foo" to "bar"),
                mapOf("foo" to "bar", "one" to "two")
            ) { m ->
                val flat = m.flatMap { (k, v) -> listOf(k, v) }.toTypedArray()
                client.onExec {
                    it shouldBe arrayOf("MSET", *flat)
                    emptyList<String>()
                }.mset(m)
            }
        }
        context("msetnx with map") {
            withData(
                mapOf("foo" to "bar"),
                mapOf("foo" to "bar", "one" to "two")
            ) { m ->
                val flat = m.flatMap { (k, v) -> listOf(k, v) }.toTypedArray()
                client.onExec {
                    it shouldBe arrayOf("MSETNX", *flat)
                    emptyList<String>()
                }.msetnx(m)
            }
        }
    }
})
