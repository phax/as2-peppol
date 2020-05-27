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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;

/**
 * Special enum for all PEPPOL AS2 versions
 *
 * @author Philip Helger
 */
public enum EPeppolAS2Version implements IHasID <String>
{
  /**
   * The PEPPOL AS2 transport profile v1 (SHA-1). Deprecated since 2020-02-01
   */
  @Deprecated
  V1 ("v1", ESMPTransportProfile.TRANSPORT_PROFILE_AS2, ECryptoAlgorithmSign.DIGEST_SHA1),

  /**
   * The PEPPOL AS2 v2 transport profile v2 (SHA-256). Mandatory (when using
   * AS2) since 2020-02-01.
   */
  V2 ("v2", ESMPTransportProfile.TRANSPORT_PROFILE_AS2_V2, ECryptoAlgorithmSign.DIGEST_SHA_256);

  private final String m_sID;
  private final ISMPTransportProfile m_aTP;
  private final ECryptoAlgorithmSign m_eSigningAlgo;

  private EPeppolAS2Version (@Nonnull @Nonempty final String sID,
                             @Nonnull final ISMPTransportProfile eTP,
                             @Nonnull final ECryptoAlgorithmSign eSigningAlgo)
  {
    m_sID = sID;
    m_aTP = eTP;
    m_eSigningAlgo = eSigningAlgo;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public ISMPTransportProfile getTransportProfile ()
  {
    return m_aTP;
  }

  @Nonnull
  public ECryptoAlgorithmSign getCryptoAlgorithmSign ()
  {
    return m_eSigningAlgo;
  }

  /**
   * Find the pre-defined transport profile with the passed ID.
   *
   * @param sID
   *        The ID to be searched. May be <code>null</code>.
   * @return <code>null</code> if no AS2 version was found.
   */
  @Nullable
  public static EPeppolAS2Version getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPeppolAS2Version.class, sID);
  }
}
