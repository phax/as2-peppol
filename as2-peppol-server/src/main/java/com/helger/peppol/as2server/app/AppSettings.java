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
package com.helger.peppol.as2server.app;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.string.StringHelper;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.security.keystore.EKeyStoreType;

/**
 * This class provides access to the web application settings.
 *
 * @author Philip Helger
 */
public final class AppSettings extends AbstractGlobalSingleton
{
  public static final String KEY_FOLDER_SENDING = "folder.sending";
  public static final String KEY_FOLDER_SENDING_ERROR = "folder.sending.error";
  public static final String KEY_FOLDER_RECEIVING = "folder.receiving";
  public static final String KEY_FOLDER_RECEIVING_ERROR = "folder.receiving.error";
  public static final String KEY_KEYSTORE_TYPE = "keystore.type";
  public static final String KEY_KEYSTORE_PATH = "keystore.path";
  public static final String KEY_KEYSTORE_PASSWORD = "keystore.password";
  public static final String KEY_KEYSTORE_KEY_ALIAS = "keystore.key.alias";
  public static final String KEY_KEYSTORE_KEY_PASSWORD = "keystore.key.password";
  public static final String KEY_TRUSTSTORE_TYPE = "truststore.type";
  public static final String KEY_TRUSTSTORE_PATH = "truststore.path";
  public static final String KEY_TRUSTSTORE_PASSWORD = "truststore.password";

  @Deprecated
  @UsedViaReflection
  private AppSettings ()
  {}

  @Nonnull
  public static IConfig getConfig ()
  {
    return ConfigFactory.getDefaultConfig ();
  }

  /**
   * @return The SML to use for the SMP client lookup. Never <code>null</code>.
   *         If an invalid value is specified, the production SML is used.
   */
  @Nonnull
  public static ISMLInfo getSMLToUse ()
  {
    final String sSMLID = getConfig ().getAsString ("sml.id");
    return ESML.getFromIDOrDefault (sSMLID, ESML.DIGIT_PRODUCTION);
  }

  @Nullable
  private static File _getAsFile (@Nullable final String sPath)
  {
    if (StringHelper.hasNoText (sPath))
      return null;
    try
    {
      return new File (sPath).getAbsoluteFile ().getCanonicalFile ();
    }
    catch (final IOException ex)
    {
      throw new UncheckedIOException ("Error in getCanonicalFile", ex);
    }
  }

  /**
   * @return The absolute folder/directory path from which sending should occur.
   *         May be <code>null</code> in which case startup should fail.
   */
  @Nullable
  public static File getFolderForSending ()
  {
    return _getAsFile (getConfig ().getAsString (KEY_FOLDER_SENDING));
  }

  /**
   * @return The absolute folder/directory path in which failed sending files
   *         are moved. May be <code>null</code> in which case startup should
   *         fail.
   */
  @Nullable
  public static File getFolderForSendingErrors ()
  {
    return _getAsFile (getConfig ().getAsString (KEY_FOLDER_SENDING_ERROR));
  }

  /**
   * @return The absolute folder/directory path in which received files are
   *         stored. May be <code>null</code> in which case startup should fail.
   */
  @Nullable
  public static File getFolderForReceiving ()
  {
    return _getAsFile (getConfig ().getAsString (KEY_FOLDER_RECEIVING));
  }

  /**
   * @return The absolute folder/directory path in which failed received files
   *         are stored. May be <code>null</code> in which case startup should
   *         fail.
   */
  @Nullable
  public static File getFolderForReceivingErrors ()
  {
    return _getAsFile (getConfig ().getAsString (KEY_FOLDER_RECEIVING_ERROR));
  }

  /**
   * @return The type to the keystore. This is usually JKS. Property
   *         <code>keystore.type</code>.
   */
  @Nonnull
  public static EKeyStoreType getKeyStoreType ()
  {
    final String sType = getConfig ().getAsString (KEY_KEYSTORE_TYPE);
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, EKeyStoreType.JKS);
  }

  /**
   * @return The path to the keystore. May be a classpath or an absolute file
   *         path. Property <code>keystore.path</code>.
   */
  @Nullable
  public static String getKeyStorePath ()
  {
    return getConfig ().getAsString (KEY_KEYSTORE_PATH);
  }

  /**
   * @return The password required to open the keystore. Property
   *         <code>keystore.password</code>.
   */
  @Nullable
  public static String getKeyStorePassword ()
  {
    return getConfig ().getAsString (KEY_KEYSTORE_PASSWORD);
  }

  /**
   * @return The alias of the SMP key in the keystore. Property
   *         <code>keystore.key.alias</code>.
   */
  @Nullable
  public static String getKeyStoreKeyAlias ()
  {
    return getConfig ().getAsString (KEY_KEYSTORE_KEY_ALIAS);
  }

  /**
   * @return The password used to access the private key. May be different than
   *         the password to the overall keystore. Property
   *         <code>keystore.key.password</code>.
   */
  @Nullable
  public static char [] getKeyStoreKeyPassword ()
  {
    return getConfig ().getAsCharArray (KEY_KEYSTORE_KEY_PASSWORD);
  }

  /**
   * @return The type to the truststore. This is usually JKS. Property
   *         <code>truststore.type</code>.
   */
  @Nonnull
  public static EKeyStoreType getTrustStoreType ()
  {
    final String sType = getConfig ().getAsString (KEY_TRUSTSTORE_TYPE);
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sType, PeppolKeyStoreHelper.TRUSTSTORE_TYPE);
  }

  /**
   * @return The path to the truststore. May be a classpath or an absolute file
   *         path. Property <code>truststore.path</code>.
   */
  @Nonnull
  @Nonempty
  public static String getTrustStorePath ()
  {
    return getConfig ().getAsString (KEY_TRUSTSTORE_PATH, PeppolKeyStoreHelper.TRUSTSTORE_COMPLETE_CLASSPATH);
  }

  /**
   * @return The password required to open the truststore. Property
   *         <code>truststore.password</code>.
   */
  @Nonnull
  @Nonempty
  public static String getTrustStorePassword ()
  {
    return getConfig ().getAsString (KEY_TRUSTSTORE_PASSWORD, PeppolKeyStoreHelper.TRUSTSTORE_PASSWORD);
  }
}
