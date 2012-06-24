/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.OutputStream;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEvent;
import com.itextpdf.text.pdf.PdfWriter;


/**
 * Utilities for dealing with PDFs.
 */
public final class PdfUtils {

  private PdfUtils() {
    // no instances
  }

  /**
   * Create a table cell from the specified {@link Chunk}.
   */
  public static PdfPCell createBasicCell(final Chunk chunk) throws BadElementException {
    final PdfPCell cell = new PdfPCell(new Phrase(chunk));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setUseDescender(true);
    return cell;
  }

  /**
   * Create a header cell for a table.
   */
  public static PdfPCell createHeaderCell(final String text) throws BadElementException {
    final Chunk chunk = new Chunk(text);
    chunk.getFont().setStyle(Font.BOLD);
    final PdfPCell cell = createBasicCell(chunk);
  
    return cell;
  }

  /**
   * Create a table with the specified number of columns that is the full page
   * width.
   */
  public static PdfPTable createTable(final int columns) throws BadElementException {
    final PdfPTable table = new PdfPTable(columns);
    // table.setCellsFitPage(true);
    table.setWidthPercentage(100);
    return table;
  }

  /**
   * Create a table cell with the specified text and background color.
   */
  public static PdfPCell createCell(final String text,
                                    final BaseColor backgroundColor) throws BadElementException {
    final PdfPCell cell = PdfUtils.createCell(text);
    if (null != backgroundColor) {
      cell.setBackgroundColor(backgroundColor);
    }
    return cell;
  }

  /**
   * Create a table cell with the specified text.
   */
  public static PdfPCell createCell(final String text) throws BadElementException {
    final PdfPCell cell = createBasicCell(new Chunk(text));
    return cell;
  }

  /**
   * Create a simple PDF document using letter orientation.
   * The document is opened by this method.
   */
  public static Document createPdfDoc(final OutputStream out,
                                final PdfPageEvent pageHandler) throws DocumentException {
    final Document pdfDoc = new Document(PageSize.LETTER);
    final PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
    writer.setPageEvent(pageHandler);
  
    // Measurements are always in points (72 per inch) - This sets up 1/2 inch
    // margins
    pdfDoc.setMargins(0.5f * 72, 0.5f * 72, 0.5f * 72, 0.5f * 72);
    
    pdfDoc.open();
    
    return pdfDoc;
  }
  
}
