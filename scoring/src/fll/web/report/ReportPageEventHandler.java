/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.text.DateFormat;
import java.util.Date;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Generate a report footer with the name of the report, challenge, tournament
 * and current date.
 */
public final class ReportPageEventHandler extends PdfPageEventHelper {
  /**
   * @param font font to use for the footer
   * @param reportTitle title of the report
   * @param challengeTitle title of the challenge
   * @param tournament the tournament name
   */
  public ReportPageEventHandler(final Font font,
                                final String reportTitle,
                                final String challengeTitle,
                                final String tournament) {
    _font = font;
    _reportTitle = reportTitle;
    _tournament = tournament;
    _challengeTitle = challengeTitle;
    _formattedDate = DateFormat.getDateInstance().format(new Date());
  }

  private final String _reportTitle;

  private final String _formattedDate;

  private final String _tournament;

  private final String _challengeTitle;

  private final Font _font;

  @Override
  // initialization of the header table
  public void onEndPage(final PdfWriter writer,
                        final Document document) {
    final PdfPTable header = new PdfPTable(2);
    final Phrase p = new Phrase();
    final Chunk ck = new Chunk(_challengeTitle
        + "\n" + _reportTitle, _font);
    p.add(ck);
    header.getDefaultCell().setBorderWidth(0);
    header.addCell(p);
    header.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
    header.addCell(new Phrase(new Chunk("Tournament: "
        + _tournament + "\nDate: " + _formattedDate, _font)));
    final PdfPCell blankCell = new PdfPCell();
    blankCell.setBorder(0);
    blankCell.setBorderWidthTop(1.0f);
    blankCell.setColspan(2);
    header.addCell(blankCell);

    final PdfContentByte cb = writer.getDirectContent();
    cb.saveState();
    header.setTotalWidth(document.right()
        - document.left());
    header.writeSelectedRows(0, -1, document.left(), document.getPageSize().getHeight() - 10, cb);
    cb.restoreState();
  }

}