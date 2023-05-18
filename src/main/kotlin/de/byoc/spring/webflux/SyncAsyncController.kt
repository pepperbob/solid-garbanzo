package de.byoc.spring.webflux

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class SyncAsyncController {

    private val logger = KotlinLogging.logger {}

    /**
     * Under a bit of load this method will deplete the pool
     * of available threads quickly as Netty uses only one
     * thread per CPU-core: request are piling up and
     * have to wait (eventually time-out).
     */
    @GetMapping("/hello-sync")
    fun helloSync(): String {
        val caller = CallerId()
        logger.info { "[${caller.id}] Enter helloSync" }

        Thread.sleep(1000)

        return "Hello, World!".also {
            logger.info { "[${caller.id}] Exit helloSync" }
        }
    }

    /**
     * This method will do exactly the same but with async features, i.e.
     * many request will share the scarce threads that are available.
     *
     * This only works because no blocking operations are made.
     */
    @GetMapping("/hello-async")
    suspend fun helloAsync(): String = coroutineScope {
        val caller = CallerId()
        logger.info { "[${caller.id}] Enter helloSync" }

        delay(1000)

        "Hello, World!".also {
            logger.info { "[${caller.id}] Exit helloSync" }
        }
    }

    /**
     * This method will use a blocking operation: there is no advantage over
     * the helloSync method, result is exactly the same: threads pile up and
     * have to wait.
     *
     */
    @GetMapping("/hello-async-sleep")
    suspend fun helloAsyncSleep(): String = coroutineScope {
        val caller = CallerId()
        logger.info { "[${caller.id}] Enter helloSync" }

        Thread.sleep(1000)

        "Hello, World!".also {
            logger.info { "[${caller.id}] Exit helloSync" }
        }
    }
}