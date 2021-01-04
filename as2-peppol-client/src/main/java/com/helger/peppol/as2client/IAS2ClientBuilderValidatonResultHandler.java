/**
 * Copyright (C) 2014-2021 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import com.helger.phive.api.result.ValidationResultList;

/**
 * Interface for handling validation errors
 *
 * @author Philip Helger
 * @since 3.0.7
 */
public interface IAS2ClientBuilderValidatonResultHandler extends Serializable
{
  /**
   * Invoked, if no validation error is present. This method is invoked if only
   * warnings are present.
   *
   * @param aValidationResult
   *        The full validation results. Never <code>null</code>.
   * @throws AS2ClientBuilderException
   *         Implementation dependent
   */
  default void onValidationSuccess (@Nonnull final ValidationResultList aValidationResult) throws AS2ClientBuilderException
  {
    // empty
  }

  /**
   * Invoked, if at least one validation error is present.
   *
   * @param aValidationResult
   *        The full validation results. Never <code>null</code>.
   * @throws AS2ClientBuilderException
   *         Implementation dependent
   */
  default void onValidationErrors (@Nonnull final ValidationResultList aValidationResult) throws AS2ClientBuilderException
  {
    throw new AS2ClientBuilderValidationException (aValidationResult);
  }
}
