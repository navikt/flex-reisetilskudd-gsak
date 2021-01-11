package no.nav.helse.flex.reisetilskudd.gsak.domain

import java.time.LocalDate

data class PdfRequest(
    val navn: String,
    val reisetilskuddId: String,
    val kvitteringer: List<PdfKvittering>
)

data class PdfKvittering(
    val b64data: String,
    val kvitteringId: String,
    val navn: String,
    val fom: LocalDate,
    val tom: LocalDate?,
    val storrelse: Long,
    val belop: Double,
    val transportmiddel: Transportmiddel
)
