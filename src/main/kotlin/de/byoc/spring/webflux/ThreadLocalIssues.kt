package de.byoc.spring.webflux

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class ThreadLocalIssues(val repo: SomeRepo) {
    private val logger = KotlinLogging.logger {}

    /**
     * This method does Count on repo, expects 0 rows, inserts one,
     * does expensive blocking-operation, deletes the inserted row
     * and returns final count.
     *
     * It is run in a single transaction and no matter what happens the table
     * should always be empty in the end.
     */
    @GetMapping("/repo-sync")
    @Transactional
    fun repoSync(): String {
        val caller = CallerId()

        logger.info { "[${caller.id}] Enter repoAsync" }

        val currentCount = repo.count()

        if (currentCount > 0) {
            // not reachable
            logger.error { "[${caller.id}] Wait what?! $currentCount > 0 ?!" }
            return "Aborted."
        }

        val e = repo.save(SomeEntity().apply { name = "${UUID.randomUUID()}" })

        Thread.sleep(1000)

        repo.delete(e)

        logger.info { "[${caller.id}] Final Count: ${currentCount}" }
        return "Repo-Count: ${repo.count()}"
    }

    /**
     * Same method but async, ie. request share threads which leads to leaking resources
     * if they are backed by ThreadLocals (like JPA)
     */
    @GetMapping("/repo-async")
    @Transactional
    suspend fun repoAsync(): String = coroutineScope {
        val caller = CallerId()

        logger.info { "[${caller.id}] Enter repoAsync" }

        val currentCount = repo.count()

        if (currentCount > 0) {
            // this is always called when 2 or more requests share the same thread.
            logger.error { "[${caller.id}] Wait what?! $currentCount > 0 ?!" }
            return@coroutineScope "Aborted."
        }

        val e = repo.save(SomeEntity().apply { name = "${UUID.randomUUID()}" })
        logger.info { "[$caller] Entered SomeEntity {${e.timestamp})" }

        delay(1000)

        logger.info { "[$caller] Done waiting." }

        // creates new TX if none active
        repo.delete(e)

        logger.info { "[${caller.id}] Final count: ${currentCount}" }
        "Repo-Count: ${repo.count()}"
    }
}
