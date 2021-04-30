package samples

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    val channel = Channel<String>() // Channel.RENDEZVOUS (default)

    // Consumer A
    launch {
        channel.send("A1")
        channel.send("A2")
        log("A done")
    }
    // Consumer B
    launch {
        delay(3000)
        channel.send("B1")
        log("B done")
    }

    // Consumer Coroutine
    launch {
        repeat(3) {
            val x = channel.receive()
            log(x)
        }
    }
}
