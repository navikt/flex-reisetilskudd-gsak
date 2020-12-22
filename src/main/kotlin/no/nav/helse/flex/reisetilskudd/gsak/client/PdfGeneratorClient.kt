package no.nav.helse.flex.reisetilskudd.gsak.client

import no.nav.helse.flex.reisetilskudd.gsak.domain.PdfRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Controller
import org.springframework.web.client.RestTemplate

@Controller
class PdfGeneratorClient(private val simpleRestTemplate: RestTemplate,
                         @Value("\${pdfgen.url:http://flex-reisetilskudd-pdfgen}") private val pdfgenUrl: String) {

    @Retryable(backoff = Backoff(delay = 5000))
    fun genererPdf(pdfRequest: PdfRequest): ByteArray {
        val url = "$pdfgenUrl/api/v1/genpdf/reisetilskudd/reisetilskudd"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(pdfRequest, headers)

        val result = simpleRestTemplate.exchange(url, HttpMethod.POST, entity, ByteArray::class.java)

        if (result.statusCode != OK) {
            throw RuntimeException("pdfgenerator feiler med HTTP-${result.statusCode} for reisetilskuddsøknad med id: ${pdfRequest.reisetilskuddId}")
        }

        return result.body
                ?: throw RuntimeException("pdfgenerator returnerer ikke data for reisetilskuddsøknad med id: ${pdfRequest.reisetilskuddId}")
    }
}


