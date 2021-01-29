package no.nav.helse.flex.reisetilskudd.gsak.domain

import org.springframework.data.annotation.Id
import java.time.Instant

data class Innsending(
    @Id
    val id: String? = null,
    val fnr: String,
    val reisetilskuddId: String,
    val saksId: String,
    val journalpostId: String,
    val opprettet: Instant,
)
