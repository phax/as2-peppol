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
package com.helger.peppol.as2servlet;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.exception.WrappedAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.module.AbstractProcessorModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.sbdh.SBDMarshaller;
import com.helger.security.certificate.CertificateHelper;
import com.helger.smpclient.peppol.ISMPServiceMetadataProvider;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.smpclient.peppol.jaxb.EndpointType;

/**
 * This processor module triggers the processing of the incoming SBD XML
 * (Standard Business Document) document.
 *
 * @author Philip Helger
 */
public class AS2ServletSBDModule extends AbstractProcessorModule
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS2ServletSBDModule.class);

  private EPeppolAS2Version m_eAS2Version;
  private final ICommonsList <IAS2IncomingSBDHandlerSPI> m_aHandlers;

  /**
   * No-argument constructor is needed because it is referenced from the server
   * configuration file.
   */
  @Deprecated
  public AS2ServletSBDModule ()
  {
    // V1 is the deprecated since 2020-02-01
    this (EPeppolAS2Version.V2);
  }

  public AS2ServletSBDModule (@Nonnull final EPeppolAS2Version eAS2Version)
  {
    setPeppolAS2Version (eAS2Version);

    m_aHandlers = ServiceLoaderHelper.getAllSPIImplementations (IAS2IncomingSBDHandlerSPI.class);
    if (m_aHandlers.isEmpty ())
    {
      LOGGER.warn ("No SPI handler of type " +
                   IAS2IncomingSBDHandlerSPI.class.getName () +
                   " for incoming SBD documents is registered. Therefore incoming documents will NOT be handled and maybe discarded if no other processors are active!");
    }
    else
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Loaded " + m_aHandlers.size () + " IAS2IncomingSBDHandlerSPI implementations");
    }
  }

  @Nonnull
  public final EPeppolAS2Version getPeppolAS2Version ()
  {
    return m_eAS2Version;
  }

  public final void setPeppolAS2Version (@Nonnull final EPeppolAS2Version eAS2Version)
  {
    ValueEnforcer.notNull (eAS2Version, "AS2Version");
    m_eAS2Version = eAS2Version;
  }

  public boolean canHandle (@Nonnull final String sAction,
                            @Nonnull final IMessage aMsg,
                            @Nullable final Map <String, Object> aOptions)
  {
    // Using the store action, because this action is automatically called upon
    // receipt
    return IProcessorStorageModule.DO_STORE.equals (sAction) && aMsg instanceof AS2Message;
  }

  /**
   * @param sLogPrefix
   *        Log prefix
   * @param aRecipientID
   *        PEPPOL Recipient ID
   * @param aDocTypeID
   *        PEPPOL document type ID
   * @param aProcessID
   *        PEPPOL process ID
   * @return The access point URL to be used or <code>null</code>
   * @throws AS2Exception
   *         In case the endpoint address could not be resolved.
   */
  @Nullable
  private EndpointType _getReceiverEndpoint (@Nonnull final String sLogPrefix,
                                             @Nullable final IParticipantIdentifier aRecipientID,
                                             @Nullable final IDocumentTypeIdentifier aDocTypeID,
                                             @Nullable final IProcessIdentifier aProcessID) throws AS2Exception
  {
    // Get configured client
    final ISMPServiceMetadataProvider aSMPClient = AS2PeppolServletConfiguration.getSMPClient ();
    if (aSMPClient == null)
      throw new AS2Exception (sLogPrefix + "No SMP client configured!");

    if (aRecipientID == null || aDocTypeID == null || aProcessID == null)
      return null;

    try
    {
      if (LOGGER.isDebugEnabled ())
      {
        LOGGER.debug (sLogPrefix +
                      "Looking up the endpoint of recipient " +
                      aRecipientID.getURIEncoded () +
                      " for " +
                      aDocTypeID.getURIEncoded () +
                      " and " +
                      aProcessID.getURIEncoded () +
                      " and " +
                      m_eAS2Version.getTransportProfile ());
      }

      // Query the SMP
      return aSMPClient.getEndpoint (aRecipientID, aDocTypeID, aProcessID, m_eAS2Version.getTransportProfile ());
    }
    catch (final Throwable t)
    {
      throw new AS2Exception (sLogPrefix + "Failed to retrieve endpoint of recipient " + aRecipientID.getURIEncoded (),
                              t);
    }
  }

  private static void _checkIfReceiverEndpointURLMatches (@Nonnull final String sLogPrefix,
                                                          @Nonnull final EndpointType aRecipientEndpoint) throws AS2Exception
  {
    // Get our public endpoint address from the configuration
    final String sOwnAPUrl = AS2PeppolServletConfiguration.getAS2EndpointURL ();
    if (StringHelper.hasNoText (sOwnAPUrl))
      throw new AS2Exception (sLogPrefix + "The endpoint URL of this AP is not configured!");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Our AP URL is " + sOwnAPUrl);

    final String sRecipientAPUrl = SMPClientReadOnly.getEndpointAddress (aRecipientEndpoint);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Recipient AP URL is " + sRecipientAPUrl);

    // Is it for us?
    if (sRecipientAPUrl == null || !sRecipientAPUrl.contains (sOwnAPUrl))
    {
      final String sErrorMsg = sLogPrefix +
                               " Internal error: The request is targeted for '" +
                               sRecipientAPUrl +
                               "' and is not for us (" +
                               sOwnAPUrl +
                               ")";
      LOGGER.error (sErrorMsg);
      throw new AS2Exception (sErrorMsg);
    }
  }

  private static void _checkIfEndpointCertificateMatches (@Nonnull final String sLogPrefix,
                                                          @Nonnull final EndpointType aRecipientEndpoint) throws AS2Exception
  {
    final X509Certificate aOurCert = AS2PeppolServletConfiguration.getAPCertificate ();
    if (aOurCert == null)
      throw new AS2Exception (sLogPrefix + "The certificate of this AP is not configured!");

    final String sRecipientCertString = aRecipientEndpoint.getCertificate ();
    X509Certificate aRecipientCert = null;
    try
    {
      aRecipientCert = CertificateHelper.convertStringToCertficate (sRecipientCertString);
    }
    catch (final CertificateException t)
    {
      throw new AS2Exception (sLogPrefix +
                              "Internal error: Failed to convert looked up endpoint certificate string '" +
                              sRecipientCertString +
                              "' to an X.509 certificate!",
                              t);
    }

    if (aRecipientCert == null)
    {
      // No certificate found - most likely because of invalid SMP entry
      throw new AS2Exception (sLogPrefix +
                              "No certificate found in looked up endpoint! Is this AP maybe NOT contained in an SMP?");
    }

    // Certificate found
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Conformant recipient certificate present: " + aRecipientCert.toString ());

    // Compare serial numbers
    if (!aOurCert.getSerialNumber ().equals (aRecipientCert.getSerialNumber ()))
    {
      final String sErrorMsg = sLogPrefix +
                               "Certificate retrieved from SMP lookup (" +
                               aRecipientCert +
                               ") does not match this APs configured Certificate (" +
                               aOurCert +
                               ") - different serial numbers - ignoring document";
      LOGGER.error (sErrorMsg);
      throw new AS2Exception (sErrorMsg);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "The certificate of the SMP lookup matches our certificate");
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws AS2Exception
  {
    try
    {
      // Set the signing algorithm, so that the MIC calculation is done
      // correctly
      aMsg.partnership ().setSigningAlgorithm (m_eAS2Version.getCryptoAlgorithmSign ());
      aMsg.partnership ().setVerifyUseCertificateInBodyPart (ETriState.TRUE);

      // Interpret content as SBD
      final StandardBusinessDocument aSBD = new SBDMarshaller ().read (aMsg.getData ().getInputStream ());
      if (aSBD == null)
        throw new IllegalArgumentException ("Failed to interpret the passed document as a Standard Business Document!");

      if (AS2PeppolServletConfiguration.isReceiverCheckEnabled ())
      {
        final PeppolSBDHDocument aDD = new PeppolSBDHDocumentReader ().extractData (aSBD);
        final String sLogPrefix = "[" + aDD.getInstanceIdentifier () + "] ";

        // Get the endpoint information required from the recipient
        final EndpointType aReceiverEndpoint = _getReceiverEndpoint (sLogPrefix,
                                                                     aDD.getReceiverAsIdentifier (),
                                                                     aDD.getDocumentTypeAsIdentifier (),
                                                                     aDD.getProcessAsIdentifier ());

        if (aReceiverEndpoint == null)
        {
          throw new AS2Exception (sLogPrefix +
                                  "Failed to resolve endpoint for provided receiver/documentType/process - not handling document");
        }
        // Check if the message is for us
        _checkIfReceiverEndpointURLMatches (sLogPrefix, aReceiverEndpoint);

        // Get the recipient certificate from the SMP
        _checkIfEndpointCertificateMatches (sLogPrefix, aReceiverEndpoint);
      }
      else
      {
        LOGGER.info ("Endpoint checks for the AS2 AP are disabled");
      }

      // Handle incoming document via SPI
      final HttpHeaderMap aHeaders = aMsg.headers ().getClone ();
      for (final IAS2IncomingSBDHandlerSPI aHandler : m_aHandlers)
        aHandler.handleIncomingSBD (aHeaders, aSBD);
    }
    catch (final Exception ex)
    {
      // Something went wrong
      throw WrappedAS2Exception.wrap (ex);
    }
  }
}
