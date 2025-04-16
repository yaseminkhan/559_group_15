#!/bin/bash
export $(grep -v '^#' .env | xargs)

##
# 2) Enter the backend directory (adjust path if needed) 
##
cd backend || {
  echo "Error: Cannot cd into backend. Modify the path if your backend is elsewhere."
  exit 1
}

##
# 3) Decide which server to run based on first argument
##
ROLE="$1"
if [ -z "$ROLE" ]; then
  echo "Usage: $0 <coordinator|primary|backup1|backup2|backup3|backup4>"
  exit 1
fi

echo "Starting $ROLE with TAILSCALE_IP=$TAILSCALE_IP ..."

case "$ROLE" in

  coordinator)
    mvn exec:java -Dexec.mainClass="com.server.ConnectionCoordinator"
    ;;

  primary)
    mvn exec:java \
      -Dexec.mainClass="com.server.WebServer" \
      -Dexec.args="8887 5001 ${BACKUP_SERVER_1_IP}:6001,${BACKUP_SERVER_2_IP}:7001,${BACKUP_SERVER_3_IP}:4001,${BACKUP_SERVER_4_IP}:8001 true"
    ;;

  backup1)
    mvn exec:java \
      -Dexec.mainClass="com.server.WebServer" \
      -Dexec.args="8892 6001 ${PRIMARY_SERVER_IP}:5001,${BACKUP_SERVER_2_IP}:7001,${BACKUP_SERVER_3_IP}:4001,${BACKUP_SERVER_4_IP}:8001 false"
    ;;

  backup2)
    mvn exec:java \
      -Dexec.mainClass="com.server.WebServer" \
      -Dexec.args="8889 7001 ${PRIMARY_SERVER_IP}:5001,${BACKUP_SERVER_1_IP}:6001,${BACKUP_SERVER_3_IP}:4001,${BACKUP_SERVER_4_IP}:8001 false"
    ;;

  backup3)
    mvn exec:java \
      -Dexec.mainClass="com.server.WebServer" \
      -Dexec.args="8890 4001 ${PRIMARY_SERVER_IP}:5001,${BACKUP_SERVER_1_IP}:6001,${BACKUP_SERVER_2_IP}:7001,${BACKUP_SERVER_4_IP}:8001 false"
    ;;

  backup4)
    mvn exec:java \
      -Dexec.mainClass="com.server.WebServer" \
      -Dexec.args="8891 8001 ${PRIMARY_SERVER_IP}:5001,${BACKUP_SERVER_1_IP}:6001,${BACKUP_SERVER_2_IP}:7001,${BACKUP_SERVER_3_IP}:4001 false"
    ;;

  *)
    echo "Unknown ROLE: $ROLE. Valid: coordinator|primary|backup1|backup2|backup3|backup4"
    exit 1
    ;;
esac
