import com.confinitum.common.redis.protocol.writeRedisServerValue
import com.confinitum.common.redis.protocol.writeRedisValue
import com.confinitum.common.redis.utils.RedisBufferPool
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.*
import java.net.BindException
import java.nio.ByteBuffer
import kotlin.text.String

private const val TEST_HOST = "localhost"
private const val TEST_PORT = 12340

class RedisTestServer(val verbose: Boolean = false) {
    private val selector = ActorSelectorManager(Dispatchers.IO)
    private var server: ServerSocket? = null
    private var serverJob: Job? = null
    @Volatile
    var running = false

    val mocks = mutableMapOf<String, Any>()

    var host: String = TEST_HOST
    var port: Int = TEST_PORT

    init {
        mocks.put("null", "####")
        request("QUIT").answerWith("####")
    }

    fun start() {
        var port = TEST_PORT
        try {
            server = aSocket(selector).tcp().bind(TEST_HOST, port)
            host=server!!.localAddress.hostname
            port=server!!.localAddress.port

            serverJob= CoroutineScope(Job()).launch {
                run()
            }
        } catch (e: BindException) {
            debug("port ${port} busy")
            if ((++port - TEST_PORT) > 25) {
                throw e
            }
        }
    }

    suspend fun run() = withContext(Dispatchers.IO) {
        debug("Server listening on ${server!!.localAddress}")
        running = true
        while (running) {
            val socket = server!!.accept()
            if (running.not()) {
                socket.close()
                break
            }
            launch {
                debug("Client connected ${socket.remoteAddress}")
                val sin = socket.openReadChannel()
                val sout = socket.openWriteChannel(autoFlush = true)
                try {
                    val buf = RedisBufferPool.borrow()
                    while (running) {
                        buf.clear()
                        sin.readAvailable(buf)
                        val output = getAnswer(buf)
                        if (output == "####") {
                            running = false
                            sout.writePacket { writeRedisValue(null) }
                        } else {
                            val packet = buildPacket { writeRedisServerValue(output) }
                            debug("packet: " + packet.copy().readText().toDebug())
                            sout.writePacket(packet)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    debug("Connection ${socket.remoteAddress} closed")
                    socket.close()
                }
            }
        }
        debug("Server stopped.")
    }

    private fun getAnswer(buf: ByteBuffer): Any {
        val input = buf.toTestString(buf.position())
        debug("input: ${input?.toDebug()}")
        val output = mocks.getOrElse(input ?: "null") {
            mocks.entries.filter { (k, _) ->
                k.startsWith(input ?: "null")
            }.firstOrNull()?.value
                ?: "-ERR unknown command: ${input}"
        }
        return output.also { debug("output: ${it.toString()}") }
    }

    fun mockRequest(request: Array<out Any>, response: Any) {
        val reqPacket = BytePacketBuilder().also {
            it.writeRedisValue(request)
        }
        val reqPacketString = reqPacket.build().readerUTF8().readText()
        mocks.put(reqPacketString, response)
    }

    fun request(vararg rargs: Any) = MockRequestBuilder(this, *rargs)

    fun stop() {
        running = false
        runBlocking {
            aSocket(selector).tcp().connect(host,port).close()
        }
        server?.close()
        serverJob?.cancel()
        debug("after server.close()")
    }

    private fun debug(msg: String) {
        if (verbose) println("TS: $msg")
    }
}

class MockRequestBuilder(private val server: RedisTestServer, private vararg val args: Any) {
    fun answerWith(response: Any) {
        server.mockRequest(args, response)
    }
}

private fun String.toDebug() = this.replace("\r", "\\r").replace("\n", "\\n")

private fun ByteBuffer.toTestString(count: Int) =
    if (count > 0) {
        val dst = ByteArray(count)
        this.flip().get(dst, 0, count)
        String(dst)
    } else null


fun main(args: Array<String>) {
    runBlocking {
        RedisTestServer().start()
    }
}