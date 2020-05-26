package fll.documents.writers;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Collections;
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

import fll.Utilities;
import fll.documents.elements.RowElement;
import fll.documents.elements.SheetElement;
import fll.documents.elements.TableElement;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.util.FOPUtils.Margins;
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

  private static final Color ROW_BLUE = new Color(0xB4, 0xCD, 0xED);

  private static final Color ROW_YELLOW = new Color(0xFF, 0xFF, 0xC8);

  private static final Color ROW_RED = new Color(0xF7, 0x98, 0x85);

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
                               final TeamScheduleInfo teamInfo) {
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

    final Element imageCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
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
    final Element categoryTeamNumberCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
    row1.appendChild(categoryTeamNumberCell);
    categoryTeamNumberCell.setAttribute("font-weight", "bold");
    categoryTeamNumberCell.setAttribute("number-columns-spanned", "2");

    final Element categoryTeamNumberContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    categoryTeamNumberCell.appendChild(categoryTeamNumberContainer);
    categoryTeamNumberContainer.setAttribute("overflow", "hidden");
    categoryTeamNumberContainer.setAttribute("wrap-option", "no-wrap");

    final Element categoryTeamNumberBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    categoryTeamNumberContainer.appendChild(categoryTeamNumberBlock);

    final Element categoryTitle = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    categoryTeamNumberBlock.appendChild(categoryTitle);
    categoryTitle.setAttribute("font-size", "20pt");
    categoryTitle.appendChild(document.createTextNode(scoreCategory.getTitle()));

    final Element teamNumber = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
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
      requiredBlock.setAttribute("space-before", "5");
      requiredBlock.appendChild(document.createTextNode("* Required for Award Consideration"));
    }

    return header;
  }

  private static int[] getTableColumnInformation(final List<String> rubricTitles) {
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

    return colWidths;
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
   */
  private static Pair<Integer, Integer> determineParameters(@Nonnull final ChallengeDescription description,
                                                            @Nonnull final String tournamentName,
                                                            @Nonnull final SheetElement sheetElement) {

    final TeamScheduleInfo teamInfo = new TeamScheduleInfo(1);
    teamInfo.setDivision("dummy");
    teamInfo.setJudgingGroup("Dummy");
    teamInfo.setOrganization("Dummy");
    teamInfo.setTeamName("Dummy");

    final List<TeamScheduleInfo> schedule = Collections.singletonList(teamInfo);

    // FIXME think about how to balance point size and comment height
    for (int commentHeight = 40; commentHeight > 2; --commentHeight) {
      for (int pointSize = 9; pointSize >= 6; --pointSize) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

          final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, sheetElement, null);

          try {
            final Document document = writer.createDocument(schedule, pointSize, commentHeight);
            final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

            FOPUtils.renderPdf(fopFactory, document, out);
          } catch (FOPException | TransformerException e) {
            throw new FLLInternalException("Error creating the subjective schedule PDF", e);
          }

          try (PDDocument testPdf = PDDocument.load(out.toByteArray())) {
            if (testPdf.getNumberOfPages() == 1) {
              return Pair.of(pointSize, commentHeight);
            }
          }
        } catch (final IOException e) {
          throw new FLLInternalException("Internal error determining parameters for subjective sheets", e);
        }

      } // point size
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
   * @throws IOException if there is an error writing the document to
   *           {@code stream}
   */
  public static void createDocument(@Nonnull final OutputStream stream,
                                    @Nonnull final ChallengeDescription description,
                                    @Nonnull final String tournamentName,
                                    @Nonnull final SheetElement sheetElement,
                                    final String schedulerColumn,
                                    @Nonnull final List<TeamScheduleInfo> schedule)
      throws IOException {

    final Pair<Integer, Integer> parameters = determineParameters(description, tournamentName, sheetElement);
    final int pointSize = parameters.getLeft();
    final int commentHeight = parameters.getRight();

    LOGGER.debug("Point size: {} comment height: {}", pointSize, commentHeight);

    final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, sheetElement,
                                                               schedulerColumn);

    try {
      final Document document = writer.createDocument(schedule, pointSize, commentHeight);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the subjective schedule PDF", e);
    }

  }

  private static final double FOOTER_HEIGHT = 0.1;

  private static final Margins MARGINS = new Margins(0.5, 0.5, 0.25, 0.1);

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
    FOPUtils.createSimplePageMaster(document, layoutMasterSet, pageMasterName, FOPUtils.PAGE_LETTER_SIZE, MARGINS, 0,
                                    FOOTER_HEIGHT);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element footer = FOPUtils.createCopyrightFooter(document, this.description);
    if (null != footer) {
      pageSequence.appendChild(footer);
    }

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final int[] columnWidths = getTableColumnInformation(sheetElement.getRubricRangeTitles());

    // Go through all of the team schedules and put them all into a pdf
    for (final TeamScheduleInfo teamInfo : schedule) {
      final Element sheet = createSheet(document, teamInfo, commentHeight, columnWidths);
      documentBody.appendChild(sheet);
    }

    return document;
  }

  private Element createSheet(final Document document,
                              final TeamScheduleInfo teamInfo,
                              final int commentHeight,
                              final int[] columnWidths) {
    final Element sheet = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    sheet.setAttribute("page-break-after", "always");

    final Element header = createHeader(document, teamInfo);
    sheet.appendChild(header);
    header.setAttribute("space-after", "5");

    final Element rubric = createRubric(document, columnWidths);
    sheet.appendChild(rubric);

    final Element comments = createCommentsBlock(document, commentHeight);
    sheet.appendChild(comments);
    comments.setAttribute("space-before", "3");

    return sheet;
  }

  private Element createRubric(final Document document,
                               final int[] columnWidths) {
    final Element rubric = FOPUtils.createBasicTable(document);

    for (final int width : columnWidths) {
      rubric.appendChild(FOPUtils.createTableColumn(document, width));
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, "table-body");
    rubric.appendChild(tableBody);

    tableBody.appendChild(createRubricHeaderRow(document, tableBody));

    for (final String category : sheetElement.getCategories()) {
      addRubricCategory(document, tableBody, sheetElement.getTableElement(category));
    }

    return rubric;
  }

  private static final String RUBRIC_TABLE_PADDING = "2pt";

  private Element createGoalGroupCell(final Document document,
                                      final String goalGroup) {
    // One should be able to just use the reference-orientation property on the
    // text cell. However when this is done the cells aren't properly sized and the
    // text gets put in the wrong place.
    //
    // Jon Schewe sent an email to the Apache FOP list 5/9/2020 and didn't find an
    // answer.
    // http://mail-archives.apache.org/mod_mbox/xmlgraphics-fop-users/202005.mbox/%3Cd8da02c550c0271943a651c13d7218377efc7137.camel%40mtu.net%3E
    final Element goalGroupCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);

    final Element categoryCellContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    goalGroupCell.appendChild(categoryCellContainer);
    final Element categoryCellBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    categoryCellContainer.appendChild(categoryCellBlock);

    final Element categoryCellForeign = FOPUtils.createXslFoElement(document, "instream-foreign-object");
    categoryCellBlock.appendChild(categoryCellForeign);
    categoryCellForeign.setAttribute("content-height", "scale-to-fit");
    categoryCellForeign.setAttribute("content-width", "scale-to-fit");
    categoryCellForeign.setAttribute("height", "100%");
    categoryCellForeign.setAttribute("width", "100%");

    final Element svg = document.createElementNS(FOPUtils.SVG_NAMESPACE, "svg");
    categoryCellForeign.appendChild(svg);
    final int svgWidth = 25;
    final int svgHeight = 25;
    svg.setAttribute("width", String.valueOf(svgWidth));
    svg.setAttribute("height", String.valueOf(svgHeight));
    svg.setAttribute("viewBox", String.format("0 0 %d %d", svgWidth, svgHeight));

    final Element text = document.createElementNS(FOPUtils.SVG_NAMESPACE, "text");
    svg.appendChild(text);
    text.setAttribute("style",
                      "fill: black; font-family:Helvetica; font-size: 8pt; font-weight: bold; font-style:normal;");
    text.setAttribute("x", String.valueOf(svgWidth
        / 2));
    text.setAttribute("y", String.valueOf(svgHeight
        / 2));
    text.setAttribute("transform", String.format("rotate(-90, %d, %d)", svgWidth
        / 2, svgHeight
            / 2));

    final Element tspan = document.createElementNS(FOPUtils.SVG_NAMESPACE, "tspan");
    text.appendChild(tspan);
    tspan.setAttribute("text-anchor", "middle");
    tspan.appendChild(document.createTextNode(goalGroup));

    return goalGroupCell;
  }

  private void addRubricCategory(final Document document,
                                 final Element tableBody,
                                 final TableElement tableData) {

    // This is the 90 degree turned title for the left side of the table
    final Element goalGroupCell = createGoalGroupCell(document, tableData.getSubjectiveCatetory());
    FOPUtils.addBottomBorder(goalGroupCell, 1);
    FOPUtils.addRightBorder(goalGroupCell, 1);
    // This is the total number of columns for this table. Each subsection of
    // the table is 2 rows (colored title row, description row)
    goalGroupCell.setAttribute("number-rows-spanned", String.valueOf(tableData.getRowElements().size()
        * 2));

    boolean firstRow = true;
    final List<RowElement> rows = tableData.getRowElements();
    for (final RowElement rowElement : rows) {
      final Element instructionsRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
      tableBody.appendChild(instructionsRow);

      final List<RubricRange> sortedRubricRanges = rowElement.getSortedRubricRanges();

      if (firstRow) {
        // need to put the goal group name in the first row
        instructionsRow.appendChild(goalGroupCell);
        firstRow = false;
      }

      final String backgroundColor = String.format("#%02x%02x%02x", sheetColor.getRed(), sheetColor.getGreen(),
                                                   sheetColor.getBlue());

      // This is the title row with the background color
      final Element topicCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
      instructionsRow.appendChild(topicCell);
      FOPUtils.addBottomBorder(topicCell, 1);
      topicCell.setAttribute("background-color", backgroundColor);
      topicCell.setAttribute("number-columns-spanned", "2");
      topicCell.setAttribute("padding-left", RUBRIC_TABLE_PADDING);
      topicCell.setAttribute("padding-top", RUBRIC_TABLE_PADDING);

      final Element topicBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      topicCell.appendChild(topicBlock);
      topicBlock.setAttribute("font-weight", "bold");

      final Element topicArea = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
      topicBlock.appendChild(topicArea);
      topicArea.appendChild(document.createTextNode(rowElement.getRowTitle()));

      if (rowElement.getGoal().isRequired()) {
        final Element required = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
        topicBlock.appendChild(required);
        required.setAttribute("color", "red");
        required.appendChild(document.createTextNode(" *"));
      }

      final Element topicInstructions = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                                 rowElement.getDescription());
      instructionsRow.appendChild(topicInstructions);
      topicInstructions.setAttribute("background-color", backgroundColor);
      topicInstructions.setAttribute("number-columns-spanned", String.valueOf(sortedRubricRanges.size()
          - 2));
      topicInstructions.setAttribute("padding-top", RUBRIC_TABLE_PADDING);
      topicInstructions.setAttribute("padding-left", RUBRIC_TABLE_PADDING);
      FOPUtils.addBottomBorder(topicInstructions, 1);
      FOPUtils.addRightBorder(topicInstructions, 1);

      // These are the cells with the descriptions for each level of
      // accomplishment
      final Element rubricRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
      tableBody.appendChild(rubricRow);

      for (final RubricRange rubricRange : sortedRubricRanges) {
        final Element rangeCell;
        final String rawShortDescription = rubricRange.getShortDescription();
        if (null == rawShortDescription) {
          rangeCell = FOPUtils.createTableCell(document, null, "");
        } else {
          final String shortDescription = rawShortDescription.trim().replaceAll("\\s+", " ");
          rangeCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, shortDescription);
        }
        rubricRow.appendChild(rangeCell);
        rangeCell.setAttribute("padding-top", RUBRIC_TABLE_PADDING);
        rangeCell.setAttribute("padding-left", RUBRIC_TABLE_PADDING);
        FOPUtils.addBottomBorder(rangeCell, 1);
        FOPUtils.addRightBorder(rangeCell, 1);

      }

    } // foreach row

  }

  private Element createRubricHeaderRow(final Document document,
                                        final Element tableBody) {
    final Element headerRow = FOPUtils.createXslFoElement(document, "table-row");
    tableBody.appendChild(headerRow);
    headerRow.setAttribute("font-size", "10pt");
    headerRow.setAttribute("font-weight", "bold");

    final List<String> rubricRangeTitles = sheetElement.getRubricRangeTitles();

    // goal group
    final Element goalGroup = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "");
    headerRow.appendChild(goalGroup);
    FOPUtils.addBottomBorder(goalGroup, 1);

    for (final String title : rubricRangeTitles) {
      final Element titleCell;
      if (null == title) {
        titleCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "");
      } else {
        titleCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, title);
      }
      FOPUtils.addBottomBorder(titleCell, 1);
      headerRow.appendChild(titleCell);
    }

    return headerRow;
  }

  private Element createCommentsBlock(final Document document,
                                      final int height) {
    final Element commentsTable = FOPUtils.createBasicTable(document);

    commentsTable.appendChild(FOPUtils.createTableColumn(document, 1));
    commentsTable.appendChild(FOPUtils.createTableColumn(document, 1));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    commentsTable.appendChild(tableBody);

    final Element commentsLabelRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
    tableBody.appendChild(commentsLabelRow);

    final Element commentsLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Comments");
    commentsLabelRow.appendChild(commentsLabel);
    commentsLabel.setAttribute("font-size", "10pt");
    commentsLabel.setAttribute("font-weight", "bold");
    commentsLabel.setAttribute("number-columns-spanned", "2");

    // great job and think about labels
    final Element labelsRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
    tableBody.appendChild(labelsRow);

    final String lightGray = String.format("#%02x%02x%02x", Color.LIGHT_GRAY.getRed(), Color.LIGHT_GRAY.getGreen(),
                                           Color.LIGHT_GRAY.getBlue());

    final Element greatJob = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Great job...");
    labelsRow.appendChild(greatJob);
    FOPUtils.addRightBorder(greatJob, 1);
    greatJob.setAttribute("font-size", "12pt");
    greatJob.setAttribute("font-weight", "bold");
    greatJob.setAttribute("font-style", "italic");
    greatJob.setAttribute("color", lightGray);

    final Element thinkAbout = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Think about...");
    labelsRow.appendChild(thinkAbout);
    thinkAbout.setAttribute("font-size", "12pt");
    thinkAbout.setAttribute("font-weight", "bold");
    thinkAbout.setAttribute("font-style", "italic");
    thinkAbout.setAttribute("color", lightGray);

    // empty space
    for (int row = 0; row < height; ++row) {
      final Element rowElement = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
      tableBody.appendChild(rowElement);

      final Element left = FOPUtils.createTableCell(document, null, String.valueOf(Utilities.NON_BREAKING_SPACE));
      rowElement.appendChild(left);
      FOPUtils.addRightBorder(left, 1);
      left.setAttribute("font-size", "6pt");

      final Element right = FOPUtils.createTableCell(document, null, String.valueOf(Utilities.NON_BREAKING_SPACE));
      rowElement.appendChild(right);
      right.setAttribute("font-size", "6pt");
    }

    // use back if needed
    final Element useBackRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
    tableBody.appendChild(useBackRow);

    final Element useBackCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                         "Judges: Use the back for additional comments if needed!");
    useBackRow.appendChild(useBackCell);
    useBackCell.setAttribute("number-columns-spanned", "2");
    useBackCell.setAttribute("font-size", "8pt");
    useBackCell.setAttribute("font-style", "italic");
    useBackCell.setAttribute("color", lightGray);

    return commentsTable;
  }

}
