# as2-peppol

## Status migration AS2 &rarr; AS4

Peppol migrated to AS4 as the mandatory transport protocol as of February 1<sup>st</sup>, 2020.
The support of AS2 will be gracefully faded out.
Personally I wouldn't recommend to start new Peppol AS2 projects.

See **phase4** as an AS4 solution that can send and receive Peppol Messages: https://github.com/phax/phase4

# Introduction
﻿
This project contains an linkage between AS2 and Peppol.
It contains an AS2-Peppol client, a servlet for reuse to receive messages and an example web application.

This library is based on my **[as2-lib](https://github.com/phax/as2-lib)** and **[peppol-commons](https://github.com/phax/peppol-commons)** libraries.

## Maven usage

Add the following to your `pom.xml` to use this artifact, replacing `x.y.z` with the effective version:

```xml
<dependency>
  <groupId>com.helger.peppol</groupId>
  <artifactId>as2-peppol-client</artifactId>
  <version>x.y.z</version>
</dependency>
```

```xml
<dependency>
  <groupId>com.helger.peppol</groupId>
  <artifactId>as2-peppol-servlet</artifactId>
  <version>x.y.z</version>
</dependency>
```

Note: prior to v5.4.0 the Maven groupId was `com.helger`.

The binary versions of the artifacts can be found on http://repo2.maven.org/maven2/com/helger/peppol/ 
It depends on several other libraries so I suggest you are going for the Maven source integration.

## AS2 Peppol Client

AS2 client to easily send AS2 messages to Peppol.

When you are looking for a Peppol AS2 server component you may have a look at my **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** project.

Please have a look at the [Peppol practical AP guide](http://peppol.helger.com/public/?menuitem=docs-setup-ap)
for a detailed description on how it works and how it fits in the overall Peppol transport infrastructure.

This project is licensed under the Apache 2 License.

### Configuration

A keystore in the format JKS or PKCS12 must be available that must contain your Peppol AP certificate.

See https://github.com/phax/peppol-commons#peppol-smp-client for the list of configuration items of the Peppol SMP client.


### Sending a document

A test class is `src/test/java/.../MainAS2TestClient` and you need to adopt the variables according to your needs to make it work.

Before being ready to send a document, the recipient participant identifier as well as the test document must be selected. This happens currently directly in the `main` method and you have to choose the recipient you want.

The test files reside in the `src/main/resources/xml` folder and are referenced via classpath relative lookups.

### Usage of a proxy server

To use a proxy server, add the following items to the `smp-client.properties` configuration file (of course with adopted values):
 
```
http.proxyHost=1.2.3.4
http.proxyPort=8080
https.proxyHost=1.2.3.4
https.proxyPort=8080
```


## AS2 Peppol Servlet
﻿
A stand alone servlet that takes AS2 requests with Peppol StandardBusinessDocuments and handles them via SPI. This is not a self-contained package, but a good starting point for handling Peppol AS2 messages.

An example application that uses *as2-peppol-servlet* for receiving Peppol AS2 messages is my **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** project. It may serve as a practical starting point.

This package depends on **[ph-commons](https://github.com/phax/ph-commons)**, **[ph-sbdh](https://github.com/phax/ph-sbdh)**, **[as2-lib and as2-servlet](https://github.com/phax/as2-lib)**. This transitively includes Bouncy Castle (1.64) and javax.mail (1.6.4) among other libraries.

*as2-peppol-servlet* handles incoming AS2 messages, and parses them as OASIS Standard Business Documents (SBD). It does not contain extraction of the SBD content or even handling of the UBL content since the purpose of this project is reusability. For validating the SBD against Peppol rules, the project **[peppol-sbdh](https://github.com/phax/peppol-commons)** is available and for handling UBL 2.0 or 2.1 files you may have a look at my **[ph-ubl](https://github.com/phax/ph-ubl)**.

This project is licensed under the Apache 2 License.

### Usage
To use this project you have to do the following:
  1. Configure the AS2 servlet as specified in the [as2-servlet docs](https://github.com/phax/as2-lib)
  2. The key store must contain your Peppol AP certificate and the alias of the only entry must be the CN-value of your certificate's subject (e.g. `APP_1000000001`).
  3. Inside your project create an SPI implementation of the `com.helger.as2servlet.sbd.IAS2IncomingSBDHandlerSPI` interface to handling incoming SBD documents.


### SPI implementation

SPI stands for "Service provider interface" and is a Java standard feature to enable loose but typed coupling. [Read more on SPI](http://docs.oracle.com/javase/tutorial/ext/basics/spi.html)

A [dummy SPI implementation](https://github.com/phax/as2-peppol-servlet/blob/master/src/test/java/com/helger/peppol/as2servlet/mock/MockIncomingSBDHandler.java) is contained in the test code of this project. Additionally you need to create a file `META-INF/services/com.helger.peppol.as2servlet.IAS2IncomingSBDHandlerSPI` (in the `src/main/resources/` folder when using Maven) which contains a single line referencing the implementation class. An [example file](https://github.com/phax/as2-peppol-servlet/blob/master/src/test/resources/META-INF/services/com.helger.peppol.as2servlet.IAS2IncomingSBDHandlerSPI) is located in the test resources of this project.

### Known issues

* Peppol AS2 specifications requires that duplicate incoming message IDs are handled specially, by ignoring multiple transmissions of the same message ID

## AS2 Peppol Demo Server

A demo AS2 server to easily receive AS2 messages from Peppol.
This project is only meant as a demo project for illustrative purposes, on how to
implement a Peppol AS2 server. This server implementation writes all incoming documents to disc and does not do anything else with them! Additional or different logic must be implemented!

This AS2 server is based on my **[as2-lib](https://github.com/phax/as2-lib)** library, as well as on **as2-peppol-servlet**, **[peppol-commons](https://github.com/phax/peppol-commons)** and **[ph-ubl](https://github.com/phax/ph-ubl)**.
A client to send messages to this server is **as2-peppol-client** contained in here.

This project is licensed under the Apache 2 License.

### Project structure

This project is a Java 1.8+ web application and as such meant to be used in an application server (like Tomcat, Jetty etc.).

It consists of the following major components:
  * A specialized servlet for retrieval: `com.helger.peppol.as2server.servlet.PEPPOLAS2ReceiveServlet` which defines the configuration for processing incoming files. The servlet is referenced from `src/main/webapp/WEB-INF/web.xml`.
  * The main handler for an incoming SBDH document in class `com.helger.peppol.as2server.handler.AS2IncomingSBDHandler`. This class is referenced via the default SPI lookup mechanism (see `src/main/resources/META-INF/services/`).
  * Additionally a configuration file `src/main/resources/as2-server.properties` is provided that defines the AS2 specific setup. It contains the path to the keystore as well as in which folders to store what
  
### Before you start
Before this project can be run in a useful way a PKCS12 keystore with your Peppol AP certificate must be provided. By default the keystore must be located in `src/main/resources/keystore/ap.pilot.p12` and must have the password `peppol`. To change this edit the `as2-server.properties` file.

Btw. you need no database to run this server. If you want one just use one - but you don't need to. 

### Run it

There are two options:

**Running locally on your machine**

To do so you need to proceed the following steps:
    * get your machine ready for java development with JDK 1.8.X, maven, some IDE tool, e.g. [Eclipse](https://www.eclipse.org/downloads/)
    * you generate keystore with your certificate obtained from PEPPOP authority or self-signed (see procedure below)
    * build the project 
    * execute class `com.helger.peppol.as2server.jetty.RunInJettyPEPPOLAS2` from within your IDE (as a standard Java application). It starts up a minimal server and listens on port 8080. The servlet that receives Peppol messages listens to path `/as2/` and supports only HTTP method POST.

**Run already prepared application in Docker container**

To do so you have [Docker](https://docs.docker.com/get-started/) installed in your machine.
This approach is useful if you need just run the reference implementation against your Peppol Access Point implementation.

**Docker notes**

The prebuild Dockerfile builds the WAR and runs it in Jetty.

Build like this: `docker build -t as2-peppol-server .`

Run like this: `docker run -d --name as2-peppol-server -p 8888:8080 as2-peppol-server`

Locate your browser to `http://localhost:8888` to check if it is running.

Stop like this: `docker stop as2-peppol-server`

And remove like this: `docker rm as2-peppol-server`

### Test it

Now that the AS2 server is running you may have a closer look at **as2-peppol-client** project which lets you send AS2 messages to a server.
If both client and server are configured correctly a successful message exchange should be easily possible.

### Notes

**Using Self-signed certificate**

* follow the [instruction](https://www.digitalocean.com/community/tutorials/openssl-essentials-working-with-ssl-certificates-private-keys-and-csrs)

**IMPORTANT** 

    * CN -- Common Name must follow Peppol AP name convention and be in format : APP_1000000XXX, for example APP_1000000312
    * it uses PKCS12 keystore
    * AS2 client uses predefined names for the following items:
        - aliasname must match to the certificate CN (Common Name)
        - key store password shall be 'peppol'
        - keystore filename shall be 'test-client-certs.p12' to work with *as2-peppol-client*

For example:
```shell
    $ openssl req -out teho3-certificate.csr -new -newkey rsa:2048 -nodes -keyout teho3-private.key
    $ openssl x509 -signkey teho3-private.key -in teho3-certificate.csr -req -days 365 -out teho3.cer
    $ openssl pkcs12 -export -in teho3.cer -inkey teho3-private.key -out test-client-certs.p12 -passout pass:peppol -name APP_1000000112
```


# News and Noteworthy

* v5.4.0 - 2020-05-27
    * Merged `as2-peppol-client`, `as2-peppol-servlet` and `as2-peppol-server` into a single repository
        * The history of `as2-peppol-servlet` can be found at https://github.com/phax/as2-peppol-servlet#news-and-noteworthy
* v3.3.2 - 2020-03-12
    * Added a possibility to retrieve the byte array representation of the created SBDH (see [issue #7](https://github.com/phax/as2-peppol/issues/7))
* v3.3.1 - 2020-02-17
    * Updated to ph-web 9.1.9
* v3.3.0 - 2020-02-07
    * Updated to peppol-commons 8.x
    * Changed the priority of AS2 specifications so that Peppol AS2 v2 is preferred over Peppol AS2 v1.
* v3.2.1 - 2020-01-19
    * Updated to peppol-commons 7.0.6
    * Made the used SMP transport profile customizable via `AS2ClientBuilder.setSMPTransportProfiles`
    * By default both Peppol AS2 V1 and V2 are now handled in the SMP lookup (v2 as the fallback)
    * Using type `ISMPServiceMetadataProvider` instead of `SMPClient` for improved configurability
* v3.2.0 - 2019-12-19
    * Updated to as2-lib 4.5.0
* v3.1.0 - 2019-11-27
    * Added static helper methods in `AS2ClientBuilder`
    * Updated to peppol-commons 7.0.4
    * Added an extended AP certificate check (see [issue #6](https://github.com/phax/as2-peppol/issues/6))
    * Added possibility to send a previously created SBDH (see [issue #5](https://github.com/phax/as2-peppol/issues/5))
    * Improved reusability of existing default values/classes in `AS2ClientBuilder`  
* v3.0.11 - 2019-11-03
    * Updated to ph-bdve 5.1.14 with Peppol validation 3.9.0
* v3.0.10 - 2019-10-11
    * Updated to as2-lib 4.4.5
    * Made the usage of `DataHandler` the default, but made it customizable.
    * Made the outgoing MIME type customizable. 
    * Added possibility to customize incoming dumper on demand.
* v3.0.9 - 2019-09-26
    * Updated to as2-lib 4.4.4
* v3.0.8 - 2019-09-11
    * Updated to as2-lib 4.4.2
    * Updated to ph-bdve 5.1.12 with Peppol validation 3.8.1
* v3.0.7 - 2019-06-17
    * Updated to as2-lib 4.4.0
    * Updated to peppol-commons 7.0.0
    * New interface `IAS2ClientBuilderValidatonResultHandler` to customize validation result handling (see [issue #3](https://github.com/phax/as2-peppol/issues/3))
    * Added possibility to provide a read-only keystore from a byte array (see [issue #4](https://github.com/phax/as2-peppol/issues/4))
* v3.0.6 - 2019-05-17
    * Updated to as2-lib 4.3.0, thereby simplifying the Random topic
    * Added support for AS2 prefix "P" (as in "PDK" or "POP")
* v3.0.5 - 2019-05-16
    * Updated to ph-bdve 5.1.8 to support Peppol Spring Release 2019 validation artefacts
* v3.0.4 - 2019-03-22
    * Updated to as2-lib 4.2.2
* v3.0.3 - 2018-11-26
    * Requires ph-commons 9.2.0
    * Includes ph-bdve 5.1.0 with Peppol validation 3.7.0 
* v3.0.2 - 2018-06-28
    * Updated to ph-commons 9.1.2
    * Updated to as2-lib 4.1.0
    * Updated ph-bdve to 5.0
    * Removed mandatory dependency to ph-bdve-simplerinvoicing
* v3.0.1 - 2018-04-05
    * Updated to as2-lib 4.0.2 fixing usage of BC PKCS 12 keystore instead of JDK PKCS 12 keystore
    * Exceptions in MDN processing are now propagated to the outside world
* v3.0.0 - 2018-02-12
    * Updated to ph-commons 9.0.1
    * The `StandardBusinessDocument` send out, uses the default XML namespace prefix ("") instead of "sh"
    * Updated to BouncyCastle 1.59
    * Arbitrary key store types can now be used for sending (no more limitation to PKCS12)
* v2.0.7 - 2017-07-27
    * Added possibility to customize `Content-Transfer-Encoding` used
    * Using ph-bdve 3.2.0 which improves validation artefact ClassLoader handling
* v2.0.6 - 2017-06-19
    * Updated to ph-bdve 3.1.3 for XSLT validation fix
* v2.0.5 - 2017-05-23
    * Added possibility to provide a custom namespace context
* v2.0.4 - 2017-05-18
    * Updated to ph-bdve 3.1.0 (Peppol validation 3.4.0)
* v2.0.3 - 2017-03-20
    * Improved customizability of `AS2ClientBuilder`
* v2.0.2 - 2017-03-01
    * Using ph-bdve 3 for validation
* v2.0.1 - 2017-01-16
    * Just a new release with no new features
    * Binds to ph-commons 8.6.x
* v2.0.0 - 2016-08-22
    * Updated to JDK 8

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a> |
Kindly supported by [YourKit Java Profiler](https://www.yourkit.com)