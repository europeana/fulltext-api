# Fulltext API
### Europeana Newspapers Fulltext API 

This Fulltext API implements the functionality described in §3.3 & 3.4 
of the IIIF API Specification.

Version history:

v.0.2.2-SNAPSHOT
Refactored .yml properties to .properties prior to deployment on Bluemix

v.0.2.1-SNAPSHOT
Added functionality to read, parse, process and save batch xml files contained within ZIP archives

v.0.2-SNAPSHOT
Added functionality to read, parse, process and save batch xml files

v.0.1.1-SNAPSHOT
Added functionality to read fulltext.properties / fulltext.user.properties

v.0.1-SNAPSHOT
This version offers the bare functionality only: there is no checking of credentials or mime-type.
It neither returns the specified HTTP status codes yet.

The application needs to be able to connect to a Mongo instance on localhost, and uses the default 'test'
database in there.

You can create a small test-set of data to play around with using this endpoint: 
[http://localhost:8084/presentation/testset]

Port number, the database used etcetera can be edited in the application.yml file.

Note that the fulltext.properties file is not used for this release yet.