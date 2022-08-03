# This script creates the Fulltext database and the Spring Batch JobRepository
mongo -- "$MONGO_INITDB_DATABASE" <<EOF
    var rootUser = '$ROOT_USERNAME';
    var rootPassword = '$ROOT_PASSWORD';

    db.auth(rootUser, rootPassword);


db.getSiblingDB('$FULLTEXT_DB').createCollection('temp');
db.getSiblingDB('$BATCH_DB').createCollection('temp');

EOF