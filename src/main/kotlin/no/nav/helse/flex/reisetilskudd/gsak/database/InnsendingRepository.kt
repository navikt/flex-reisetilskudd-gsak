package no.nav.helse.flex.reisetilskudd.gsak.database

import no.nav.helse.flex.reisetilskudd.gsak.domain.Innsending
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InnsendingRepository : CrudRepository<Innsending, String> {
    fun findInnsendingByReisetilskuddId(reisetilskuddId: String): Innsending?
    fun existsByReisetilskuddId(reisetilskuddId: String): Boolean
}
