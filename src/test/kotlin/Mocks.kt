import com.confinitum.common.redis.Redis
import kotlinx.coroutines.Job

class RedisClientMock: Redis {

    private var executor: (args: Array<out Any?>) -> Any? = {_ -> "-ERR"}

    fun onExec(block: (args: Array<out Any?>)-> Any?): RedisClientMock {
        executor = block
        return this
    }
    override val context: Job
        get() = Job()

    override suspend fun execute(vararg args: Any?): Any? {
        return executor.invoke(args)
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    internal fun argString(args: Any?): String {
        return when (args) {
            is List<*> -> args.joinToString() { it.toString() }
            is Array<*> -> args.joinToString() { it.toString() }
            is String -> args.toString()
            else -> args.toString()
        }
    }
}