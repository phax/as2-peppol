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
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.transform.stream.StreamResult;

import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.as2lib.client.AS2Client;
import com.helger.as2lib.client.AS2ClientRequest;
import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.email.EmailAddressHelper;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.commons.xml.serialize.read.DOMReader;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IProcessIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.sbdh.DocumentData;
import com.helger.peppol.sbdh.write.DocumentDataWriter;
import com.helger.sbdh.SBDMarshaller;

/**
 * A builder class for easy usage of the AS2 client for sending messages to a
 * PEPPOL participant. After building use the {@link #sendSynchronous()} message
 * to trigger the sending.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class AS2ClientBuilder
{
  private IAS2ClientBuilderMessageHandler m_aMessageHandler = new DefaultAS2ClientBuilderMessageHandler ();
  private File m_aKeyStoreFile;
  private String m_sKeyStorePassword;
  private String m_sAS2Subject = "OpenPEPPOL AS2 message";
  private String m_sSenderAS2ID;
  private String m_sSenderAS2Email;
  private String m_sSenderAS2KeyAlias;
  private String m_sReceiverAS2ID;
  private String m_sReceiverAS2KeyAlias;
  private String m_sReceiverAS2Url;
  private X509Certificate m_aReceiverCert;
  private ECryptoAlgorithmSign m_eSigningAlgo = ECryptoAlgorithmSign.DIGEST_SHA1;
  private String m_sMessageIDFormat = "OpenPEPPOL-$date.ddMMyyyyHHmmssZ$-$rand.1234$@$msg.sender.as2_id$_$msg.receiver.as2_id$";
  private IReadableResource m_aBusinessDocument;
  private IParticipantIdentifier m_aPeppolSenderID;
  private IParticipantIdentifier m_aPeppolReceiverID;
  private IDocumentTypeIdentifier m_aPeppolDocumentTypeID;
  private IProcessIdentifier m_aPeppolProcessID;

  /**
   * Default constructor.
   */
  public AS2ClientBuilder ()
  {}

  @Nonnull
  public AS2ClientBuilder setMessageHandler (@Nonnull final IAS2ClientBuilderMessageHandler aMessageHandler)
  {
    m_aMessageHandler = ValueEnforcer.notNull (aMessageHandler, "MessageHandler");
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setPKCS12KeyStore (@Nullable final File aKeyStoreFile,
                                             @Nullable final String sKeyStorePassword)
  {
    m_aKeyStoreFile = aKeyStoreFile;
    m_sKeyStorePassword = sKeyStorePassword;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setAS2Subject (@Nullable final String sAS2Subject)
  {
    m_sAS2Subject = sAS2Subject;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setSenderAS2ID (@Nullable final String sSenderAS2ID)
  {
    m_sSenderAS2ID = sSenderAS2ID;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setSenderAS2Email (@Nullable final String sSenderAS2Email)
  {
    m_sSenderAS2Email = sSenderAS2Email;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setSenderAS2KeyAlias (@Nullable final String sSenderAS2KeyAlias)
  {
    m_sSenderAS2KeyAlias = sSenderAS2KeyAlias;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setReceiverAS2ID (@Nullable final String sReceiverAS2ID)
  {
    m_sReceiverAS2ID = sReceiverAS2ID;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setReceiverAS2KeyAlias (@Nullable final String sReceiverAS2KeyAlias)
  {
    m_sReceiverAS2KeyAlias = sReceiverAS2KeyAlias;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setReceiverAS2Url (@Nullable final String sReceiverAS2Url)
  {
    m_sReceiverAS2Url = sReceiverAS2Url;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setReceiverCertificate (@Nullable final X509Certificate aReceiverCert)
  {
    m_aReceiverCert = aReceiverCert;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setAS2SigningAlgorithm (@Nullable final ECryptoAlgorithmSign eSigningAlgo)
  {
    m_eSigningAlgo = eSigningAlgo;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setAS2MessageIDFormat (@Nullable final String sMessageIDFormat)
  {
    m_sMessageIDFormat = sMessageIDFormat;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setBusinessDocument (@Nullable final IReadableResource aBusinessDocument)
  {
    m_aBusinessDocument = aBusinessDocument;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setPeppolSenderID (@Nullable final IParticipantIdentifier aPeppolSenderID)
  {
    m_aPeppolSenderID = aPeppolSenderID;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setPeppolReceiverID (@Nullable final IParticipantIdentifier aPeppolReceiverID)
  {
    m_aPeppolReceiverID = aPeppolReceiverID;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setPeppolDocumentTypeID (@Nullable final IDocumentTypeIdentifier aPeppolDocumentTypeID)
  {
    m_aPeppolDocumentTypeID = aPeppolDocumentTypeID;
    return this;
  }

  @Nonnull
  public AS2ClientBuilder setPeppolProcessID (@Nullable final IProcessIdentifier aPeppolProcessID)
  {
    m_aPeppolProcessID = aPeppolProcessID;
    return this;
  }

  public void verifyContent () throws AS2ClientBuilderException
  {
    if (m_aKeyStoreFile == null)
      m_aMessageHandler.error ("No AS2 key store is defined");
    else
    {
      if (!m_aKeyStoreFile.exists ())
        m_aMessageHandler.error ("The provided AS2 key store '" +
                                 m_aKeyStoreFile.getAbsolutePath () +
                                 "' does not exist.");
      else
        if (!m_aKeyStoreFile.isFile ())
          m_aMessageHandler.error ("The provided AS2 key store '" +
                                   m_aKeyStoreFile.getAbsolutePath () +
                                   "' is not a file but potentially a directory.");
    }
    if (m_sKeyStorePassword == null)
      m_aMessageHandler.error ("No key store password provided. If you need an empty password, please provide an empty String!");

    if (StringHelper.hasNoText (m_sAS2Subject))
      m_aMessageHandler.error ("The AS2 message subject is missing");

    if (StringHelper.hasNoText (m_sSenderAS2ID))
      m_aMessageHandler.error ("The AS2 sender ID is missing");
    else
      if (!m_sSenderAS2ID.startsWith ("APP_"))
        m_aMessageHandler.warn ("The AS2 sender ID '" +
                                m_sSenderAS2ID +
                                "' should start with 'APP_' as required by the PEPPOL specification");

    if (StringHelper.hasNoText (m_sSenderAS2Email))
      m_aMessageHandler.error ("The AS2 sender email address is missing");
    else
      if (!EmailAddressHelper.isValid (m_sSenderAS2Email))
        m_aMessageHandler.warn ("The AS2 sender email address '" +
                                m_sSenderAS2Email +
                                "' seems to be an invalid email address.");

    if (StringHelper.hasNoText (m_sSenderAS2KeyAlias))
      m_aMessageHandler.error ("The AS2 sender key alias is missing");
    else
      if (!m_sSenderAS2KeyAlias.startsWith ("APP_"))
        m_aMessageHandler.warn ("The AS2 sender key alias '" +
                                m_sSenderAS2KeyAlias +
                                "' should start with 'APP_' for the use with the dynamic AS2 partnerships");
      else
        if (m_sSenderAS2ID != null && !m_sSenderAS2ID.equals (m_sSenderAS2KeyAlias))
          m_aMessageHandler.warn ("The AS2 sender key alias ('" +
                                  m_sSenderAS2KeyAlias +
                                  "') should match the AS2 sender ID ('" +
                                  m_sSenderAS2ID +
                                  "')");

    if (StringHelper.hasNoText (m_sReceiverAS2ID))
      m_aMessageHandler.error ("The AS2 receiver ID is missing");
    else
      if (!m_sReceiverAS2ID.startsWith ("APP_"))
        m_aMessageHandler.warn ("The AS2 receiver ID '" +
                                m_sReceiverAS2ID +
                                "' should start with 'APP_' as required by the PEPPOL specification");

    if (StringHelper.hasNoText (m_sReceiverAS2KeyAlias))
      m_aMessageHandler.error ("The AS2 receiver key alias is missing");
    else
      if (!m_sReceiverAS2KeyAlias.startsWith ("APP_"))
        m_aMessageHandler.warn ("The AS2 receiver key alias '" +
                                m_sReceiverAS2KeyAlias +
                                "' should start with 'APP_' for the use with the dynamic AS2 partnerships");
      else
        if (m_sReceiverAS2ID != null && !m_sReceiverAS2ID.equals (m_sReceiverAS2KeyAlias))
          m_aMessageHandler.warn ("The AS2 receiver key alias ('" +
                                  m_sReceiverAS2KeyAlias +
                                  "') should match the AS2 receiver ID ('" +
                                  m_sReceiverAS2ID +
                                  "')");

    if (StringHelper.hasNoText (m_sReceiverAS2Url))
      m_aMessageHandler.error ("The AS2 receiver URL (AS2 endpoint URL) is missing");
    else
      if (URLHelper.getAsURL (m_sReceiverAS2Url) == null)
        m_aMessageHandler.warn ("The provided AS2 receiver URL '" + m_sReceiverAS2Url + "' seems to be an invalid URL");

    if (m_aReceiverCert == null)
      m_aMessageHandler.error ("The receiver X.509 certificate is missing. Usually this is extracted from the SMP response");

    if (m_eSigningAlgo == null)
      m_aMessageHandler.error ("The signing algorithm for the AS2 message is missing");

    if (StringHelper.hasNoText (m_sMessageIDFormat))
      m_aMessageHandler.error ("The AS2 message ID format is missing.");

    if (m_aBusinessDocument == null)
      m_aMessageHandler.error ("The XML business document to be send is missing.");
    else
      if (!m_aBusinessDocument.exists ())
        m_aMessageHandler.error ("The XML business document to be send '" +
                                 m_aBusinessDocument.getPath () +
                                 "' does not exist.");

    if (m_aPeppolSenderID == null)
      m_aMessageHandler.error ("The PEPPOL sender participant ID is missing");
    else
      if (!IdentifierHelper.hasDefaultParticipantIdentifierScheme (m_aPeppolSenderID))
        m_aMessageHandler.warn ("The PEPPOL sender participant ID '" +
                                IdentifierHelper.getIdentifierURIEncoded (m_aPeppolSenderID) +
                                "' is using a non-standard scheme!");

    if (m_aPeppolReceiverID == null)
      m_aMessageHandler.error ("The PEPPOL receiver participant ID is missing");
    else
      if (!IdentifierHelper.hasDefaultParticipantIdentifierScheme (m_aPeppolReceiverID))
        m_aMessageHandler.warn ("The PEPPOL receiver participant ID '" +
                                IdentifierHelper.getIdentifierURIEncoded (m_aPeppolReceiverID) +
                                "' is using a non-standard scheme!");

    if (m_aPeppolDocumentTypeID == null)
      m_aMessageHandler.error ("The PEPPOL document type ID is missing");
    else
      if (!IdentifierHelper.hasDefaultDocumentTypeIdentifierScheme (m_aPeppolDocumentTypeID))
        m_aMessageHandler.warn ("The PEPPOL document type ID '" +
                                IdentifierHelper.getIdentifierURIEncoded (m_aPeppolDocumentTypeID) +
                                "' is using a non-standard scheme!");

    if (m_aPeppolProcessID == null)
      m_aMessageHandler.error ("The PEPPOL process ID is missing");
    else
      if (!IdentifierHelper.hasDefaultProcessIdentifierScheme (m_aPeppolProcessID))
        m_aMessageHandler.warn ("The PEPPOL process ID '" +
                                IdentifierHelper.getIdentifierURIEncoded (m_aPeppolProcessID) +
                                "' is using a non-standard scheme!");
  }

  @Nonnull
  public AS2ClientResponse sendSynchronous () throws AS2ClientBuilderException
  {
    verifyContent ();

    final AS2ClientSettings aSettings = new AS2ClientSettings ();
    // Key store
    aSettings.setKeyStore (m_aKeyStoreFile, m_sKeyStorePassword);
    // Fixed sender
    aSettings.setSenderData (m_sSenderAS2ID, m_sSenderAS2Email, m_sSenderAS2KeyAlias);

    // Dynamic receiver
    aSettings.setReceiverData (m_sReceiverAS2ID, m_sReceiverAS2KeyAlias, m_sReceiverAS2Url);
    aSettings.setReceiverCertificate (m_aReceiverCert);

    // AS2 stuff - no need to change anything in this block
    aSettings.setPartnershipName (aSettings.getSenderAS2ID () + "-" + aSettings.getReceiverAS2ID ());
    aSettings.setMDNOptions (new DispositionOptions ().setMICAlg (m_eSigningAlgo)
                                                      .setMICAlgImportance (DispositionOptions.IMPORTANCE_REQUIRED)
                                                      .setProtocol (DispositionOptions.PROTOCOL_PKCS7_SIGNATURE)
                                                      .setProtocolImportance (DispositionOptions.IMPORTANCE_REQUIRED));
    aSettings.setEncryptAndSign (null, m_eSigningAlgo);
    aSettings.setMessageIDFormat (m_sMessageIDFormat);

    // Build message

    // 1. read XML
    Document aXMLDocument = null;
    try
    {
      aXMLDocument = DOMReader.readXMLDOM (m_aBusinessDocument);
    }
    catch (final SAXException ex)
    {
      throw new AS2ClientBuilderException ("Failed to read business document as XML", ex);
    }

    // 2. build SBD data
    final DocumentData aDD = DocumentData.create (aXMLDocument.getDocumentElement ());
    aDD.setSenderWithDefaultScheme (m_aPeppolSenderID.getValue ());
    aDD.setReceiver (m_aPeppolReceiverID.getScheme (), m_aPeppolReceiverID.getValue ());
    aDD.setDocumentType (m_aPeppolDocumentTypeID.getScheme (), m_aPeppolDocumentTypeID.getValue ());
    aDD.setProcess (m_aPeppolProcessID.getScheme (), m_aPeppolProcessID.getValue ());

    // 3. build SBD
    final StandardBusinessDocument aSBD = new DocumentDataWriter ().createStandardBusinessDocument (aDD);
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    if (new SBDMarshaller ().write (aSBD, new StreamResult (aBAOS)).isFailure ())
      throw new AS2ClientBuilderException ("Failed to serialize SBD!");
    aBAOS.close ();

    // 4. send message
    final AS2ClientRequest aRequest = new AS2ClientRequest (m_sAS2Subject);
    aRequest.setData (aBAOS.toByteArray ());
    final AS2ClientResponse aResponse = new AS2Client ().sendSynchronous (aSettings, aRequest);
    return aResponse;
  }
}
