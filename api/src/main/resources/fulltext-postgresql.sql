-- fulltext.resource definition

-- Drop table

-- DROP TABLE fulltext.resource;

CREATE TABLE fulltext.resource (
                                   id bigserial NOT NULL,
                                   "language" varchar(3) NOT NULL,
                                   rights text NOT NULL,
                                   original bool NULL DEFAULT false,
                                   value text NOT NULL,
                                   "source" text NOT NULL,
                                   CONSTRAINT resource_pk PRIMARY KEY (id)
);
CREATE INDEX resource_lang_rights_idx ON fulltext.resource USING btree (language, rights);
CREATE INDEX resource_language_idx ON fulltext.resource USING btree (language);
CREATE INDEX resource_rights_idx ON fulltext.resource USING btree (rights);

-- Permissions

ALTER TABLE fulltext.resource OWNER TO europeana;
GRANT ALL ON TABLE fulltext.resource TO europeana;


-- fulltext.annopage definition

-- Drop table

-- DROP TABLE fulltext.annopage;

CREATE TABLE fulltext.annopage (
                                   id bigserial NOT NULL,
                                   dataset int4 NOT NULL,
                                   localdoc text NOT NULL,
                                   page int4 NOT NULL,
                                   res_id int8 NULL,
                                   date_modified timestamptz NULL,
                                   target_url text NULL,
                                   CONSTRAINT annopage_pk PRIMARY KEY (id),
                                   CONSTRAINT annopage_un UNIQUE (dataset, localdoc, page),
                                   CONSTRAINT annopage_resource_fk FOREIGN KEY (res_id) REFERENCES fulltext.resource(id)
);
CREATE INDEX annopage_ds_lc_pg_idx ON fulltext.annopage USING btree (dataset, localdoc, page);
CREATE INDEX annopage_ds_lcl_idx ON fulltext.annopage USING btree (dataset, localdoc);
CREATE INDEX annopage_ds_lcl_pg_rsid_idx ON fulltext.annopage USING btree (dataset, localdoc, page, res_id);
CREATE INDEX annopage_lcl_idx ON fulltext.annopage USING btree (localdoc);

-- Permissions

ALTER TABLE fulltext.annopage OWNER TO europeana;
GRANT ALL ON TABLE fulltext.annopage TO europeana;


-- fulltext.annotation definition

-- Drop table

-- DROP TABLE fulltext.annotation;

CREATE TABLE fulltext.annotation (
                                     id bigserial NOT NULL,
                                     annopage_id int8 NOT NULL,
                                     dc_type varchar(1) NOT NULL,
                                     from_index int4 NULL,
                                     to_index int4 NULL,
                                     CONSTRAINT annotation_pk PRIMARY KEY (id),
                                     CONSTRAINT annotation_annopage_fk FOREIGN KEY (annopage_id) REFERENCES fulltext.annopage(id)
);
CREATE INDEX annotation_annopage_id_idx ON fulltext.annotation USING btree (annopage_id);

-- Permissions

ALTER TABLE fulltext.annotation OWNER TO europeana;
GRANT ALL ON TABLE fulltext.annotation TO europeana;


-- fulltext.target definition

-- Drop table

-- DROP TABLE fulltext.target;

CREATE TABLE fulltext.target (
                                 id bigserial NOT NULL,
                                 annotation_id int8 NOT NULL,
                                 x_start int4 NULL,
                                 y_end int4 NULL,
                                 width int4 NULL,
                                 height int4 NULL,
                                 CONSTRAINT target_pk PRIMARY KEY (id),
                                 CONSTRAINT target_annotation_fk FOREIGN KEY (annotation_id) REFERENCES fulltext.annotation(id)
);

-- Permissions

ALTER TABLE fulltext.target OWNER TO europeana;
GRANT ALL ON TABLE fulltext.target TO europeana;

-- fulltext.v_annopages source

CREATE OR REPLACE VIEW fulltext.v_annopages
AS SELECT a.id,
          a.dataset,
          a.localdoc,
          a.page,
          r.language,
          r.rights,
          r.original,
          r.value,
          r.source,
          a.date_modified,
          a.target_url,
          a.id AS res_id
   FROM fulltext.annopage a
            JOIN fulltext.resource r ON a.res_id = r.id;

-- Permissions

ALTER TABLE fulltext.v_annopages OWNER TO europeana;
GRANT ALL ON TABLE fulltext.v_annopages TO europeana;

-- fulltext.annopage_id_seq definition

-- DROP SEQUENCE fulltext.annopage_id_seq;

CREATE SEQUENCE fulltext.annopage_id_seq
    INCREMENT BY 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;

-- Permissions

ALTER SEQUENCE fulltext.annopage_id_seq OWNER TO europeana;
GRANT ALL ON SEQUENCE fulltext.annopage_id_seq TO europeana;


-- fulltext.annotation_id_seq definition

-- DROP SEQUENCE fulltext.annotation_id_seq;

CREATE SEQUENCE fulltext.annotation_id_seq
    INCREMENT BY 1000
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;

-- Permissions

ALTER SEQUENCE fulltext.annotation_id_seq OWNER TO europeana;
GRANT ALL ON SEQUENCE fulltext.annotation_id_seq TO europeana;


-- fulltext.resource_id_seq definition

-- DROP SEQUENCE fulltext.resource_id_seq;

CREATE SEQUENCE fulltext.resource_id_seq
    INCREMENT BY 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;

-- Permissions

ALTER SEQUENCE fulltext.resource_id_seq OWNER TO europeana;
GRANT ALL ON SEQUENCE fulltext.resource_id_seq TO europeana;


-- fulltext.target_id_seq definition

-- DROP SEQUENCE fulltext.target_id_seq;

CREATE SEQUENCE fulltext.target_id_seq
    INCREMENT BY 1000
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;

-- Permissions

ALTER SEQUENCE fulltext.target_id_seq OWNER TO europeana;
GRANT ALL ON SEQUENCE fulltext.target_id_seq TO europeana;