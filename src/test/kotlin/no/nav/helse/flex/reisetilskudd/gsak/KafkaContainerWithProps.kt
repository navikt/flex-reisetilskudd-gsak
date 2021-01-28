package no.nav.helse.flex.reisetilskudd.gsak

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class KafkaContainerWithProps : KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.5.3")) {

    override fun start() {
        super.start()
        System.setProperty("on-prem-kafka.bootstrap-servers", this.bootstrapServers)
        System.setProperty("spring.kafka.bootstrap-servers", this.bootstrapServers)
    }
}
