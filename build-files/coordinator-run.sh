#/bin/bash
# This script is used to run the coordinator server
export COORDINATOR_IP=100.78.239.70
export PRIMARY_SERVER_IP=100.78.239.70
cd ../backend
set PRIMARY_SERVER_IP=100.78.239.70
set COORDINATOR_IP=100.78.239.70
mvn exec:java -Dexec.mainClass="com.server.ConnectionCoordinator"