package fll.documents.writers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import fll.documents.elements.RowElement;
import fll.documents.elements.TableElement;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.LogUtils;
import fll.xml.RubricRange;
import fll.xml.ScoreCategory;

public class SubjectivePdfWriter {
  private static final Logger LOGGER = LogUtils.getLogger();

  private static Font COMMENTS_FONT = new Font(Font.FontFamily.HELVETICA, 7);

  private static Font RUBRIC_FONT = COMMENTS_FONT;

  private static final String copyRightStatement = "2011 The United States Foundation for Inspiration and Recognition of Science and Technology (FIRST) and The LEGO Group. Used by special permission. All rights reserved.";

  private final static int NO_BORDERS = 0;

  private final static int NO_LEFT_RIGHT = 1;

  private final static int NO_TOP_BOTTOM = 2;

  private final static int NO_LEFT = 3;

  private final static int NO_TOP = 4;

  private final static int NO_TOP_LEFT = 5;

  private final static int TOP_ONLY = 6;

  private static final int[] colWidths = { 4, 4, 23, 23, 23, 23 };

  private static final BaseColor rowBlue = new BaseColor(0xB2, 0xCB, 0xE3);

  private static final BaseColor rowYellow = new BaseColor(0xEE, 0xF1, 0x97);

  private static final BaseColor rowRed = new BaseColor(0xE6, 0xA7, 0xA7);

  private final Font f6i;

  private final Font f8b;

  private final Font f9b;

  private final Font f10b;

  private final Font f12b;

  private final Font f20b;

  private final BaseColor sheetColor;

  private final ScoreCategory scoreCategory;

  private final String scheduleColumn;

  public SubjectivePdfWriter(final ScoreCategory scoreCategory,
                             final String scheduleColumn) {
    this.scoreCategory = scoreCategory;
    this.scheduleColumn = scheduleColumn;

    f6i = new Font(Font.FontFamily.HELVETICA, 6, Font.ITALIC);
    f8b = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
    f9b = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
    f10b = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    f12b = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    f20b = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);

    switch (scoreCategory.getName()) {
    case SubjectiveConstants.CORE_VALUES_NAME:
      sheetColor = rowBlue;
      break;
    case SubjectiveConstants.PROJECT_NAME:
      sheetColor = rowYellow;
      break;
    case SubjectiveConstants.ROBOT_DESIGN_NAME:
    case SubjectiveConstants.PROGRAMMING_NAME:
    default:
      sheetColor = rowRed;
      break;
    }

  }

  public void writeHeader(Document doc,
                          TeamScheduleInfo teamInfo) throws MalformedURLException, IOException, DocumentException {
    Image image = null;
    PdfPTable pageHeaderTable = null;
    PdfPTable columnTitlesTable = null;
    PdfPCell headerCell = null;
    Paragraph directions = null;
    String dirText = null;
    Phrase text = null;

    // set up the header for proper spacing
    pageHeaderTable = new PdfPTable(4);
    pageHeaderTable.setSpacingAfter(5f);
    pageHeaderTable.setWidthPercentage(100f);
    pageHeaderTable.setSpacingBefore(0f);

    // get the FLL image to put on the document
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    final URL imageUrl = classLoader.getResource("fll/resources/documents/FLLHeader.png");
    image = Image.getInstance(imageUrl);

    // make it a little smaller
    image.scalePercent(85);

    // put the image in the header cell
    headerCell = new PdfPCell(image, false);
    headerCell.setRowspan(2);
    headerCell.setBorder(0);
    headerCell.setVerticalAlignment(Element.ALIGN_TOP);

    // put the rest of the header cells on the table
    pageHeaderTable.addCell(headerCell);
    pageHeaderTable.addCell(createCell(scoreCategory.getTitle(), f20b, NO_BORDERS, Element.ALIGN_LEFT));
    pageHeaderTable.addCell(createCell("Team Number: "
        + teamInfo.getTeamNumber(), f12b, NO_BORDERS, Element.ALIGN_LEFT));
    pageHeaderTable.addCell(createCell("Time: "
        + TournamentSchedule.OUTPUT_DATE_FORMAT.get()
                                               .format(teamInfo.getSubjectiveTimeByName(scheduleColumn).getTime()),
                                       f12b, NO_BORDERS, Element.ALIGN_RIGHT));
    pageHeaderTable.addCell(createCell("Judging Room: "
        + teamInfo.getDivision(), f10b, NO_BORDERS, Element.ALIGN_LEFT));
    PdfPCell c = createCell("Team Name: "
        + teamInfo.getTeamName(), f12b, NO_BORDERS, Element.ALIGN_LEFT);
    c.setColspan(2);
    c.setVerticalAlignment(Element.ALIGN_LEFT);
    pageHeaderTable.addCell(c);

    // add the instructions to the header
    dirText = "Directions: For each skill area, clearly mark the box that best describes the team's accomplishments.  "
        + "If the team does not demonstrate skill in a particular area, then put an 'X' in the first box for Not Demonstrated (ND).  "
        + "Please provide as many written comments as you can to acknowledge each teams's hard work and to help teams improve.";
    text = new Phrase(dirText, f9b);
    directions = new Paragraph();
    directions.add(text);
    directions.setLeading(10f);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TOP TITLE BAR START
    //
    // This is the top of the comment sheet where the titles from left to right
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////// are
    // Beginning, Developing, Accomplished, Exemplary
    // This is the same title bar across the tops of all the comment sheet
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////// tables.
    //

    columnTitlesTable = new PdfPTable(6);
    columnTitlesTable.setSpacingBefore(5);
    columnTitlesTable.setWidthPercentage(100f);
    columnTitlesTable.setWidths(colWidths);
    columnTitlesTable.addCell(createCell("", f10b, NO_BORDERS));
    columnTitlesTable.addCell(createCell("", f10b, NO_BORDERS));

    // FIXME get these off the sorted rubric ranges, need to check that all
    // goals have the same rubric titles
    columnTitlesTable.addCell(createCell("Beginning", f10b, NO_BORDERS));
    columnTitlesTable.addCell(createCell("Developing", f10b, NO_BORDERS));
    columnTitlesTable.addCell(createCell("Accomplished", f10b, NO_BORDERS));
    columnTitlesTable.addCell(createCell("Exemplary", f10b, NO_BORDERS));
    columnTitlesTable.setSpacingAfter(3);

    // add the header, instructions and section column titles to the document
    try {
      doc.add(pageHeaderTable);
      doc.add(directions);
      doc.add(columnTitlesTable);
    } catch (DocumentException de) {
      LOGGER.error("Unable to write out the document.", de);
    }
  }

  public void writeEndOfPageRow(Document doc) throws DocumentException {
    PdfPTable closingTable = new PdfPTable(1);

    closingTable.setWidthPercentage(100f);
    closingTable.addCell(createCell(" ", f6i, TOP_ONLY, sheetColor));
    closingTable.addCell(createCell(" ", f6i, NO_BORDERS));

    // add the copy right statement
    Paragraph copyRight = new Paragraph(new Phrase(copyRightStatement, f6i));
    copyRight.setLeading(1f);
    copyRight.setAlignment(Element.ALIGN_CENTER);

    doc.add(closingTable);
    doc.add(copyRight);
    doc.newPage();
  }

  public static Document createStandardDocument() {
    return new Document(PageSize.LETTER, 36, 36, 20, 36);
  }

  public PdfPTable createStandardRubricTable() throws DocumentException {
    PdfPTable table = new PdfPTable(6);
    table.setWidths(colWidths);
    table.setWidthPercentage(100f);
    return table;
  }

  public void writeCommentsSection(PdfPTable table) {
    PdfPCell commentLabel = null;
    PdfPCell emptySpace = null;
    final int height = 1;
    final Font font = COMMENTS_FONT;

    font.setStyle(Font.ITALIC);
    // This is the 'Comments' section at the bottom of every table for the judge
    // to write in
    commentLabel = createCell("Comments", font, NO_BORDERS);
    commentLabel.setRotation(90);
    commentLabel.setRowspan(1);
    commentLabel.setBorderWidthLeft(0);
    commentLabel.setBorderWidthBottom(0);
    commentLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
    commentLabel.setVerticalAlignment(Element.ALIGN_CENTER);
    emptySpace = createCell(" ", font, TOP_ONLY);
    table.addCell(commentLabel);
    // Need to add the empty cells so the row is complete and is displayed in
    // the pdf
    for (int i1 = 0; i1 < 5; i1++) {
      table.addCell(emptySpace);
    }

    emptySpace = createCell(" ", font, NO_BORDERS);
    for (int i2 = 0; i2 < height; i2++) {
      for (int i3 = 0; i3 < 6; i3++) {
        table.addCell(emptySpace);
      }
    }
  }

  public void writeRubricTable(PdfPTable table,
                               TableElement tableData) {
    PdfPCell focusArea = null;
    PdfPCell topicArea = null;
    PdfPCell topicInstructions = null;
    final Font font = RUBRIC_FONT;

    // This is the 90 degree turned title for the left side of the table
    focusArea = createCell(tableData.getSubjectiveCatetory(), f8b, NO_TOP_LEFT);
    focusArea.setRotation(90);
    // This is the total number of columns for this table. Each subsection of
    // the table is 2 rows (colored title row, description row)
    focusArea.setRowspan(tableData.getRowElements().size()
        * 2);
    table.addCell(focusArea);

    ArrayList<RowElement> rows = tableData.getRowElements();
    for (RowElement rowElement : rows) {
      // This is the title row with the background color
      topicArea = createCell(rowElement.getRowTitle(), f8b, NO_LEFT_RIGHT, sheetColor);
      topicArea.setColspan(2);
      topicInstructions = createCell(rowElement.getDescription(), font, NO_LEFT, sheetColor);
      topicInstructions.setColspan(3);

      // Add the title row to the table
      table.addCell(topicArea);
      table.addCell(topicInstructions);

      // These are the cells with the descriptions for each level of
      // accomplishment
      table.addCell(createCell("ND", font, NO_BORDERS));

      for (final RubricRange rubricRange : rowElement.getSortedRubricRanges()) {
        table.addCell(createCell(rubricRange.getShortDescription(), font, NO_TOP_BOTTOM));
      }
    }
  }

  private PdfPCell createCell(String text,
                              Font f,
                              int borders,
                              BaseColor color) {
    PdfPCell result = createCell(text, f, borders);
    result.setBackgroundColor(color);
    return result;
  }

  private PdfPCell createCell(String text,
                              Font f,
                              int borders,
                              int alignment) {
    PdfPCell result = createCell(text, f, borders);
    result.setHorizontalAlignment(alignment);
    return result;
  }

  private PdfPCell createCell(String text,
                              Font f,
                              int borders) {
    PdfPCell result = new PdfPCell(new Paragraph(text, f));
    switch (borders) {
    case NO_BORDERS:
      result.setBorder(0);
      result.setHorizontalAlignment(Element.ALIGN_CENTER);
      break;
    case NO_LEFT:
      result.setBorderWidthLeft(0);
      break;
    case NO_LEFT_RIGHT:
      result.setVerticalAlignment(Element.ALIGN_CENTER);
      result.setBorderWidthRight(0);
      result.setBorderWidthLeft(0);
      result.setMinimumHeight(20f);
      break;
    case NO_TOP_BOTTOM:
      result.setBorderWidthTop(0);
      result.setBorderWidthBottom(0);
      result.setMinimumHeight(20f);
      result.setVerticalAlignment(Element.ALIGN_CENTER);
      result.setHorizontalAlignment(Element.ALIGN_CENTER);
      break;
    case NO_TOP:
      result.setBorderWidthTop(0);
      result.setMinimumHeight(20f);
      result.setVerticalAlignment(Element.ALIGN_CENTER);
      result.setHorizontalAlignment(Element.ALIGN_CENTER);
      break;
    case NO_TOP_LEFT:
      result.setBorderWidthLeft(0);
      result.setBorderWidthBottom(0);
      result.setHorizontalAlignment(Element.ALIGN_CENTER);
      result.setVerticalAlignment(Element.ALIGN_CENTER);
      break;
    case TOP_ONLY:
      result.setBorderWidth(0);
      result.setBorderWidthTop(1);
      result.setMinimumHeight(18f);
      break;
    default:
      break;
    }
    return result;
  }

}
