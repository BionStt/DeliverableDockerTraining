# Docker Networking
---
Docker networking is basically used to establish communication between the docker containers and the outside world via host machine or you can say it is a communication passage through which all the isolated containers communicate with each other in various situations to perform the required actions. In this step, we will explain basic Docker networking concepts with practical examples on Ubuntu.

## Types of Docker Networks
Docker’s networking subsystem is pluggable, using drivers. Several drivers exist by default, and provide core networking functionality:

- **Bridge network** : When you start Docker, a default bridge network is created automatically. A newly-started containers will connect automatically to it. You can also create user-defined custom bridge networks. User-defined bridge networks are superior to the default bridge network.
- **Host network** : It removes network isolation between the container and the Docker host, and use the host's networking directly. 
- **overlay**: Overlay networks connect multiple Docker daemons together and enable swarm services to communicate with each other. You can also use overlay networks to facilitate communication between a swarm service and a standalone container, or between two standalone containers on different Docker daemons. 

- **Macvlan network** : Some applications, especially legacy applications or applications which monitor network traffic, expect to be directly connected to the physical network. In this type of situation, you can use the Macvlan network driver to assign a MAC address to each container's virtual network interface, making it appear to be a physical network interface directly connected to the physical network.

- **None network** : In this kind of network, containers are not attached to any network and do not have any access to the external network or other containers. So, this network is used when you want to completely disable the networking stack on a container.
Overlay network : Creates an internal private network that spans across all the nodes participating in the swarm cluster. So, Overlay networks facilitate communication between a docker swarm service and a standalone container, or between two standalone containers on different Docker Daemons.
By default, we already have a bridge network , a host network and a none network . We cannot create an additional host or none network . This step will therefore explain the use of bridge networks . For the overlay network , this type being used for inter-host communication, we will see this in the swarm part of this tutorial.

To list all your networks, run:
```shell
$ docker network ls
NETWORK ID          NAME                DRIVER              SCOPE
466452fbcd65        bridge              bridge              local
9c7a9895e729        host                host                local
585f508793d7        none                null                local
```
## Work with network commands
 The commands are available through the Docker Engine CLI. These commands are:

```shell
docker network create
docker network connect
docker network ls
docker network rm
docker network disconnect
docker network inspect
```

## Bridge network

We will be using two Alpine containers to explain this type of network.

Let's run two Alpine containers namely C1 and C2 using commands:

```shell
$ docker run -it -d --name c1 alpine sh
$ docker run -it -d --name c2 alpine sh
```
Next, let us find out the IP address of those running containers. To do so, run:

```shell
$ docker exec -it c1 sh –c "ip a"
# You will see this line inet: 172.17.0.6/16 brd 172.17.255.255 scope global eth0
$ docker exec -it c2 sh –c  "ip a"
# You will see this line: inet 172.17.0.7/16 brd 172.17.255.255 scope global eth0
```
As you can see, the IP address of C1 container is 172.17.0.6 and IP address of C2 is 172.17.0.7.

Now let us go ahead and try to ping each other to ensure if they can be able to communicate.
First, attach to the running C1 container and try to ping the C2 container:

```shell
$ docker attach c1
$ ping –c 5 172.17.0.7
PING 172.17.0.7 (172.17.0.7): 56 data bytes
64 bytes from 172.17.0.7: seq=0 ttl=64 time=0.102 ms
64 bytes from 172.17.0.7: seq=1 ttl=64 time=0.070 ms
64 bytes from 172.17.0.7: seq=2 ttl=64 time=0.080 ms
...
```
As you see in the above screenshots, the communication is happening between the containers with in the same network.
We can also verify it by inspecting the bridge network using command:
```shell
$ docker network inspect bridge
```
## Creating user-defined bridge network
Like we already said, when you start Docker, a default bridge network is created automatically. All newly-started containers will connect automatically to it. However, you can also create user-defined custom bridge networks.

To create new network driver, simply run:

```shell
$ docker network create my_net
```
Or,
```shell
$ docker network create --driver bridge my_net
```
Both commands will do the same work. If you will not specify the driver name, it will create in the default network driver i.e. bridge.
On user-defined networks like my_net, containers can not only communicate by IP address, but can also resolve a container name to an IP address. This capability is called automatic service discovery.

To ensure if the containers can communicate with each other, let us run three alpine containers namely B1, B2 and B3 on my_net network which we created earlier.
```shell
$ docker run -it -d --name B1 --network my_net alpine ash
$ docker run -it -d --name B2 --network my_net alpine ash
$ docker run -it -d --name B3 --network my_net alpine ash
```
Now try to attach to any one of the containers and ping the other two using container name.
```shell
$ docker container attach B1
# ping -c 5 B2
```
You will see that containers are able to comminicate with each other.

# Connecting a Container to a Network

Let's create a network test1
```shell
$ docker network create --subnet 192.168.200.0/24 -o "com.docker.network.bridge.name = br-test " test1
```

To attach a network to a container, just use the argument --network with docker run:
```shell
$ docker run -it --network test1 --name ctest alpine sh
# ifconfig
```
We can also add an additional network with `docker network connect`: In another terminal, type:

```shell
$ docker network create test2
$ docker network connect test2 ctest
```
Then in the container:

```shell
/ # ifconfig
```

## Removing Networks
Now that our tests are finished, we can delete our networks:

```shell
$ docker network rm test1 test2
Error response from daemon: network test3 has active endpoints
```
The test3 network is still used by containers. We should stop and remove the containers first.
```shell
$ docker stop ctest && docker rm ctest
$ docker network rm test1 test2
```