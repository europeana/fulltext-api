# Fulltext API
### Europeana Newspapers Fulltext API 

This Fulltext API implements the functionality described in §3.3 & 3.4 
of the IIIF API Specification.

Version history:

v.0.3-SNAPSHOT
(hopefully) final adjustments to Mongo schema; removed _class column generation; used Spring mongodb 
data consistently through the application (instead of Morphia); cleaning up

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

REQUIREMENTS
The application needs Java8 jre and a Mongo instance

### FUNCTIONALITY
* Batch loading zipped EMD xml files: ensure that the batch.base.directory property is set correctly. 
The loader can read a single .zip file from that directory by calling the **zipbatch** endpoint: 
`[http://{server:port}/presentation/zipbatch?archive={archive.zip}]`; alternatively, it will process all the files in the
specified directory by speficying **all** as archive name, e.g.: [http://{server:port}/presentation/zipbatch?archive=all]

* A JSON-LD representation of an Annotation Page can be requested like this: 
`[http://{server:port}/presentation/{dataset_id}/{local_id}/annopage/{page_id}?format={2/3}]` _(**format** defaults to 2)_

* A JSON-LD representation of an individual Annotation can be requested like this: 
`[http://{server:port}/presentation/{dataset_id}/{local_id}/anno/{annotation_id}?format={2/3}]` _(**format** defaults to 2)_


### PROPERTIES
Application name, port number, Mongodb connection settings, path elements (for rendering URL's in JSON output), etc. are 
all managed in the fulltext.properties file.
Note that any sensitive data (e.g. passwords) are omitted from this file; they can be overridden in a local 
fulltext.user.properties file in src/main/resources.

