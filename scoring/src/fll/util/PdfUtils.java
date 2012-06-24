/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;


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
  
}
