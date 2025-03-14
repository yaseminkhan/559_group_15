# Docker Configuration Instructions

## Creating Images

Getting Docker set up for multiple containers on a single machine is simple. Everything is already configured in `compose.yaml`. Before starting the containers, it is important to build the images for both servers and clients (frontend and backend). To build the images, type in the following commands in the terminal:

```
docker build -t server ./backend
docker build -t client ./frontend
```

The images should build without any issues. To verify that the images have been built, you can type in `docker image ls -a`, which should list both images since they have been built.

## Creating and Running Containers

To create the containers that run using the Docker images, simply type in `docker-compose up --build` and hit enter. This should build all containers and run them as needed. Additional clients can be added in `compose.yaml` as needed by copying and pasting existing code, and changing the parameters like port numbers where necessary.

At any time to check that all containers are running, type in `docker ps -a` to view all containers.

## Deleting Containers

If you want to delete all stopped containers, you run the following command:

```
docker container prune
```

and then hit `y`. This should delete all existing containers.
