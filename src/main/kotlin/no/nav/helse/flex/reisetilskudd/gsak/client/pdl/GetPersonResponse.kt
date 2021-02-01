package no.nav.helse.flex.reisetilskudd.gsak.client.pdl

const val AKTORID = "AKTORID"

data class GetPersonResponse(
    val data: ResponseData,
    val errors: List<ResponseError>?
)

data class ResponseError(
    val message: String?,
    val locations: List<ErrorLocation>?,
    val path: List<String>?,
    val extensions: ErrorExtension?
)

data class ResponseData(
    val hentPerson: HentPerson? = null,
    val hentIdenter: HentIdenter? = null,
)

data class HentIdenter(
    val identer: List<PdlIdent>
)

data class PdlIdent(val gruppe: String, val ident: String)

data class ErrorLocation(
    val line: String?,
    val column: String?
)

data class ErrorExtension(
    val code: String?,
    val classification: String?
)

data class HentPerson(
    val navn: List<Navn>? = null,
    val adressebeskyttelse: List<Adressebeskyttelse>? = null
)

data class Adressebeskyttelse(
    val gradering: String
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

fun Navn.format(): String =
    if (mellomnavn != null) {
        "$fornavn $mellomnavn $etternavn"
    } else {
        "$fornavn $etternavn"
    }

fun ResponseData.aktorId() = this.hentIdenter?.identer?.firstOrNull { it.gruppe == AKTORID }?.ident
