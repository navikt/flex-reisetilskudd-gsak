package no.nav.helse.flex.reisetilskudd.gsak.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.reisetilskudd.gsak.objectMapper

data class Reisetilskudd(
    val status: ReisetilskuddStatus,
    val reisetilskuddId: String,
    val fnr: String,
)

enum class ReisetilskuddStatus {
    FREMTIDIG, ÅPEN, SENDT
}

fun String.tilReisetilskudd(): Reisetilskudd = objectMapper.readValue(this)
