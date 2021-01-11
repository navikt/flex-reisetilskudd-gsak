package no.nav.helse.flex.reisetilskudd.gsak.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Controller
import org.springframework.web.client.RestTemplate

@Controller
class FlexBucketUploaderClient(
    private val flexBucketUploaderRestTemplate: RestTemplate,
    @Value("\${flex.bucket.uploader.url:http://flex-bucket-uploader}") private val flexBucketUploaderUrl: String
) {

    @Retryable(backoff = Backoff(delay = 5000))
    fun hentVedlegg(vedleggId: String): ByteArray {
        val url = "$flexBucketUploaderUrl/maskin/kvittering/$vedleggId"

        val result = flexBucketUploaderRestTemplate.exchange(url, HttpMethod.GET, null, ByteArray::class.java)

        if (!result.statusCode.is2xxSuccessful) {
            throw RuntimeException("flex-bucket-uploader feiler med HTTP-${result.statusCode} for vedlegg med id: $vedleggId")
        }

        return result.body
            ?: throw RuntimeException("flex-bucket-uploader returnerer ikke data for vedlegg med id: $vedleggId")
    }
}
