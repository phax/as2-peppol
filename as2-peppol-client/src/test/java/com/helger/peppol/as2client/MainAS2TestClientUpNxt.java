/**
 * Copyright (C) 2014-2020 Philip Helger (www.helger.com)
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
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.util.dump.HTTPOutgoingDumperStreamBased;
import com.helger.as2lib.util.dump.IHTTPOutgoingDumper;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.bdve.api.executorset.VESID;
import com.helger.bdve.api.executorset.ValidationExecutorSetRegistry;
import com.helger.bdve.api.result.ValidationResult;
import com.helger.bdve.engine.source.IValidationSourceXML;
import com.helger.bdve.simplerinvoicing.SimplerInvoicingValidation;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.error.IError;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.IKeyStoreType;
import com.helger.smpclient.config.SMPClientConfiguration;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.url.IPeppolURLProvider;
import com.helger.smpclient.url.PeppolURLProvider;

/**
 * Main class to send AS2 messages.
 *
 * @author Philip Helger
 */
public final class MainAS2TestClientUpNxt
{
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  private static final IPeppolURLProvider URL_PROVIDER = PeppolURLProvider.INSTANCE;
  /** The PEPPOL sender participant ID */
  private static final IParticipantIdentifier SENDER_PEPPOL_ID = IF.createParticipantIdentifierWithDefaultScheme ("9999:test-sender");
  /** Your AS2 sender email address */
  private static final String SENDER_EMAIL = "peppol@example.org";

  private static final Logger LOGGER = LoggerFactory.getLogger (MainAS2TestClientUpNxt.class);

  static
  {
    // Set Proxy Settings from property file.
    SMPClientConfiguration.applyAllNetworkSystemProperties ();

    // Enable or disable debug mode
    GlobalDebug.setDebugModeDirect (false);
  }

  @Nonnull
  private static IKeyStoreType _getKeyStoreType ()
  {
    return EKeyStoreType.PKCS12;
  }

  /** The file path to the PKCS12 key store */
  private static String _getKeyStorePath ()
  {
    return "as2-client-data/client-certs.p12";
  }

  /** The password to open the PKCS12 key store */
  private static String _getKeyStorePassword ()
  {
    return "peppol";
  }

  /** Your AS2 sender ID */
  private static final String _getSenderAS2ID ()
  {
    return "PDK000270";
  }

  /** Your AS2 key alias in the PKCS12 key store */
  private static final String _getSenderKeyAlias ()
  {
    return _getSenderAS2ID ();
  }

  @SuppressWarnings ({ "null", "resource" })
  public static void main (final String [] args) throws Exception
  {
    /** The PEPPOL document type to use. */
    final IDocumentTypeIdentifier aDocTypeID = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
    /** The PEPPOL process to use. */
    final IProcessIdentifier aProcessID = EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ();
    final IParticipantIdentifier aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
    final IReadableResource aTestResource = new ClassPathResource ("xml/si/a1_extended.xml");
    final String sReceiverID = _getSenderAS2ID ();
    final String sReceiverKeyAlias = _getSenderKeyAlias ();
    // Avoid SMP lookup
    final String sReceiverAddress = "http://localhost:8080/as2";
    final ISMLInfo aSML = ESML.DIGIT_TEST;
    final VESID aValidationKey = SimplerInvoicingValidation.VID_SI_INVOICE_V11;
    final ECryptoAlgorithmSign eMICAlg = ECryptoAlgorithmSign.DIGEST_SHA_1;
    final HttpHost aProxy = SMPClientConfiguration.getHttpProxy ();
    final int nConnectTimeoutMS = AS2ClientSettings.DEFAULT_CONNECT_TIMEOUT_MS;
    final int nReadTimeoutMS = AS2ClientSettings.DEFAULT_READ_TIMEOUT_MS;

    // Debug outgoing (AS2 message)?
    NonBlockingByteArrayOutputStream aDebugOS;
    IHTTPOutgoingDumper aOutgoingDumper = null;
    if (true)
    {
      aDebugOS = new NonBlockingByteArrayOutputStream ();
      aOutgoingDumper = new HTTPOutgoingDumperStreamBased (aDebugOS).setDumpComment (false);
    }
    else
      aDebugOS = null;

    // Debug incoming (AS2 MDN)?
    if (false)
      HTTPHelper.setHTTPIncomingDumperFactory ( () -> (aHeaderLines, aPayload, aMsg) -> {
        LOGGER.info ("Received Headers:\n" + StringHelper.getImploded ("\n  ", aHeaderLines));
        LOGGER.info ("Received Payload:\n" + new String (aPayload, StandardCharsets.UTF_8));
        LOGGER.info ("Received Message:\n" + aMsg);
      });

    final URI aSMPURI = URL_PROVIDER.getSMPURIOfParticipant (aReceiver, aSML);
    final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aSMPURI);

    // No proxy for local host
    if (!aSMPClient.getSMPHostURI ().startsWith ("http://localhost") && !aSMPClient.getSMPHostURI ().startsWith ("http://127."))
    {
      aSMPClient.httpClientSettings ().setProxyHost (aProxy);
    }

    try
    {
      final AS2ClientResponse aResponse = new AS2ClientBuilder ()
      {
        @Override
        @OverrideOnDemand
        @Nonnull
        protected ValidationExecutorSetRegistry <IValidationSourceXML> createValidationRegistry ()
        {
          final ValidationExecutorSetRegistry <IValidationSourceXML> aVESRegistry = super.createValidationRegistry ();
          SimplerInvoicingValidation.initSimplerInvoicing (aVESRegistry);
          return aVESRegistry;
        }
      }.setSMPClient (aSMPClient)
       .setKeyStore (_getKeyStoreType (), new File (_getKeyStorePath ()), _getKeyStorePassword ())
       .setSaveKeyStoreChangesToFile (false)
       .setSenderAS2ID (_getSenderAS2ID ())
       .setSenderAS2Email (SENDER_EMAIL)
       .setSenderAS2KeyAlias (_getSenderKeyAlias ())
       .setReceiverAS2ID (sReceiverID)
       .setReceiverAS2KeyAlias (sReceiverKeyAlias)
       .setReceiverAS2Url (sReceiverAddress)
       .setAS2SigningAlgorithm (eMICAlg)
       .setConnectTimeoutMS (nConnectTimeoutMS)
       .setReadTimeoutMS (nReadTimeoutMS)
       .setBusinessDocument (aTestResource)
       .setPeppolSenderID (SENDER_PEPPOL_ID)
       .setPeppolReceiverID (aReceiver)
       .setPeppolDocumentTypeID (aDocTypeID)
       .setPeppolProcessID (aProcessID)
       .setValidationKey (aValidationKey)
       .setOutgoingDumper (aOutgoingDumper)
       .sendSynchronous ();
      if (aResponse.hasException ())
        LOGGER.warn (aResponse.getAsString ());

      if (aDebugOS != null)
        LOGGER.info ("Outgoing request:\n" + aDebugOS.getAsString (StandardCharsets.UTF_8));

      LOGGER.info ("Done");
    }
    catch (final AS2ClientBuilderValidationException ex)
    {
      for (final ValidationResult aVR : ex.getValidationResult ())
        for (final IError aError : aVR.getErrorList ())
          if (aError.isError ())
            LOGGER.error (aError.getAsString (Locale.US));
    }
    catch (final AS2ClientBuilderException ex)
    {
      ex.printStackTrace ();
    }
    finally
    {
      StreamHelper.close (aOutgoingDumper);
      StreamHelper.close (aDebugOS);
    }
  }
}
