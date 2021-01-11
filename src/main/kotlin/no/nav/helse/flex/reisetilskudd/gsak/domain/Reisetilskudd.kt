package no.nav.helse.flex.reisetilskudd.gsak.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.reisetilskudd.gsak.objectMapper
import java.time.LocalDate

data class Reisetilskudd(
    val status: ReisetilskuddStatus,
    val reisetilskuddId: String,
    val fnr: String,
    val kvitteringer: List<Kvittering> = emptyList(),
)

enum class ReisetilskuddStatus {
    FREMTIDIG, ÅPEN, SENDT
}

enum class Transportmiddel {
    KOLLEKTIVT, TAXI, EGEN_BIL
}

data class Kvittering(
    val kvitteringId: String? = null,
    val blobId: String,
    val navn: String,
    val datoForReise: LocalDate,
    val storrelse: Long,
    val belop: Int, // Beløp i øre . 100kr = 10000
    val transportmiddel: Transportmiddel
)

fun String.tilReisetilskudd(): Reisetilskudd = objectMapper.readValue(this)
