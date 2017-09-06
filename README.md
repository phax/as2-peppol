# as2-peppol-client

[![Build Status](https://travis-ci.org/phax/as2-peppol-client.svg?branch=master)](https://travis-ci.org/phax/as2-peppol-client)
﻿

An example AS2 client to easily send AS2 messages to PEPPOL.
This AS2 client is based on my **[as2-lib](https://github.com/phax/as2-lib)** library.

When you are looking for a PEPPOL AS2 server component you may have a look at my **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** project.

Please have a look at the [PEPPOL practical AP guide](http://peppol.helger.com/public/?menuitem=docs-setup-ap)
for a detailed description on how it works and how it fits in the overall PEPPOL transport infrastructure.

This project is licensed under the Apache 2 License.

# News
  * v3.0.0 - work in progress
    * Updated to ph-commons 9.0.0
    * The `StandardBusinessDocument` send out, uses the default XML namespace prefix ("") instead of "sh"
    * Updated to BouncyCastle 1.58
  * v2.0.7 - 2017-07-27
    * Added possibility to customize `Content-Transfer-Encoding` used
    * Using ph-bdve 3.2.0 which improves validation artefact ClassLoader handling
  * v2.0.6 - 2017-06-19
    * Updated to ph-bdve 3.1.3 for XSLT validation fix
  * v2.0.5 - 2017-05-23
    * Added possibility to provide a custom namespace context
  * v2.0.4 - 2017-05-18
    * Updated to ph-bdve 3.1.0 (PEPPOL validation 3.4.0)
  * v2.0.3 - 2017-03-20
    * Improved customizability of `AS2ClientBuilder`
  * v2.0.2 - 2017-03-01
    * Using ph-bdve 3 for validation
  * v2.0.1 - 2017-01-16
    * Just a new release with no new features
    * Binds to ph-commons 8.6.x
  * v2.0.0 - 2016-08-22
    * Updated to JDK 8

# Configuration

A keystore in the format PKCS12 must be available.
By default it is expected to be called `client-certs.p12` residing in the `as2-client-data` folder. It must contain one certificate, namely your PEPPOL AP certificate.

To convert a JKS keystore to a PKCS12 keystore you can e.g. use [Portecle](http://portecle.sourceforge.net/) - a user friendly GUI application for creating, managing and examining keystores, keys, certificates, certificate requests, certificate revocation lists and more.

A test class is `MainAS2TestClient` and you need to adopt the constants on top to make it work.
  * **PKCS12_CERTSTORE_PATH** file path to the PKCS12 keystore
  * **PKCS12_CERTSTORE_PASSWORD** password to access the PKCS12 keystore
  * **SENDER_AS2_ID** your AS2-from ID. Must match the OpenPEPPOL requirements (AP certificate CN name - e.g. `APP_1000000001`).
  * **SENDER_EMAIL** your email address for out of band resolutions.
  * **SENDER_KEY_ALIAS** the alias name of your PEPPOL-AP-certificate within the PKCS12 keystore. This should be the same value as **SENDER_AS2_ID** 

# Sending a document

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

# Maven usage
Add the following to your `pom.xml` to use this artifact:
```xml
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-peppol-client</artifactId>
  <version>2.0.7</version>
</dependency>
```

The binary version of this library can be found on http://repo2.maven.org/maven2/com/helger/as2-peppol-client/ 
It depends on several other libraries so I suggest you are going for the Maven source integration.

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodeingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
