package no.nav.helse.flex.reisetilskudd.gsak.domain

import org.springframework.data.annotation.Id
import java.time.Instant

data class Innsending(
    @Id
    val id: String? = null,
    val fnr: String,
    val reisetilskuddId: String,
    val oppgaveId: Int? = null,
    val journalpostId: String,
    val opprettet: Instant,
)
