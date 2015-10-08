package com.helger.peppol.as2client;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

/**
 * Common functionality for AS2 clients
 *
 * @author Philip Helger
 */
@Immutable
public final class AS2ClientHelper
{
  private AS2ClientHelper ()
  {}

  /**
   * @param aCert
   *        Source certificate. May not be <code>null</code>.
   * @return The common name of the certificate subject
   * @throws CertificateEncodingException
   *         In case of an internal error
   */
  @Nonnull
  public static String getSubjectCommonName (@Nonnull final X509Certificate aCert) throws CertificateEncodingException
  {
    final X500Name x500name = new JcaX509CertificateHolder (aCert).getSubject ();
    final RDN cn = x500name.getRDNs (BCStyle.CN)[0];
    return IETFUtils.valueToString (cn.getFirst ().getValue ());
  }
}
