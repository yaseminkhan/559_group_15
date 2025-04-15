# Inkblink
## WIKI Page for Runtime Environment
https://wiki.jinsongdev.com/books/orchestration/page/nodocker-environment-for-inkblink

## Install Kafka Manually
Extract downloaded tar file

```shell
tar -xzf kafka_2.13-3.5.1.tgz
cd kafka_2.13-3.5.1
```

## Create kraft.properties File
Create a file kraft.properties and place it in the extracted directory

```ini
node.id=1
process.roles=broker,controller
listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://127.0.0.1:19092
advertised.listeners=PLAINTEXT://100.78.239.70:9092
controller.quorum.voters=1@127.0.0.1:19092
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
inter.broker.listener.name=PLAINTEXT
offsets.topic.replication.factor=1
group.initial.rebalance.delay.ms=0
log.retention.hours=1
log.cleanup.policy=delete
message.max.bytes=33554432
```

## Format Storage
```shell
#In your extracted directory, run
bin/kafka-storage.sh format -t $(bin/kafka-storage.sh random-uuid) -c config/kraft.properties
```
## Start Kafka
```shell
bin/kafka-server-start.sh config/kraft.properties
```

Check if Kafka is running properly:

```shell
nc -vz <your_ip> 9092
# Or
kafka-topics --bootstrap-server <your_ip>:9092 --list
```

## Configure .env file
```ini
# Replace with corresponding tailscale IPs 
# TAILSCALE_IP is your machine IP
# Replace all others with your TAILSCALE_IP
TAILSCALE_IP=100.78.239.70 
KAFKA_IP=100.78.239.70
COORDINATOR_IP=100.76.248.111
PRIMARY_SERVER_IP=100.78.239.70
BACKUP_SERVER_1_IP=100.78.239.70
BACKUP_SERVER_2_IP=100.72.227.86
BACKUP_SERVER_3_IP=100.78.239.70
BACKUP_SERVER_4_IP=100.72.227.86
KAFKA_BOOTSTRAP_SERVERS=100.78.239.70:9092
```
## Run Servers Individually
You must run servers in the following order:
1. Kafka
2. Coordinator
3. Backups
4. Primary
```shell
# Run export once on each terminal
export $(grep -v '^#' .env | xargs)

# arguments: coordinator, primary, backup1, backup2, backup3, backup4
sh run-server.sh <server>
```

## Reset Kafka When Restarting Servers
```shell
# for MAC
rm -rf kafka-logs # Run inside Kafka directory

# for Windows
rm -rf C:\tmp\kafka-logs  # Using non PowerShell
Remove-Item -Recurse -Force C:\tmp\kafka-logs # Using PowerShell
```

# Starting Clients (Frontend)
Navigate to frontend/ in project directory

```shelll
npm start
```
