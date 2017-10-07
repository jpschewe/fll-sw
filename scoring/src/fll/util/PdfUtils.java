/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEvent;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Utilities for dealing with PDFs.
 */
public final class PdfUtils {

  private static final Logger LOGGER = LogUtils.getLogger();

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
                                    final BaseColor backgroundColor)
      throws BadElementException {
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
   * Create a simple PDF document using portrait letter orientation.
   * The document is opened by this method.
   */
  public static Document createPortraitPdfDoc(final OutputStream out,
                                              final PdfPageEvent pageHandler)
      throws DocumentException {
    final Document pdfDoc = new Document(PageSize.LETTER);
    commonPdfDocCreate(out, pageHandler, pdfDoc);

    return pdfDoc;
  }

  /**
   * Create a simple PDF document using landscape letter orientation.
   * The document is opened by this method.
   */
  public static Document createLandscapePdfDoc(final OutputStream out,
                                               final PdfPageEvent pageHandler)
      throws DocumentException {
    final Document pdfDoc = new Document(PageSize.LETTER.rotate());
    commonPdfDocCreate(out, pageHandler, pdfDoc);

    return pdfDoc;
  }

  /**
   * Common code for document creation. Sets margins and page event handler.
   * This method opens the document.
   * 
   * @throws DocumentException
   */
  private static void commonPdfDocCreate(final OutputStream out,
                                         final PdfPageEvent pageHandler,
                                         final Document pdfDoc)
      throws DocumentException {
    final PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
    writer.setPageEvent(pageHandler);

    // Measurements are always in points (72 per inch) - This sets up 1/2 inch
    // margins
    pdfDoc.setMargins(0.5f
        * 72, 0.5f
            * 72, 0.5f
                * 72, 0.5f
                    * 72);

    pdfDoc.open();
  }

  /**
   * This class can be used as a cell event to truncate text that is too large for
   * a table column.
   * If this class is used to define all cells in a row or a column of a table the
   * data will not appear. This is likely because the cell has no width or height
   * and therefore an auto-sized table assumes that the width of the column is
   * zero or the height of the row is zero.
   * Based on the example at
   * https://developers.itextpdf.com/examples/tables-itext5/fit-text-cell.
   * Use this class by calling
   * <code>cell.setCellEvent(new TruncateContent(...))</code>
   * 
   * @see PdfPCell#setCellEvent(PdfPCellEvent)
   */
  public static final class TruncateContent implements PdfPCellEvent {
    private final String content;

    private final Font font;

    /**
     * Create an object that will display the text with the specified font and
     * truncate the text if needed.
     * 
     * @param content what to display
     * @param font the font to use
     */
    public TruncateContent(final String content,
                           final Font font) {
      this.content = content;
      this.font = font;
    }

    public void cellLayout(final PdfPCell cell,
                           final Rectangle position,
                           final PdfContentByte[] canvases) {
      try {
        final BaseFont bf = font.getCalculatedBaseFont(false);
        float availableWidth = position.getWidth();
        float contentWidth = bf.getWidthPoint(content, font.getSize());

        final String newContent;
        if (contentWidth > availableWidth) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Need to truncate '"
                + content
                + "'");
          }

          final int contentLength = content.length();
          int leftChar = 0;
          availableWidth -= bf.getWidthPoint("...", font.getSize());
          while (leftChar < contentLength) {
            availableWidth -= bf.getWidthPoint(content.charAt(leftChar), font.getSize());
            if (availableWidth > 0) {
              leftChar++;
            } else {
              break;
            }
          }
          newContent = content.substring(0, leftChar)
              + "...";
        } else {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Not truncating '"
                + content
                + "'");
          }

          newContent = content;
        }
        final PdfContentByte canvas = canvases[PdfPTable.TEXTCANVAS];
        final ColumnText ct = new ColumnText(canvas);
        ct.setSimpleColumn(position);
        ct.addElement(new Paragraph(newContent, font));
        ct.go();
      } catch (final DocumentException e) {
        throw new ExceptionConverter(e);
      }
    }

  } // class TruncateContent

}
