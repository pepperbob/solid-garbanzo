package de.byoc.spring.webflux

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Entity
class SomeEntity {
    @Id
    var id: String = UUID.randomUUID().toString()
    @Column
    var name: String = ""
    @Column
    var timestamp: Instant = Instant.now()
}

@Transactional(propagation = Propagation.MANDATORY)
interface SomeRepo : JpaRepository<SomeEntity, String>

data class CallerId(val id: String = "${UUID.randomUUID()}".substring(0, 5))
