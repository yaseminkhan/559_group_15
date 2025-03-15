FROM openjdk:21-jdk-slim

RUN apt-get update
RUN apt-get install -y maven

COPY pom.xml /usr/local/service/pom.xml
COPY src /usr/local/service/src
WORKDIR /usr/local/service

EXPOSE 8887

RUN mvn clean package

CMD [ "mvn", "exec:java" ]
