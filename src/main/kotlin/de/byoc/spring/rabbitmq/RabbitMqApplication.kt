package de.byoc.spring.rabbitmq

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.rabbitmq.*
import java.time.Instant
import kotlin.random.Random


@Configuration
class RabbitConfigBeans {

    @Bean
    fun connectionMono(): Mono<Connection> {
        val connectionFactory = ConnectionFactory()
        connectionFactory.useNio()
        return Mono.fromCallable {
            connectionFactory.newConnection("reactor-rabbit")
        }.cache().subscribeOn(Schedulers.boundedElastic())
    }

    @Bean
    fun senderOptions(connectionMono: Mono<Connection>): SenderOptions {
        return SenderOptions()
            .connectionMono(connectionMono)
            .resourceManagementScheduler(Schedulers.boundedElastic())
    }

    @Bean
    fun sender(senderOptions: SenderOptions): Sender {
        return RabbitFlux.createSender(senderOptions)
    }

    @Bean
    fun receiver(connectionMono: Mono<Connection>): Receiver {
        val xx = ReceiverOptions().connectionMono(connectionMono)
        return RabbitFlux.createReceiver(xx)
    }

}


@SpringBootApplication
class RabbitMqApplication(
    private val sender: Sender,
    private val receiver: Receiver,
) : CommandLineRunner {

    override fun run(vararg args: String?) {

        sender.declareQueue(QueueSpecification.queue("xx.xx")).block()
        val theScheduler = Schedulers.newBoundedElastic(100, 5, "newbe")
        receiver.consumeManualAck("xx.xx")
            .flatMap { dee ->
                Mono.fromCallable {
                    val sleeping = Random.nextLong(500)
                    val thename = Thread.currentThread().name
                    Thread.sleep(sleeping)
                    "R: Message in $thename: ${dee.body.decodeToString()} (slept $sleeping ms)"
                }.publishOn(theScheduler)
                    .doOnSuccess {
                        dee.ack()
                    }
                    .doOnNext {
                        println("${Instant.now()} - $it")
                    }
            }
            .subscribe {
                // println(it)
            }

        val xx = Flux.range(1, 100)
            .map { "$it;Hello" }
            .map { OutboundMessage("", "xx.xx", it.toByteArray()) }
            .doOnNext {
                println("S ${it.body.decodeToString()}")
            }

        sender.send(xx)
            .block()
    }

}

fun main(vararg args: String) {
    SpringApplication.run(RabbitMqApplication::class.java, *args)
}