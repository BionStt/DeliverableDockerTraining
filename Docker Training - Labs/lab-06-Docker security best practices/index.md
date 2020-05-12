# Lab 06- Docker Security Best Practices
---


### Lab Steps


- [Step 1. Remapping Container Root with User Namespaces](#step-1-remapping-container-root-with-user-namespaces)
  - [Task 1 - Identify current Docker user](#task-1---identify-current-docker-user)
  - [Task 2 - Change Container User](#task-2---change-container-user)
  - [Task 3 - Enabling User Namespaces](#task-3---enabling-user-namespaces)
  - [Task 4 - User Namespaces Protection](#task-4---user-namespaces-protection)
- [Step 2. Auditing Docker Security with Docker Desktop Security Tool](#step-2-auditing-docker-security-with-docker-desktop-security-tool)
- [Step 3. Scanning Docker Images for Vulnerabilities with CoreOS Clair Tool](#step-3-scanning-docker-images-for-vulnerabilities-with-coreos-clair-tool)
  - [Task 1 - Deploy Postgres](#task-1---deploy-postgres)
  - [Task 2 - Populate DB](#task-2---populate-db)
  - [Task 3 - Deploy Clair](#task-3---deploy-clair)
  - [Task 4 - Scan Image](#task-4---scan-image)
  - [Task 5 - JSON Output](#task-5---json-output)
  - [Task 6 - Scan Private Image](#task-6---scan-private-image)
- [Step 4. Secure Private Registry with TLS/SSL](#step-4-secure-private-registry-with-tlsssl)
  - [Task 1 - Starting Registry](#task-1---starting-registry)
  - [Task 2 - SSL](#task-2---ssl)
  - [Task 4 - Pushing Images](#task-4---pushing-images)
  - [Task 5 - Pulling Images](#task-5---pulling-images)



<div style="page-break-after: always"></div>


# Step 1. Remapping Container Root with User Namespaces
In this step you'll learn how to configure User Namespaces to add additional user isolation and remap container root users to non-privileged users on the host machine.
This content is based on Katacoda, an online interactive lab available. Jump to this [https://www.katacoda.com/courses/docker-security/userns-user-namespaces](https://www.katacoda.com/courses/docker-security/userns-user-namespaces), login using either a social account (Google, LinkedIn, GitHub, or Twitter) or create a Katacoda account.

## Task 1 - Identify current Docker user
By default, the Docker Daemon runs as root user on the host. By listing all the processes on the box you can identify which user the Docker Daemon runs as.

```shell
ps aux | grep docker
```
As a result of the Daemon running as root, any containers started will have the same security context as the host's root user.

```shell
docker run --rm alpine id
```
This has the side-effect that if files owned by the root user are accessible from the container, then can be modified by the running container.

The following command identifies the risk of running containers as root user.

First, create a copy of the touch command on our host.

```shell
sudo cp /bin/touch /bin/touch.bak && ls -lha /bin/touch.bak
```
Because the container is both root inside the container and on the host, the file can be removed.

```shell
docker run -it -v /bin/:/host/ alpine rm -f /host/touch.bak
```
As a result, the command no longer exists.

```shell
ls -lha /bin/touch.bak
```
In this case, the container is capable of deleting the touch binary from the host.

## Task 2 - Change Container User
This risk can be migrated by changing the user and group context and the container running as an unprivileged user.

```shell
docker run --user=1000:1000 --rm alpine id
```
As a unprivileged user, it would not be possible to delete the binary.

```shell
sudo cp /bin/touch /bin/touch.bak
docker run --user=1000:1000 -it -v /bin:/host/ alpine rm -f /host/touch.bak
```
## Task 3 - Enabling User Namespaces

User Namespaces are a Linux Kernel security feature. The feature allows a root user inside a namespace, or container, to a unprivileged user id on the Host.

> **Note**: Docker recommends not to switch the Docker daemon back and forth between having user namespace mode enabled, and user namespace mode disabled. Doing this can cause issues with image permissions and visibility.

User namespaces are enabled when the Docker Daemon is started using the parameter userns-remap. Run the command below to modify the Docker daemin settings and restart the process.

```shell
curl https://gist.githubusercontent.com/BenHall/bb878c99d06a63cd8ed4d1c0a6941df4/raw/76136ffbca341846619086cfe40ab8e013683f47/daemon.json -o /etc/docker/daemon.json && sudo service docker restart
```
View the settings using `cat /etc/docker/daemon.json`

Once restarted, you can verify that user namespaces are in place using the following command

```shell
docker info | grep "Root Dir"
```
Docker will no longer store files on disk as the root user. Instead, everything is processed as the mapped user. The Docker Root Dir defines where Docker is storing data for the mapped user.

  > **Note**: When enabling this feature on an existing system, Docker Images will be required to be re-downloaded.

However, if we required root level access inside the container, then we still expose ourselves to the previous scenario. This is where user namespaces come in.

## Task 4 - User Namespaces Protection
With userns enabled, the Docker Dameon will be running as a different user.

```shell
ps aux | grep dockerd
```
When containers are launched, the user inside the container will have root privileges.

```shell
docker run --rm alpine id
```
However, the user will not be able to modify anything running on the host.

```shell
sudo cp /bin/touch /bin/touch.bak
docker run -it -v /bin/:/host/ alpine rm -f /host/touch.bak
```
Unlike before, our ps command still exists.

```shell
ls -lha /bin/touch.bak
```
By using User Namespaces, it's possible to separate Docker root users and provide more security and isolation than previously available.

# Step 2. Auditing Docker Security with Docker Desktop Security Tool

The [Docker Bench for Security](https://github.com/docker/docker-bench-security) is a script that checks for dozens of common best-practices around deploying Docker containers in production. The tests are all automated, and are inspired by the [CIS Docker Benchmark v1.2.0](https://www.cisecurity.org/benchmark/docker/).

The easiest way to run Docker Bench for Security is to make use of the pre-built container:
```shell
docker run -it --net host --pid host --userns host --cap-add audit_control \
    -e DOCKER_CONTENT_TRUST=$DOCKER_CONTENT_TRUST \
    -v /etc:/etc:ro \
    -v /usr/bin/containerd:/usr/bin/containerd:ro \
    -v /usr/bin/runc:/usr/bin/runc:ro \
    -v /usr/lib/systemd:/usr/lib/systemd:ro \
    -v /var/lib:/var/lib:ro \
    -v /var/run/docker.sock:/var/run/docker.sock:ro \
    --label docker_bench_security \
    docker/docker-bench-security
```
Docker Security with Docker Desktop has not been tested on Docker Desktop for Windows [[Ref](https://github.com/docker/docker-bench-security/issues/261)]. You are required to run it on a Linux host such as Katacoda or on Play With Docker, also known as PWD, which is available [here](https://labs.play-with-docker.com). PWD is a Docker playground which allows users to run Docker commands in a matter of seconds.  In addition to the playground, PWD also includes a training site composed of a large set of Docker labs and quizzes from beginner to advanced level available at [training.play-with-docker.com](http://training.play-with-docker.com/).

- Login in to Play With Docker palyground available [here](https://labs.play-with-docker.com) and start a new instance.

- Start a simple Alpine Container in the detached mode.
   ```shell
   docker run -itd alpine
   ```
- To scan the alpine container and see if it contains any issues, start Docker Bench for Security using this command.
   ```shell
   docker run -it --net host --pid host --userns host --cap-add audit_control \
    -e DOCKER_CONTENT_TRUST=$DOCKER_CONTENT_TRUST \
    -v /etc:/etc:ro \
    -v /usr/bin/containerd:/usr/bin/containerd:ro \
    -v /usr/bin/runc:/usr/bin/runc:ro \
    -v /usr/lib/systemd:/usr/lib/systemd:ro \
    -v /var/lib:/var/lib:ro \
    -v /var/run/docker.sock:/var/run/docker.sock:ro \
    --label docker_bench_security \
    docker/docker-bench-security
   ```
Note down the score. Notice that the log is highlighting an issue related to running the alpine container as root (Section 4.1).  
  ```
  [WARN] 4.1  - Ensure a user for the container has been created
  [WARN]      * Running as root: musing_panini
  ```
- Remove the Alpine container.
   ```shell
    docker stop $(docker ps -aq)
    docker rm $(docker ps -aq)
   ```
- To address the issue, run a new Alpine container with a **non root user**.
   ```shell
   docker run -itd alpine --user 1001:1001 alpine
   ```
  To check the user running in the container, let's exec within the container. In order to get the id (or the name) of the container, run the `docker ps` command. Then Run the following `docker exec` command (`cb6d47` is supposed to be the container id).

   ```shell
   docker exec -it cb6d47 sh
   ```
  Within the container shell, check the user identity using command like `id`, `whoami`

- Run Docker Desktop for security again and check if the root issue is flagged again.
   ```shell
   docker run -itd alpine --user 1001:1001 alpine
   ```  
- Note down the score and compare it with the previous score. Notice that this time the control in section `4.1  - Ensure a user for the container has been created` passes. You can optionnaly diff the reports using an online tool such as [DiffNow](https://www.diffnow.com/compare-clips). Copy the reports from the console and paste them to the online text areas.

# Step 3. Scanning Docker Images for Vulnerabilities with CoreOS Clair Tool
[Clair](https://github.com/quay/clair) is an Open Source project from [CoreOS](http://coreos.com/), designed to scan Docker Images for Security Vulnerabilities.

In this step, you will learn how to deploy Clair and scan Docker Images from the Docker Hub and private registries for known Vulnerabilities.

This content is based on Katacoda, an online interactive lab available. Jump to this [https://www.katacoda.com/courses/docker-security/image-scanning-with-clair](https://www.katacoda.com/courses/docker-security/image-scanning-with-clair), login using either a social account (Google, LinkedIn, GitHub, or Twitter) or create a Katacoda account.

## Task 1 - Deploy Postgres
**Download Clair's Docker Compose File and Config**
Clair requires a Postgres instance for storing the CVE data and it's service that will scan Docker Images for vulnerabilities. This has been defined within a Docker Compose file. Download it with the command below:

```shell
curl -LO https://raw.githubusercontent.com/coreos/clair/05cbf328aa6b00a167124dbdbec229e348d97c04/contrib/compose/docker-compose.yml
```
The Clair configuration defines how Images should be scanned. Download it with:
```shell
mkdir clair_config && curl -L https://raw.githubusercontent.com/coreos/clair/master/config.yaml.sample -o clair_config/config.yaml
```
**Update Config**
Set the version of Clair to the last stable release and the default database password. sed 's/clair-git:latest/clair:v2.0.1/' -i docker-compose.yml && \
```shell
  sed 's/host=localhost/host=postgres password=password/' -i clair_config/config.yaml
```
**Start DB**
Start the database below.

```shell
docker-compose up -d postgres
```
In the next task we'll populate the DB

## Task 2 - Populate DB
Download and load the CVE details for Clair to use.

```shell
curl -LO https://gist.githubusercontent.com/BenHall/34ae4e6129d81f871e353c63b6a869a7/raw/5818fba954b0b00352d07771fabab6b9daba5510/clair.sql
docker run -it \
    -v $(pwd):/sql/ \
    --network "${USER}_default" \
    --link clair_postgres:clair_postgres \
    postgres:latest \
        bash -c "PGPASSWORD=password psql -h clair_postgres -U postgres < /sql/clair.sql"
```
Note: Clair would do this by default, but can take 10/15 minutes to download.
## Task 3 - Deploy Clair
With the DB populated, start the Clair service.

```shell
docker-compose up -d clair
```
We can now send it Docker Images to scan and return which vulnerabilities it contains.

## Task 4 - Scan Image
Clair works by accepting Image Layers via a HTTP API. To scan all the layers, we need an way to send each layer and aggregate the respond. [Klar](https://github.com/optiopay/klar) is a simple tool to analyze images stored in a private or public Docker registry for security vulnerabilities using Clair.

Download the latest release from Github. 
```shell
curl -L https://github.com/optiopay/klar/releases/download/v1.5/klar-1.5-linux-amd64 -o /usr/local/bin/klar && chmod +x $_
```
Using klar, we can now point it at images and see what vulnerabilities they contain, for example `quay.io/coreos/clair:v2.0.1`.

```shell
CLAIR_ADDR=http://localhost:6060 CLAIR_OUTPUT=Low CLAIR_THRESHOLD=10 \
klar quay.io/coreos/clair:v2.0.1
```  

The output can be tweaked, for example, only reporting High or Critical vulnerabilities. In this case, we return Low and Above.

## Task 5 - JSON Output
To help with automation, Klar can return the results as JSON. This allows developers plug the scanning into an automated process, such as CI/CD pipeline.

Start by installing `jq` to provide an easy way to view the JSON output 
```shell
pacman -Syy jq
```
By setting the `_JSONOUTPUT=true` parameter, the results will be in JSON which can be piped to another process, like `jq`.

```shell
CLAIR_ADDR=http://localhost:6060 CLAIR_OUTPUT=High CLAIR_THRESHOLD=10 JSON_OUTPUT=true klar postgres:latest | jq
```
## Task 6 - Scan Private Image
The previous step scanned images from the public Docker Registry, but the same can be done for private Docker Registries.

Start a simple registry using 
```shell
docker run -d --name registry -p 5000:5000 registry:2
```
This will be made available via the Katacoda Proxy. Tag an existing Docker Image with the newly started Registry URL.

```shell
docker tag postgres:latest 2886795326-5000-elsy06.environments.katacoda.com/postgres:latest
```
Once tagged, the Image can be pushed to the Registry.

```shell
docker push 2886795326-5000-elsy06.environments.katacoda.com/postgres:latest
```
In the same way, the private registry image can now be scanned.

```shell
CLAIR_ADDR=http://localhost:6060 \
  CLAIR_OUTPUT=Low CLAIR_THRESHOLD=10 \
  klar 2886795326-5000-elsy06.environments.katacoda.com/postgres:latest
```

# Step 4. Secure Private Registry with TLS/SSL
In this step, we'll cover how to launch a private Docker Registry with TLS via SSL.

A private Registry enables you to distribute Docker Images without being dependent on external providers or the public cloud. This allows you to increase security and confidence of your image sources and versioning.

This content is based on Katacoda, an online interactive lab available. Jump to this [https://www.katacoda.com/courses/docker-production/launch-private-registry](https://www.katacoda.com/courses/docker-production/launch-private-registry), login using either a social account (Google, LinkedIn, GitHub, or Twitter) or create a Katacoda account.

## Task 1 - Starting Registry
The Registry is deployed as a container and accessible via port 5000. Docker clients will use this domain to access the registry and push/pull images. By specifying a domain, a client can access multiple registries.

In this example our Docker registry is located at registry.test.training.katacoda.com.

```shell
docker run -d -p 5000:5000 \
    -v /root/certs:/certs \
    -e REGISTRY_HTTP_TLS_CERTIFICATE=/certs/registry.test.training.katacoda.com.crt \
    -e REGISTRY_HTTP_TLS_KEY=/certs/registry.test.training.katacoda.com.key \
    -v /opt/registry/data:/var/lib/registry \
    --name registry registry:2
```

Mounting the volume /var/lib/registry is important. This is where the Registry will store all of the pushed images. Mounting the directory will allow you to restart and upgrade the container in future.

## Task 2 - SSL
Securing access to the Registry via TLS is important. If the Registry is insecure, then you'll need to configure every Docker daemon accessing the Registry to allow access.

To secure the Registry, we'll use SSL certificates combined with NGINX to manage the SSL termination. We've added the certificate and key to the certs directory on the client.

```shell
ls /root/certs/
```

When creating the Registry container, the certs where mounted in with the correct environment variables set for the Registry to pickup the certificates.

```shell
-v /root/certs:/certs \
-e REGISTRY_HTTP_TLS_CERTIFICATE=/certs/registry.test.training.katacoda.com.crt \
-e REGISTRY_HTTP_TLS_KEY=/certs/registry.test.training.katacoda.com.key \
```

**Important**
These certificates are only for test purposes and not production use. Visit letsencrypt.org to obtain a certificate for your domain.

##Task 3 - Testing
We can test access to the registry using curl. The response should provide headers, for example Docker-Distribution-API-Version, indicating the request was processed by the Registry server.

```shell
curl -i https://registry.test.training.katacoda.com:5000/v2/
```
## Task 4 - Pushing Images
The Registry is now running.

We now need to push images to our new Registry. To push/pull images from non-default Registries we need to include the URL in the image name. Generally, an image follows a <name>:<tag> format as Docker defaults to the public registry. The full format is <registry-url>:<name>:<tag>.

We can use the docker tag to add additional tags to existing images. In this case the Redis image.

```shell
docker pull redis:alpine; docker tag redis:alpine registry.test.training.katacoda.com:5000/redis:alpine
```

Once tagged we can push the image. The different layers of the image will be pushed.

```shell
docker push registry.test.training.katacoda.com:5000/redis:alpine
```
## Task 5 - Pulling Images
First, remove images so they need to be pulled again

```shell
docker rmi redis:alpine && docker rmi registry.test.training.katacoda.com:5000/redis:alpine
```
As with push, pulling also includes the URL of our target Registry.

```shell
docker pull registry.test.training.katacoda.com:5000/redis:alpine
```