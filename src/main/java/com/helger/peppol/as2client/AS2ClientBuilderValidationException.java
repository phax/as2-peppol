package com.helger.peppol.as2client;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.peppol.validation.ValidationLayerResultList;

/**
 * Special {@link AS2ClientBuilderException} exception for validation errors.
 * 
 * @author Philip Helger
 */
public class AS2ClientBuilderValidationException extends AS2ClientBuilderException
{
  private final ValidationLayerResultList m_aValidationResult;

  public AS2ClientBuilderValidationException (@Nonnull final ValidationLayerResultList aValidationResult)
  {
    super ("Error validating business document");
    m_aValidationResult = ValueEnforcer.notNull (aValidationResult, "ValidationResult");
  }

  @Nonnull
  public ValidationLayerResultList getValidationResult ()
  {
    return m_aValidationResult;
  }
}
