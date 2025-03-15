FROM openjdk:21-jdk-slim

# ENV PARAM=-D"exec.args"="8887 5001 localhost:5002"

RUN apt-get update
RUN apt-get install -y maven

COPY pom.xml /usr/local/service/pom.xml
COPY src /usr/local/service/src
WORKDIR /usr/local/service

EXPOSE 8887

RUN mvn clean package

CMD [ "mvn", "exec:java" ]
