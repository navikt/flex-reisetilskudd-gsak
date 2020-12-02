package no.nav.helse.flex.reisetilskudd.gsak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableRetry
class Application

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
