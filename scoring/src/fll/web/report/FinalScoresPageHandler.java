/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Put the challenge title, tournament and division in header. Put page number
 * in the footer.
 */
class FinalScoresPageHandler extends PdfPageEventHelper {

  private PdfTemplate _tpl;

  private BaseFont _headerFooterFont;

  private PdfPTable _header;

  private final String _tournamentName;

  private final String _challengeTitle;

  private String _division;

  public String getDivision() {
    return _division;
  }

  public void setDivision(final String division) {
    this._division = division;
  }

  private static final Font TIMES_12PT_NORMAL = FontFactory.getFont(FontFactory.TIMES, 12, Font.NORMAL);

  public FinalScoresPageHandler(final String challengeTitle, final String tournamentName) {
    _challengeTitle = challengeTitle;
    _tournamentName = tournamentName;
  }

  private void updateHeader() {
    // initialization of the header table
    _header = new PdfPTable(2);
    final Phrase p = new Phrase();
    final Chunk ck = new Chunk(_challengeTitle
        + "\nFinal Computed Scores", TIMES_12PT_NORMAL);
    p.add(ck);
    _header.getDefaultCell().setBorderWidth(0);
    _header.addCell(p);
    _header.getDefaultCell().setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
    _header.addCell(new Phrase(new Chunk("Tournament: "
        + _tournamentName + "\nDivision: " + _division, TIMES_12PT_NORMAL)));
    final PdfPCell blankCell = new PdfPCell();
    blankCell.setBorder(0);
    blankCell.setBorderWidthTop(1.0f);
    blankCell.setColspan(2);
    _header.addCell(blankCell);
  }

  @Override
  public void onOpenDocument(final PdfWriter writer, final Document document) {
    _headerFooterFont = TIMES_12PT_NORMAL.getBaseFont();

    // initialization of the footer template
    _tpl = writer.getDirectContent().createTemplate(100, 100);
    _tpl.setBoundingBox(new Rectangle(-20, -20, 100, 100));
  }

  @Override
  public void onEndPage(final PdfWriter writer, final Document document) {
    final PdfContentByte cb = writer.getDirectContent();
    cb.saveState();
    // write the headertable
    updateHeader(); // creates the header table with current
    // division, etc.
    _header.setTotalWidth(document.right()
        - document.left());
    _header.writeSelectedRows(0, -1, document.left(), document.getPageSize().getHeight() - 10, cb);

    // compose the footer
    final String text = "Page "
        + writer.getPageNumber() + " of ";
    final float textSize = _headerFooterFont.getWidthPoint(text, 12);
    final float textBase = document.bottom() - 20;
    cb.beginText();
    cb.setFontAndSize(_headerFooterFont, 12);

    final float adjust = _headerFooterFont.getWidthPoint("0", 12);
    cb.setTextMatrix(document.right()
        - textSize - adjust, textBase);
    cb.showText(text);
    cb.endText();
    cb.addTemplate(_tpl, document.right()
        - adjust, textBase);

    cb.restoreState();
  }

  @Override
  public void onCloseDocument(final PdfWriter writer, final Document document) {
    _tpl.beginText();
    _tpl.setFontAndSize(_headerFooterFont, 12);
    _tpl.setTextMatrix(0, 0);
    _tpl.showText(""
        + (writer.getPageNumber() - 1));
    _tpl.endText();
  }

}
