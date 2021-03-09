package no.nav.helse.flex.reisetilskudd.gsak.domain

import java.time.Instant
import java.time.LocalDate

data class PdfRequest(
    val navn: String,
    val fnr: String,
    val reisetilskuddId: String,
    val kvitteringer: List<PdfKvittering>,
    val sporsmal: List<Sporsmal>,
    val sendt: Instant,
    val sum: Int
)

data class PdfKvittering(
    val b64data: String,
    val blobId: String,
    val datoForUtgift: LocalDate,
    val belop: Int,
    val typeUtgift: String,
)
