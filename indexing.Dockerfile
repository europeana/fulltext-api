FROM eclipse-temurin:17-jre-ubi9-minimal
LABEL Author="Europeana Foundation <development@europeana.eu>"

#ARG APM_VERSION=1.34.1
#ENV ELASTIC_APM_VERSION=$APM_VERSION
#ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$APM_VERSION/elastic-apm-agent-$APM_VERSION.jar /usr/local/elastic-apm-agent.jar

ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.34.1/elastic-apm-agent-1.34.1.jar /usr/local/elastic-apm-agent.jar

COPY indexing/target/fulltext-indexing.jar /opt/app/fulltext-indexing.jar
ENTRYPOINT ["java","-jar","/opt/app/fulltext-indexing.jar"]
