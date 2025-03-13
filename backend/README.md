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
To specify different servers for now:

```
mvn exec:java -D"exec.mainClass"="WebServer" -D"exec.args"="8887 5001 localhost:5002"
mvn exec:java -D"exec.mainClass"="WebServer" -D"exec.args"="8888 5002 localhost:5001"
```

# Test
```
websocat ws://localhost:8887
```
 OR

You could set up a basic javascript webpage to send messages over to `localhost:8887`