package fll.documents.writers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.log4j.Logger;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
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
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import fll.documents.elements.RowElement;
import fll.documents.elements.SheetElement;
import fll.documents.elements.TableElement;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.LogUtils;
import fll.util.PdfUtils;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.RubricRange;
import fll.xml.ScoreCategory;
import net.mtu.eggplant.util.Pair;

public class SubjectivePdfWriter {
  private static final Logger LOGGER = LogUtils.getLogger();

  private final ChallengeDescription description;

  private final String tournamentName;

  private final static int NO_BORDERS = 0;

  private final static int NO_LEFT_RIGHT = 1;

  private final static int NO_TOP_BOTTOM = 2;

  private final static int NO_LEFT = 3;

  private final static int NO_TOP = 4;

  private final static int NO_TOP_LEFT = 5;

  private final static int TOP_ONLY = 6;

  private static final int[] colWidths = { 4, 4, 23, 23, 23, 23 };

  private static final BaseColor rowBlue = new BaseColor(0xB4, 0xCD, 0xED);

  private static final BaseColor rowYellow = new BaseColor(0xFF, 0xFF, 0xC8);

  private static final BaseColor rowRed = new BaseColor(0xF7, 0x98, 0x85);

  private final Font f6Red = new Font(Font.FontFamily.HELVETICA, 6, Font.NORMAL, BaseColor.RED);

  private final Font f6i = new Font(Font.FontFamily.HELVETICA, 6, Font.ITALIC);

  private final Font f8b = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);

  private final Font f8bRed = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.RED);

  private final Font f9b = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

  private final Font f10b = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

  private final Font f12b = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

  private final Font f20b = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);

  private final BaseColor sheetColor;

  private final ScoreCategory scoreCategory;

  private final SheetElement sheetElement;

  private final String scheduleColumn;

  /**
   * @param sheet information about the rubric
   * @param scheduleColumn the column in the schedule used to find the times,
   *          may be null
   * @param tournamentName the name of the tournament to display on the sheets
   */
  public SubjectivePdfWriter(@Nonnull final ChallengeDescription description,
                             @Nonnull final String tournamentName,
                             @Nonnull final SheetElement sheet,
                             final String scheduleColumn) {
    this.description = description;
    this.tournamentName = tournamentName;
    this.sheetElement = sheet;
    this.scoreCategory = sheetElement.getSheetData();
    this.scheduleColumn = scheduleColumn;

    // uses hard coded constants to make the folors look like FIRST and default
    // to red.
    switch (scoreCategory.getName()) {
    case SubjectiveConstants.CORE_VALUES_NAME:
      sheetColor = rowRed;
      break;
    case SubjectiveConstants.PROJECT_NAME:
      sheetColor = rowYellow;
      break;
    case SubjectiveConstants.ROBOT_DESIGN_NAME:
    case SubjectiveConstants.PROGRAMMING_NAME:
    default:
      sheetColor = rowBlue;
      break;
    }

  }

  /**
   * Write out a subjective sheet for the specified team.
   * 
   * @param doc where to write to
   * @param teamInfo the team information to use when writing
   * @param font the font to use for comments and the rubric
   * @param commentHeight number of rows to put in the comment section
   * @throws MalformedURLException
   * @throws IOException
   * @throws DocumentException
   */
  public void writeTeamSubjectivePdf(final Document doc,
                                     final TeamScheduleInfo teamInfo,
                                     final Font font,
                                     final int commentHeight)
      throws MalformedURLException, IOException, DocumentException {
    final PdfPTable table = createStandardRubricTable();
    writeHeader(doc, teamInfo);
    for (final String category : sheetElement.getCategories()) {
      writeRubricTable(table, sheetElement.getTableElement(category), font);
      writeCommentsSection(table, font, commentHeight);
    }

    doc.add(table);

    writeEndOfPageRow(doc);
  }

  private void writeHeader(final Document doc,
                           final TeamScheduleInfo teamInfo)
      throws MalformedURLException, IOException, DocumentException {
    Image image = null;
    PdfPTable pageHeaderTable = null;
    PdfPTable columnTitlesTable = null;
    PdfPCell headerCell = null;
    Paragraph directions = null;
    String dirText = null;
    Phrase text = null;

    // set up the header for proper spacing
    final float[] headerRelativeWidths = new float[4];
    headerRelativeWidths[0] = 1f; // image
    headerRelativeWidths[1] = 1.3f; // title/room
    headerRelativeWidths[2] = 2f; // team number/name
    headerRelativeWidths[3] = 0.75f; // time/tournament
    pageHeaderTable = new PdfPTable(headerRelativeWidths);
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
    final PdfPCell titleCell = createCell(scoreCategory.getTitle(), f20b, NO_BORDERS, Element.ALIGN_LEFT);
    titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    pageHeaderTable.addCell(titleCell);
    final PdfPCell teamNumberCell = createCell("Team Number: "
        + teamInfo.getTeamNumber(), f12b, NO_BORDERS, Element.ALIGN_LEFT);
    pageHeaderTable.addCell(teamNumberCell);

    final String scheduledTimeStr;
    if (null == scheduleColumn) {
      scheduledTimeStr = "N/A";
    } else {
      final LocalTime scheduledTime = teamInfo.getSubjectiveTimeByName(scheduleColumn).getTime();
      scheduledTimeStr = TournamentSchedule.formatTime(scheduledTime);
    }
    pageHeaderTable.addCell(createCell("Time: "
        + scheduledTimeStr, f12b, NO_BORDERS, Element.ALIGN_RIGHT));

    final PdfPCell roomCell = createCell("Judging Room: "
        + teamInfo.getAwardGroup(), f10b, NO_BORDERS, Element.ALIGN_LEFT);
    roomCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    pageHeaderTable.addCell(roomCell);

    final PdfPCell teamNameCell = createCell(null, f12b, NO_BORDERS, Element.ALIGN_LEFT);
    final String teamNameText = "Team Name: "
        + teamInfo.getTeamName();
    teamNameCell.setCellEvent(new PdfUtils.TruncateContent(teamNameText, f12b));
    teamNameCell.setVerticalAlignment(Element.ALIGN_LEFT);
    pageHeaderTable.addCell(teamNameCell);

    pageHeaderTable.addCell(createCell(tournamentName, f6i, NO_BORDERS, Element.ALIGN_RIGHT));

    // add the instructions to the header
    dirText = "Directions: For each skill area, clearly mark the box that best describes the team's accomplishments.  "
        + "If the team does not demonstrate skill in a particular area, then put an 'X' in the first box for Not Demonstrated (ND).  "
        + "Please provide as many written comments as you can to acknowledge each teams's hard work and to help teams improve. "
        + "When you have completed the evaluation, please circle the team's areas of strength.";
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

    final List<String> rubricRangeTitles = sheetElement.getRubricRangeTitles();

    columnTitlesTable = new PdfPTable(rubricRangeTitles.size()
        + 2);
    columnTitlesTable.setSpacingBefore(5);
    columnTitlesTable.setWidthPercentage(100f);
    columnTitlesTable.setWidths(colWidths);
    columnTitlesTable.addCell(createCell("", f10b, NO_BORDERS));
    columnTitlesTable.addCell(createCell("", f10b, NO_BORDERS));

    for (final String title : rubricRangeTitles) {
      columnTitlesTable.addCell(createCell(title, f10b, NO_BORDERS));
    }
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

  public void writeEndOfPageRow(final Document doc) throws DocumentException {
    PdfPTable closingTable = new PdfPTable(1);

    closingTable.setWidthPercentage(100f);
    final StringBuilder strengths = new StringBuilder();
    strengths.append("Strengths:");
    for (final String category : sheetElement.getCategories()) {
      strengths.append("            ");
      strengths.append(category);
    }
    final PdfPCell strengthsCell = createCell(strengths.toString(), f9b, TOP_ONLY, sheetColor);
    strengthsCell.setMinimumHeight(18f);
    closingTable.addCell(strengthsCell);

    boolean somethingRequired = false;
    for (final AbstractGoal goal : sheetElement.getSheetData().getGoals()) {
      if (goal.isRequired()) {
        somethingRequired = true;
      }
    }
    if (somethingRequired) {
      final PdfPCell requiredC = createCell("* Required for Award Consideration ", f6Red, NO_BORDERS);
      // NO_BORDERS centers
      requiredC.setHorizontalAlignment(Element.ALIGN_LEFT);
      closingTable.addCell(requiredC);
    } else {
      closingTable.addCell(createCell(" ", f6Red, NO_BORDERS));
    }

    if (null != description.getCopyright()) {
      // add the copy right statement
      final PdfPCell copyrightC = createCell("\u00A9"
          + description.getCopyright(), f6i, NO_BORDERS);
      closingTable.addCell(copyrightC);
    }

    doc.add(closingTable);

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

  private void writeCommentsSection(final PdfPTable table,
                                    final Font baseFont,
                                    final int height) {
    PdfPCell commentLabel = null;
    PdfPCell emptySpace = null;

    Font font = new Font(baseFont);
    font.setStyle(Font.ITALIC);
    // This is the 'Comments' section at the bottom of every table for the judge
    // to write in
    commentLabel = createCell("Comments:", font, NO_BORDERS);
    commentLabel.setRotation(90);
    commentLabel.setRowspan(1);
    commentLabel.setBorderWidthLeft(0);
    commentLabel.setBorderWidthBottom(0);
    commentLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
    commentLabel.setVerticalAlignment(Element.ALIGN_CENTER);
    emptySpace = createCell(" ", font, TOP_ONLY);
    emptySpace.setMinimumHeight(18f);
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

  private void writeRubricTable(final PdfPTable table,
                                final TableElement tableData,
                                final Font font) {
    PdfPCell focusArea = null;
    PdfPCell topicArea = null;
    PdfPCell topicInstructions = null;

    // This is the 90 degree turned title for the left side of the table
    focusArea = createCell(tableData.getSubjectiveCatetory(), f8b, NO_TOP_LEFT);
    focusArea.setRotation(90);
    // This is the total number of columns for this table. Each subsection of
    // the table is 2 rows (colored title row, description row)
    focusArea.setRowspan(tableData.getRowElements().size()
        * 2);
    table.addCell(focusArea);

    final List<RowElement> rows = tableData.getRowElements();
    for (final RowElement rowElement : rows) {
      // This is the title row with the background color
      final Chunk topicAreaC = new Chunk(rowElement.getRowTitle(), f8b);

      final Phrase topicAreaP = new Phrase();
      topicAreaP.add(topicAreaC);

      if (rowElement.getGoal().isRequired()) {
        final Chunk required = new Chunk(" *", f8bRed);
        topicAreaP.add(required);
      }

      topicArea = new PdfPCell(topicAreaP);
      topicArea.setVerticalAlignment(Element.ALIGN_CENTER);
      topicArea.setBorderWidthRight(0);
      topicArea.setBorderWidthLeft(0);
      topicArea.setBackgroundColor(sheetColor);

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
        final String shortDescription = rubricRange.getShortDescription().trim().replaceAll("\\s+", " ");
        table.addCell(createCell(shortDescription, font, NO_TOP_BOTTOM));
      }
    }
  }

  private PdfPCell createCell(final String text,
                              final Font f,
                              final int borders,
                              final BaseColor color) {
    PdfPCell result = createCell(text, f, borders);
    result.setBackgroundColor(color);
    return result;
  }

  private PdfPCell createCell(final String text,
                              final Font f,
                              final int borders,
                              final int alignment) {
    PdfPCell result = createCell(text, f, borders);
    result.setHorizontalAlignment(alignment);
    return result;
  }

  private PdfPCell createCell(final String text,
                              final Font f,
                              final int borders) {
    final PdfPCell result;
    if (null == text) {
      result = new PdfPCell();
    } else {
      result = new PdfPCell(new Paragraph(text, f));
    }
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
      break;
    case NO_TOP_BOTTOM:
      result.setBorderWidthTop(0);
      result.setBorderWidthBottom(0);
      result.setVerticalAlignment(Element.ALIGN_CENTER);
      result.setHorizontalAlignment(Element.ALIGN_CENTER);
      break;
    case NO_TOP:
      result.setBorderWidthTop(0);
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
      break;
    default:
      break;
    }
    return result;
  }

  private static final class SubjectiveConstants {

    // Core Values catagory constants
    public static final String CORE_VALUES_NAME = "core_values";

    // Project catagory constants
    public static final String PROJECT_NAME = "project";

    // Robot Design constants
    public static final String ROBOT_DESIGN_NAME = "robot_design";

    // Robot Programming constants
    public static final String PROGRAMMING_NAME = "robot_programming";
  }

  /**
   * @param sheetElement describes the subjective category to output
   * @param tournamentName displayed on the sheets
   * @return Font to use and the number of rows for the comment sheet
   * @throws MalformedURLException
   * @throws IOException
   * @throws DocumentException
   */
  private static Pair<Font, Integer> determineParameters(@Nonnull final ChallengeDescription description,
                                                         @Nonnull final String tournamentName,
                                                         @Nonnull final SheetElement sheetElement)
      throws MalformedURLException, IOException, DocumentException {

    final TeamScheduleInfo teamInfo = new TeamScheduleInfo(1, 1);
    teamInfo.setDivision("dummy");
    teamInfo.setJudgingGroup("Dummy");
    teamInfo.setOrganization("Dummy");
    teamInfo.setTeamName("Dummy");

    for (int commentHeight = 2; commentHeight > 0; --commentHeight) {
      for (int pointSize = 12; pointSize >= 6; --pointSize) {
        final Font font = new Font(Font.FontFamily.HELVETICA, pointSize);

        com.itextpdf.text.Document pdf = SubjectivePdfWriter.createStandardDocument();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(pdf, out);

        pdf.open();

        final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, sheetElement, null);

        writer.writeTeamSubjectivePdf(pdf, teamInfo, font, commentHeight);

        pdf.close();

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final PdfReader reader = new PdfReader(in);
        if (reader.getNumberOfPages() == 1) {
          return new Pair<>(font, commentHeight);
        }
      } // font size
    } // comment height

    // no font size fit, just use 10 with comment height 2
    return new Pair<>(new Font(Font.FontFamily.HELVETICA, 10), 2);
  }

  /**
   * Create the document
   * 
   * @param stream where to write the document
   * @param sheetElement describes the category to write
   * @param schedulerColumn used to determine the schedule information to output
   * @param schedule the schedule to get team information and time information
   *          from
   * @param tournamentName tournament name to display on the sheets
   * @throws DocumentException
   * @throws IOException
   * @throws MalformedURLException
   */
  public static void createDocument(@Nonnull final OutputStream stream,
                                    @Nonnull final ChallengeDescription description,
                                    @Nonnull final String tournamentName,
                                    @Nonnull final SheetElement sheetElement,
                                    final String schedulerColumn,
                                    final List<TeamScheduleInfo> schedule)
      throws DocumentException, MalformedURLException, IOException {

    final Pair<Font, Integer> parameters = determineParameters(description, tournamentName, sheetElement);
    final Font font = parameters.getOne();
    final int commentHeight = parameters.getTwo();

    com.itextpdf.text.Document pdf = SubjectivePdfWriter.createStandardDocument();

    PdfWriter.getInstance(pdf, stream);

    pdf.open();

    final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, sheetElement,
                                                               schedulerColumn);

    // Go through all of the team schedules and put them all into a pdf
    for (final TeamScheduleInfo teamInfo : schedule) {
      writer.writeTeamSubjectivePdf(pdf, teamInfo, font, commentHeight);
    }

    pdf.close();
  }

}
