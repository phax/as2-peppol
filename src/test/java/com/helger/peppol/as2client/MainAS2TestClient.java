/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.as2client;

import java.io.File;
import java.security.Security;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.process.EPredefinedProcessIdentifier;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpclient.SMPClientConfiguration;
import com.helger.peppol.smpclient.SMPClientReadOnly;

/**
 * Main class to send AS2 messages.
 *
 * @author Philip Helger
 */
public final class MainAS2TestClient
{
  /** The file path to the PKCS12 key store */
  private static final String PKCS12_CERTSTORE_PATH = "as2-client-data/client-certs.p12";
  /** The password to open the PKCS12 key store */
  private static final String PKCS12_CERTSTORE_PASSWORD = "peppol";
  /** Your AS2 sender ID */
  private static final String SENDER_AS2_ID = "APP_1000000004";
  /** Your AS2 sender email address */
  private static final String SENDER_EMAIL = "peppol@example.org";
  /** Your AS2 key alias in the PKCS12 key store */
  private static final String SENDER_KEY_ALIAS = "APP_1000000004";
  /** The PEPPOL sender participant ID */
  private static final SimpleParticipantIdentifier SENDER_PEPPOL_ID = SimpleParticipantIdentifier.createWithDefaultScheme ("9999:test-sender");
  /** The PEPPOL document type to use. */
  private static final SimpleDocumentTypeIdentifier DOCTYPE = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
  /** The PEPPOL process to use. */
  private static final SimpleProcessIdentifier PROCESS = EPredefinedProcessIdentifier.BIS4A_V20.getAsProcessIdentifier ();
  /** The PEPPOL transport profile to use */
  private static final ISMPTransportProfile TRANSPORT_PROFILE = ESMPTransportProfile.TRANSPORT_PROFILE_AS2;

  private static final Logger s_aLogger = LoggerFactory.getLogger (MainAS2TestClient.class);

  static
  {
    // Set Proxy Settings from property file.
    SMPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();

    // Must be first!
    Security.addProvider (new BouncyCastleProvider ());

    // Enable or disable debug mode
    GlobalDebug.setDebugModeDirect (false);
  }

  @SuppressWarnings ("null")
  public static void main (final String [] args) throws Exception
  {
    IParticipantIdentifier aReceiver = null;
    String sTestFilename = null;
    String sReceiverID = null;
    String sReceiverKeyAlias = null;
    String sReceiverAddress = null;
    X509Certificate aReceiverCertificate = null;

    if (false)
    {
      // mysupply test client
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:5798009883995");
      sTestFilename = "xml/as2-mysupply_TEST_NO.xml";
    }
    if (false)
    {
      // DIFI test endpoint
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9908:810418052");
      sTestFilename = "xml/as2-pagero.xml";
    }
    if (false)
    {
      // Pagero test participant 9908:222222222
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9908:222222222");
      sTestFilename = "xml/as2-pagero.xml";
    }
    if (false)
    {
      // Unit4 test participant 9908:810017902
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9908:810017902");
      sTestFilename = "xml/as2-pagero.xml";
    }
    if (false)
    {
      // Unit4 debug test endpoint
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9908:810017902");
      sTestFilename = "xml/as2-pagero.xml";
      // Override since not in SMP
      sReceiverAddress = "https://ap-test.unit4.com/oxalis/as2";
    }
    if (false)
    {
      // DERWID test endpoint
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9914:atu66313919");
      sTestFilename = "xml/as2-test-at-gov.xml";
    }
    if (true)
    {
      // BRZ test endpoint
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test");
      sTestFilename = "xml/as2-test-at-gov.xml";
    }
    if (false)
    {
      // localhost test endpoint
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test");
      sTestFilename = "xml/as2-test-at-gov.xml";
      // Avoid SMP lookup
      sReceiverAddress = "http://localhost:8080/as2";
      sReceiverID = "APP_1000000004";
      sReceiverKeyAlias = "APP_1000000004";
    }

    if (sReceiverAddress == null || sReceiverID == null)
    {
      // Fallback
      if (aReceiver == null)
        throw new IllegalStateException ("Receiver ID must be present!");

      s_aLogger.info ("SMP lookup for " + aReceiver.getValue ());

      // Query SMP
      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aReceiver, ESML.DIGIT_PRODUCTION);
      final EndpointType aEndpoint = aSMPClient.getEndpoint (aReceiver, DOCTYPE, PROCESS, TRANSPORT_PROFILE);
      if (aEndpoint == null)
        throw new NullPointerException ("Failed to resolve endpoint for docType/process");

      // Extract from SMP response
      if (sReceiverAddress == null)
        sReceiverAddress = SMPClientReadOnly.getEndpointAddress (aEndpoint);
      if (aReceiverCertificate == null)
        aReceiverCertificate = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
      if (sReceiverID == null)
        sReceiverID = AS2ClientHelper.getSubjectCommonName (aReceiverCertificate);

      // SMP lookup done
      s_aLogger.info ("Receiver URL: " + sReceiverAddress);
      s_aLogger.info ("Receiver DN:  " + sReceiverID);
    }

    if (sReceiverKeyAlias == null)
    {
      // No key alias is specified, so use the same as the receiver ID
      sReceiverKeyAlias = sReceiverID;
    }

    final AS2ClientResponse aResponse = new AS2ClientBuilder ().setPKCS12KeyStore (new File (PKCS12_CERTSTORE_PATH),
                                                                                   PKCS12_CERTSTORE_PASSWORD)
                                                               .setSenderAS2ID (SENDER_AS2_ID)
                                                               .setSenderAS2Email (SENDER_EMAIL)
                                                               .setSenderAS2KeyAlias (SENDER_KEY_ALIAS)
                                                               .setReceiverAS2ID (sReceiverID)
                                                               .setReceiverAS2KeyAlias (sReceiverKeyAlias)
                                                               .setReceiverAS2Url (sReceiverAddress)
                                                               .setReceiverCertificate (aReceiverCertificate)
                                                               .setAS2SigningAlgorithm (ECryptoAlgorithmSign.DIGEST_SHA_256)
                                                               .setBusinessDocument (new ClassPathResource (sTestFilename))
                                                               .setPeppolSenderID (SENDER_PEPPOL_ID)
                                                               .setPeppolReceiverID (aReceiver)
                                                               .setPeppolDocumentTypeID (DOCTYPE)
                                                               .setPeppolProcessID (PROCESS)
                                                               .sendSynchronous ();
    if (aResponse.hasException ())
      s_aLogger.warn (aResponse.getAsString ());

    s_aLogger.info ("Done");
  }
}
