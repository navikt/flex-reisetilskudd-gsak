package no.nav.helse.flex.reisetilskudd.gsak.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.reisetilskudd.gsak.objectMapper
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class Reisetilskudd(
    val id: String,
    val status: ReisetilskuddStatus,
    val fnr: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val sendt: Instant? = null,
    val avbrutt: Instant? = null,
    val arbeidsgiverOrgnummer: String? = null,
    val arbeidsgiverNavn: String? = null,
    val sporsmal: List<Sporsmal> = emptyList()
)

enum class ReisetilskuddStatus {
    FREMTIDIG, ÅPEN, PÅBEGYNT, SENDBAR, SENDT, AVBRUTT
}

enum class Utgiftstype {
    OFFENTLIG_TRANSPORT, TAXI, PARKERING, ANNET
}

data class Kvittering(
    val blobId: String,
    val datoForUtgift: LocalDate,
    val belop: Int, // Beløp i øre . 100kr = 10000
    val typeUtgift: Utgiftstype,
    val opprettet: Instant? = Instant.now(),
)

fun String.tilReisetilskudd(): Reisetilskudd = objectMapper.readValue(this)
