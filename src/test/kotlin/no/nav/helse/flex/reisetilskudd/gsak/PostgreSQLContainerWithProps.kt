package no.nav.helse.flex.reisetilskudd.gsak

import org.testcontainers.containers.PostgreSQLContainer

class PostgreSQLContainerWithProps : PostgreSQLContainer<PostgreSQLContainerWithProps>("postgres:11.4-alpine") {

    override fun start() {
        super.start()
        System.setProperty("spring.datasource.url", this.jdbcUrl)
        System.setProperty("spring.datasource.username", this.username)
        System.setProperty("spring.datasource.password", this.password)
    }
}
