# Use same version as chaos db
FROM library/mongo:4.4.4

EXPOSE 27017

# default mongo credentials
ENV MONGO_INITDB_ROOT_USERNAME admin
ENV MONGO_INITDB_ROOT_PASSWORD password

COPY init-mongo.sh /docker-entrypoint-initdb.d/init-mongo.sh