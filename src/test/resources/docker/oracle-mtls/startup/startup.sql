-- Ignore pluggable databases
alter session set "_ORACLE_SCRIPT"=true;  
  
-- Add authenticated user
create user sslclient identified externally as'CN=localhost';
grant all privileges to sslclient;