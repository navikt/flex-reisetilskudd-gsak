package no.nav.helse.flex.reisetilskudd.gsak

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@DirtiesContext
class ApplicationTest {

    companion object {
        @Container
        val postgreSQLContainer = PostgreSQLContainerWithProps()

        @Container
        val kafkaContainer = KafkaContainerWithProps()
    }

    @Test
    fun contextLoads() {
    }
}
