import com.confinitum.common.redis.commands.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class CommandSyntaxTest : StringSpec({
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
        }.bitcount("foo", 1, 2)

        client.onExec {
            it shouldBe arrayOf("BITCOUNT", "foo", 2, 3)
            0
        }.bitcount("foo", 2L..3L)

        client.onExec {
            it shouldBe arrayOf(
                "BITFIELD", "foo", "INCRBY", "i5", 100, 1,
                "GET", "u4", 0,
                "SET", "u8", 8, 255
            )
            listOf<Any>()
        }.bitfield("foo") {
            incrby(i(5), 100, 1)
            get(u(4), 0)
            set(u(8), 8, 255)
        }

        client.onExec {
            it shouldBe arrayOf("BITOP", "AND", "foo", "bar")
            3
        }.bitop(RedisBitop.AND, "foo", "bar")
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
        }.bitpos("foo", 0, 0L..2L)

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
        }.getrange("foo", 0, 3)

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

    "hashes commands" {
        val client = RedisClientMock()

        client.onExec {
            it shouldBe arrayOf("HDEL", "foo", "bar")
            0
        }.hdel("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("HDEL", "foo", "bar", "one", "two")
            0
        }.hdel("foo", "bar", "one", "two")

        client.onExec {
            it shouldBe arrayOf("HEXISTS", "foo", "bar")
            true
        }.hexists("foo", "bar")

        client.onExec {
            it shouldBe arrayOf("HGET", "foo", "bar")
            0
        }.hget("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("HGETALL", "foo")
            listOf<String>()
        }.hgetall("foo")
        client.onExec {
            it shouldBe arrayOf("HINCRBY", "foo", "bar", 10)
            0
        }.hincrby("foo", "bar", 10)
        client.onExec {
            it shouldBe arrayOf("HINCRBYFLOAT", "foo", "bar", 1.0)
            0
        }.hincrbyfloat("foo", "bar", 1.0)

        client.onExec {
            it shouldBe arrayOf("HKEYS", "foo")
            listOf<String>()
        }.hkeys("foo")
        client.onExec {
            it shouldBe arrayOf("HLEN", "foo")
            0
        }.hlen("foo")

        client.onExec {
            it shouldBe arrayOf("HSET", "hash", "foo", "bar")
            ""
        }.hset("hash", "foo", "bar")
        client.onExec {
            it shouldBe arrayOf("HSETNX", "hash", "foo", "bar")
            0
        }.hsetnx("hash", "foo", "bar")

        client.onExec {
            it shouldBe arrayOf("HSTRLEN", "hash", "foo")
            0
        }.hstrlen("hash", "foo")
        client.onExec {
            it shouldBe arrayOf("HVALS", "foo")
            listOf<String>()
        }.hvals("foo")

    }

    "keys commands" {
        val client = RedisClientMock()

//        client.onExec {
//            it shouldBe arrayOf("COPY", "foo", "bar")
//            0
//        }.copy("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("DEL", "foo", "bar")
            0
        }.del("foo", "bar")

        client.onExec {
            it shouldBe arrayOf("DUMP", "foo")
            byteArrayOf()
        }.dump("foo")

        client.onExec {
            it shouldBe arrayOf("EXISTS", "foo")
            0
        }.exists("foo")
        client.onExec {
            it shouldBe arrayOf("EXISTS", "foo", "bar", "foobar")
            0
        }.exists("foo", "bar", "foobar")

        client.onExec {
            it shouldBe arrayOf("EXPIRE", "foo", 10)
            0
        }.expire("foo", 10)
        val date = LocalDateTime.now()
        client.onExec {
            it shouldBe arrayOf("EXPIREAT", "foo", date.toEpochSecond(ZoneOffset.UTC))
            0
        }.expireat("foo", date)

        client.onExec {
            it shouldBe arrayOf("KEYS", "foo*")
            listOf<String>()
        }.keys("foo*")

        //TODO migrate

        client.onExec {
            it shouldBe arrayOf("MOVE", "foo", 42)
            0
        }.move("foo", 42)

        //TODO objectXXXX
        client.onExec {
            it shouldBe arrayOf("PERSIST", "foo")
            0
        }.persist("foo")
        client.onExec {
            it shouldBe arrayOf("PEXPIRE", "foo", 10)
            0
        }.pexpire("foo", 10)
        val ddat = Date()
        client.onExec {
            it shouldBe arrayOf("PEXPIREAT", "foo", ddat.time)
            0
        }.pexpireat("foo", ddat)
        client.onExec {
            it shouldBe arrayOf("PTTL", "foo")
            0
        }.pttl("foo")
        client.onExec {
            it shouldBe arrayOf("TTL", "foo")
            0
        }.ttl("foo")
        client.onExec {
            it shouldBe arrayOf("RANDOMKEY")
            ""
        }.randomkey()

        client.onExec {
            it shouldBe arrayOf("RENAME", "foo", "bar")
            ""
        }.rename("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("RENAMENX", "foo", "bar")
            0
        }.renamenx("foo", "bar")

        client.onExec {
            it shouldBe arrayOf("RESTORE", "foo", 0, "bar".toByteArray(), "REPLACE")
            ""
        }.restore("foo", "bar".toByteArray(), 0, true)

        client.onExec {
            it shouldBe arrayOf("SCAN", null, "foo*")
            produce<Any> { }
        }.scan("foo*")
        client.onExec {
            it shouldBe arrayOf("TOUCH", "foo", "bar")
            0
        }.touch("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("TYPE", "foo")
            ""
        }.type("foo")
        client.onExec {
            it shouldBe arrayOf("UNLINK", "foo", "bar")
            0
        }.unlink("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("WAIT", 1, 1000)
            0
        }.wait(1, 1000)

    }

    "list commands" {
        val client = RedisClientMock()

        client.onExec {
            it shouldBe arrayOf("BLMOVE", "foo", "bar", "LEFT", "RIGHT", 1 )
            ""
        }.blmove("foo", "bar", MovePosition.LEFT, MovePosition.RIGHT, 1)
        client.onExec {
            it shouldBe arrayOf("BLPOP", "foo", "bar", 1)
            listOf<Any>()
        }.blpop("foo", "bar", timeout = 1)
        client.onExec {
            it shouldBe arrayOf("BRPOP", "foo", "bar", 1)
            listOf<Any>()
        }.brpop("foo", "bar", timeout = 1)
        client.onExec {
            it shouldBe arrayOf("BRPOPLPUSH", "foo", "bar", 1)
            ""
        }.brpoplpush("foo", "bar", timeout = 1)
        client.onExec {
            it shouldBe arrayOf("LINDEX", "foo", -1)
            ""
        }.lindex("foo", -1)
        client.onExec {
            it shouldBe arrayOf("LINSERT", "foo", "AFTER", "bar", "foobar")
            1
        }.linsertAfter("foo", "bar", "foobar")
        client.onExec {
            it shouldBe arrayOf("LINSERT", "foo", "BEFORE", "bar", "foobar")
            1
        }.linsertBefore("foo", "bar", "foobar")
        client.onExec {
            it shouldBe arrayOf("LLEN", "foo")
            0
        }.llen("foo")
        client.onExec {
            it shouldBe arrayOf("LMOVE", "foo", "bar", "LEFT", "RIGHT" )
            0
        }.lmove("foo", "bar", MovePosition.LEFT, MovePosition.RIGHT)
        client.onExec {
            it shouldBe arrayOf("LPOP", "foo")
            0
        }.lpop("foo")
        client.onExec {
            it shouldBe arrayOf("LPOS", "foo", "bar")
            listOf<Long>()
        }.lpos("foo", "bar")
        client.onExec {
            it shouldBe arrayOf("LPOS", "foo", "bar", "COUNT", 0)
            listOf<Long>()
        }.lpos("foo", "bar") {count(0)}
        client.onExec {
            it shouldBe arrayOf("LPOS", "foo", "bar", "RANK", 1, "COUNT", 0)
            listOf<Long>()
        }.lpos("foo", "bar") {count(0); rank(1)}
        client.onExec {
            it shouldBe arrayOf("LPOS", "foo", "bar", "RANK", 1)
            listOf<Long>()
        }.lpos("foo", "bar") {rank(1)}

        client.onExec {
            it shouldBe arrayOf("LPUSH", "foo", "bar", "foobar")
            0
        }.lpush("foo", "bar", "foobar")
        client.onExec {
            it shouldBe arrayOf("LPUSHX", "foo", "bar")
            0
        }.lpushx("foo", "bar")

        client.onExec {
            it shouldBe arrayOf("LRANGE", "foo", 1, 10)
            listOf<String>()
        }.lrange("foo", 1L..10L)
        client.onExec {
            it shouldBe arrayOf("LREM", "foo", 3, "bar")
            0
        }.lrem("foo", 3, "bar")
        client.onExec {
            it shouldBe arrayOf("LSET", "foo", 1, "bar")
            "OK"
        }.lset("foo", 1, "bar")
        client.onExec {
            it shouldBe arrayOf("LTRIM", "foo", 1, -1)
            "OK"
        }.ltrim("foo", 1L..-1L)

        client.onExec {
            it shouldBe arrayOf("RPOP", "foo")
            0
        }.rpop("foo")
        client.onExec {
            it shouldBe arrayOf("RPUSH", "foo", "bar", "foobar")
            0
        }.rpush("foo", "bar", "foobar")
        client.onExec {
            it shouldBe arrayOf("RPUSHX", "foo", "bar")
            0
        }.rpushx("foo", "bar")
    }

    "! commands" {
        val client = RedisClientMock()

        client.onExec {
            it shouldBe arrayOf("PING")
            "PONG"
        }.ping()
    }

})

class PermutationTests : FunSpec({
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
    context("hash commands") {
        context("hmget") {
            withData(
                arrayOf("foo"),
                arrayOf("foo", "bar"),
                arrayOf("foo", "bar", "foobar")
            ) { m ->
                client.onExec {
                    it shouldBe arrayOf("HMGET", "hash", *m)
                    emptyList<String>()
                }.hmget("hash", *m)
            }
        }
        context("hmset") {
            withData(
                arrayOf("foo" to "bar"),
                arrayOf("foo" to "bar", "one" to "two")
            ) { m ->
                val flat = m.flatMap { (k, v) -> listOf(k, v) }.toTypedArray()
                client.onExec {
                    it shouldBe arrayOf("HMSET", "hash", *flat)
                    ""
                }.hmset("hash", *m)
            }
        }
        context("hmset with map") {
            withData(
                mapOf("foo" to "bar"),
                mapOf("foo" to "bar", "one" to "two")
            ) { m ->
                val flat = m.flatMap { (k, v) -> listOf(k, v) }.toTypedArray()
                client.onExec {
                    it shouldBe arrayOf("HMSET", "hash", *flat)
                    ""
                }.hmset("hash", m)
            }
        }
    }
    context("keys commands") {
        context("sort") {
            withData(
                SortParams(),
                SortParams(alpha = true, expected = arrayOf("ALPHA") ),
                SortParams(sortDirection = -1, expected = arrayOf("DESC") ),
                SortParams(sortDirection = -1,alpha = true, expected = arrayOf("DESC", "ALPHA") ),
                SortParams(range = 0L .. 10L, expected = arrayOf("LIMIT", 0, 10) ),
                SortParams(range = 0L .. 10L, sortDirection = -1,alpha = true, expected = arrayOf("LIMIT", 0, 10, "DESC", "ALPHA") ),
                SortParams(pattern = "weight_*", expected = arrayOf("BY", "weight_*") ),
                SortParams(pattern = "weight_*", getPatterns = listOf("object_*", "#"), expected = arrayOf("BY", "weight_*", "GET", "object_*", "GET", "#") ),
                SortParams(pattern = "weight_*", storeDestination = "resultKey", expected = arrayOf("BY", "weight_*", "STORE", "resultKey") ),
                SortParams(pattern = "weight_*->fn", getPatterns = listOf("object_*->fn"), expected = arrayOf("BY", "weight_*->fn", "GET", "object_*->fn") ),
            ) { p ->
                client.onExec {
                    it shouldBe arrayOf("SORT", "foo", *p.expected)
                    p.storeDestination?.let {
                        0
                    } ?: emptyList<String>()
                }.sort("foo", p.pattern, p.range,
                    *p.getPatterns.toTypedArray(),
                    sortDirection = p.sortDirection,
                    alpha = p.alpha,
                    storeDestination = p.storeDestination
                )
            }

        }
    }
})

data class SortParams(
    val pattern: String? = null,
    val range: LongRange? = null,
    val getPatterns: List<String> = emptyList(),
    val sortDirection: Int = 0,
    val alpha: Boolean = false,
    val storeDestination: String? = null,
    val expected: Array<Any?> = emptyArray()
)