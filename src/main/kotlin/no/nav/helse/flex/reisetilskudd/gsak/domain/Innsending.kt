package no.nav.helse.flex.reisetilskudd.gsak.domain

import java.time.Instant

data class Innsending(
        val fnr: String,
        val reisetilskuddId: String,
        val saksId: String,
        val journalpostId: String,
        val opprettet: Instant,
)



