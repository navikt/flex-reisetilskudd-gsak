DROP TABLE INNSENDING;

CREATE TABLE INNSENDING
(
    ID               VARCHAR(36) DEFAULT UUID_GENERATE_V4() PRIMARY KEY,
    REISETILSKUDD_ID VARCHAR(36)              NOT NULL UNIQUE,
    FNR              VARCHAR(11)              NOT NULL,
    OPPGAVE_ID       INTEGER                  NULL,
    JOURNALPOST_ID   VARCHAR(20)              NOT NULL,
    OPPRETTET        TIMESTAMP WITH TIME ZONE NOT NULL
);