package de.byoc.spring.webflux

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.transaction.annotation.Transactional

@SpringBootApplication
class SpringWebfluxFunApplication(val repo: SomeRepo) {
    @PostConstruct
    @Transactional
    fun init() {
        repo.deleteAll()
    }
}

fun main(args: Array<String>) {
    runApplication<SpringWebfluxFunApplication>(*args)
}