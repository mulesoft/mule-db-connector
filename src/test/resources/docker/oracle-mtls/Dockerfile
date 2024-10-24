FROM oracle-ssl

COPY setup/ /tmp/setup/
COPY startup/ /opt/oracle/scripts/startup

USER root
RUN chmod 777 /tmp/setup/enable_ssl.sh
RUN /tmp/setup/enable_ssl.sh
RUN rm -rf /tmp/setup

EXPOSE 1521 1522

USER oracle
CMD ["/bin/sh","-c", "exec $ORACLE_BASE/$RUN_FILE"]



