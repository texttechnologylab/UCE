-- Table: public.documentembeddings

-- DROP TABLE IF EXISTS public.documentembeddings;

CREATE TABLE IF NOT EXISTS public.documentembeddings
(
    document_id bigint NOT NULL,
    embedding vector,
    coveredtext text COLLATE pg_catalog."default",
    beginn integer,
    endd integer,
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 )
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.documentembeddings
    OWNER to postgres;