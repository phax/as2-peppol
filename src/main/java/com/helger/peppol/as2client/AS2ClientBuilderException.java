/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
import javax.annotation.Nullable;

/**
 * Generic exception to be thrown from the {@link AS2ClientBuilder}.
 *
 * @author Philip Helger
 */
public class AS2ClientBuilderException extends Exception
{
  /**
   * @param sMessage
   *        Error message
   */
  public AS2ClientBuilderException (@Nonnull final String sMessage)
  {
    super (sMessage);
  }

  /**
   * @param sMessage
   *        Error message
   * @param aCause
   *        Optional causing exception
   */
  public AS2ClientBuilderException (@Nonnull final String sMessage, @Nullable final Throwable aCause)
  {
    super (sMessage, aCause);
  }
}
