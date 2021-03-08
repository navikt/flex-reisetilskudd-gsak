package no.nav.helse.flex.reisetilskudd.gsak.domain

import java.time.LocalDate

data class PdfRequest(
    val navn: String,
    val reisetilskuddId: String,
    val kvitteringer: List<PdfKvittering>,
    val sum: Int
)

data class PdfKvittering(
    val b64data: String,
    val kvitteringId: String? = null,
    val blobId: String,
    val navn: String,
    val datoForReise: LocalDate,
    val storrelse: Long,
    val belop: Int, // Beløp i øre . 100kr = 10000
    val transportmiddel: Transportmiddel
)
