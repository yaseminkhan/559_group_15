FROM openjdk:21-jdk-slim

RUN apt-get update
RUN apt-get install -y maven
RUN apt-get install -y netcat-openbsd

COPY pom.xml /usr/local/service/pom.xml
COPY src /usr/local/service/src
WORKDIR /usr/local/service

COPY wait-for-backups.sh /wait-for-backups.sh
RUN chmod +x /wait-for-backups.sh

COPY wait-for-kafka.sh /wait-for-kafka.sh
RUN chmod +x /wait-for-kafka.sh

EXPOSE 8887 5001 5002 5003 6001 7001 4001 9999

RUN mvn clean package

CMD [ "mvn", "exec:java" ]
