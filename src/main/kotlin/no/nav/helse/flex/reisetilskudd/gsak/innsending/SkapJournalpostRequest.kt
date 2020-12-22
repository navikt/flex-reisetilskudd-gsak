package no.nav.helse.flex.reisetilskudd.gsak.innsending

import no.nav.helse.flex.reisetilskudd.gsak.domain.*

fun skapJournalpostRequest(
        reisetilskudd: Reisetilskudd,
        pdf: ByteArray
): JournalpostRequest {
    val tittel = "Søknad om Reisetilskudd"
    return JournalpostRequest(
            bruker = Bruker(
                    id = reisetilskudd.fnr,
                    idType = "FNR"
            ),
            dokumenter = listOf(Dokument(
                    dokumentvarianter = listOf(
                            Dokumentvarianter(
                                    filnavn = tittel,
                                    filtype = "PDFA",
                                    variantformat = "ARKIV",
                                    fysiskDokument = pdf
                            )
                    ),
                    tittel = tittel,
            )),
            tema = "SYK",
            tittel = tittel
    )
}



