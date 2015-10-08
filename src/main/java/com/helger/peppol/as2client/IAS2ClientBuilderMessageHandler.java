package com.helger.peppol.as2client;

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
}
