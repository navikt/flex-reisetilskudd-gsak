package no.nav.helse.flex.reisetilskudd.gsak.domain

import java.util.*

data class Sporsmal(
    val id: String = UUID.randomUUID().toString(),
    val tag: Tag,
    val overskrift: String? = null,
    val sporsmalstekst: String? = null,
    val undertekst: String? = null,
    val svartype: Svartype,
    val min: String? = null,
    val max: String? = null,
    val kriterieForVisningAvUndersporsmal: KriterieForVisningAvUndersporsmal? = null,
    val svar: List<Svar> = emptyList(),
    val undersporsmal: List<Sporsmal> = emptyList()
)

enum class KriterieForVisningAvUndersporsmal {
    NEI,
    JA,
    CHECKED
}

enum class Svartype {
    JA_NEI,
    CHECKBOX,
    CHECKBOX_PANEL,
    CHECKBOX_GRUPPE,
    DATOER,
    BELOP,
    KILOMETER,
    KVITTERING,
}

enum class Tag {
    ANSVARSERKLARING,
    TRANSPORT_TIL_DAGLIG,
    TYPE_TRANSPORT,
    BIL_TIL_DAGLIG,
    KM_HJEM_JOBB,
    OFFENTLIG_TRANSPORT_TIL_DAGLIG,
    OFFENTLIG_TRANSPORT_BELOP,
    REISE_MED_BIL,
    BIL_DATOER,
    BIL_BOMPENGER,
    BIL_BOMPENGER_BELOP,
    KVITTERINGER,
    UTBETALING
}
