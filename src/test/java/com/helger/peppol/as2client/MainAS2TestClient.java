/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import org.apache.http.HttpHost;
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
import com.helger.peppol.smpclient.SMPClientConfiguration;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.validation.ValidationLayerResult;
import com.helger.peppol.validation.domain.ValidationKey;
import com.helger.peppol.validation.domain.peppol.PeppolValidationKeys;

/**
 * Main class to send AS2 messages.
 *
 * @author Philip Helger
 */
public class MainAS2TestClient
{
  /** The file path to the PKCS12 key store */
  private static final String PKCS12_CERTSTORE_PATH = "as2-client-data/client-certs.p12";
  /** The password to open the PKCS12 key store */
  private static final String PKCS12_CERTSTORE_PASSWORD = "peppol";
  /** Your AS2 sender ID */
  private static final String SENDER_AS2_ID = "APP_1000000101";
  /** Your AS2 sender email address */
  private static final String SENDER_EMAIL = "peppol@example.org";
  /** Your AS2 key alias in the PKCS12 key store */
  private static final String SENDER_KEY_ALIAS = SENDER_AS2_ID;
  /** The PEPPOL sender participant ID */
  private static final SimpleParticipantIdentifier SENDER_PEPPOL_ID = SimpleParticipantIdentifier.createWithDefaultScheme ("9999:test-sender");

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

  public static void main (final String [] args) throws Exception
  {
    /** The PEPPOL document type to use. */
    SimpleDocumentTypeIdentifier aDocTypeID = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
    /** The PEPPOL process to use. */
    SimpleProcessIdentifier aProcessID = EPredefinedProcessIdentifier.BIS4A_V20.getAsProcessIdentifier ();
    IParticipantIdentifier aReceiver = null;
    String sTestFilename = null;
    String sReceiverID = null;
    String sReceiverKeyAlias = null;
    String sReceiverAddress = null;
    ESML eSML = ESML.DIGIT_PRODUCTION;
    final ValidationKey aValidationKey = PeppolValidationKeys.INVOICE_04_T10;

    HttpHost aProxy = null;
    final String sProxyHost = SMPClientConfiguration.getConfigFile ().getString ("http.proxyHost");
    final int nProxyPort = SMPClientConfiguration.getConfigFile ().getInt ("http.proxyPort", 0);
    if (sProxyHost != null && nProxyPort > 0)
      aProxy = new HttpHost (sProxyHost, nProxyPort);

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
    if (false)
    {
      // TESISQUARE test endpoint
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:0000000000003");
      sTestFilename = "xml/as2-tesisquare_test_file_noheader.xml";
    }
    if (false)
    {
      // CONSIP test endpoint
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9907:consiptestap2");
      aDocTypeID = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Order-2::Order##urn:www.cenbii.eu:transaction:biitrns001:ver2.0:extended:urn:www.peppol.eu:bis:peppol3a:ver2.0::2.1");
      aProcessID = SimpleProcessIdentifier.createWithDefaultScheme ("urn:www.cenbii.eu:profile:bii03:ver2.0");
      eSML = ESML.DIGIT_TEST;
      sTestFilename = "xml/as2-order.xml";
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
      sReceiverID = SENDER_AS2_ID;
      sReceiverKeyAlias = SENDER_KEY_ALIAS;
    }

    final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aReceiver, eSML).setProxy (aProxy);
    try
    {
      final AS2ClientResponse aResponse = new AS2ClientBuilder ().setSMPClient (aSMPClient)
                                                                 .setPKCS12KeyStore (new File (PKCS12_CERTSTORE_PATH),
                                                                                     PKCS12_CERTSTORE_PASSWORD)
                                                                 .setSaveKeyStoreChangesToFile (false)
                                                                 .setSenderAS2ID (SENDER_AS2_ID)
                                                                 .setSenderAS2Email (SENDER_EMAIL)
                                                                 .setSenderAS2KeyAlias (SENDER_KEY_ALIAS)
                                                                 .setReceiverAS2ID (sReceiverID)
                                                                 .setReceiverAS2KeyAlias (sReceiverKeyAlias)
                                                                 .setReceiverAS2Url (sReceiverAddress)
                                                                 .setAS2SigningAlgorithm (ECryptoAlgorithmSign.DIGEST_SHA1)
                                                                 .setBusinessDocument (new ClassPathResource (sTestFilename))
                                                                 .setPeppolSenderID (SENDER_PEPPOL_ID)
                                                                 .setPeppolReceiverID (aReceiver)
                                                                 .setPeppolDocumentTypeID (aDocTypeID)
                                                                 .setPeppolProcessID (aProcessID)
                                                                 .setValidationKey (aValidationKey)
                                                                 .sendSynchronous ();
      if (aResponse.hasException ())
        s_aLogger.warn (aResponse.getAsString ());

      s_aLogger.info ("Done");
    }
    catch (final AS2ClientBuilderValidationException ex)
    {
      for (final ValidationLayerResult aVLR : ex.getValidationResult ())
        if (aVLR.isFailure ())
          s_aLogger.error (aVLR.toString ());
        else
          s_aLogger.info (aVLR.toString ());
    }
    catch (final AS2ClientBuilderException ex)
    {
      ex.printStackTrace ();
    }
  }
}
