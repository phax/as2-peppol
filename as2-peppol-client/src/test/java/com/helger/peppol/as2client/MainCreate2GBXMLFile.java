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
package com.helger.peppol.as2client;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.CGlobal;
import com.helger.commons.io.EAppend;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.string.StringHelper;
import com.helger.ubl21.UBL21Reader;
import com.helger.ubl21.UBL21Writer;

import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

/**
 * A dummy class that uses an existing invoice, bloats it to 2 GB and writes it
 * back to disk.
 *
 * @author Philip Helger
 */
public class MainCreate2GBXMLFile
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainCreate2GBXMLFile.class);

  public static void main (final String [] args) throws IOException
  {
    LOGGER.info ("Reading");
    final InvoiceType aInvoice = UBL21Reader.invoice ().read (new FileSystemResource ("src/test/resources/xml/at-gov-peppol-ubl.xml"));

    final String sNote = StringHelper.getRepeated ("Lorem ipsum - who cares", 1024);
    final long nNotes = 2 * CGlobal.BYTES_PER_GIGABYTE / sNote.length ();
    LOGGER.info ("Adding " + nNotes + " notes");
    for (long i = 0; i < nNotes; ++i)
      aInvoice.addNote (new NoteType (sNote));
    LOGGER.info ("Writing");
    UBL21Writer.invoice ()
               .write (aInvoice,
                       new GZIPOutputStream (new FileSystemResource ("src/test/resources/xml/at-gov-peppol-ubl-2gb.gz").getOutputStream (EAppend.TRUNCATE)));
    LOGGER.info ("Done");
  }
}
