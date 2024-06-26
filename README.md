# Full Text API
### Europeana Fulltext API 

This project consists of several modules:

1. A common module that contains the data model (Fulltext Resources, AnnoPages, Annotations, AnnotationType) used by other modules
2. An API that 
   - reads data from the Mongo database and makes it available via IIIF presentation requests (JSON-LD)
   - allows one to search the fulltext of a particular newspaper issue (record) and return corresponding annotations.
   - write new fulltext resources (not open to the public)
3. The indexing module for generating a Solr index
4. The annosync module for synchronization with Annotation API.
5. A loader module to read and parse Europeana Newspaper xml files that are the result of the newspaper OCR process. The
resulting objects are stored in a Mongo database (DEPRECATED!)

### Implementation details ###

This Fulltext API implements the functionality described in §3.3 & 3.4 of the Europeana IIIF API Specification.

### Requirements ###
- Java 11 and a Mongo database
- Optionally also a Solr search engine (for search)

### Functionality

## API ##

Start the server and do a request to one of the endpoints:

* Resource
`[http://{server:port}/presentation/{dataset_id}/{local_id}/{resource_id}?format={2/3}]` _(**format** defaults to 2)_

* Annotation Page 
`[http://{server:port}/presentation/{dataset_id}/{local_id}/annopage/{page_id}?format={2/3}]` _(**format** defaults to 2)_

* Annotation
`[http://{server:port}/presentation/{dataset_id}/{local_id}/anno/{annotation_id}?format={2/3}]` _(**format** defaults to 2)_

## Loader (Deprecated)

For batch loading zipped EDM xml files, ensure that the batch.base.directory property is set correctly in the `loader.properties` file.
 
The loader can read a single .zip file from that directory by calling the **zipbatch** endpoint: 
`[http://{server:port}/fulltext/zipbatch?archive={archive.zip}]`.
Alternatively, it will process all the files in the specified directory by specifying **all** as archive name, 
e.g.: [http://{server:port}/fulltext/zipbatch?archive=all]

## Build
``mvn clean install`` (add ``-DskipTests``) to skip the unit tests during build

## Deployment
1. Generate a Docker image using the project's [API Dockerfile](api.Dockerfile). 

2. Configure the application by generating a `fulltext.user.properties` file and placing this in the 
[k8s](k8s) folder. After deployment this file will override the settings specified in the `fulltext.properties` file
located in the [api/src/main/resources](api/src/main/resources) folder. The .gitignore file makes sure the .user.properties file
is never committed.

3. Configure the deployment by setting the proper environment variables specified in the configuration template files
in the [k8s](k8s) folder

4. Deploy to Kubernetes infrastructure

5. Follow the same steps for running a 'Sync' with Annotation API or generating a Solr index (cron deployments) using
the project's [AnnoSync Dockerfile](annosync.Dockerfile) and [Indexing Dockerfile](indexing.Dockerfile). 



