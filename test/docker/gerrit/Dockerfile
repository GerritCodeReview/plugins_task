FROM gerritcodereview/gerrit:3.8.3-ubuntu22

ENV GERRIT_SITE /var/gerrit
RUN git config -f "$GERRIT_SITE/etc/gerrit.config" auth.type \
    DEVELOPMENT_BECOME_ANY_ACCOUNT
RUN touch "$GERRIT_SITE"/.firstTimeRedirect

COPY artifacts /tmp/
RUN cp /tmp/task.jar "$GERRIT_SITE/plugins/task.jar"
RUN { [ -e /tmp/gerrit.war ] && cp /tmp/gerrit.war "$GERRIT_SITE/bin/gerrit.war" ; } || true
RUN chmod 777 "$GERRIT_SITE/git"
