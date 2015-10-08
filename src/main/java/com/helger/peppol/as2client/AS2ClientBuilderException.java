package com.helger.peppol.as2client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Exception to be thrown from the {@link AS2ClientBuilder}.
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
