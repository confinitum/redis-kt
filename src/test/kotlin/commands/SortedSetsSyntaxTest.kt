package commands

import RedisClientMock
import com.confinitum.common.redis.commands.RedisBzPopResult
import com.confinitum.common.redis.commands.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.produce

class SortedSetsSyntaxTest: StringSpec({

        "set commands" {
            val client = RedisClientMock()

            client.onExec {
                it shouldBe arrayOf("SADD", "foo", "bar", "foobar")
                0
            }.sadd("foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SCARD", "foo")
                0
            }.scard("foo")
            client.onExec {
                it shouldBe arrayOf("SDIFF", "foo", "bar", "foobar")
                listOf<Any>()
            }.sdiff("foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SDIFFSTORE", "store", "foo", "bar", "foobar")
                0
            }.sdiffstore("store", "foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SINTER", "foo", "bar", "foobar")
                listOf<Any>()
            }.sinter("foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SINTERSTORE", "store", "foo", "bar", "foobar")
                0
            }.sinterstore("store", "foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SISMEMBER", "foo", "bar")
                0
            }.sismember("foo", "bar")
            client.onExec {
                it shouldBe arrayOf("SMEMBERS", "foo")
                listOf<String>()
            }.smembers("foo")
            client.onExec {
                it shouldBe arrayOf("SMISMEMBER", "foo", "bar", "foobar")
                listOf<Long>()
            }.smismember("foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SMOVE", "foo", "bar", "foobar")
                listOf<Long>()
            }.smove("foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SPOP", "foo")
                ""
            }.spop("foo")
            client.onExec {
                it shouldBe arrayOf("SRANDMEMBER", "foo")
                ""
            }.srandmember("foo")
            client.onExec {
                it shouldBe arrayOf("SRANDMEMBER", "foo", -1)
                listOf<Any>()
            }.srandmember("foo", -1)
            client.onExec {
                it shouldBe arrayOf("SREM", "foo", "bar", "foobar")
                0
            }.srem("foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SSCAN", "foo", "bar")
                produce<String> {}
            }.sscan("foo", "bar")
            client.onExec {
                it shouldBe arrayOf("SUNION", "foo", "bar", "foobar")
                listOf<Any>()
            }.sunion("foo", "bar", "foobar")
            client.onExec {
                it shouldBe arrayOf("SUNIONSTORE", "store", "foo", "bar", "foobar")
                0
            }.sunionstore("store", "foo", "bar", "foobar")

        }

        "sorted set commands" {
            val client = RedisClientMock()

            client.onExec {
                it shouldBe arrayOf("BZPOPMAX", "foo", "bar", 2)
                listOf<String>()
            }.bzpopmax("foo", "bar", timeout = 2)
            client.onExec {
                it shouldBe arrayOf("BZPOPMIN", "foo", "bar", 2)
                listOf<String>()
            }.bzpopmin("foo", "bar", timeout = 2)
            client.onExec {
                it shouldBe arrayOf("ZADD", "key", 1.1, "foo", 2.2, "bar")
                0
            }.zadd("key", "foo" to 1.1, "bar" to 2.2)
            client.onExec {
                it shouldBe arrayOf("ZADD", "key", 1.1, "foo", 2.2, "bar")
                0
            }.zadd("key", mapOf("foo" to 1.1, "bar" to 2.2))
            client.onExec {
                it shouldBe arrayOf("ZADD", "key", 1.1, "foo")
                0
            }.zadd("key", "foo", 1.1)
            client.onExec {
                it shouldBe arrayOf("ZADD", "key", 1.1, "foo", 2.2, "bar")
                0
            }.zadd("key") {
                score("foo", 1.1)
                score("bar", 2.2)
            }
            client.onExec {
                it shouldBe arrayOf("ZADD", "key", "NX", "CH", 1.1, "foo", 2.2, "bar")
                0
            }.zadd("key") {
                score("foo", 1.1)
                score("bar", 2.2)
                nx=true; ch=true
            }
            client.onExec {
                it shouldBe arrayOf("ZCARD", "key")
                0
            }.zcard("key")
            client.onExec {
                it shouldBe arrayOf("ZCARD", "key")
                0
            }.zcount("key")
        }

        " commands" {
            val client = RedisClientMock()

            client.onExec {
                it shouldBe arrayOf("PING")
                "PONG"
            }.ping()
        }

    })
