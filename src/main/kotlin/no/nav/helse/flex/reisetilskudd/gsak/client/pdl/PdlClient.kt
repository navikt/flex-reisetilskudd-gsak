package no.nav.helse.flex.reisetilskudd.gsak.client.pdl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*

@Component
class PdlClient(
    private val flexFssProxyRestTemplate: RestTemplate,
    @Value("\${flex.fss.proxy.url}") private val flexFssProxyUrl: String
) {

    private val TEMA = "Tema"
    private val TEMA_SYK = "SYK"
    private val IDENT = "ident"
    private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val HENT_PERSON_QUERY =
        """
query(${"$"}ident: ID!){
  hentPerson(ident: ${"$"}ident) {
  	navn(historikk: false) {
  	  fornavn
  	  mellomnavn
  	  etternavn
    }
    adressebeskyttelse {
      gradering
    }
  }
  hentIdenter(ident: ${"$"}ident, historikk: false) {
    identer {
      ident,
      gruppe
    }
  }
}
"""

    @Retryable(exclude = [FunctionalPdlError::class])
    fun hentPerson(fnr: String): ResponseData {

        val graphQLRequest = GraphQLRequest(
            query = HENT_PERSON_QUERY,
            variables = Collections.singletonMap(IDENT, fnr)
        )

        val responseEntity = flexFssProxyRestTemplate.exchange("$flexFssProxyUrl/api/pdl/graphql", HttpMethod.POST, HttpEntity(requestToJson(graphQLRequest), createHeaderWithTema()), String::class.java)

        if (responseEntity.statusCode != HttpStatus.OK) {
            throw RuntimeException("PDL svarer med status ${responseEntity.statusCode} - ${responseEntity.body}")
        }

        val parsedResponse: GetPersonResponse? = responseEntity.body?.let { objectMapper.readValue(it) }

        parsedResponse?.data?.let {
            return it
        }
        throw FunctionalPdlError("Fant ikke person, ingen body eller data. ${parsedResponse.hentErrors()}")
    }

    private fun createHeaderWithTema(): HttpHeaders {
        val headers = createHeader()
        headers[TEMA] = TEMA_SYK
        return headers
    }

    private fun createHeader(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }

    private fun requestToJson(graphQLRequest: GraphQLRequest): String {
        return try {
            ObjectMapper().writeValueAsString(graphQLRequest)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    private fun GetPersonResponse?.hentErrors(): String? {
        return this?.errors?.map { it.message }?.joinToString(" - ")
    }

    data class GraphQLRequest(val query: String, val variables: Map<String, String>)

    class FunctionalPdlError(message: String) : RuntimeException(message)
}
