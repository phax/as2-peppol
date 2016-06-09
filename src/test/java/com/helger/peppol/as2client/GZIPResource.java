package com.helger.peppol.as2client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.resource.IReadableResource;

public class GZIPResource implements IReadableResource
{
  private final IReadableResource m_aBaseResource;

  public GZIPResource (@Nonnull final IReadableResource aBaseResource)
  {
    m_aBaseResource = ValueEnforcer.notNull (aBaseResource, "BaseResource");
  }

  @Nullable
  public InputStream getInputStream ()
  {
    final InputStream aIS = m_aBaseResource.getInputStream ();
    if (aIS == null)
      return null;
    try
    {
      return new GZIPInputStream (aIS);
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open GZIP InputStream", ex);
    }
  }

  @Nonnull
  public String getResourceID ()
  {
    return m_aBaseResource.getResourceID ();
  }

  @Nonnull
  public String getPath ()
  {
    return m_aBaseResource.getPath ();
  }

  public boolean exists ()
  {
    return m_aBaseResource.exists ();
  }

  @Nullable
  public URL getAsURL ()
  {
    return m_aBaseResource.getAsURL ();
  }

  @Nullable
  public File getAsFile ()
  {
    return m_aBaseResource.getAsFile ();
  }

  @Nonnull
  public IReadableResource getReadableCloneForPath (@Nonnull final String sPath)
  {
    return new GZIPResource (m_aBaseResource.getReadableCloneForPath (sPath));
  }
}
