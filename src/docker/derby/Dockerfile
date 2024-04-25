FROM eclipse-temurin:17.0.11_9-jre

ENV DERBY_VERSION=10.16.1.1
ENV DERBY_HOME=/derby
ENV DERBY_LIB=${DERBY_HOME}/lib
ENV CLASSPATH=${DERBY_LIB}/derby.jar:${DERBY_LIB}/derbynet.jar:${DERBY_LIB}/derbytools.jar:${DERBY_LIB}/derbyoptionaltools.jar:${DERBY_LIB}/derbyclient.jar

RUN \
    apt-get update && apt-get install -y wget netcat && \
    wget https://dist.apache.org/repos/dist/release/db/derby/db-derby-${DERBY_VERSION}/db-derby-${DERBY_VERSION}-bin.tar.gz && \
    tar xzf /db-derby-${DERBY_VERSION}-bin.tar.gz && \
    mv /db-derby-${DERBY_VERSION}-bin /derby && \
    rm -Rf /*.tar.gz ${DERBY_HOME}/demo ${DERBY_HOME}/javadoc ${DERBY_HOME}/docs ${DERBY_HOME}/test ${DERBY_HOME}/*.html ${DERBY_HOME}/KEYS \
    apt-get purge wget netcat && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /dbs
VOLUME ["/dbs"]
EXPOSE 1527
