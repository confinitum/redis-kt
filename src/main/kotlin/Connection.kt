package com.confinitum.common.redis

import com.confinitum.common.redis.protocol.*
import com.confinitum.common.redis.utils.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.slf4j.LoggerFactory
import java.io.*
import java.net.SocketAddress
import java.nio.charset.*
import kotlin.coroutines.cancellation.CancellationException

internal class RedisRequest(val args: Any?, val result: CompletableDeferred<Any?>?) {
    override fun toString(): String {
        return when (args) {
            is List<*> -> args.joinToString() { it.toString() }
            is Array<*> -> args.joinToString() { it.toString() }
            is String -> args.toString()
            else -> args.toString()
        }
    }
}

private const val DEFAULT_PIPELINE_SIZE = 10

/**
 * Redis socket connection
 */
internal class RedisConnection(
    private val address: SocketAddress,
    private val password: String?,
    private val charset: Charset,
    pipelineSize: Int = DEFAULT_PIPELINE_SIZE,
    scope: CoroutineScope = GlobalScope,
) : Closeable {
    val queue = Channel<RedisRequest>()
    @Volatile
    var inUse = false

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val selectorManager = ActorSelectorManager(scope.coroutineContext)
    private lateinit var socket: Socket
    private lateinit var input: ByteReadChannel
    private lateinit var output: ByteWriteChannel

    val context: Job = scope.launch(Dispatchers.IO) {
        inUse =true
        try {
            socket = aSocket(selectorManager)
                .tcpNoDelay()
                .tcp()
                .connect(address)
            input = socket.openReadChannel()
            output = socket.openWriteChannel()

            password?.let { auth(it) }
        } catch (cause: Throwable) {
            queue.close()
            for (it in queue) {
                it.result?.completeExceptionally(cause)
            }
            queue.cancel(CancellationException(cause))
            socket.close()
        } finally {
            inUse = false
        }

        if (socket.isClosed.not()) {
            queue.consumeEach { request ->
                if (request.result != null) {
                    receiver.send(request.result)
                }

                if (request.args != null) {
                    output.writePacket {
                        writeRedisValue(request.args, charset = charset)
                    }
                    output.flush()
                }
            }
        }
    }

    private val receiver = scope.actor<CompletableDeferred<Any?>>(
        context, capacity = pipelineSize
    ) {
        val decoder = charset.newDecoder()!!

        consumeEach { result ->
            inUse = true
            try {
                result.complete(input.readRedisMessage(decoder))
            } catch (cause: Throwable) {
                result.completeExceptionally(cause)
            } finally {
                inUse = false
            }
        }
    }

    override fun close() {
        queue.close()
        receiver.close()
        socket.close()
        context.cancel()
    }

    private suspend fun auth(password: String) {
        output.writePacket {
            writeRedisValue(listOf("AUTH", password), charset = charset)
        }
        output.flush()

        input.readRedisMessage(charset.newDecoder())
    }
}
