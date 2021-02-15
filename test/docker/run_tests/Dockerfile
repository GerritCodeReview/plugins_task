FROM alpine:3.11

ARG UID=1000
ARG GID=1000
ENV USER gerrit_admin
ENV USER_HOME /home/$USER
ENV RUN_TESTS_DIR task/test/docker/run_tests
ENV WORKSPACE $USER_HOME/workspace

RUN apk --update add --no-cache openssh bash git python2 shadow
RUN echo "StrictHostKeyChecking no" >> /etc/ssh/ssh_config

RUN groupadd -f -g $GID users2
RUN useradd -u $UID -g $GID $USER
RUN mkdir -p $WORKSPACE $USER_HOME/.ssh
RUN chown -R $USER $USER_HOME
USER $USER
ENTRYPOINT $RUN_TESTS_DIR/start.sh
