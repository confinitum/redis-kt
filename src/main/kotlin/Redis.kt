package com.confinitum.common.redis

import com.confinitum.common.redis.protocol.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.slf4j.LoggerFactory
import java.io.*
import java.net.*
import java.nio.charset.*
import java.util.concurrent.atomic.*

/**
 * A Redis basic interface exposing emiting commands receiving their responses.
 *
 * Specific commands are exposed as extension methods.
 */
interface Redis : Closeable {
    companion object {
        val DEFAULT_PORT = 6379
        val DEFAULT_MAX_CONNECTIONS = 10
        val DEFAULT_CHARSET = Charsets.UTF_8
    }

    /**
     * Use [context] to await client close or terminate
     */
    val context: Job

    /**
     * Chatset that
     */
    val charset: Charset get() = DEFAULT_CHARSET

    /**
     * Executes a raw command. Each [args] will be sent as a String.
     *
     * It returns a type depending on the command.
     * The returned value can be of type [String], [Long] or [List].
     *
     * It may throw a [RedisResponseException]
     */
    suspend fun execute(vararg args: Any?): Any?
}

/**
 * Constructs a Redis client that will connect to [address] keeping a connection pool,
 * keeping as much as [maxConnections] and using the [charset].
 * Optionally you can define the [password] of the connection.
 */
class RedisClient(
    private val address: SocketAddress,
    maxConnections: Int = Redis.DEFAULT_MAX_CONNECTIONS,
    private val password: String? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : Redis {
    constructor(
        host: String,
        port: Int = Redis.DEFAULT_PORT,
        maxConnections: Int = Redis.DEFAULT_MAX_CONNECTIONS,
        password: String? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) : this(
        InetSocketAddress(host, port),
        maxConnections,
        password,
        scope
    )

    val logger = LoggerFactory.getLogger(this.javaClass)

    override val context: Job = SupervisorJob()
    private val connections = mutableListOf<RedisConnection>()
    private var pipeline: RedisConnection? = null

    private val connectionService = scope.actor<RedisRequest>(context) {
        channel.consumeEach {
            val connection = getConnection()
            try {
                connection.send(it)
            } catch (e: Exception) { //this shouldn't be possible, yet
                it.result?.completeExceptionally(e)
                if (connection.isClosedForReceive) {
                    cleanConnection(connection)
                }
            }
        }
    }

    init {
        context.invokeOnCompletion {
            connectionService.close(it)
        }
    }

    override suspend fun execute(vararg args: Any?): Any? {
        val result = CompletableDeferred<Any?>()
        connectionService.send(RedisRequest(args, result))
        try {
            return result.await()
        } catch (e: RedisException) {
            throw RedisException(e.message ?: "error", args)
        }
    }

    override fun close() {
        connectionService.close()
        closeConnections()
        context.cancel()
    }

    private fun getConnection() : Channel<RedisRequest> {
        return pipeline?.queue
            ?: connections.find { it.inUse.not() }?.queue
            ?: createConnection().queue
    }

    private fun createConnection(): RedisConnection {
        val conn = RedisConnection(address, password, charset, scope = scope)
        conn.queue.invokeOnClose {
            connections.remove(conn)
        }
        connections.add(conn)
        return conn
    }

    private fun closeConnections() {
        val clist = listOf(*connections.toTypedArray())
        clist.forEach { it.close() }
    }

    private fun cleanConnection(connection: Channel<RedisRequest>) {
        connections.find { it.queue.equals(connection) }?.let { connections.remove(it) }
    }

}
