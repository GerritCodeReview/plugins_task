#!/usr/bin/env bash

echo "Initializing Gerrit site ..."
java -jar "$GERRIT_SITE"/bin/gerrit.war init --batch -d "$GERRIT_SITE"
java -jar "$GERRIT_SITE"/bin/gerrit.war reindex -d "$GERRIT_SITE"
echo "Running Gerrit ..."
"$GERRIT_SITE"/bin/gerrit.sh run
