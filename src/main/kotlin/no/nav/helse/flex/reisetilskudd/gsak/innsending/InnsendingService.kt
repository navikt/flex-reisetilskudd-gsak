package no.nav.helse.flex.reisetilskudd.gsak.innsending

import no.nav.helse.flex.reisetilskudd.gsak.client.DokArkivClient
import no.nav.helse.flex.reisetilskudd.gsak.client.FlexBucketUploaderClient
import no.nav.helse.flex.reisetilskudd.gsak.client.OppgaveClient
import no.nav.helse.flex.reisetilskudd.gsak.client.PdfGeneratorClient
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.PdlClient
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.aktorId
import no.nav.helse.flex.reisetilskudd.gsak.client.pdl.format
import no.nav.helse.flex.reisetilskudd.gsak.database.InnsendingRepository
import no.nav.helse.flex.reisetilskudd.gsak.domain.*
import no.nav.helse.flex.reisetilskudd.gsak.log
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class InnsendingService(
    private val innsendingRepository: InnsendingRepository,
    private val pdfGeneratorClient: PdfGeneratorClient,
    private val dokArkivClient: DokArkivClient,
    private val oppgaveClient: OppgaveClient,
    private val pdlClient: PdlClient,
    private val bucketUploaderClient: FlexBucketUploaderClient
) {

    private val log = log()

    fun behandleReisetilskuddSoknad(soknadString: String) {
        val reisetilskudd = soknadString.tilReisetilskudd()
        if (reisetilskudd.status != ReisetilskuddStatus.SENDT) {
            log.info("Ignorerer reisetilskuddsøknad ${reisetilskudd.id} og status ${reisetilskudd.status}")
            return
        }

        var innsending = innsendingRepository.findInnsendingByReisetilskuddId(reisetilskudd.id)
        if (innsending?.oppgaveId != null) {
            log.warn("Har allerede behandlet reisetilskuddsøknad ${innsending.reisetilskuddId} ${innsending.opprettet}")
            return
        }

        val person = pdlClient.hentPerson(fnr = reisetilskudd.fnr)
        val navn = person.hentPerson?.navn?.firstOrNull()?.format()
            ?: throw RuntimeException("Fant ikke navn i PDL for reisetilskudd ${reisetilskudd.id}")

        val aktorId = person.aktorId()
            ?: throw RuntimeException("Fant ikke aktorid i PDL for reisetilskudd ${reisetilskudd.id}")

        if (innsending == null) {
            val encoder = Base64.getEncoder()

            val kvitteringer = reisetilskudd.sporsmal.find {
                it.tag == Tag.KVITTERINGER
            }!!.svar.map {
                it.kvittering!!.tilPdfKvittering(bucketUploaderClient, encoder)
            }

            val sporsmal = reisetilskudd.sporsmal.filterNot {
                it.tag == Tag.KVITTERINGER
            }

            val pdf = pdfGeneratorClient.genererPdf(
                PdfRequest(
                    navn = navn,
                    fnr = reisetilskudd.fnr,
                    reisetilskuddId = reisetilskudd.id,
                    kvitteringer = kvitteringer,
                    sporsmal = sporsmal,
                    sendt = reisetilskudd.sendt!!,
                    sum = kvitteringer.sumBy { it.belop }
                )
            )

            val journalpostRequest = skapJournalpostRequest(reisetilskudd = reisetilskudd, pdf = pdf)

            val journalpostResponse = dokArkivClient.opprettJournalpost(journalpostRequest, reisetilskudd.id)

            innsending = innsendingRepository.save(
                Innsending(
                    fnr = reisetilskudd.fnr,
                    reisetilskuddId = reisetilskudd.id,
                    journalpostId = journalpostResponse.journalpostId,
                    opprettet = Instant.now(),
                )
            )
        }

        val oppgaveDato: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val oppgaveResponse = oppgaveClient.opprettOppgave(
            OppgaveRequest(
                tildeltEnhetsnr = null,
                opprettetAvEnhetsnr = "9999",
                aktoerId = aktorId,
                journalpostId = innsending.journalpostId,
                beskrivelse = reisetilskudd.beskrivelse(),
                tema = "SYK",
                oppgavetype = "SOK",
                aktivDato = LocalDate.now().format(oppgaveDato),
                fristFerdigstillelse = omTreUkedager(LocalDate.now()).format(oppgaveDato),
                prioritet = "NORM",
                behandlingstema = "ab0237"
            ),
            reisetilskuddId = reisetilskudd.id
        )

        innsendingRepository.save(innsending.copy(oppgaveId = oppgaveResponse.id))

        log.info("Behandlet reisetilskuddsøknad ${reisetilskudd.id} og status ${reisetilskudd.status}")
    }
}

fun Kvittering.tilPdfKvittering(bucket: FlexBucketUploaderClient, encoder: Base64.Encoder) =
    PdfKvittering(
        encoder.encodeToString(bucket.hentVedlegg(this.blobId)),
        this.blobId,
        this.datoForUtgift,
        this.belop,
        this.typeUtgift.tilString()
    )

fun omTreUkedager(idag: LocalDate) = when (idag.dayOfWeek) {
    DayOfWeek.SUNDAY -> idag.plusDays(4)
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY -> idag.plusDays(3)
    else -> idag.plusDays(5)
}

fun Reisetilskudd.beskrivelse(): String {
    val norskDato = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return "Søknad om reisetilskudd for perioden ${fom.format(norskDato)} - ${tom.format(norskDato)}"
}
