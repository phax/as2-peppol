#as2-peppol-client

[![Build Status](https://travis-ci.org/phax/as2-peppol-client.svg?branch=master)](https://travis-ci.org/phax/as2-peppol-client)
﻿

An example AS2 client to easily send AS2 messages to PEPPOL.
This AS2 client is based on my **[as2-lib](https://github.com/phax/as2-lib)** library.

When you are looking for a PEPPOL AS2 server component you may have a look at my **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** project.

Please have a look at the [PEPPOL practical AP guide](http://peppol.helger.com/public/?menuitem=docs-setup-ap)
for a detailed description on how it works and how it fits in the overall PEPPOL transport infrastructure.

This project is licensed under the Apache 2 License.

#Configuration

A keystore in the format PKCS12 must be available.
By default it is expected to be called `client-certs.p12` residing in the `as2-client-data` folder. It must contain one certificate, namely your PEPPOL AP certificate.

To convert a JKS keystore to a PKCS12 keystore you can e.g. use [Portecle](http://portecle.sourceforge.net/) - a user friendly GUI application for creating, managing and examining keystores, keys, certificates, certificate requests, certificate revocation lists and more.

A test class is `MainAS2TestClient` and you need to adopt the constants on top to make it work.
  * **PKCS12_CERTSTORE_PATH** file path to the PKCS12 keystore
  * **PKCS12_CERTSTORE_PASSWORD** password to access the PKCS12 keystore
  * **SENDER_AS2_ID** your AS2-from ID. Must match the OpenPEPPOL requirements (AP certificate CN name - e.g. `APP_1000000001`).
  * **SENDER_EMAIL** your email address for out of band resolutions.
  * **SENDER_KEY_ALIAS** the alias name of your PEPPOL-AP-certificate within the PKCS12 keystore. This should be the same value as **SENDER_AS2_ID** 

#Sending a document

Before being ready to send a document, the recipient participant identifier as well as the test document must be selected. This happens currently directly in the `main` method and you have to choose the recipient you want.

The test files reside in the `src/main/resources/xml` folder and are referenced via classpath relative lookups.

## Usage of a proxy server

To use a proxy server, add the following items to the `smp-client.properties` configuration file (of course with adopted values): 
```
http.proxyHost=1.2.3.4
http.proxyPort=8080
https.proxyHost=1.2.3.4
https.proxyPort=8080
```

If you need a username and password for your proxy see [here](http://rolandtapken.de/blog/2012-04/java-process-httpproxyuser-and-httpproxypassword) for a guideline.

#Maven usage
Add the following to your `pom.xml` to use this artifact:
```
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-peppol-client</artifactId>
  <version>1.0.4</version>
</dependency>
```

The binary version of this library can be found on http://repo2.maven.org/maven2/com/helger/as2-peppol-client/ 
It depends on several other libraries so I suggest you are going for the Maven source integration.

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
