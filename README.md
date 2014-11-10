#as2-peppol-client

An example AS2 client to easily send AS2 messages to PEPPOL.
This AS2 client is based on my [as2-lib](https://github.com/phax/as2-lib) library.

#Configuration

A keystore in the format PKCS12 must be available.
By default it is expected to be called `client-certs.p12` residing in the `as2-client-data` folder. It must contain one certificate, namely your PEPPOL AP certificate.
To convert a JKS keystore to a PKCS12 keystore you can e.g. use [Portecle](http://portecle.sourceforge.net/) - a user friendly GUI application for creating, managing and examining keystores, keys, certificates, certificate requests, certificate revocation lists and more.

The main class is `MainAS2TestClient` and you need to adopt the constants on top to make it work.
  * **PKCS12_CERTSTORE_PATH** file path to the PKCS12 keystore
  * **PKCS12_CERTSTORE_PASSWORD** password to access the PKCS12 keystore
  * **SENDER_AS2_ID** your AS2-from ID. Must match the OpenPEPPOL requirements (AP certificate CN name - e.g. `APP_1000000001`).
  * **SENDER_EMAIL** your email address for out of band resolutions.
  * **SENDER_KEY_ALIAS** the alias name of your PEPPOL-AP-certificate within the PKCS12 keystore. Ideally this is the same value as **SENDER_AS2_ID** 


---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
