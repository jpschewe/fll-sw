package fll.documents.writers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;

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
import fll.util.PdfUtils;
import fll.xml.ChallengeDescription;
import fll.xml.RubricRange;
import fll.xml.SubjectiveScoreCategory;

public class SubjectivePdfWriter {
  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final ChallengeDescription description;

  private final String tournamentName;

  private final static int NO_BORDERS = 0;

  private final static int NO_LEFT_RIGHT = 1;

  private final static int NO_TOP_BOTTOM = 2;

  private final static int NO_LEFT = 3;

  private final static int NO_TOP = 4;

  private final static int NO_TOP_LEFT = 5;

  private final static int TOP_ONLY = 6;

  private static final BaseColor rowBlue = new BaseColor(0xB4, 0xCD, 0xED);

  private static final BaseColor rowYellow = new BaseColor(0xFF, 0xFF, 0xC8);

  private static final BaseColor rowRed = new BaseColor(0xF7, 0x98, 0x85);

  private final Font f6i = new Font(Font.FontFamily.HELVETICA, 6, Font.ITALIC);

  private final Font f8b = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);

  private final Font f9b = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

  private final Font f10b = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

  private final Font f12b = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

  private final Font f20b = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);

  private final BaseColor sheetColor;

  private final SubjectiveScoreCategory scoreCategory;

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
  private void writeTeamSubjectivePdf(final Document doc,
                                      final TeamScheduleInfo teamInfo,
                                      final Font font,
                                      final int commentHeight)
      throws MalformedURLException, IOException, DocumentException {
    final Pair<Integer, int[]> headerInfo = getTableColumnInformation(sheetElement.getRubricRangeTitles());

    final PdfPTable table = createStandardRubricTable(headerInfo.getLeft(), headerInfo.getRight());
    writeHeader(doc, teamInfo, headerInfo.getLeft(), headerInfo.getRight());
    for (final String category : sheetElement.getCategories()) {
      writeRubricTable(table, sheetElement.getTableElement(category), font);
    }

    doc.add(table);

    writeCommentsBlock(doc, commentHeight);

    writeEndOfPageRow(doc);
  }

  private void writeHeader(final Document doc,
                           final TeamScheduleInfo teamInfo,
                           final int numColumns,
                           final int[] colWidths)
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
    image.scaleToFit(115, 60);

    // put the image in the header cell
    headerCell = new PdfPCell(image, false);
    headerCell.setRowspan(2);
    headerCell.setBorder(0);
    headerCell.setVerticalAlignment(Element.ALIGN_TOP);
    // make sure there is enough height for the team number and the team name
    headerCell.setMinimumHeight(45);

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
    final String teamNameText = "Name: "
        + teamInfo.getTeamName();
    teamNameCell.setCellEvent(new PdfUtils.TruncateContent(teamNameText, f12b));
    pageHeaderTable.addCell(teamNameCell);

    pageHeaderTable.addCell(createCell(tournamentName, f6i, NO_BORDERS, Element.ALIGN_RIGHT));

    // add the instructions to the header
    dirText = "Directions: For each skill area, clearly mark the box that best describes the team's accomplishments.  "
        + "If the team does not demonstrate skill in a particular area, then put an 'X' in the first box for Not Demonstrated (ND).  "
        + "Please provide as many written comments as you can to acknowledge each teams's hard work and to help teams improve. ";
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

    columnTitlesTable = new PdfPTable(numColumns);

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
    } catch (final DocumentException de) {
      LOGGER.error("Unable to write out the document.", de);
    }
  }

  private static Pair<Integer, int[]> getTableColumnInformation(final List<String> rubricTitles) {
    final List<Integer> colWidthsList = new LinkedList<>();
    colWidthsList.add(4); // goal group

    rubricTitles.stream().forEach(title -> {
      if (title.length() <= 2) {
        // likely "ND", save some space
        colWidthsList.add(4);
      } else {
        colWidthsList.add(23);
      }
    });

    final int[] colWidths = colWidthsList.stream().mapToInt(Integer::intValue).toArray();

    return Pair.of(colWidths.length, colWidths);
  }

  private void writeCommentsBlock(final Document doc,
                                  final int height)
      throws DocumentException {
    final PdfPTable commentsTable = new PdfPTable(2);
    commentsTable.setWidthPercentage(100f);

    final PdfPCell commentsLabel = createCell("Comments", f10b, TOP_ONLY);
    commentsLabel.setColspan(2);
    commentsLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
    commentsTable.addCell(commentsLabel);

    // great job and think about labels
    final Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD
        | Font.ITALIC, BaseColor.LIGHT_GRAY);

    final PdfPCell greatJob = createCell("Great job...", sectionFont, NO_BORDERS);
    greatJob.setBorderWidthRight(1);
    commentsTable.addCell(greatJob);

    final PdfPCell thinkAboutLabel = createCell("Think about...", sectionFont, NO_BORDERS);
    commentsTable.addCell(thinkAboutLabel);

    // empty space
    final PdfPCell emptySpaceLeft = createCell(" ", f6i, NO_BORDERS);
    emptySpaceLeft.setBorderWidthRight(1);
    final PdfPCell emptySpaceRight = createCell(" ", f6i, NO_BORDERS);
    for (int row = 0; row < height; ++row) {
      commentsTable.addCell(emptySpaceLeft);
      commentsTable.addCell(emptySpaceRight);
    }

    // use back if needed
    final Font useBackFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.LIGHT_GRAY);
    final PdfPCell useBackLabel = createCell("Judges: Use the back for additional comments if needed!", useBackFont,
                                             NO_BORDERS);
    useBackLabel.setHorizontalAlignment(Element.ALIGN_CENTER);
    useBackLabel.setColspan(2);
    commentsTable.addCell(useBackLabel);

    doc.add(commentsTable);
  }

  private void writeEndOfPageRow(final Document doc) throws DocumentException {
    final PdfPTable closingTable = new PdfPTable(1);

    closingTable.setWidthPercentage(100f);

    if (null != description.getCopyright()) {
      // add the copy right statement
      final PdfPCell copyrightC = createCell("\u00A9"
          + description.getCopyright(), f6i, NO_BORDERS);
      closingTable.addCell(copyrightC);
    }

    doc.add(closingTable);

    doc.newPage();
  }

  private static Document createStandardDocument() {
    return new Document(PageSize.LETTER, 36, 36, 20, 0);
  }

  private PdfPTable createStandardRubricTable(final int numColumns,
                                              final int[] colWidths)
      throws DocumentException {
    final PdfPTable table = new PdfPTable(numColumns);
    table.setWidths(colWidths);
    table.setWidthPercentage(100f);
    return table;
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
      for (final RubricRange rubricRange : rowElement.getSortedRubricRanges()) {
        final String rawShortDescription = rubricRange.getShortDescription();
        if (null == rawShortDescription) {
          table.addCell(createCell("", font, NO_TOP_BOTTOM));
        } else {
          final String shortDescription = rawShortDescription.trim().replaceAll("\\s+", " ");
          table.addCell(createCell(shortDescription, font, NO_TOP_BOTTOM));
        }
      }
    }
  }

  private PdfPCell createCell(final String text,
                              final Font f,
                              final int borders,
                              final BaseColor color) {
    final PdfPCell result = createCell(text, f, borders);
    result.setBackgroundColor(color);
    return result;
  }

  private PdfPCell createCell(final String text,
                              final Font f,
                              final int borders,
                              final int alignment) {
    final PdfPCell result = createCell(text, f, borders);
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

    final TeamScheduleInfo teamInfo = new TeamScheduleInfo(1);
    teamInfo.setDivision("dummy");
    teamInfo.setJudgingGroup("Dummy");
    teamInfo.setOrganization("Dummy");
    teamInfo.setTeamName("Dummy");

    for (int commentHeight = 20; commentHeight > 2; --commentHeight) {
      for (int pointSize = 9; pointSize >= 6; --pointSize) {
        final Font font = new Font(Font.FontFamily.HELVETICA, pointSize);

        final com.itextpdf.text.Document pdf = SubjectivePdfWriter.createStandardDocument();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(pdf, out);

        pdf.open();

        final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, sheetElement, null);

        writer.writeTeamSubjectivePdf(pdf, teamInfo, font, commentHeight);

        pdf.close();

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final PdfReader reader = new PdfReader(in);
        if (reader.getNumberOfPages() == 1) {
          return Pair.of(font, commentHeight);
        }
      } // font size
    } // comment height

    // no font size fit, just use 10 with comment height 2
    return Pair.of(new Font(Font.FontFamily.HELVETICA, 10), 2);
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
    final Font font = parameters.getLeft();
    final int commentHeight = parameters.getRight();

    final com.itextpdf.text.Document pdf = SubjectivePdfWriter.createStandardDocument();

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
