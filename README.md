# Full Text API
### Europeana Fulltext API 

This project consists of 3 modules:

1. A loader module to read and parse Europeana Newspaper xml files that are the result of the newspaper OCR process. The
resulting objects are stored in a Mongo database 
2. An API that 
   - reads data from the Mongo database and makes it available via IIIF presentation requests (JSON-LD)
   - allows one to search the fulltext of a particular newspaper issue (record) and return corresponding annotations.
3. A common module that contains the data model (Fulltext Resources, AnnoPages, Annotations, AnnotationType) for both the
 loader and API

### Implementation details ###

This Fulltext API implements the functionality described in §3.3 & 3.4 of the Europeana IIIF API Specification.

### REQUIREMENTS ###
- Java 11 and a Mongo database
- Optionally also a Solr search engine (for search)

### FUNCTIONALITY

## API ##

Start the server and do a request to one of the endpoints:

* Resource
`[http://{server:port}/presentation/{dataset_id}/{local_id}/{resource_id}?format={2/3}]` _(**format** defaults to 2)_

* Annotation Page 
`[http://{server:port}/presentation/{dataset_id}/{local_id}/annopage/{page_id}?format={2/3}]` _(**format** defaults to 2)_

* Annotation
`[http://{server:port}/presentation/{dataset_id}/{local_id}/anno/{annotation_id}?format={2/3}]` _(**format** defaults to 2)_

## Loader ##

For batch loading zipped EDM xml files, ensure that the batch.base.directory property is set correctly in the `loader.properties` file.
 
The loader can read a single .zip file from that directory by calling the **zipbatch** endpoint: 
`[http://{server:port}/fulltext/zipbatch?archive={archive.zip}]`.
Alternatively, it will process all the files in the specified directory by specifying **all** as archive name, 
e.g.: [http://{server:port}/fulltext/zipbatch?archive=all]


### KNOWN ISSUES
* the current version does not yet implement usage of an API key


