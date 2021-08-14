# Experimental PostgreSQL version
### Europeana Newspapers Fulltext API 

Prompted by the recent issues we had adding fields to MongoDB storage for fulltext I decided to build a proof-of-concept using PostgreSQL instead of MongoDB.

Over the past year we have made some changes to the data model:

- adding a Rights field to the Resource entity;
- expanding the Annopage entity to allow for translations which required (1) adding two new collections: TranslationAnnoPage & TranslationResource, and
- (2) adding a Language field to the Annopage collection, with the value copied from the linked Resource document

All these tickets turned out way more difficult and time-consuming than foreseen, mostly because we're trying to use MongoDB for something that it was
never intended for, because the Fulltext data model is essentially a relational one, with: 

- a 1:N relationship between Annopage and Annotation
- a 1:1 between Annopage and Resource
- and another 1:N between Annotation and Target 
  
And that's not even considering the already existing complications introduced by multilinguality (TranslationAnnoPage, TranslationResource), let alone what may come in further requirements, still over the horizon.

Other than increasingly tedious maintenance issues we are also facing crippling performance problems when doing anything more complex than a simple 
fetch. We've seen that with the summary and multi-lingual endpoints, both of which need two separate queries to fetch the original and the translated Annopages.
As it turns out, Morphia (the DAO layer we use) is a further handicap because (besides being basically undocumented) it can't perform the joined aggregation that would be possible (though barely so)
in MongoDB itself.

Add to that the fact that the criteria for choosing NOSQL storage (like MongoDB) have shifted considerably in the past 5 to 10 years. 
In performance, PostgreSQL [now outperforms MongoDB](https://www.enterprisedb.com/news/new-benchmarks-show-postgres-dominating-mongodb-varied-workloads) hands-down. 
And since it has become possible to store JSON columns, it does so even on its own terrain. 

And last, but definitely not least given our humongous fulltext database, using PostgreSQL may reduce required store to 10% compared to MongoDB.

A change like this is not trivial, I'm very much aware of that. I felt that instead of discussing this, it might be much more useful to show how it could be done.

Hence, this branch.
It has added code to store a given dataset in a connected PostgreSQL database and to retrieve Fulltext Annopages 
(json format only for now, V2 or V3 format) from it.

To test it, create a PostgreSQL instance and create the objects defined in:

`/src/main/resources/fulltext-postgresql.sql`

This will create four tables (annopage, annotation, resource and target) and one view (v_annopages) plus the indexes and sequences needed. 
I chose to not fully normalise the schema (eg not create language, rights, dataset and localdoc tables) for performance reasons. 
We're looking at a considerable number of rows on the Annotation table already, so it's better to keep it nice and simple. 
The schema and Hibernate configuration are optimised as far as I could achieve in a few days, so it may be further improved. 
Batch mode is in any case active, hence the increment size of 1000 on the annotation sequence (this allows Hibernate to pre-fetch 1000 indexes at once, 
so that it can save up to 1000 rows at once). 

Set the `spring.datasource.jdbc-url` property to point to the created instance, and provide username & password.
Note that loading data in PostgreSQL does not require the loader.war! It's a new endpoint in the FTController:

`../presentation/postgres/{datasetId}/{localId}/` - just like in the loader, loads the pages identified by "localID", when you 
use ALL it will load the whole dataset.

You can retrieve Annopages using this endpoint:
`../{dataset}/{local}/pgannopage/{page}`

That's all for now, I think. I only tested it with my small local collection, so I am very curious if this will work with a more serious dataset, or if that
needs further Hibernate magic - or maybe using plain old JDBC, that wouldn't be half bad either. 