FROM kyleaure/oracle-18.4.0-expanded:1.0.full.ssl

COPY setup/ /tmp/setup/
COPY startup/ /opt/oracle/scripts/startup

RUN chmod 777 /tmp/setup/enable_ssl.sh
RUN /tmp/setup/enable_ssl.sh
RUN rm -rf /tmp/setup

EXPOSE 1521 1522

CMD ["/bin/sh","-c", "exec $ORACLE_BASE/$RUN_FILE"]

# Currently tagged in DockerHub as: kyleaure/oracle-ssl-18.4.0-xe-prebuilt:2.0