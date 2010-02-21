/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Put page number
 * in the footer.
 */
class SimpleFooterHandler extends PdfPageEventHelper {

  private PdfTemplate _tpl;

  private BaseFont _headerFooterFont;

  private static final Font TIMES_12PT_NORMAL = FontFactory.getFont(FontFactory.TIMES, 12, Font.NORMAL);

  public SimpleFooterHandler() {
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
