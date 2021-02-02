package no.nav.helse.flex.reisetilskudd.gsak.client

import no.nav.helse.flex.reisetilskudd.gsak.domain.OppgaveRequest
import no.nav.helse.flex.reisetilskudd.gsak.domain.OppgaveResponse
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
class OppgaveClient(
    private val oppgaveRestTemplate: RestTemplate,
    @Value("\${oppgave.url}") private val oppgaveUrl: String
) {

    @Retryable(backoff = Backoff(delay = 5000))
    fun opprettOppgave(pdfRequest: OppgaveRequest, reisetilskuddId: String): OppgaveResponse {
        val url = "$oppgaveUrl/api/v1/oppgaver"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["X-Correlation-ID"] = reisetilskuddId

        val entity = HttpEntity(pdfRequest, headers)

        val result = oppgaveRestTemplate.exchange(url, HttpMethod.POST, entity, OppgaveResponse::class.java)

        if (!result.statusCode.is2xxSuccessful) {
            throw RuntimeException("oppgave feiler med HTTP-${result.statusCode} for reisetilskuddsøknad med id: $reisetilskuddId")
        }

        return result.body
            ?: throw RuntimeException("oppgave returnerer ikke data for reisetilskuddsøknad med id: $reisetilskuddId")
    }
}
