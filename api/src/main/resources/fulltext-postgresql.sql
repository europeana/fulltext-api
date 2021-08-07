-- fulltext.dataset definition

-- Drop table

-- DROP TABLE fulltext.dataset;

CREATE TABLE fulltext.dataset (
                                  id int4 NOT NULL GENERATED BY DEFAULT AS IDENTITY,
                                  value text NOT NULL,
                                  CONSTRAINT dataset_pk PRIMARY KEY (id)
);
CREATE INDEX dataset_value_idx ON fulltext.dataset USING btree (value);

-- Permissions

ALTER TABLE fulltext.dataset OWNER TO europeana;
GRANT ALL ON TABLE fulltext.dataset TO europeana;


-- fulltext."language" definition

-- Drop table

-- DROP TABLE fulltext."language";

CREATE TABLE fulltext."language" (
                                     id int4 NOT NULL GENERATED BY DEFAULT AS IDENTITY,
                                     value text NOT NULL,
                                     CONSTRAINT language_pk PRIMARY KEY (id)
);
CREATE INDEX language_value_idx ON fulltext.language USING btree (value);

-- Permissions

ALTER TABLE fulltext."language" OWNER TO europeana;
GRANT ALL ON TABLE fulltext."language" TO europeana;


-- fulltext.localdoc definition

-- Drop table

-- DROP TABLE fulltext.localdoc;

CREATE TABLE fulltext.localdoc (
                                   id int8 NOT NULL GENERATED BY DEFAULT AS IDENTITY,
                                   value text NOT NULL,
                                   CONSTRAINT localdoc_pk PRIMARY KEY (id)
);
CREATE INDEX localdoc_value_idx ON fulltext.localdoc USING btree (value);

-- Permissions

ALTER TABLE fulltext.localdoc OWNER TO europeana;
GRANT ALL ON TABLE fulltext.localdoc TO europeana;


-- fulltext.rights definition

-- Drop table

-- DROP TABLE fulltext.rights;

CREATE TABLE fulltext.rights (
                                 id int4 NOT NULL GENERATED BY DEFAULT AS IDENTITY,
                                 value text NOT NULL,
                                 CONSTRAINT rights_pk PRIMARY KEY (id)
);
CREATE INDEX rights_value_idx ON fulltext.rights USING btree (value);

-- Permissions

ALTER TABLE fulltext.rights OWNER TO europeana;
GRANT ALL ON TABLE fulltext.rights TO europeana;


-- fulltext.resource definition

-- Drop table

-- DROP TABLE fulltext.resource;

CREATE TABLE fulltext.resource (
                                   id int8 NOT NULL GENERATED BY DEFAULT AS IDENTITY,
                                   lang_id int4 NOT NULL,
                                   rights_id int4 NOT NULL,
                                   original bool NULL DEFAULT false,
                                   value text NOT NULL,
                                   "source" text NOT NULL,
                                   CONSTRAINT resource_pk PRIMARY KEY (id),
                                   CONSTRAINT resource_lang_fk FOREIGN KEY (lang_id) REFERENCES fulltext."language"(id),
                                   CONSTRAINT resource_rights_fk FOREIGN KEY (rights_id) REFERENCES fulltext.rights(id)
);

-- Permissions

ALTER TABLE fulltext.resource OWNER TO europeana;
GRANT ALL ON TABLE fulltext.resource TO europeana;


-- fulltext.annopage definition

-- Drop table

-- DROP TABLE fulltext.annopage;

CREATE TABLE fulltext.annopage (
                                   id int8 NOT NULL GENERATED BY DEFAULT AS IDENTITY,
                                   dataset_id int4 NOT NULL,
                                   local_id int8 NOT NULL,
                                   page int4 NOT NULL,
                                   res_id int8 NULL,
                                   date_modified timestamptz NULL,
                                   target_url text NULL,
                                   CONSTRAINT annopage_pk PRIMARY KEY (id),
                                   CONSTRAINT annopage_dataset_fk FOREIGN KEY (dataset_id) REFERENCES fulltext.dataset(id),
                                   CONSTRAINT annopage_local_fk FOREIGN KEY (local_id) REFERENCES fulltext.localdoc(id),
                                   CONSTRAINT annopage_resource_fk FOREIGN KEY (res_id) REFERENCES fulltext.resource(id)
);
CREATE INDEX annopage_dataset_id_idx ON fulltext.annopage USING btree (dataset_id, local_id, page);
CREATE INDEX annopage_dataset_id_res_idx ON fulltext.annopage USING btree (dataset_id, local_id, page, res_id);

-- Permissions

ALTER TABLE fulltext.annopage OWNER TO europeana;
GRANT ALL ON TABLE fulltext.annopage TO europeana;


-- fulltext.annotation definition

-- Drop table

-- DROP TABLE fulltext.annotation;

CREATE TABLE fulltext.annotation (
                                     id int8 NOT NULL GENERATED BY DEFAULT AS IDENTITY,
                                     annopage_id int8 NOT NULL,
                                     dc_type varchar(1) NOT NULL,
                                     from_index int4 NULL,
                                     to_index int4 NULL,
                                     CONSTRAINT annotation_pk PRIMARY KEY (id),
                                     CONSTRAINT annotation_annopage_fk FOREIGN KEY (annopage_id) REFERENCES fulltext.annopage(id)
);

-- Permissions

ALTER TABLE fulltext.annotation OWNER TO europeana;
GRANT ALL ON TABLE fulltext.annotation TO europeana;


-- fulltext.target definition

-- Drop table

-- DROP TABLE fulltext.target;

CREATE TABLE fulltext.target (
                                 id int8 NOT NULL GENERATED BY DEFAULT AS IDENTITY,
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