#!/bin/sh

set -e

# Perform all actions as $POSTGRES_USER
export PGUSER="$POSTGRES_USER"


# Load uuid-ossp into both  $POSTGRES_DB
echo "Loading uuid-ossp extension into $POSTGRES_DB"
"${psql[@]}" --dbname="$POSTGRES_DB" <<-'EOSQL'
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOSQL