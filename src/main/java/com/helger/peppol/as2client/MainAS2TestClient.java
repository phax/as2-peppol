package com.helger.peppol.as2client;

import java.io.File;
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
import org.busdox.servicemetadata.publishing._1.EndpointType;
import org.unece.cefact.namespaces.sbdh.SBDMarshaller;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;

import com.helger.as2lib.client.AS2Client;
import com.helger.as2lib.client.AS2ClientRequest;
import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECryptoAlgorithm;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.helger.commons.xml.serialize.DOMReader;
import com.helger.peppol.sbdh.DocumentData;
import com.helger.peppol.sbdh.write.DocumentDataWriter;

import eu.europa.ec.cipa.busdox.identifier.IReadonlyParticipantIdentifier;
import eu.europa.ec.cipa.peppol.identifier.doctype.EPredefinedDocumentTypeIdentifier;
import eu.europa.ec.cipa.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import eu.europa.ec.cipa.peppol.identifier.participant.SimpleParticipantIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.EPredefinedProcessIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.SimpleProcessIdentifier;
import eu.europa.ec.cipa.peppol.sml.ESML;
import eu.europa.ec.cipa.smp.client.ESMPTransportProfile;
import eu.europa.ec.cipa.smp.client.SMPServiceCaller;
import eu.europa.ec.cipa.smp.client.SMPServiceCallerReadonly;

/**
 * Main class to
 *
 * @author Philip Helger
 */
public final class MainAS2TestClient
{
  private static final String PKCS12_CERTSTORE_PATH = "as2-client-data/client-certs.p12";
  private static final String PKCS12_CERTSTORE_PASSWORD = "peppol";
  private static final String SENDER_AS2_ID = "APP_1000000004";
  private static final String SENDER_EMAIL = "support-erb@brz.gv.at";
  private static final String SENDER_KEY_ALIAS = SENDER_AS2_ID;
  private static final SimpleDocumentTypeIdentifier DOCTYPE = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
  private static final SimpleProcessIdentifier PROCESS = EPredefinedProcessIdentifier.BIS4A_V20.getAsProcessIdentifier ();
  private static final ESMPTransportProfile TRANSPORT_PROFILE = ESMPTransportProfile.TRANSPORT_PROFILE_AS2;

  static
  {
    // Set Proxy Settings
    System.setProperty ("java.net.useSystemProxies", "true");
    if (false)
    {
      System.setProperty ("http.proxyHost", "1.2.3.4");
      System.setProperty ("http.proxyPort", "8080");
      System.setProperty ("https.proxyHost", "1.2.3.4");
      System.setProperty ("https.proxyPort", "8080");
    }
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

    IReadonlyParticipantIdentifier aReceiver;
    String sTestFilename;
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
    if (true)
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
      // BRZ test endpoint
      aReceiver = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test");
      sTestFilename = "xml/as2-test-at-gov.xml";
    }

    if (sReceiverAddress == null || sReceiverID == null)
    {
      System.out.println ("SMP lookup");
      final SMPServiceCaller aSMPClient = new SMPServiceCaller (aReceiver, ESML.PRODUCTION);

      final EndpointType aEndpoint = aSMPClient.getEndpoint (aReceiver, DOCTYPE, PROCESS, TRANSPORT_PROFILE);
      if (aEndpoint == null)
        throw new NullPointerException ("Endpoint");

      if (sReceiverAddress == null)
        sReceiverAddress = SMPServiceCallerReadonly.getEndpointAddress (aEndpoint);
      if (aReceiverCertificate == null)
        aReceiverCertificate = SMPServiceCallerReadonly.getEndpointCertificate (aEndpoint);
      if (sReceiverID == null)
        sReceiverID = _getCN (aReceiverCertificate);
      System.out.println ("Receiver URL: " + sReceiverAddress);
      System.out.println ("Receiver DN:  " + sReceiverID);
    }

    if (sReceiverKeyAlias == null)
      sReceiverKeyAlias = sReceiverID;

    final AS2ClientSettings aSettings = new AS2ClientSettings ();
    aSettings.setKeyStore (new File (PKCS12_CERTSTORE_PATH), PKCS12_CERTSTORE_PASSWORD);

    // Fixed sender
    aSettings.setSenderData (SENDER_AS2_ID, SENDER_EMAIL, SENDER_KEY_ALIAS);

    // Dynamic receiver
    aSettings.setReceiverData (sReceiverID, sReceiverKeyAlias, sReceiverAddress);
    aSettings.setReceiverCertificate (aReceiverCertificate);

    // AS2 stuff
    aSettings.setPartnershipName (aSettings.getSenderAS2ID () + "_" + aSettings.getReceiverAS2ID ());
    aSettings.setMDNOptions ("signed-receipt-protocol=required, pkcs7-signature; signed-receipt-micalg=required, sha1");
    aSettings.setEncryptAndSign (null, ECryptoAlgorithm.DIGEST_SHA1);
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
    new AS2Client ().sendSynchronous (aSettings, aRequest);
  }
}
