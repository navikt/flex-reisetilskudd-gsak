package no.nav.helse.flex.reisetilskudd.gsak.innsending

import no.nav.helse.flex.reisetilskudd.gsak.client.DokArkivClient
import no.nav.helse.flex.reisetilskudd.gsak.client.FlexBucketUploaderClient
import no.nav.helse.flex.reisetilskudd.gsak.client.PdfGeneratorClient
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.PdlClient
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.format
import no.nav.helse.flex.reisetilskudd.gsak.database.InnsendingDao
import no.nav.helse.flex.reisetilskudd.gsak.domain.*
import no.nav.helse.flex.reisetilskudd.gsak.log
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class InnsendingService(
    private val innsendingDao: InnsendingDao,
    private val pdfGeneratorClient: PdfGeneratorClient,
    private val dokArkivClient: DokArkivClient,
    private val pdlClient: PdlClient,
    private val bucketUploaderClient: FlexBucketUploaderClient
) {

    private val log = log()

    fun behandleReisetilskuddSoknad(soknadString: String) {
        val reisetilskudd = soknadString.tilReisetilskudd()
        if (reisetilskudd.status != ReisetilskuddStatus.SENDT) {
            log.info("Ignorerer reisetilskuddsøknad ${reisetilskudd.reisetilskuddId} og status ${reisetilskudd.status}")
            return
        }

        innsendingDao.hentInnsending(reisetilskudd.reisetilskuddId)?.let {
            log.warn("Har allerede behandlet reisetilskuddsøknad ${it.reisetilskuddId} ${it.opprettet}")
            return
        }

        val navn = pdlClient.hentPerson(fnr = reisetilskudd.fnr).navn?.firstOrNull()?.format()
            ?: throw RuntimeException("Fant ikke navn i PDL")

        val encoder = Base64.getEncoder()

        val vedlegg = reisetilskudd.kvitteringer.map {
            it.tilPdfKvittering(bucketUploaderClient, encoder)
        }

        val pdf = pdfGeneratorClient.genererPdf(
            PdfRequest(
                navn = navn,
                reisetilskuddId = reisetilskudd.reisetilskuddId,
                kvitteringer = vedlegg
            )
        )

        val journalpostRequest = skapJournalpostRequest(reisetilskudd = reisetilskudd, pdf = pdf)

        val journalpostResponse = dokArkivClient.opprettJournalpost(journalpostRequest, reisetilskudd.reisetilskuddId)

        val innsending = Innsending(
            fnr = reisetilskudd.fnr,
            reisetilskuddId = reisetilskudd.reisetilskuddId,
            saksId = "TODO",
            journalpostId = journalpostResponse.journalpostId,
            opprettet = Instant.now(),
        )

        innsendingDao.lagreInnsending(innsending)

        log.info("Behandlet reisetilskuddsøknad ${reisetilskudd.reisetilskuddId} og status ${reisetilskudd.status}")
    }
}

fun Kvittering.tilPdfKvittering(bucket: FlexBucketUploaderClient, encoder: Base64.Encoder) =
    PdfKvittering(
        encoder.encodeToString(bucket.hentVedlegg(this.kvitteringId)),
        this.kvitteringId,
        this.navn,
        this.fom,
        this.tom,
        this.storrelse,
        this.belop,
        this.transportmiddel,
    )

