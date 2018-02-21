CAS
============================

Customization of CAS overlay for use of ICP-Brazil digital certificate, with SAML and OIDC configured and working.
Original Project [here](https://apereo.github.io/cas/5.2.x/index.html).

# Requirements
* JDK 1.8+

# Configuration

The `etc` directory contains the configuration files and should be copied to `/etc/cas/config`.

# Compiling

To see the commands available for the build script:

```bash
./build.sh help
```

To package the final application:

```bash
./build.sh package
```

# Deploy

- Create a keystore file `thekeystore` in `/etc/cas`. Use `changeit` password for both the keystore and the key/certificate entries.
- Make sure that the keystore is loaded with server keys and certificates.

In a successful deploy cas will be available in:

* `http://cas.server.name:8080/cas`
* `https://cas.server.name:8443/cas`

Or

* `http://127.0.0.1:8080/cas`
* `https://127.0.0.1:8443/cas`



With the default configuration the login/password is a static list with the following credential, username casuser and password Mellon, so you might want to add a LDAP auth or another method and configure it in the cas.properties file.



## Executable WAR

To run WAR as an application run the following commands.

```bash
./mvnw clean package
java -jar target/cas.war
```

Or with the following script

```bash
./build.sh run
```


## Spring Boot

Run WAR with Spring Boot.

```bash
./build.sh bootrun
```

## Docker

Adjust DOCKER_HOST variable for image publishing
```bash
export DOCKER_HOST=tcp://<host>:2375
```
Or for local
```bash
export DOCKER_HOST=unix:///var/run/docker.sock
```
Build
```bash
./mvnw clean package docker:build
```
Run
```bash
docker run --name=cas --net=host -t lucasferreira/cas
```

## External deploy

Deploy the resulting `target/cas.war` file to a servlet container of your choice, but before compiling blank out app.server parameter in the pom.xml file.
