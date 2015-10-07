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
import java.net.URI;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.xml.transform.stream.StreamResult;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;

import com.helger.as2lib.client.AS2Client;
import com.helger.as2lib.client.AS2ClientRequest;
import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.xml.serialize.read.DOMReader;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.sbdh.DocumentData;
import com.helger.peppol.sbdh.write.DocumentDataWriter;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpclient.SMPClientConfiguration;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.sbdh.SBDMarshaller;

/**
 * Main class to send AS2 messages.
 *
 * @author Philip Helger
 */
public final class MainAS2TestClientGHX
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
  /** The PEPPOL document type to use. */
  private static final SimpleDocumentTypeIdentifier DOCTYPE = SimpleDocumentTypeIdentifier.createWithDefaultScheme ("abc");
  /** The PEPPOL process to use. */
  private static final SimpleProcessIdentifier PROCESS = SimpleProcessIdentifier.createWithDefaultScheme ("123");
  /** The PEPPOL transport profile to use */
  private static final ISMPTransportProfile TRANSPORT_PROFILE = ESMPTransportProfile.TRANSPORT_PROFILE_AS2;

  private static final Logger s_aLogger = LoggerFactory.getLogger (MainAS2TestClientGHX.class);

  static
  {
    // Set Proxy Settings from property file.
    SMPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();

    // Sanity check
    if (!new File (PKCS12_CERTSTORE_PATH).exists ())
      throw new InitializationException ("The PKCS12 key store file '" + PKCS12_CERTSTORE_PATH + "' does not exist!");
  }

  /**
   * @param aCert
   *        Source certificate. May not be <code>null</code>.
   * @return The common name of the certificate subject
   * @throws CertificateEncodingException
   */
  @Nonnull
  private static String _getCN (@Nonnull final X509Certificate aCert) throws CertificateEncodingException
  {
    final X500Name x500name = new JcaX509CertificateHolder (aCert).getSubject ();
    final RDN cn = x500name.getRDNs (BCStyle.CN)[0];
    return IETFUtils.valueToString (cn.getFirst ().getValue ());
  }

  @SuppressWarnings ("null")
  public static void main (final String [] args) throws Exception
  {
    // Must be first!
    Security.addProvider (new BouncyCastleProvider ());

    // Enable or disable debug mode
    GlobalDebug.setDebugModeDirect (false);

    String sReceiverID = null;
    String sReceiverKeyAlias = null;
    String sReceiverAddress = null;
    X509Certificate aReceiverCertificate = null;

    // localhost test endpoint
    final IParticipantIdentifier aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test");
    final String sTestFilename = "xml/as2-test-at-gov.xml";
    if (true)
    {
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
      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (URI.create ("http://127.0.0.1"));
      final EndpointType aEndpoint = aSMPClient.getEndpoint (aReceiver, DOCTYPE, PROCESS, TRANSPORT_PROFILE);
      if (aEndpoint == null)
        throw new NullPointerException ("Failed to resolve endpoint for docType/process");

      // Extract from SMP response
      if (sReceiverAddress == null)
        sReceiverAddress = SMPClientReadOnly.getEndpointAddress (aEndpoint);
      if (aReceiverCertificate == null)
        aReceiverCertificate = SMPClientReadOnly.getEndpointCertificate (aEndpoint);
      if (sReceiverID == null)
        sReceiverID = _getCN (aReceiverCertificate);

      // SMP lookup done
      s_aLogger.info ("Receiver URL: " + sReceiverAddress);
      s_aLogger.info ("Receiver DN:  " + sReceiverID);
    }

    if (sReceiverKeyAlias == null)
    {
      // No key alias is specified, so use the same as the receiver ID
      sReceiverKeyAlias = sReceiverID;
    }

    if (sTestFilename == null)
      throw new IllegalStateException ("No test filename present!");

    // Start client configuration
    final AS2ClientSettings aSettings = new AS2ClientSettings ();
    aSettings.setKeyStore (new File (PKCS12_CERTSTORE_PATH), PKCS12_CERTSTORE_PASSWORD);

    // Fixed sender
    aSettings.setSenderData (SENDER_AS2_ID, SENDER_EMAIL, SENDER_KEY_ALIAS);

    // Dynamic receiver
    aSettings.setReceiverData (sReceiverID, sReceiverKeyAlias, sReceiverAddress);
    aSettings.setReceiverCertificate (aReceiverCertificate);

    // AS2 stuff - no need to change anything in this block
    aSettings.setPartnershipName (aSettings.getSenderAS2ID () + "_" + aSettings.getReceiverAS2ID ());
    aSettings.setMDNOptions (new DispositionOptions ().setMICAlg (ECryptoAlgorithmSign.DIGEST_SHA1)
                                                      .setMICAlgImportance (DispositionOptions.IMPORTANCE_REQUIRED)
                                                      .setProtocol (DispositionOptions.PROTOCOL_PKCS7_SIGNATURE)
                                                      .setProtocolImportance (DispositionOptions.IMPORTANCE_REQUIRED));
    aSettings.setEncryptAndSign (null, ECryptoAlgorithmSign.DIGEST_SHA1);
    aSettings.setMessageIDFormat ("OpenPEPPOL-$date.ddMMyyyyHHmmssZ$-$rand.1234$@$msg.sender.as2_id$_$msg.receiver.as2_id$");

    // Build message

    // 1. read XML
    final Document aTestXML = DOMReader.readXMLDOM (new ClassPathResource (sTestFilename));

    // 2. build SBD data
    final DocumentData aDD = DocumentData.create (aTestXML.getDocumentElement ());
    aDD.setSenderWithDefaultScheme (aReceiver.getValue ());
    aDD.setReceiver (aReceiver.getScheme (), aReceiver.getValue ());
    aDD.setDocumentType (DOCTYPE.getScheme (), DOCTYPE.getValue ());
    aDD.setProcess (PROCESS.getScheme (), PROCESS.getValue ());

    // 3. build SBD
    final StandardBusinessDocument aSBD = new DocumentDataWriter ().createStandardBusinessDocument (aDD);
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    if (new SBDMarshaller ().write (aSBD, new StreamResult (aBAOS)).isFailure ())
      throw new IllegalStateException ("Failed to serialize SBD!");
    aBAOS.close ();

    // 4. send message
    final AS2ClientRequest aRequest = new AS2ClientRequest ("OpenPEPPOL AS2 message");
    aRequest.setData (aBAOS.toByteArray ());
    final AS2ClientResponse aResponse = new AS2Client ().sendSynchronous (aSettings, aRequest);
    if (aResponse.hasException ())
      s_aLogger.info (aResponse.getAsString ());

    s_aLogger.info ("Done");
  }
}
