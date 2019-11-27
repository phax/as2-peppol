# as2-peppol-client

[![Build Status](https://travis-ci.org/phax/as2-peppol-client.svg?branch=master)](https://travis-ci.org/phax/as2-peppol-client)
﻿
An example AS2 client to easily send AS2 messages to PEPPOL.
This AS2 client is based on my **[as2-lib](https://github.com/phax/as2-lib)** library.

When you are looking for a PEPPOL AS2 server component you may have a look at my **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** project.

Please have a look at the [PEPPOL practical AP guide](http://peppol.helger.com/public/?menuitem=docs-setup-ap)
for a detailed description on how it works and how it fits in the overall PEPPOL transport infrastructure.

This project is licensed under the Apache 2 License.

# Configuration

A keystore in the format JKS or PKCS12 must be available that must contain your Peppol AP certificate.

See https://github.com/phax/peppol-commons#peppol-smp-client for the list of configuration items of the Peppol SMP client.


# Sending a document

A test class is `src/test/java/.../MainAS2TestClient` and you need to adopt the variables according to your needs to make it work.

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


# Maven usage
Add the following to your `pom.xml` to use this artifact:

```xml
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-peppol-client</artifactId>
  <version>3.1.0</version>
</dependency>
```

The binary version of this library can be found on http://repo2.maven.org/maven2/com/helger/as2-peppol-client/ 
It depends on several other libraries so I suggest you are going for the Maven source integration.

# News and Noteworthy

* v3.1.0 - 2019-11-27
    * Added static helper methods in `AS2ClientBuilder`
    * Updated to peppol-commons 7.0.4
    * Added an extended AP certificate check (see [issue #6](https://github.com/phax/as2-peppol-client/issues/6))
    * Added possibility to send a previously created SBDH (see [issue #5](https://github.com/phax/as2-peppol-client/issues/5))
    * Improved reusability of existing default values/classes in `AS2ClientBuilder`  
* v3.0.11 - 2019-11-03
    * Updated to ph-bdve 5.1.14 with PEPPOL validation 3.9.0
* v3.0.10 - 2019-10-11
    * Updated to as2-lib 4.4.5
    * Made the usage of `DataHandler` the default, but made it customizable.
    * Made the outgoing MIME type customizable. 
    * Added possibility to customize incoming dumper on demand.
* v3.0.9 - 2019-09-26
    * Updated to as2-lib 4.4.4
* v3.0.8 - 2019-09-11
    * Updated to as2-lib 4.4.2
    * Updated to ph-bdve 5.1.12 with PEPPOL validation 3.8.1
* v3.0.7 - 2019-06-17
    * Updated to as2-lib 4.4.0
    * Updated to peppol-commons 7.0.0
    * New interface `IAS2ClientBuilderValidatonResultHandler` to customize validation result handling (see [issue #3](https://github.com/phax/as2-peppol-client/issues/3))
    * Added possibility to provide a read-only keystore from a byte array (see [issue #4](https://github.com/phax/as2-peppol-client/issues/4))
* v3.0.6 - 2019-05-17
    * Updated to as2-lib 4.3.0, thereby simplifying the Random topic
    * Added support for AS2 prefix "P" (as in "PDK" or "POP")
* v3.0.5 - 2019-05-16
    * Updated to ph-bdve 5.1.8 to support PEPPOL Spring Release 2019 validation artefacts
* v3.0.4 - 2019-03-22
    * Updated to as2-lib 4.2.2
* v3.0.3 - 2018-11-26
    * Requires ph-commons 9.2.0
    * Includes ph-bdve 5.1.0 with PEPPOL validation 3.7.0 
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

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
