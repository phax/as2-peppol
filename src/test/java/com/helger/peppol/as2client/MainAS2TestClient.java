/**
 * Copyright (C) 2014-2017 Philip Helger (www.helger.com)
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
import java.security.Security;
import java.util.Map;

import org.apache.http.HttpHost;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.bdve.executorset.VESID;
import com.helger.bdve.peppol.PeppolValidation330;
import com.helger.bdve.result.ValidationResult;
import com.helger.commons.CGlobal;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.resource.URLResource;
import com.helger.commons.io.resource.wrapped.GZIPReadableResource;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemHelper;
import com.helger.commons.url.URLHelper;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.network.proxy.autoconf.ProxyAutoConfigHelper;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpclient.SMPClientConfiguration;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.url.IPeppolURLProvider;
import com.helger.peppol.url.PeppolURLProvider;

/**
 * Main class to send AS2 messages.
 *
 * @author Philip Helger
 */
public final class MainAS2TestClient
{
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  private static final IPeppolURLProvider URL_PROVIDER = PeppolURLProvider.INSTANCE;
  /** The PEPPOL sender participant ID */
  private static final IParticipantIdentifier SENDER_PEPPOL_ID = IF.createParticipantIdentifierWithDefaultScheme ("9999:test-sender");
  /** Your AS2 sender email address */
  private static final String SENDER_EMAIL = "peppol@example.org";

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

  /** The file path to the PKCS12 key store */
  private static String _getKeyStorePath (final boolean bProd)
  {
    return bProd ? "as2-client-data/client-certs.p12" : "as2-client-data/test-client-certs.p12";
  }

  /** The password to open the PKCS12 key store */
  private static String _getKeyStorePassword ()
  {
    return "peppol";
  }

  /** Your AS2 sender ID */
  private static final String _getSenderAS2ID (final boolean bProd)
  {
    // Pro: 306 Test: 309
    return bProd ? "APP_1000000306" : "APP_1000000309";
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
    IDocumentTypeIdentifier aDocTypeID = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
    /** The PEPPOL process to use. */
    IProcessIdentifier aProcessID = EPredefinedProcessIdentifier.BIS4A_V20.getAsProcessIdentifier ();
    IParticipantIdentifier aReceiver = null;
    String sTestFilename = null;
    IReadableResource aTestResource = null;
    String sReceiverID = null;
    String sReceiverKeyAlias = null;
    String sReceiverAddress = null;
    ISMLInfo aSML = ESML.DIGIT_PRODUCTION;
    final VESID aValidationKey = true ? null : PeppolValidation330.VID_OPENPEPPOL_T10_V2;
    URI aSMPURI = null;
    ECryptoAlgorithmSign eMICAlg = ECryptoAlgorithmSign.DIGEST_SHA_1;
    HttpHost aProxy = SMPClientConfiguration.getHttpProxy ();
    final int nConnectTimeoutMS = AS2ClientSettings.DEFAULT_CONNECT_TIMEOUT_MS;
    int nReadTimeoutMS = AS2ClientSettings.DEFAULT_READ_TIMEOUT_MS;
    String sWPAD = null;
    boolean bDebugOutgoing = false;
    boolean bDebugIncoming = false;
    EContentTransferEncoding eCTE = EContentTransferEncoding.AS2_DEFAULT;

    if (false)
    {
      // mysupply test client
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("0088:5798009883995");
      sTestFilename = "xml/as2-mysupply_TEST_NO.xml";
    }
    if (false)
    {
      // DIFI test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9908:810418052");
      sTestFilename = "xml/as2-pagero.xml";
    }
    if (false)
    {
      // Pagero test participant 9908:222222222
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9908:222222222");
      sTestFilename = "xml/as2-pagero.xml";
    }
    if (false)
    {
      // Unit4 test participant 9908:810017902
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9908:810017902");
      sTestFilename = "xml/as2-pagero.xml";
    }
    if (false)
    {
      // Unit4 debug test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9908:810017902");
      sTestFilename = "xml/as2-pagero.xml";
      // Override since not in SMP
      sReceiverAddress = "https://ap-test.unit4.com/oxalis/as2";
    }
    if (false)
    {
      // DERWID test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9914:atu66313919");
      sTestFilename = "xml/as2-test-at-gov.xml";
    }
    if (false)
    {
      // TESISQUARE test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("0088:0000000000003");
      sTestFilename = "xml/as2-tesisquare_test_file_noheader.xml";
    }
    if (false)
    {
      // CONSIP test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9907:consiptestap2");
      aDocTypeID = IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Order-2::Order##urn:www.cenbii.eu:transaction:biitrns001:ver2.0:extended:urn:www.peppol.eu:bis:peppol3a:ver2.0::2.1");
      aProcessID = IF.createProcessIdentifierWithDefaultScheme ("urn:www.cenbii.eu:profile:bii03:ver2.0");
      aSML = ESML.DIGIT_TEST;
      sTestFilename = "xml/as2-order.xml";
    }
    if (false)
    {
      // ESPAP test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9946:espap");
      sTestFilename = "xml/as2-test-at-gov.xml";
      aSML = ESML.DIGIT_TEST;
    }
    if (false)
    {
      // ecosio test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("0088:ecosio");
      sTestFilename = "xml/as2-test-at-gov.xml";
      aSML = ESML.DIGIT_TEST;
    }
    if (false)
    {
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9925:0883663268");
      sTestFilename = "xml/as2-test-at-gov.xml";
      aSML = ESML.DIGIT_TEST;
    }
    if (false)
    {
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("0007:5560760737");
      sTestFilename = "xml/as2-test_logiq_stanley.xml";
      eMICAlg = ECryptoAlgorithmSign.DIGEST_SHA1;
    }
    if (false)
    {
      // Doclogistics test participant 9948:rs062525164
      // or 9944:nl807881958b01
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9948:rs062525164");
      if (false)
        sTestFilename = "xml/as2-pagero.xml";
      else
        aTestResource = new GZIPReadableResource (new ClassPathResource ("xml/as2-test-at-gov-2gb.gz"));
      aSML = ESML.DIGIT_TEST;
      if (false)
        sReceiverAddress = "https://connect.docslogistics.net/AccessPoint/Home/Receive";

      // For debugging
      nReadTimeoutMS = 500 * (int) CGlobal.MILLISECONDS_PER_SECOND;
    }
    if (false)
    {
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9932:856922195");
      aDocTypeID = EPredefinedDocumentTypeIdentifier.ORDER_T001_BIS03A_V20.getAsDocumentTypeIdentifier ();
      aProcessID = EPredefinedProcessIdentifier.BIS03A_V20.getAsProcessIdentifier ();
      sTestFilename = "xml/as2-order.xml";
    }
    if (false)
    {
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9921:ITUFX1HE");
      if (false)
        sReceiverAddress = "https://test-notier.regione.emilia-romagna.it/notier/rest/v1.0/documenti/invio";

      // Next two settings, to specify I'm seding an order, NOT an invoice
      aDocTypeID = EPredefinedDocumentTypeIdentifier.ORDER_T001_BIS03A_V20.getAsDocumentTypeIdentifier ();
      aDocTypeID = IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Order-2::Order#urn:www.cenbii.eu:transaction:biitrns001:ver2.0:extended:urn:www.peppol.eu:bis:peppol3a:ver2.0::2.1");
      aProcessID = EPredefinedProcessIdentifier.BIS03A_V20.getAsProcessIdentifier ();

      sTestFilename = "xml/ordine-FA-2017-896-RIETI.xml";
      aSML = ESML.DIGIT_TEST;

      sWPAD = "http://wpad.ente.regione.emr.it/wpad.dat";
    }
    if (false)
    {
      // IBM test
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("0088:5050689000018");
      sTestFilename = "xml/Use Case 1.a_ExampleFile_PEPPOL BIS.xml";
      aSML = ESML.DIGIT_TEST;
      eCTE = EContentTransferEncoding.BINARY;
    }
    if (true)
    {
      // IBM test 2
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("0088:1234567890111");
      if (false)
        sReceiverAddress = "http://na1t40.as2.b2b.ibmcloud.com/as2";
      sTestFilename = "xml/Use Case 1.a_ExampleFile_PEPPOL BIS.xml";
      aSML = ESML.DIGIT_TEST;
      bDebugOutgoing = false;
      eCTE = EContentTransferEncoding.BINARY;
    }
    if (false)
    {
      // BRZ test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
      sTestFilename = "xml/as2-test-at-gov.xml";
      aSML = ESML.DIGIT_TEST;
    }
    if (false)
    {
      // localhost test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
      sTestFilename = "xml/as2-test-at-gov.xml";
      // Avoid SMP lookup
      sReceiverAddress = "http://localhost:8080/as2";
      sReceiverID = _getSenderAS2ID (false);
      sReceiverKeyAlias = _getSenderKeyAlias (false);
      aSML = ESML.DIGIT_TEST;
    }
    if (false)
    {
      // localhost test endpoint with 2 GB file
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
      aTestResource = new GZIPReadableResource (new ClassPathResource ("xml/as2-test-at-gov-2gb.gz"));
      // Avoid SMP lookup
      sReceiverAddress = "http://localhost:8080/as2";
      sReceiverID = _getSenderAS2ID (false);
      sReceiverKeyAlias = _getSenderKeyAlias (false);
      aSML = ESML.DIGIT_TEST;
    }
    if (false)
    {
      // localhost test redirect with test SMP on localhost:90
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
      sTestFilename = "xml/as2-test-at-gov.xml";
      aSMPURI = URLHelper.getAsURI ("http://127.0.0.1:90");
      aDocTypeID = IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2::CreditNote##urn:www.cenbii.eu:transaction:biitrns014:ver2.0:extended:urn:www.peppol.eu:bis:peppol5a:ver2.0:extended:urn:www.erechnung.gv.at:ver1.0::2.1");
      aProcessID = IF.createProcessIdentifierWithDefaultScheme ("urn:www.cenbii.eu:profile:bii05:ver2.0");
      bDebugIncoming = true;
    }

    // Debug outgoing (AS2 message)?
    NonBlockingByteArrayOutputStream aDebugOS;
    if (bDebugOutgoing)
    {
      aDebugOS = new NonBlockingByteArrayOutputStream ();
      HTTPHelper.setHTTPOutgoingDumper (aMsg -> {
        final StringBuilder aSB = new StringBuilder ();
        // Write all attributes first
        aSB.append ("Attributes:\n");
        for (final Map.Entry <String, String> aEntry : aMsg.getAllAttributes ())
        {
          aSB.append ("  ").append (aEntry.getKey ()).append ('=').append (aEntry.getValue ()).append ('\n');
        }
        // Than all headers
        aSB.append ("Headers:\n");
        aMsg.forEachHeader ( (k, v) -> aSB.append ("  ").append (k).append ('=').append (v).append ('\n'));

        aDebugOS.write (aSB.toString ().getBytes (SystemHelper.getSystemCharset ()));
        return aDebugOS;
      });
    }
    else
      aDebugOS = null;

    // Debug incoming (AS2 MDN)?
    if (bDebugIncoming)
      HTTPHelper.setHTTPIncomingDumper ( (aHeaderLines, aPayload, aMsg) -> {
        s_aLogger.info ("Received Headers: " + StringHelper.getImploded ("\n  ", aHeaderLines));
        s_aLogger.info ("Received Payload: " + new String (aPayload, StandardCharsets.UTF_8));
        s_aLogger.info ("Received Message: " + aMsg);
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
        s_aLogger.info ("Using proxy '" + sProxyHost + "'");
        final URL aURL = URLHelper.getAsURL (sProxyHost);
        if (aURL != null)
        {
          aProxy = new HttpHost (aURL.getHost (), aURL.getPort ());
          s_aLogger.info ("  Resolved proxy to '" + aProxy + "'");
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
      final AS2ClientResponse aResponse = new AS2ClientBuilder ().setSMPClient (aSMPClient)
                                                                 .setPKCS12KeyStore (new File (_getKeyStorePath (bProd)),
                                                                                     _getKeyStorePassword ())
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
                                                                 .setValidationKey (aValidationKey)
                                                                 .setContentTransferEncoding (eCTE)
                                                                 .sendSynchronous ();
      if (aResponse.hasException ())
        s_aLogger.warn (aResponse.getAsString ());

      if (aDebugOS != null)
        s_aLogger.info ("Outgoing request:\n" + aDebugOS.getAsString (StandardCharsets.UTF_8));

      s_aLogger.info ("Done");
    }
    catch (final AS2ClientBuilderValidationException ex)
    {
      for (final ValidationResult aVR : ex.getValidationResult ())
        if (aVR.isFailure ())
          s_aLogger.error (aVR.toString ());
        else
          s_aLogger.info (aVR.toString ());
    }
    catch (final AS2ClientBuilderException ex)
    {
      ex.printStackTrace ();
    }
  }
}
