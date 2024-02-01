FROM gerritcodereview/gerrit:3.9.1-ubuntu22

ENV GERRIT_SITE /var/gerrit
RUN git config -f "$GERRIT_SITE/etc/gerrit.config" auth.type \
    DEVELOPMENT_BECOME_ANY_ACCOUNT
RUN git config -f "$GERRIT_SITE/etc/gerrit.config" --add container.javaOptions \
    "--add-opens java.base/java.lang=ALL-UNNAMED"
RUN touch "$GERRIT_SITE"/.firstTimeRedirect

COPY artifacts /tmp/
RUN cp /tmp/task.jar "$GERRIT_SITE/plugins/task.jar"
RUN { [ -e /tmp/gerrit.war ] && cp /tmp/gerrit.war "$GERRIT_SITE/bin/gerrit.war" ; } || true
RUN chmod 777 "$GERRIT_SITE/git"
