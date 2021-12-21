#!/bin/sh

# This is an enablement script we want to run during image creation

echoDebug() {
    local GREEN="\033[32m"
    local NOCOLOR="\033[0m"
    echo
    echo -e $GREEN$1$NOCOLOR
    echo
}

#These steps follow this guide:
# https://docs.oracle.com/en-us/iaas/data-safe/doc/create-jks-wallets-tls-connection-db-system-that-has-client-authentication-enabled.html

if [ -z ${ORACLE_HOME} ]; then
    echoDebug "No ORACLE_HOME variable.  Exiting..."
    exit 1
fi

WALLET_PWD="WalletPasswd123"
DN="CN=localhost"

#PART 1: Creating server wallet and cert
SERVER_WALLET="/u01/app/oracle/wallet"
SERVER_CERT="/tmp/oracle-server-certificate.crt"
echoDebug "START >>> Creating server wallet and cert"
mkdir -p /u01/app/oracle/wallet
orapki wallet create  -wallet $SERVER_WALLET -pwd $WALLET_PWD -auto_login
orapki wallet add     -wallet $SERVER_WALLET -pwd $WALLET_PWD -dn $DN -keysize 1024 -self_signed -validity 36500
orapki wallet display -wallet $SERVER_WALLET -pwd $WALLET_PWD
orapki wallet export  -wallet $SERVER_WALLET -pwd $WALLET_PWD -dn $DN -cert $SERVER_CERT
orapki cert display -cert $SERVER_CERT -complete
echoDebug "DONE >>> Creating server wallet and cert"

#PART 2: Create a Client Wallet and Certificate
CLIENT_WALLET="/client/oracle/wallet"
CLIENT_CERT="/tmp/oracle-client-certificate.crt"
echoDebug "START >>> Create client wallet and cert"
mkdir -p /client/oracle/wallet
orapki wallet create  -wallet $CLIENT_WALLET -pwd $WALLET_PWD -auto_login
orapki wallet add     -wallet $CLIENT_WALLET -pwd $WALLET_PWD -dn $DN -keysize 1024 -self_signed -validity 36500
orapki wallet display -wallet $CLIENT_WALLET -pwd $WALLET_PWD
orapki wallet export  -wallet $CLIENT_WALLET -pwd $WALLET_PWD -dn $DN -cert $CLIENT_CERT
orapki cert display -cert $CLIENT_CERT -complete
echoDebug "DONE >>> Create client wallet and cert"

# PART 3: Exchange Client and Server Certificates
echoDebug "START >>> Exchange certs"
orapki wallet add     -wallet $CLIENT_WALLET -pwd $WALLET_PWD -trusted_cert -cert $SERVER_CERT -validity 36500
orapki wallet display -wallet $CLIENT_WALLET -pwd $WALLET_PWD
orapki wallet add     -wallet $SERVER_WALLET -pwd $WALLET_PWD -trusted_cert -cert $CLIENT_CERT -validity 36500
orapki wallet display -wallet $SERVER_WALLET -pwd $WALLET_PWD
echoDebug "DONE >>> Exchanging certs"

# PART 3.1: Modify owner of server wallet
chown -R oracle:oinstall $SERVER_WALLET
chown -R oracle:oinstall $CLIENT_WALLET

# PART 4: Create JKS wallet from oracle wallet
echoDebug "START >>> Create JKS wallet"
CLIENT_KEYSTORE="/client/oracle/store/client-keystore.jks"
CLIENT_TRUSTSTORE="/client/oracle/store/client-truststore.jks"
mkdir -p /client/oracle/store
orapki wallet pkcs12_to_jks \
  -wallet $CLIENT_WALLET/ewallet.p12 -pwd $WALLET_PWD \
  -jksKeyStoreLoc   $CLIENT_KEYSTORE   -jksKeyStorepwd $WALLET_PWD \
  -jksTrustStoreLoc $CLIENT_TRUSTSTORE -jksTrustStorepwd $WALLET_PWD
echoDebug "DONE >>> Create JKS wallet"

# PART 5: Configure server network
## Overwrite to sqlnet.ora
SQLNET=/opt/oracle/oradata/dbconfig/XE/sqlnet.ora
SQLNET_BACKUP=/opt/oracle/oradata/dbconfig/XE/sqlnet.backup
touch $SQLNET_BACKUP && cat $SQLNET > $SQLNET_BACKUP
cat <<EOF > $SQLNET
# Generated by Oracle configuration tools.

NAMES.DIRECTORY_PATH= (TNSNAMES, EZCONNECT)

WALLET_LOCATION =
   (SOURCE =
     (METHOD = FILE)
     (METHOD_DATA =
       (DIRECTORY = $SERVER_WALLET)
     )
   )

# General Settings
SSL_CLIENT_AUTHENTICATION = TRUE

# SQLNET Settings
SQLNET.AUTHENTICATION_SERVICES = (TCPS, BEQ, NONE)
EOF

echoDebug "DIFF: $SQLNET_BACKUP >>> $SQLNET"
diff -w $SQLNET_BACKUP $SQLNET

## Overwrite to listener.ora
LISTENER=/opt/oracle/oradata/dbconfig/XE/listener.ora
LISTENER_BACKUP=/opt/oracle/oradata/dbconfig/XE/listener.backup
touch $LISTENER_BACKUP && cat $LISTENER > $LISTENER_BACKUP
cat <<EOF > $LISTENER
# listener.ora Network Configuration File:

SID_LIST_LISTENER =
  (SID_LIST =
    (SID_DESC =
      (SID_NAME = PLSExtProc)
      (ORACLE_HOME = /opt/oracle/product/18c/dbhomeXE)
      (PROGRAM = extproc)
    )
  )

LISTENER =
  (DESCRIPTION_LIST =
    (DESCRIPTION =
      (ADDRESS = (PROTOCOL = TCP)(HOST = 0.0.0.0)(PORT = 1521))
    )
    (DESCRIPTION =
      (ADDRESS = (PROTOCOL = TCPS)(HOST = 0.0.0.0)(PORT = 1522))
    )
  )

DEFAULT_SERVICE_LISTENER = (XE)

WALLET_LOCATION =
  (SOURCE =
    (METHOD = FILE)
    (METHOD_DATA =
      (DIRECTORY = $SERVER_WALLET)
    )
  )

SSL_CLIENT_AUTHENTICATION = TRUE
EOF

echoDebug "DIFF: $SQLNET_BACKUP >>> $SQLNET"
diff -w $LISTENER_BACKUP $LISTENER

#Overwrite to tnsnames.ora
TNSNAMES=/opt/oracle/oradata/dbconfig/XE/tnsnames.ora
TNSNAMES_BACKUP=/opt/oracle/oradata/dbconfig/XE/tnsnames.backup
touch $TNSNAMES_BACKUP && cat $TNSNAMES > $TNSNAMES_BACKUP
cat <<EOF > $TNSNAMES
XE =
  (DESCRIPTION =
    (ADDRESS_LIST =
      (ADDRESS = (PROTOCOL = TCP )(HOST = 0.0.0.0)(PORT = 1521))
      (ADDRESS = (PROTOCOL = TCPS)(HOST = 0.0.0.0)(PORT = 1522))
    )
    (CONNECT_DATA =
      (SERVER = DEDICATED)
      (SERVICE_NAME = XE)
    )
  )

LISTENER_XE =
  (ADDRESS_LIST =
    (ADDRESS = (PROTOCOL = TCP )(HOST = 0.0.0.0)(PORT = 1521))
    (ADDRESS = (PROTOCOL = TCPS)(HOST = 0.0.0.0)(PORT = 1522))
  )

XEPDB1 =
  (DESCRIPTION_LIST =
    (DESCRIPTION =
      (ADDRESS_LIST =
        (ADDRESS = (PROTOCOL = TCP )(HOST = 0.0.0.0)(PORT = 1521))
        (ADDRESS = (PROTOCOL = TCPS)(HOST = 0.0.0.0)(PORT = 1522))
      )
      (CONNECT_DATA =
        (SERVER = DEDICATED)
        (SERVICE_NAME = XEPDB1)
      )
    )
  )

EXTPROC_CONNECTION_DATA =
  (DESCRIPTION =
     (ADDRESS_LIST =
       (ADDRESS = (PROTOCOL = IPC)(KEY = EXTPROC_FOR_XE))
     )
     (CONNECT_DATA =
       (SID = PLSExtProc)
       (PRESENTATION = RO)
     )
  )
EOF

echoDebug "DIFF: $TNSNAMES_BACKUP >>> $TNSNAMES"
diff -w $TNSNAMES_BACKUP $TNSNAMES

# For some reason this oracle image does not allow user 'oracle' to run the oracle process *sigh*
chmod 6751 $ORACLE_HOME/bin/oracle