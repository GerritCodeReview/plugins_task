FROM openjdk:8
ARG GERRIT_WAR
ARG TASK_PLUGIN_JAR
ARG UID=1000
ARG GID=1000

ENV GERRIT_USER gerrit
ENV GERRIT_SITE /var/gerrit
ENV USER_HOME /home/$GERRIT_USER
RUN mkdir -p $GERRIT_SITE/bin $GERRIT_SITE/plugins $GERRIT_SITE/etc $USER_HOME/.ssh
COPY $GERRIT_WAR $GERRIT_SITE/bin/gerrit.war
COPY $TASK_PLUGIN_JAR $GERRIT_SITE/plugins/task.jar
RUN touch $GERRIT_SITE/etc/gerrit.config && \
    git config -f $GERRIT_SITE/etc/gerrit.config auth.type DEVELOPMENT_BECOME_ANY_ACCOUNT

EXPOSE 29418 8080
RUN groupadd -f -g $GID users2 && \
  useradd -u $UID -g $GID $GERRIT_USER && \
  chown -R $GERRIT_USER $GERRIT_SITE $USER_HOME

USER $GERRIT_USER
ENTRYPOINT /docker/gerrit_start.sh
