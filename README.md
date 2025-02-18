# Prerequisites
- Maven
- JDK 21

# Compile
Navigate to `backend` before compiling 

Run on your terminal :
```
mvn compile
```

# Run
```
mvn exec:java 
```
 OR
```
mvn exec:java -Dexec.mainClass="com.server.Webserver"
```

# Test
```
websocat ws://localhost:8887
```
 OR

You could set up a basic javascript webpage to send messages over to localhost:8887