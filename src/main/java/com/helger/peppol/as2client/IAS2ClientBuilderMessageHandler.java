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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface for warnings and errors emitted by the {@link AS2ClientBuilder}
 *
 * @author Philip Helger
 */
public interface IAS2ClientBuilderMessageHandler
{
  /**
   * Emit a warning
   *
   * @param sMessage
   *        The warning message. May not be <code>null</code>.
   */
  void warn (@Nonnull String sMessage);

  /**
   * @return The number of warnings logged in this handler. Must be &ge; 0.
   */
  @Nonnegative
  int getWarnCount ();

  /**
   * Emit an error
   *
   * @param sMessage
   *        The error message. May not be <code>null</code>.
   * @throws AS2ClientBuilderException
   *         In case the implementation wants to throw an exception
   */
  void error (@Nonnull String sMessage) throws AS2ClientBuilderException;

  /**
   * Emit an error
   *
   * @param sMessage
   *        The error message. May not be <code>null</code>.
   * @param aCause
   *        An optional exception of any type. May be <code>null</code>.
   * @throws AS2ClientBuilderException
   *         In case the implementation wants to throw an exception
   */
  void error (@Nonnull String sMessage, @Nullable Throwable aCause) throws AS2ClientBuilderException;

  /**
   * @return The number of errors logged in this handler. Must be &ge; 0.
   */
  @Nonnegative
  int getErrorCount ();
}
