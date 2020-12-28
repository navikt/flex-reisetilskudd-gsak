package no.nav.helse.flex.reisetilskudd.gsak

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = ["flex.aapen-reisetilskudd"]
)
class ApplicationTest {

    @Test
    fun contextLoads() {
    }
}
