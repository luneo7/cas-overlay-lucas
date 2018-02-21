FROM azul/zulu-openjdk-alpine:8

MAINTAINER Lucas Rogerio Caetano Ferreira "luneo7@gmail.com"

ENV JAVA_OPTS=""
ENV PROG_OPTS=""
ENV CAS_USER CAS
ENV CAS_UID 8983

# Install prerequisites
RUN apk add --no-cache \
        lsof \
        gnupg \
        procps \
        tar \
        bash \
        ca-certificates \
        wget \
        unzip \
        git \
        curl \
        supervisor && \
    update-ca-certificates && \
    mkdir /etc/supervisord.d

RUN addgroup -S -g $CAS_UID $CAS_USER && \
adduser -S -u $CAS_UID -G $CAS_USER -g $CAS_USER $CAS_USER

# Download the CAS overlay project \
RUN cd / \
    && mkdir -p /etc/cas \
    && mkdir -p /etc/cas/services \
    && mkdir -p /etc/cas/config \
    && mkdir -p /etc/cas/saml \
    && mkdir -p /var/log/supervisord \
    && mkdir -p /var/log/cas \
    && mkdir -p /opt/cas \
    && chown -R $CAS_USER:$CAS_USER /etc/cas \
    && chown -R $CAS_USER:$CAS_USER /opt \
    && chown -R $CAS_USER:$CAS_USER /var/log/cas

COPY cas.war /opt/cas.war
RUN sh -c 'touch /opt/cas.war'

COPY etc/cas/cas.cer /etc/cas/
COPY etc/cas/truststore /etc/cas/
COPY etc/cas/thekeystore /etc/cas/
COPY etc/cas/config/ /etc/cas/config/
COPY etc/cas/services/ /etc/cas/services/

EXPOSE 8080 8443 8009

WORKDIR /opt/cas
USER $CAS_USER

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /opt/cas.war" ]
