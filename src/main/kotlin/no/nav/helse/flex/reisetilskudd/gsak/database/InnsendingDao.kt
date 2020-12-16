package no.nav.helse.flex.reisetilskudd.gsak.database


import no.nav.helse.flex.reisetilskudd.gsak.domain.Innsending
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime


@Repository
class InnsendingDao(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun hentInnsending(reisetilskuddId: String): Innsending? {
        return (namedParameterJdbcTemplate.query(
                """
                    SELECT REISETILSKUDD_ID
                    ,      FNR          
                    ,      SAKS_ID
                    ,      JOURNALPOST_ID          
                    ,      OPPRETTET          
                    FROM INNSENDING
                    WHERE REISETILSKUDD_ID = :reisetilskuddId
                    """,
                MapSqlParameterSource().addValue("reisetilskuddId", reisetilskuddId)
        ) { resultSet, _ ->
            Innsending(
                    reisetilskuddId = resultSet.getString("REISETILSKUDD_ID"),
                    fnr = resultSet.getString("FNR"),
                    saksId = resultSet.getString("SAKS_ID"),
                    journalpostId = resultSet.getString("JOURNALPOST_ID"),
                    opprettet = resultSet.getTimestamp("OPPRETTET").toInstant()
            )
        }).firstOrNull()
    }

    fun lagreInnsending(innsending: Innsending) {
        namedParameterJdbcTemplate.update("""
                    INSERT INTO INNSENDING (REISETILSKUDD_ID
                    ,      FNR          
                    ,      SAKS_ID
                    ,      JOURNALPOST_ID
                    ,      OPPRETTET
                    )
                    VALUES ( :reisetilskuddId
                    ,        :fnr
                    ,        :saksId
                    ,        :journalpostId
                    ,        :opprettet
                    )
                    """,
                MapSqlParameterSource()
                        .addValue("reisetilskuddId", innsending.reisetilskuddId)
                        .addValue("fnr", innsending.fnr)
                        .addValue("saksId", innsending.saksId)
                        .addValue("journalpostId", innsending.journalpostId)
                        .addValue("opprettet", Timestamp.from(innsending.opprettet))
        )
    }
}
