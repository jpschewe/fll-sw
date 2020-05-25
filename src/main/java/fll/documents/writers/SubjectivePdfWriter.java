package fll.documents.writers;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.io.ByteStreams;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import fll.documents.elements.RowElement;
import fll.documents.elements.SheetElement;
import fll.documents.elements.TableElement;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.Goal;
import fll.xml.RubricRange;
import fll.xml.SubjectiveScoreCategory;
import net.mtu.eggplant.xml.XMLUtils;

public class SubjectivePdfWriter {
  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final ChallengeDescription description;

  private final String tournamentName;

  private static final int NO_BORDERS = 0;

  private static final int NO_LEFT_RIGHT = 1;

  private static final int NO_TOP_BOTTOM = 2;

  private static final int NO_LEFT = 3;

  private static final int NO_TOP = 4;

  private static final int NO_TOP_LEFT = 5;

  private static final int TOP_ONLY = 6;

  private static final int ALL_BORDERS = 7;

  private static final Color ROW_BLUE = new Color(0xB4, 0xCD, 0xED);

  private static final Color ROW_YELLOW = new Color(0xFF, 0xFF, 0xC8);

  private static final Color ROW_RED = new Color(0xF7, 0x98, 0x85);

  private final Font f8bRed = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.RED);

  private final Font f9bRed = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.RED);

  private final Font f6i = new Font(Font.FontFamily.HELVETICA, 6, Font.ITALIC);

  private final Font f8b = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);

  private final Font f9b = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

  private final Font f10b = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

  private final Font f12b = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

  private final Font f20b = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);

  private final Color sheetColor;

  private final SubjectiveScoreCategory scoreCategory;

  private final SheetElement sheetElement;

  private final String scheduleColumn;

  /**
   * @param description the challenge description
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

    // uses hard coded constants to make the colors look like FIRST and default
    // to blue.
    switch (scoreCategory.getName()) {
    case SubjectiveConstants.CORE_VALUES_NAME:
      sheetColor = ROW_RED;
      break;
    case SubjectiveConstants.PROJECT_NAME:
      sheetColor = ROW_YELLOW;
      break;
    case SubjectiveConstants.ROBOT_DESIGN_NAME:
    case SubjectiveConstants.PROGRAMMING_NAME:
    default:
      sheetColor = ROW_BLUE;
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
  private void writeTeamSubjectivePdf(final com.itextpdf.text.Document doc,
                                      final TeamScheduleInfo teamInfo,
                                      final Font font,
                                      final int commentHeight)
      throws MalformedURLException, IOException, DocumentException {
    final Pair<Integer, int[]> headerInfo = getTableColumnInformation(sheetElement.getRubricRangeTitles());

    final PdfPTable table = createStandardRubricTable(headerInfo.getLeft(), headerInfo.getRight());
    // writeHeader(doc, teamInfo, headerInfo.getLeft(), headerInfo.getRight());
    for (final String category : sheetElement.getCategories()) {
      writeRubricTable(table, sheetElement.getTableElement(category), font);
    }

    doc.add(table);

    writeCommentsBlock(doc, commentHeight);

    writeEndOfPageRow(doc);
  }

  private String getHeaderImageAsBase64() {

    final Base64.Encoder encoder = Base64.getEncoder();

    try (
        InputStream input = this.getClass().getClassLoader()
                                .getResourceAsStream("fll/resources/documents/FLLHeader.png");
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Objects.requireNonNull(input);
      // TODO JDK 9 has StreamsTransfer.transferTo, so can use
      // input.transferTo(output)
      ByteStreams.copy(input, output);

      final String encoded = encoder.encodeToString(output.toByteArray());
      return encoded;
    } catch (final IOException e) {
      throw new FLLInternalException("Unable to read subjective header image", e);
    }

  }

  private Element createHeader(final Document document,
                               final TeamScheduleInfo teamInfo,
                               final int numColumns,
                               final int[] colWidths) {
    final Element header = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element pageHeaderTable = FOPUtils.createBasicTable(document);
    header.appendChild(pageHeaderTable);
    pageHeaderTable.appendChild(FOPUtils.createTableColumn(document, 100)); // image
    pageHeaderTable.appendChild(FOPUtils.createTableColumn(document, 130)); // title/room
    pageHeaderTable.appendChild(FOPUtils.createTableColumn(document, 200)); // team number / name
    pageHeaderTable.appendChild(FOPUtils.createTableColumn(document, 75)); // time/tournament

    pageHeaderTable.setAttribute("space-after", "5");

    final Element tableBody = FOPUtils.createXslFoElement(document, "table-body");
    pageHeaderTable.appendChild(tableBody);

    final Element row1 = FOPUtils.createXslFoElement(document, "table-row");
    tableBody.appendChild(row1);

    final Element imageCell = FOPUtils.createXslFoElement(document, "table-cell");
    row1.appendChild(imageCell);
    imageCell.setAttribute("number-rows-spanned", "2");

    final Element imageBlockContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    imageCell.appendChild(imageBlockContainer);

    final Element imageBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    imageBlockContainer.appendChild(imageBlock);

    // get the FLL image to put on the document
    final String imageBase64 = getHeaderImageAsBase64();

    final Element imageGraphic = FOPUtils.createXslFoElement(document, "external-graphic");
    imageBlock.appendChild(imageGraphic);
    // make it a little smaller
    imageGraphic.setAttribute("content-width", "115px");
    imageGraphic.setAttribute("content-height", "100px");
    imageGraphic.setAttribute("scaling", "uniform");
    imageGraphic.setAttribute("src", String.format("url('data:image/png;base64,%s')", imageBase64));

    // Combine category title and team number to make better use of space
    final Element categoryTeamNumberCell = FOPUtils.createXslFoElement(document, "table-cell");
    row1.appendChild(categoryTeamNumberCell);
    categoryTeamNumberCell.setAttribute("font-weight", "bold");
    categoryTeamNumberCell.setAttribute("number-columns-spanned", "2");

    final Element categoryTeamNumberContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    categoryTeamNumberCell.appendChild(categoryTeamNumberContainer);
    categoryTeamNumberContainer.setAttribute("overflow", "hidden");
    categoryTeamNumberContainer.setAttribute("wrap-option", "no-wrap");

    final Element categoryTeamNumberBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    categoryTeamNumberContainer.appendChild(categoryTeamNumberBlock);

    final Element categoryTitle = FOPUtils.createXslFoElement(document, "inline");
    categoryTeamNumberBlock.appendChild(categoryTitle);
    categoryTitle.setAttribute("font-size", "20pt");
    categoryTitle.appendChild(document.createTextNode(scoreCategory.getTitle()));

    final Element teamNumber = FOPUtils.createXslFoElement(document, "inline");
    categoryTeamNumberBlock.appendChild(teamNumber);
    teamNumber.setAttribute("font-size", "12pt");
    teamNumber.appendChild(document.createTextNode(String.format("    Team Number: %d", teamInfo.getTeamNumber())));

    final String scheduledTimeStr;
    if (null == scheduleColumn) {
      scheduledTimeStr = "N/A";
    } else {
      final LocalTime scheduledTime = teamInfo.getSubjectiveTimeByName(scheduleColumn).getTime();
      scheduledTimeStr = TournamentSchedule.formatTime(scheduledTime);
    }
    final Element timeCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                            String.format("Time: %s", scheduledTimeStr));
    row1.appendChild(timeCell);
    timeCell.setAttribute("font-size", "12pt");
    timeCell.setAttribute("font-weight", "bold");

    final Element row2 = FOPUtils.createXslFoElement(document, "table-row");
    tableBody.appendChild(row2);

    final Element roomCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                            String.format("Judging Room: %s",
                                                                          teamInfo.getAwardGroup()));
    row2.appendChild(roomCell);
    roomCell.setAttribute("font-size", "10pt");
    roomCell.setAttribute("font-weight", "bold");

    final Element teamNameCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                                String.format("Name: %s", teamInfo.getTeamName()));
    row2.appendChild(teamNameCell);
    teamNameCell.setAttribute("font-size", "12pt");
    teamNameCell.setAttribute("font-weight", "bold");

    final Element tournamentCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, tournamentName);
    row2.appendChild(tournamentCell);
    tournamentCell.setAttribute("font-size", "6pt");
    tournamentCell.setAttribute("font-style", "italic");

    // add the instructions to the header
    final Element directionsBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    header.appendChild(directionsBlock);
    directionsBlock.setAttribute("font-size", "9pt");
    directionsBlock.setAttribute("font-weight", "bold");
    directionsBlock.appendChild(document.createTextNode(scoreCategory.getScoreSheetInstructions()));

    boolean somethingRequired = false;
    for (final AbstractGoal agoal : sheetElement.getSheetData().getGoals()) {
      if (agoal instanceof Goal) {
        final Goal goal = (Goal) agoal;
        if (goal.isRequired()) {
          somethingRequired = true;
        }
      }
    }

    if (somethingRequired) {
      final Element requiredBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      header.appendChild(requiredBlock);
      requiredBlock.setAttribute("font-size", "9pt");
      requiredBlock.setAttribute("font-weight", "bold");
      requiredBlock.setAttribute("color", "red");
      requiredBlock.appendChild(document.createTextNode("* Required for Award Consideration"));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TOP TITLE BAR START
    //
    // This is the top of the comment sheet where the titles from left to right
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////// are
    // Beginning, Developing, Accomplished, Exemplary
    // This is the same title bar across the tops of all the comment sheet
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////// tables.
    //
    // FIXME this should be combined into the table used for the score sheet
    //
    // final List<String> rubricRangeTitles = sheetElement.getRubricRangeTitles();
    //
    // final PdfPTable columnTitlesTable = new PdfPTable(numColumns);
    //
    // columnTitlesTable.setSpacingBefore(5);
    // columnTitlesTable.setWidthPercentage(100f);
    // columnTitlesTable.setWidths(colWidths);
    // columnTitlesTable.addCell(createCell("", f10b, borders)); // goal group
    //
    // for (final String title : rubricRangeTitles) {
    // if (null == title) {
    // columnTitlesTable.addCell(createCell("", f10b, borders));
    // } else {
    // columnTitlesTable.addCell(createCell(title, f10b, borders));
    // }
    // }
    // columnTitlesTable.setSpacingAfter(3);
    //

    return header;
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

  private void writeCommentsBlock(final com.itextpdf.text.Document doc,
                                  final int height)
      throws DocumentException {
    final PdfPTable commentsTable = new PdfPTable(2);
    commentsTable.setWidthPercentage(100f);

    final PdfPCell commentsLabel = createCell("Comments", f10b, TOP_ONLY);
    commentsLabel.setColspan(2);
    commentsLabel.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
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
    useBackLabel.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
    useBackLabel.setColspan(2);
    commentsTable.addCell(useBackLabel);

    doc.add(commentsTable);
  }

  private void writeEndOfPageRow(final com.itextpdf.text.Document doc) throws DocumentException {
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

  private static com.itextpdf.text.Document createStandardDocument() {
    return new com.itextpdf.text.Document(PageSize.LETTER, 36, 36, 20, 0);
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

      if (rowElement.getGoal().isRequired()) {
        final Chunk required = new Chunk(" *", f8bRed);
        topicAreaP.add(required);
      }

      topicArea = new PdfPCell(topicAreaP);
      topicArea.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      topicArea.setBorderWidthRight(0);
      topicArea.setBorderWidthLeft(0);
      topicArea.setBackgroundColor(new BaseColor(sheetColor.getRed(), sheetColor.getGreen(), sheetColor.getBlue()));

      topicArea.setColspan(2);
      topicInstructions = createCell(rowElement.getDescription(), font, NO_LEFT,
                                     new BaseColor(sheetColor.getRed(), sheetColor.getGreen(), sheetColor.getBlue()));
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
      result.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      break;
    case NO_LEFT:
      result.setBorderWidthLeft(0);
      break;
    case NO_LEFT_RIGHT:
      result.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      result.setBorderWidthRight(0);
      result.setBorderWidthLeft(0);
      break;
    case NO_TOP_BOTTOM:
      result.setBorderWidthTop(0);
      result.setBorderWidthBottom(0);
      result.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      result.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      break;
    case NO_TOP:
      result.setBorderWidthTop(0);
      result.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      result.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      break;
    case NO_TOP_LEFT:
      result.setBorderWidthLeft(0);
      result.setBorderWidthBottom(0);
      result.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      result.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      break;
    case TOP_ONLY:
      result.setBorderWidth(0);
      result.setBorderWidthTop(1);
      break;
    case ALL_BORDERS:
      result.setBorderWidthTop(1);
      result.setBorderWidthBottom(1);
      result.setBorderWidthLeft(1);
      result.setBorderWidthRight(1);
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
   * @return point size to use and the number of rows for the comment sheet
   * @throws MalformedURLException
   * @throws IOException
   * @throws DocumentException
   */
  private static Pair<Integer, Integer> determineParameters(@Nonnull final ChallengeDescription description,
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

        try (PDDocument testPdf = PDDocument.load(out.toByteArray())) {
          if (testPdf.getNumberOfPages() == 1) {
            return Pair.of(pointSize, commentHeight);
          }
        }
      } // font size
    } // comment height

    // no font size fit, just use 10 with comment height 2
    return Pair.of(10, 2);
  }

  /**
   * Create the PDF document with all sheets for the specified schedule and the
   * category specified by {@code sheetElement}.
   *
   * @param description the challenge description
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
                                    @Nonnull final List<TeamScheduleInfo> schedule)
      throws DocumentException, MalformedURLException, IOException {

    final Pair<Integer, Integer> parameters = determineParameters(description, tournamentName, sheetElement);
    final int pointSize = parameters.getLeft();
    final int commentHeight = parameters.getRight();

    final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, sheetElement,
                                                               schedulerColumn);

    try {
      final Document document = writer.createDocument(schedule, pointSize, commentHeight);
      try (Writer w = Files.newBufferedWriter(Paths.get("test.xml"))) {
        XMLUtils.writeXML(document, w);
      }
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the subjective schedule PDF", e);
    }

  }

  private Document createDocument(final List<TeamScheduleInfo> schedule,
                                  final int pointSize,
                                  final int commentHeight) {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);
    rootElement.setAttribute("font-family", "Helvetica");
    rootElement.setAttribute("font-size", String.format("%dpt", pointSize));

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    FOPUtils.createSimplePageMaster(document, layoutMasterSet, pageMasterName);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element footer = FOPUtils.createCopyrightFooter(document, this.description);
    if (null != footer) {
      pageSequence.appendChild(footer);
    }

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Pair<Integer, int[]> headerInfo = getTableColumnInformation(sheetElement.getRubricRangeTitles());

    // Go through all of the team schedules and put them all into a pdf
    for (final TeamScheduleInfo teamInfo : schedule) {
      final Element sheet = createSheet(document, teamInfo, commentHeight, headerInfo);
      documentBody.appendChild(sheet);
    }

    return document;
  }

  private Element createSheet(final Document document,
                              final TeamScheduleInfo teamInfo,
                              final int commentHeight,
                              final Pair<Integer, int[]> headerInfo) {
    final Element sheet = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    sheet.setAttribute("page-break-after", "always");

    final Element header = createHeader(document, teamInfo, headerInfo.getLeft(), headerInfo.getRight());
    sheet.appendChild(header);

    return sheet;
  }

}
