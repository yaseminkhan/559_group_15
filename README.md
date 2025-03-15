# Docker Configuration Instructions

## Run Docker Containers

```docker-compose up```

To force build: ```docker-compose up --build```

At any time to check that all containers are running, type in `docker ps -a` to view all containers.

## Remove Old Images:
```docker-compose down --rmi all```

## Deleting Containers

If you want to delete all stopped containers, you run the following command:

```
docker container prune
```

and then hit `y`. This should delete all existing containers.
