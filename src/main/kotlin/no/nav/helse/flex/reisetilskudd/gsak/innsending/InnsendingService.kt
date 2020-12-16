package no.nav.helse.flex.reisetilskudd.gsak.innsending

import no.nav.helse.flex.reisetilskudd.gsak.client.PdfGeneratorClient
import no.nav.helse.flex.reisetilskudd.gsak.database.InnsendingDao
import no.nav.helse.flex.reisetilskudd.gsak.domain.*
import no.nav.helse.flex.reisetilskudd.gsak.log
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class InnsendingService(
        private val innsendingDao: InnsendingDao,
        private val pdfGeneratorClient: PdfGeneratorClient,
) {

    private val log = log()

    fun behandleReisetilskuddSoknad(soknadString: String) {
        val soknad = soknadString.tilReisetilskudd()
        if (soknad.status != ReisetilskuddStatus.SENDT) {
            log.info("Ignorerer reisetilskuddsøknad ${soknad.reisetilskuddId} og status ${soknad.status}")
            return
        }

        innsendingDao.hentInnsending(soknad.reisetilskuddId)?.let {
            log.warn("Har allerede behandlet reisetilskuddsøknad ${it.reisetilskuddId} ${it.opprettet}")
            return
        }

        val innsending = Innsending(
                fnr = soknad.fnr,
                reisetilskuddId = soknad.reisetilskuddId,
                saksId = "TODO",
                journalpostId = "TODO",
                opprettet = Instant.now(),
        )

        val pdf = pdfGeneratorClient.genererPdf(PdfRequest(
                navn = "Navn Navnesen Fra Pdl",
                reisetilskuddId = soknad.reisetilskuddId
        ))

        innsendingDao.lagreInnsending(innsending)

        log.info("Behandlet reisetilskuddsøknad ${soknad.reisetilskuddId} og status ${soknad.status}")

    }
}
