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

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import com.helger.peppol.utils.EPeppolCertificateCheckResult;

/**
 * Interface for handling certification validations results
 *
 * @author Philip Helger
 * @since 3.1.0
 */
public interface IAS2ClientBuilderCertificateCheckResultHandler extends Serializable
{
  /**
   * Invoked after certificate check.
   *
   * @param aAPCertificate
   *        The AP certificate that was checked. Never <code>null</code>.
   * @param aCheckDT
   *        The date and time that was used to check the certificate. Never
   *        <code>null</code>.
   * @param eCertCheckResult
   *        The result of the certificate check. Never <code>null</code>.
   * @throws AS2ClientBuilderException
   *         Implementation dependent
   */
  void onCertificateCheckResult (@Nonnull X509Certificate aAPCertificate,
                                 @Nonnull LocalDateTime aCheckDT,
                                 @Nonnull EPeppolCertificateCheckResult eCertCheckResult) throws AS2ClientBuilderException;
}