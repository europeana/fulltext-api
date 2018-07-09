# Fulltext API
### Europeana Newspapers Fulltext API 

This initial (v.0.1-SNAPSHOT) version implements the functionality described in §3.3 & 3.4 
of the IIIF API Specification.

This version offers the bare functionality only: there is no checking of credentials or mime-type.
It neither returns the specified HTTP status codes yet.

The application needs to be able to connect to a Mongo instance on localhost, and uses the default 'test'
database in there.

You can create a small test-set of data to play around with using this endpoint: 
[http://localhost:8084/presentation/testset]

Port number, the database used etcetera can be edited in the application.yml file.

Note that the fulltext.properties file is not used for this release yet.