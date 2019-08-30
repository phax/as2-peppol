/**
 * Copyright (C) 2014-2019 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.processor.sender.AS2SenderModule;

/**
 * Specialized sender module for the client.
 *
 * @author Philip Helger
 */
public class PeppolAS2SenderModule extends AS2SenderModule
{
  @Override
  protected void onReceivedMDNError (@Nonnull final AS2Message aMsg,
                                     @Nonnull final OpenAS2Exception ex) throws OpenAS2Exception
  {
    final OpenAS2Exception oae2 = new OpenAS2Exception ("Message was sent but an error occured while receiving the MDN",
                                                        ex);
    oae2.setSourceMsg (aMsg);
    // Compared to the base implementation, this version propagates the message
    // to the outside
    throw oae2;
  }
}
