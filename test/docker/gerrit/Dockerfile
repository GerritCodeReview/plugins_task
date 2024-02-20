FROM gerritcodereview/gerrit:3.5.6-ubuntu20
ARG ROOT_CFG_PRJ
ARG ROOT_CFG_BRANCH

ENV GERRIT_SITE /var/gerrit
RUN git config -f "$GERRIT_SITE/etc/gerrit.config" auth.type \
    DEVELOPMENT_BECOME_ANY_ACCOUNT
RUN touch "$GERRIT_SITE"/.firstTimeRedirect

RUN if [ -n "$ROOT_CFG_PRJ" ] ; then \
       git config -f "$GERRIT_SITE/etc/task.config" rootConfig.project "$ROOT_CFG_PRJ"; \
    fi
RUN if [ -n "$ROOT_CFG_BRANCH" ] ; then \
       git config -f "$GERRIT_SITE/etc/task.config" rootConfig.branch "$ROOT_CFG_BRANCH"; \
    fi

COPY artifacts /tmp/
RUN cp /tmp/task.jar "$GERRIT_SITE/plugins/task.jar"
RUN { [ -e /tmp/gerrit.war ] && cp /tmp/gerrit.war "$GERRIT_SITE/bin/gerrit.war" ; } || true
RUN chmod 777 "$GERRIT_SITE/git"
