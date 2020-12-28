package no.nav.helse.flex.reisetilskudd.gsak.client

import no.nav.helse.flex.reisetilskudd.gsak.domain.JournalpostRequest
import no.nav.helse.flex.reisetilskudd.gsak.domain.JournalpostResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Controller
import org.springframework.web.client.RestTemplate

@Controller
class DokArkivClient(
    private val dokarkivRestTemplate: RestTemplate,
    @Value("\${dokarkiv.url}") private val dokarkivUrl: String
) {

    @Retryable(backoff = Backoff(delay = 5000))
    fun opprettJournalpost(pdfRequest: JournalpostRequest, reisetilskuddId: String): JournalpostResponse {
        val url = "$dokarkivUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Nav-Callid"] = reisetilskuddId

        val entity = HttpEntity(pdfRequest, headers)

        val result = dokarkivRestTemplate.exchange(url, HttpMethod.POST, entity, JournalpostResponse::class.java)

        if (!result.statusCode.is2xxSuccessful) {
            throw RuntimeException("dokarkiv feiler med HTTP-${result.statusCode} for reisetilskuddsøknad med id: $reisetilskuddId")
        }

        return result.body
            ?: throw RuntimeException("dokarkiv returnerer ikke data for reisetilskuddsøknad med id: $reisetilskuddId")
    }
}
