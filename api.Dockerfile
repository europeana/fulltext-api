# Builds a docker image from a locally built Maven war. Requires 'mvn package' to have been run beforehand
FROM tomcat:9-jre17
LABEL Author="Europeana Foundation <development@europeana.eu>"
WORKDIR /usr/local/tomcat/webapps

#ARG APM_VERSION=1.34.1
#ENV ELASTIC_APM_VERSION=$APM_VERSION
#ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$APM_VERSION/elastic-apm-agent-$APM_VERSION.jar /usr/local/elastic-apm-agent.jar

ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.34.1/elastic-apm-agent-1.34.1.jar /usr/local/elastic-apm-agent.jar


# Copy unzipped directory so we can mount config files in Kubernetes pod
# Ensure sensitive files aren't copied
COPY api/target/fulltext ./ROOT/
