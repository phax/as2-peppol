#
# Copyright (C) 2014-2021 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM maven:3.6-jdk-8-alpine
WORKDIR /as2-peppol

# Parent POM
COPY pom.xml /as2-peppol/pom.xml

# Server POM
COPY as2-peppol-server/pom.xml /as2-peppol/server/pom.xml
COPY as2-peppol-server/src /as2-peppol/server/src
VOLUME [ "/var/www/peppol-as2/receive", "/var/www/peppol-as2/send", "/var/www/peppol-as2/data" ]
EXPOSE 8080

RUN apk update
RUN apk add openssl
RUN apk add openssh

# generating self signing certificate
# follow the [instruction](https://www.digitalocean.com/community/tutorials/openssl-essentials-working-with-ssl-certificates-private-keys-and-csrs)
# **IMPORTANT** 
#     * CN -- Common Name must follow PEPPOL AP name convention and be in format : APP_1000000XXX, for example APP_1000000312
#     * it uses PKCS12 keystore
#     * AS2 client uses predefined names for the following items:
#         - aliasname must match to the certificate CN (Common Name)
#         - key store password shall be 'peppol'
#         - keystore filename shall be 'test-client-certs.p12' to work with "as2-peppol-client"
WORKDIR /as2-peppol/server/src/main/resources/keystore
RUN openssl req \
    -new \
    -newkey rsa:2048 \
    -nodes \
    -subj "/emailAddress=as2-peppol-server@example.org/CN=APP_1000000312/O=IWA.fi/L=Helsinki/ST=Uusimaa/C=FI" \
    -keyout my-private.key \
    -out my-certificate.csr
RUN openssl x509 -signkey my-private.key -in my-certificate.csr -req -days 365 -out my-certificate.cer
RUN openssl pkcs12 -export -in my-certificate.cer -inkey my-private.key \
    -out ap.pilot.p12 -passout pass:peppol -name ap.pilot


# NOTE: in Maven Docker image default Maven repository location is configured as a volume 
# so anything copied there in a Dockerfile at build time is lost in /root/.m2/repository
# To preserve the project dependencies in the image we use special Maven settings
# for details see https://hub.docker.com/_/maven/

# building step 1
WORKDIR /as2-peppol
# Use "-N" to avoid submodules are build
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml install -DskipTests=true -N

# building step 2
WORKDIR /as2-peppol/server
# Use "-U" for SNAPSHOTs
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml package -DskipTests=true -U

RUN mvn jetty:deploy-war

#NOTE : please refer to jetty version in './pom.xml'
RUN ln -s /root/.m2/repository/org/eclipse/jetty/jetty-runner/9.4.31.v20200723/jetty-runner-9.4.31.v20200723.jar jetty-runner.jar

CMD ["java", "-jar", "jetty-runner.jar", "./target/as2-peppol-server.war"]
