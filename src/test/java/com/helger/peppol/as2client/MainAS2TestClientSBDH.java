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
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;

import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.util.dump.HTTPOutgoingDumperFileBased;
import com.helger.as2lib.util.dump.HTTPOutgoingDumperStreamBased;
import com.helger.as2lib.util.dump.IHTTPOutgoingDumper;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.bdve.executorset.VESID;
import com.helger.bdve.executorset.ValidationExecutorSetRegistry;
import com.helger.bdve.peppol.PeppolValidation370;
import com.helger.bdve.result.ValidationResult;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.resource.URLResource;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.commons.url.URLHelper;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.network.proxy.autoconf.ProxyAutoConfigHelper;
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
import com.helger.xml.serialize.read.DOMReader;

/**
 * Main class to send AS2 messages. In this case using a prebuild SBDH.
 *
 * @author Philip Helger
 */
public final class MainAS2TestClientSBDH
{
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  private static final IPeppolURLProvider URL_PROVIDER = PeppolURLProvider.INSTANCE;
  /** The PEPPOL sender participant ID */
  private static final IParticipantIdentifier SENDER_PEPPOL_ID = IF.createParticipantIdentifierWithDefaultScheme ("9999:test-sender");
  /** Your AS2 sender email address */
  private static final String SENDER_EMAIL = "peppol@example.org";

  private static final Logger LOGGER = LoggerFactory.getLogger (MainAS2TestClientSBDH.class);

  static
  {
    // Set Proxy Settings from property file.
    SMPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();

    // Enable or disable debug mode
    GlobalDebug.setDebugModeDirect (false);

    SystemProperties.setPropertyValue ("AS2.dumpDecryptedDirectory", "as2-in-decrypted");
    SystemProperties.setPropertyValue ("AS2.httpDumpDirectoryIncoming", "as2-in-http");
    SystemProperties.setPropertyValue ("AS2.httpDumpDirectoryOutgoing", "as2-out-http");
  }

  @Nonnull
  private static IKeyStoreType _getKeyStoreType (final boolean bProd)
  {
    return bProd ? EKeyStoreType.PKCS12 : EKeyStoreType.PKCS12;
  }

  /** The file path to the PKCS12 key store */
  private static String _getKeyStorePath (final boolean bProd)
  {
    return bProd ? "as2-client-data/client-certs.p12" : "as2-client-data/test-client-certs.p12";
  }

  /** The password to open the PKCS12 key store */
  private static String _getKeyStorePassword (final boolean bProd)
  {
    return bProd ? "peppol" : "peppol";
  }

  /** Your AS2 sender ID */
  private static final String _getSenderAS2ID (final boolean bProd)
  {
    // Pro: 306 Test: 309
    return bProd ? "PDK000270" : "PDK000270";
  }

  /** Your AS2 key alias in the PKCS12 key store */
  private static final String _getSenderKeyAlias (final boolean bProd)
  {
    return _getSenderAS2ID (bProd);
  }

  @SuppressWarnings ({ "null", "resource" })
  public static void main (final String [] args) throws Exception
  {
    /** The PEPPOL document type to use. */
    final IDocumentTypeIdentifier aDocTypeID = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
    /** The PEPPOL process to use. */
    final IProcessIdentifier aProcessID = EPredefinedProcessIdentifier.BIS4A_V2.getAsProcessIdentifier ();
    final IParticipantIdentifier aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
    final String sTestFilename = "xml/as2-test-at-gov.xml";
    IReadableResource aTestResource = null;
    final String sReceiverID = null;
    final String sReceiverKeyAlias = null;
    final String sReceiverAddress = null;
    final ISMLInfo aSML = ESML.DIGIT_TEST;
    final VESID aValidationKey = PeppolValidation370.VID_OPENPEPPOL_T10_V2;
    URI aSMPURI = null;
    final ECryptoAlgorithmSign eMICAlg = ECryptoAlgorithmSign.DIGEST_SHA_1;
    HttpHost aProxy = SMPClientConfiguration.getHttpProxy ();
    final int nConnectTimeoutMS = AS2ClientSettings.DEFAULT_CONNECT_TIMEOUT_MS;
    final int nReadTimeoutMS = AS2ClientSettings.DEFAULT_READ_TIMEOUT_MS;
    final String sWPAD = null;
    final boolean bDebugOutgoing = true;
    final String sOutgoingDumpFilename = "outgoing.dump";
    final boolean bDebugIncoming = false;
    final EContentTransferEncoding eCTE = EContentTransferEncoding.AS2_DEFAULT;

    // Debug outgoing (AS2 message)?
    NonBlockingByteArrayOutputStream aDebugOS = null;
    IHTTPOutgoingDumper aOutgoingDumper = null;
    if (bDebugOutgoing)
    {
      if (StringHelper.hasText (sOutgoingDumpFilename))
      {
        aOutgoingDumper = new HTTPOutgoingDumperFileBased (new File (sOutgoingDumpFilename)).setDumpComment (false)
                                                                                            .setDumpHeader (false);
      }
      else
      {
        aDebugOS = new NonBlockingByteArrayOutputStream ();
        aOutgoingDumper = new HTTPOutgoingDumperStreamBased (aDebugOS).setDumpComment (false);
      }
    }

    // Debug incoming (AS2 MDN)?
    if (bDebugIncoming)
      HTTPHelper.setHTTPIncomingDumperFactory ( () -> (aHeaderLines, aPayload, aMsg) -> {
        LOGGER.info ("Received Headers:\n" + StringHelper.getImploded ("\n  ", aHeaderLines));
        LOGGER.info ("Received Payload:\n" + new String (aPayload, StandardCharsets.UTF_8));
        LOGGER.info ("Received Message:\n" + aMsg);
      });

    // Read resource by filename
    if (aTestResource == null && sTestFilename != null)
      aTestResource = new ClassPathResource (sTestFilename);

    if (aSMPURI == null)
      aSMPURI = URL_PROVIDER.getSMPURIOfParticipant (aReceiver, aSML);

    if (sWPAD != null)
    {
      final ProxyAutoConfigHelper aPACHelper = new ProxyAutoConfigHelper (new URLResource (sWPAD));
      final String sProxyHost = aPACHelper.findProxyForURL (aSMPURI.toURL ().toExternalForm (), aSMPURI.getHost ());
      if (sProxyHost != null)
      {
        LOGGER.info ("Using proxy '" + sProxyHost + "'");
        final URL aURL = URLHelper.getAsURL (sProxyHost);
        if (aURL != null)
        {
          aProxy = new HttpHost (aURL.getHost (), aURL.getPort ());
          LOGGER.info ("  Resolved proxy to '" + aProxy + "'");
        }
      }
    }

    final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aSMPURI);

    // No proxy for local host
    if (!aSMPClient.getSMPHostURI ().startsWith ("http://localhost") &&
        !aSMPClient.getSMPHostURI ().startsWith ("http://127."))
    {
      aSMPClient.setProxy (aProxy);
    }

    try
    {
      final boolean bProd = aSML == ESML.DIGIT_PRODUCTION;

      final File aKeyStoreFile = new File (_getKeyStorePath (bProd));
      final byte [] aKeyStoreBytes = SimpleFileIO.getAllFileBytes (aKeyStoreFile);

      // 1. Read XML into memory
      final Document aXMLDocument = DOMReader.readXMLDOM (aTestResource);
      if (aXMLDocument == null)
        throw new AS2ClientBuilderException ("Failed to read business document '" +
                                             aTestResource.getPath () +
                                             "' as XML");

      // 2. Validate
      final ValidationExecutorSetRegistry aVESRegistry = AS2ClientBuilder.createDefaultValidationRegistry ();
      final IAS2ClientBuilderValidatonResultHandler aValidationResultHandler = new IAS2ClientBuilderValidatonResultHandler ()
      {
        public void onValidationErrors (final com.helger.bdve.result.ValidationResultList aValidationResult) throws AS2ClientBuilderException
        {
          for (final ValidationResult aVR : aValidationResult)
          {
            if (aVR.isFailure ())
              LOGGER.error (aVR.toString ());
            else
              LOGGER.info (aVR.toString ());
          }
        }
      };
      AS2ClientBuilder.validateBusinessDocument (aVESRegistry,
                                                 aValidationKey,
                                                 aValidationResultHandler,
                                                 aXMLDocument.getDocumentElement ());

      // 3. Build SBDH
      final StandardBusinessDocument aSBD = AS2ClientBuilder.createSBDH (SENDER_PEPPOL_ID,
                                                                         aReceiver,
                                                                         aDocTypeID,
                                                                         aProcessID,
                                                                         null,
                                                                         null,
                                                                         aXMLDocument.getDocumentElement ());
      final NonBlockingByteArrayOutputStream aBAOS = AS2ClientBuilder.getSerializedSBDH (aSBD, null);

      // 4. send as SBDH
      final AS2ClientResponse aResponse = new AS2ClientBuilder ().setSMPClient (aSMPClient)
                                                                 .setKeyStore (_getKeyStoreType (bProd),
                                                                               aKeyStoreBytes,
                                                                               _getKeyStorePassword (bProd))
                                                                 .setSaveKeyStoreChangesToFile (false)
                                                                 .setSenderAS2ID (_getSenderAS2ID (bProd))
                                                                 .setSenderAS2Email (SENDER_EMAIL)
                                                                 .setSenderAS2KeyAlias (_getSenderKeyAlias (bProd))
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
                                                                 .setContentTransferEncoding (eCTE)
                                                                 .setOutgoingDumper (aOutgoingDumper)
                                                                 .sendSynchronousSBDH (aBAOS);
      if (aResponse.hasException ())
        LOGGER.warn (aResponse.getAsString ());

      if (false)
        if (aResponse.hasMDN ())
          LOGGER.info ("Certificate of MDN:\n" + aResponse.getMDNVerificationCertificate ());

      if (aDebugOS != null)
        LOGGER.info ("Outgoing request:\n" + aDebugOS.getAsString (StandardCharsets.UTF_8));

      LOGGER.info ("Done");
    }
    catch (final AS2ClientBuilderValidationException ex)
    {
      for (final ValidationResult aVR : ex.getValidationResult ())
        if (aVR.isFailure ())
          LOGGER.error (aVR.toString ());
        else
          LOGGER.info (aVR.toString ());
    }
    catch (final AS2ClientBuilderException ex)
    {
      LOGGER.error ("AS2 client error", ex);
    }
  }
}
