#!/bin/bash
set -e
shopt -s nullglob

# Create new configset based off default one
cp -r /opt/solr/server/solr/configsets/_default/ /opt/solr/server/solr/configsets/"$FULLTEXT_INDEXING_CORE"/

# Overwrite Files copied to /opt/fulltext-conf in Dockerfile
mv /opt/fulltext-conf/* /opt/solr/server/solr/configsets/"$FULLTEXT_INDEXING_CORE"/conf/

# Set access rights for fulltext configset
chown -R solr:solr /opt/solr/server/solr/configsets/"$FULLTEXT_INDEXING_CORE"

precreate-core "$FULLTEXT_INDEXING_CORE" /opt/solr/server/solr/configsets/"$FULLTEXT_INDEXING_CORE"

# Set access rights for fulltext core data
chown -R solr:solr /opt/solr/server/solr/mycores/"$FULLTEXT_INDEXING_CORE"/

## drop access to solr and run cmd
exec gosu solr "$@"
