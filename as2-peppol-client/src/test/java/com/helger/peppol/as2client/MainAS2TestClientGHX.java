/**
 * Copyright (C) 2014-2021 Philip Helger (www.helger.com)
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
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.bdve.peppol.PeppolValidation3_10_1;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.smpclient.config.SMPClientConfiguration;
import com.helger.smpclient.peppol.SMPClientReadOnly;

/**
 * Main class to send AS2 messages.
 *
 * @author Philip Helger
 */
public final class MainAS2TestClientGHX
{
  /** The file path to the PKCS12 key store */
  private static final String PKCS12_CERTSTORE_PATH = "as2-client-data/test-ap.p12";
  /** The password to open the PKCS12 key store */
  private static final String PKCS12_CERTSTORE_PASSWORD = "peppol";
  /** Your AS2 sender ID */
  private static final String SENDER_AS2_ID = "APP_1000000004";
  /** Your AS2 sender email address */
  private static final String SENDER_EMAIL = "peppol@example.org";
  /** Your AS2 key alias in the PKCS12 key store */
  private static final String SENDER_KEY_ALIAS = "openpeppol aisbl id von pop000306";
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  /** The PEPPOL sender participant ID */
  private static final IParticipantIdentifier SENDER_PEPPOL_ID = IF.createParticipantIdentifierWithDefaultScheme ("9999:test-sender");
  /** The PEPPOL document type to use. */
  private static final IDocumentTypeIdentifier DOCTYPE = IF.createDocumentTypeIdentifierWithDefaultScheme ("abc");
  /** The PEPPOL process to use. */
  private static final IProcessIdentifier PROCESS = IF.createProcessIdentifierWithDefaultScheme ("123");

  private static final Logger LOGGER = LoggerFactory.getLogger (MainAS2TestClientGHX.class);

  static
  {
    // Set Proxy Settings from property file.
    SMPClientConfiguration.applyAllNetworkSystemProperties ();

    // Enable or disable debug mode
    GlobalDebug.setDebugModeDirect (false);
  }

  public static void main (final String [] args) throws Exception
  {
    String sReceiverID = null;
    String sReceiverKeyAlias = null;
    String sReceiverAddress = null;

    // localhost test endpoint
    final IParticipantIdentifier aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
    final String sTestFilename = "xml/as2-test-at-gov.xml";

    if (true)
    {
      // Avoid SMP lookup
      sReceiverAddress = "http://localhost:8080/as2";
      sReceiverID = "APP_1000000004";
      sReceiverKeyAlias = "APP_1000000004";
    }

    final AS2ClientResponse aResponse = new AS2ClientBuilder ().setSMPClient (new SMPClientReadOnly (URI.create ("http://127.0.0.1")))
                                                               .setKeyStore (EKeyStoreType.PKCS12,
                                                                             new File (PKCS12_CERTSTORE_PATH),
                                                                             PKCS12_CERTSTORE_PASSWORD)
                                                               .setSenderAS2ID (SENDER_AS2_ID)
                                                               .setSenderAS2Email (SENDER_EMAIL)
                                                               .setSenderAS2KeyAlias (SENDER_KEY_ALIAS)
                                                               .setReceiverAS2ID (sReceiverID)
                                                               .setReceiverAS2KeyAlias (sReceiverKeyAlias)
                                                               .setReceiverAS2Url (sReceiverAddress)
                                                               .setAS2SigningAlgorithm (ECryptoAlgorithmSign.DIGEST_SHA_256)
                                                               .setBusinessDocument (new ClassPathResource (sTestFilename))
                                                               .setPeppolSenderID (SENDER_PEPPOL_ID)
                                                               .setPeppolReceiverID (aReceiver)
                                                               .setPeppolDocumentTypeID (DOCTYPE)
                                                               .setPeppolProcessID (PROCESS)
                                                               .setValidationKey (PeppolValidation3_10_1.VID_OPENPEPPOL_INVOICE_V3)
                                                               .sendSynchronous ();
    if (aResponse.hasException ())
      LOGGER.warn (aResponse.getAsString ());

    LOGGER.info ("Done");
  }
}
