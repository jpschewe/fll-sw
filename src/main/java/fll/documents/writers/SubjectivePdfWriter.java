package fll.documents.writers;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.base14.Helvetica;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.SubjectiveScore;
import fll.Utilities;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.util.FOPUtils.Margins;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.Goal;
import fll.xml.GoalElement;
import fll.xml.GoalGroup;
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

  private final List<String> masterRubricRangeTitles;

  private static List<String> computeRubricRangeTitles(final SubjectiveScoreCategory category) {
    List<String> masterRubricRangeTitles = null;

    // Go through the sheet (ScoreCategory) and put all the rows (abstractGoal)
    // into the right tables (GoalGRoup)
    for (final AbstractGoal abstractGoal : category.getAllGoals()) {
      if (abstractGoal instanceof Goal) {
        final Goal goal = (Goal) abstractGoal;

        // getRubric returns a sorted list, so we can just add the titles in order
        final List<String> rubricRangeTitles = new LinkedList<>();
        for (final RubricRange range : goal.getRubric()) {
          rubricRangeTitles.add(range.getTitle());
        }

        if (null == masterRubricRangeTitles) {
          masterRubricRangeTitles = rubricRangeTitles;
        } else if (!masterRubricRangeTitles.equals(rubricRangeTitles)) {
          throw new FLLRuntimeException("Rubric range titles not consistent across all goals in score category: "
              + category.getTitle());
        }
      }
    }

    if (null == masterRubricRangeTitles) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(masterRubricRangeTitles);
    }
  }

  /**
   * @param description the challenge description
   * @param scoreCategory category to generate the sheet for
   * @param tournamentName the name of the tournament to display on the sheets
   * @throws FLLInternalException if the rubric titles are not consistent across
   *           the goals in the category
   */
  public SubjectivePdfWriter(@Nonnull final ChallengeDescription description,
                             @Nonnull final String tournamentName,
                             @Nonnull final SubjectiveScoreCategory scoreCategory) {
    this.description = description;
    this.tournamentName = tournamentName;
    this.scoreCategory = scoreCategory;
    this.masterRubricRangeTitles = computeRubricRangeTitles(scoreCategory);

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
      input.transferTo(output);

      final String encoded = encoder.encodeToString(output.toByteArray());
      return encoded;
    } catch (final IOException e) {
      throw new FLLInternalException("Unable to read subjective header image", e);
    }

  }

  private Element createHeader(final Document document,
                               final int teamNumber,
                               final String teamName,
                               final String awardGroup,
                               final @Nullable LocalTime scheduledTime) {
    final Element header = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element pageHeaderTable = FOPUtils.createBasicTable(document);
    header.appendChild(pageHeaderTable);
    pageHeaderTable.appendChild(FOPUtils.createTableColumn(document, 100)); // image
    pageHeaderTable.appendChild(FOPUtils.createTableColumn(document, 130)); // title/room
    pageHeaderTable.appendChild(FOPUtils.createTableColumn(document, 200)); // team number / name
    pageHeaderTable.appendChild(FOPUtils.createTableColumn(document, 75)); // time/tournament

    pageHeaderTable.setAttribute("space-after", "5");

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    pageHeaderTable.appendChild(tableBody);

    final Element row1 = FOPUtils.createTableRow(document);
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

    final Element teamNumberEle = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    categoryTeamNumberBlock.appendChild(teamNumberEle);
    teamNumberEle.setAttribute("font-size", "12pt");
    teamNumberEle.appendChild(document.createTextNode(String.format("    Team Number: %d", teamNumber)));

    final String scheduledTimeStr;
    if (null == scheduledTime) {
      scheduledTimeStr = "N/A";
    } else {
      scheduledTimeStr = TournamentSchedule.formatTime(scheduledTime);
    }
    final Element timeCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                            String.format("Time: %s", scheduledTimeStr));
    row1.appendChild(timeCell);
    timeCell.setAttribute("font-size", "12pt");
    timeCell.setAttribute("font-weight", "bold");

    final Element row2 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row2);

    final Element roomCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                            String.format("Judging Room: %s", awardGroup));
    row2.appendChild(roomCell);
    roomCell.setAttribute("font-size", "10pt");
    roomCell.setAttribute("font-weight", "bold");

    final Element teamNameCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                                String.format("Name: %s", teamName));
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

    final boolean somethingRequired = scoreCategory.getGoalElements().stream()//
                                                   .filter(GoalElement::isGoal)//
                                                   .map(AbstractGoal.class::cast)//
                                                   .filter(Predicate.not(AbstractGoal::isComputed)) //
                                                   .map(Goal.class::cast)//
                                                   .anyMatch(Goal::isRequired);
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

  private static boolean checkPages(final ChallengeDescription description,
                                    final String tournamentName,
                                    final SubjectiveScoreCategory category,
                                    final List<TeamScheduleInfo> schedule,
                                    final int pointSize,
                                    final double commentHeight) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, category);

      try {
        final Document document = writer.createDocumentForSchedule(schedule, null, pointSize, commentHeight);
        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, out);
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the subjective schedule PDF", e);
      }

      try (PDDocument testPdf = PDDocument.load(out.toByteArray())) {
        return testPdf.getNumberOfPages() == 1;
      }
    } catch (final IOException e) {
      throw new FLLInternalException("Internal error determining parameters for subjective sheets", e);
    }

  }

  /**
   * @param category the subjective category to output
   * @param tournamentName displayed on the sheets
   * @return point size to use and the number of rows for the comment sheet
   */
  private static Pair<Integer, Double> determineParameters(@Nonnull final ChallengeDescription description,
                                                           @Nonnull final String tournamentName,
                                                           @Nonnull final SubjectiveScoreCategory category) {

    final TeamScheduleInfo teamInfo = new TeamScheduleInfo(1);
    teamInfo.setDivision("dummy");
    teamInfo.setJudgingGroup("Dummy");
    teamInfo.setOrganization("Dummy");
    teamInfo.setTeamName("Dummy");

    final List<TeamScheduleInfo> schedule = Collections.singletonList(teamInfo);

    // The 2 sets of loops are setup to balance comments and point size. We want to
    // have more comment space, but not have too small of a font.
    // Comment height is in inches.
    for (int pointSize = 10; pointSize >= 8; --pointSize) {
      for (double commentHeight = 5; commentHeight > 3; commentHeight -= 0.2) {
        if (checkPages(description, tournamentName, category, schedule, pointSize, commentHeight)) {
          return Pair.of(pointSize, commentHeight);
        }
      }
    }

    for (int pointSize = 8; pointSize >= 6; --pointSize) {
      for (double commentHeight = 3; commentHeight > 1; commentHeight -= 0.2) {
        if (checkPages(description, tournamentName, category, schedule, pointSize, commentHeight)) {
          return Pair.of(pointSize, commentHeight);
        }
      }
    }

    // no font size fit, just use 10 with comment height 0.2
    LOGGER.warn("Unable to find a point size and comment height that fits on 1 page. Subjective sheets will be multiple pages.");
    return Pair.of(10, 0.2);
  }

  /**
   * Create the PDF document with all sheets for the specified schedule and the
   * category specified by {@code sheetElement}.
   *
   * @param description the challenge description
   * @param stream where to write the document
   * @param category the category to write
   * @param schedulerColumn used to determine the schedule information to output
   * @param schedule the schedule to get team information and time information
   *          from
   * @param tournamentName tournament name to display on the sheets
   * @throws IOException if there is an error writing the document to
   *           {@code stream}
   */
  public static void createDocumentForSchedule(@Nonnull final OutputStream stream,
                                               @Nonnull final ChallengeDescription description,
                                               @Nonnull final String tournamentName,
                                               @Nonnull final SubjectiveScoreCategory category,
                                               final String schedulerColumn,
                                               @Nonnull final List<TeamScheduleInfo> schedule)
      throws IOException {

    final Pair<Integer, Double> parameters = determineParameters(description, tournamentName, category);
    final int pointSize = parameters.getLeft();
    final double commentHeight = parameters.getRight();

    LOGGER.debug("Point size: {} comment height: {}", pointSize, commentHeight);

    final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, category);

    try {
      final Document document = writer.createDocumentForSchedule(schedule, schedulerColumn, pointSize, commentHeight);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();
      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the subjective schedule PDF", e);
    }

  }

  private static final double FOOTER_HEIGHT = 0.1;

  private static final Margins MARGINS = new Margins(0.20, FOOTER_HEIGHT, 0.45, 0.45);

  private Element createBaseDocument(final Document document,
                                     final int pointSize) {

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);
    rootElement.setAttribute("font-family", "Helvetica");
    rootElement.setAttribute("font-size", String.format("%dpt", pointSize));

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                                               MARGINS, 0, FOOTER_HEIGHT);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element footer = FOPUtils.createCopyrightFooter(document, this.description);
    if (null != footer) {
      pageSequence.appendChild(footer);
    }

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    return documentBody;
  }

  /**
   * Create the PDF document for the specified team and scores. It is assumed that
   * {@code score} is consistent with {@code sheetElement}.
   *
   * @param description the challenge description
   * @param stream where to write the document
   * @param category the category to write
   * @param scores the team scores to populate the sheet with
   * @param teamNumber the team number
   * @param teamName the team name
   * @param awardGroup the award group the team is in
   * @param scheduledTime the time that the judging session was scheduled at, may
   *          be null
   * @param tournamentName tournament name to display on the sheets
   * @throws IOException if there is an error writing the document to
   *           {@code stream}
   */
  public static void createDocumentForScores(final OutputStream stream,
                                             final ChallengeDescription description,
                                             final String tournamentName,
                                             final SubjectiveScoreCategory category,
                                             final Collection<SubjectiveScore> scores,
                                             final int teamNumber,
                                             final String teamName,
                                             final String awardGroup,
                                             final @Nullable LocalTime scheduledTime)
      throws IOException {

    final Pair<Integer, Double> parameters = determineParameters(description, tournamentName, category);
    final int pointSize = parameters.getLeft();
    final double commentHeight = parameters.getRight();

    LOGGER.debug("Point size: {} comment height: {}", pointSize, commentHeight);

    final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournamentName, category);

    try {
      final Document document = writer.createDocumentForScores(scores, teamNumber, teamName, awardGroup, scheduledTime,
                                                               pointSize);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();
      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the subjective schedule PDF", e);
    }
  }

  private Document createDocumentForScores(final Collection<SubjectiveScore> scores,
                                           final int teamNumber,
                                           final String teamName,
                                           final String awardGroup,
                                           final @Nullable LocalTime scheduledTime,
                                           final int pointSize) {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();
    final Element documentBody = createBaseDocument(document, pointSize);

    final int[] columnWidths = getTableColumnInformation(masterRubricRangeTitles);

    if (scores.isEmpty()) {
      final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      documentBody.appendChild(block);
      block.appendChild(document.createTextNode("Team "
          + teamName
          + " does not have results for category "
          + scoreCategory.getTitle()));
    } else {
      for (final SubjectiveScore score : scores) {
        final Element sheet = createSheet(document, teamNumber, teamName, awardGroup, scheduledTime, pointSize,
                                          Double.NaN /* ignored when there is a score */, columnWidths, score);
        documentBody.appendChild(sheet);
      }
    }

    return document;
  }

  private Document createDocumentForSchedule(final List<TeamScheduleInfo> schedule,
                                             final @Nullable String scheduleColumn,
                                             final int pointSize,
                                             final double commentHeight) {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element documentBody = createBaseDocument(document, pointSize);

    final int[] columnWidths = getTableColumnInformation(masterRubricRangeTitles);

    // Go through all of the team schedules and put them all into a pdf
    for (final TeamScheduleInfo teamInfo : schedule) {
      final int teamNumber = teamInfo.getTeamNumber();
      final String teamName = teamInfo.getTeamName();
      final String awardGroup = teamInfo.getAwardGroup();
      final LocalTime scheduledTime;
      if (null == scheduleColumn) {
        scheduledTime = null;
      } else {
        scheduledTime = teamInfo.getSubjectiveTimeByName(scheduleColumn).getTime();
      }

      final Element sheet = createSheet(document, teamNumber, teamName, awardGroup, scheduledTime, pointSize,
                                        commentHeight, columnWidths, null);
      documentBody.appendChild(sheet);
    }

    return document;
  }

  private Element createSheet(final Document document,
                              final int teamNumber,
                              final String teamName,
                              final String awardGroup,
                              final @Nullable LocalTime scheduledTime,
                              final int fontSize,
                              final double commentHeight,
                              final int[] columnWidths,
                              final @Nullable SubjectiveScore score) {
    final Element sheet = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    sheet.setAttribute("page-break-after", "always");

    final Element header = createHeader(document, teamNumber, teamName, awardGroup, scheduledTime);
    sheet.appendChild(header);
    header.setAttribute("space-after", "5");

    final Element rubric = createRubric(document, fontSize, columnWidths, score);
    sheet.appendChild(rubric);

    final Element comments = createCommentsBlock(document, commentHeight, score);
    sheet.appendChild(comments);
    comments.setAttribute("space-before", "3");

    return sheet;
  }

  private Element createRubric(final Document document,
                               final int fontSize,
                               final int[] columnWidths,
                               final @Nullable SubjectiveScore score) {
    final Element rubric = FOPUtils.createBasicTable(document);

    for (final int width : columnWidths) {
      rubric.appendChild(FOPUtils.createTableColumn(document, width));
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    rubric.appendChild(tableBody);

    tableBody.appendChild(createRubricHeaderRow(document, tableBody));

    for (final GoalElement ge : scoreCategory.getGoalElements()) {
      if (ge.isGoalGroup()) {
        final GoalGroup group = (GoalGroup) ge;
        addRubricCategory(document, tableBody, fontSize, group, score);
      } else if (ge.isGoal()) {
        // goal outside of a group gets a group of it's own
        final AbstractGoal abstractGoal = (AbstractGoal) ge;
        if (!abstractGoal.isComputed()) {
          final Goal goal = (Goal) abstractGoal;
          addRubricCategory(document, tableBody, fontSize, "", Collections.singletonList(goal), score);
        }
      } else {
        throw new FLLInternalException("Unexpected goal element type: "
            + ge.getClass());
      }
    }

    return rubric;
  }

  private static final String RUBRIC_TABLE_PADDING = "2pt";

  // ideally the width of the string should be used, but for some reason that is
  // too small
  private static final double STRING_WIDTH_MULTIPLIER = 1.25;

  private Element createGoalGroupCell(final Document document,
                                      final int fontSize,
                                      final String goalGroupTitle) {
    if (StringUtils.isBlank(goalGroupTitle)) {
      return FOPUtils.createTableCell(document, null, String.valueOf(Utilities.NON_BREAKING_SPACE));
    } else {
      // Ideally we should not need to determine how big to make the block-container.
      // Bug https://issues.apache.org/jira/browse/FOP-2946 is open for this
      final Element goalGroupCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);

      final Element categoryCellContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
      goalGroupCell.appendChild(categoryCellContainer);
      categoryCellContainer.setAttribute("reference-orientation", "90");
      final Pair<Integer, Integer> stringParameters = determineStringParameters(goalGroupTitle, "Helvetica", true,
                                                                                false, fontSize);
      LOGGER.trace("String '{}' width: {} height: {} font-size: {}", goalGroupTitle, stringParameters.getLeft(),
                   stringParameters.getRight(), fontSize);

      final int containerWidth = (int) Math.ceil(stringParameters.getLeft()
          * STRING_WIDTH_MULTIPLIER);
      categoryCellContainer.setAttribute("inline-progression-dimension", String.format("%dpx", containerWidth));

      final Element categoryCellBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      categoryCellContainer.appendChild(categoryCellBlock);
      categoryCellBlock.setAttribute("text-align", FOPUtils.TEXT_ALIGN_CENTER);
      categoryCellBlock.setAttribute("font-weight", "bold");

      categoryCellBlock.appendChild(document.createTextNode(goalGroupTitle));

      return goalGroupCell;
    }
  }

  /**
   * @param text the text to get the width and height of
   * @param fontFamily the family of the font
   * @param bold true if a bold font
   * @param italic true if an italic font
   * @param fontSize the size of the font in points
   * @return (width, height) in pixels
   */
  public static Pair<Integer, Integer> determineStringParameters(final String text,
                                                                 final String fontFamily,
                                                                 final boolean bold,
                                                                 final boolean italic,
                                                                 final int fontSize) {
    final FontTriplet triplet = new FontTriplet(fontFamily, //
                                                italic ? Font.STYLE_ITALIC : Font.STYLE_NORMAL, //
                                                bold ? Font.WEIGHT_NORMAL : Font.WEIGHT_BOLD, //
                                                Font.PRIORITY_DEFAULT);
    final FontInfo fontInfo = new FontInfo();

    final String key = fontInfo.getInternalFontKey(triplet);

    final FontMetrics metrics = new Helvetica();

    final Font font = new Font(key, triplet, metrics, fontSize);

    final int textWidth = font.getWordWidth(text);
    final int textHeight = font.getXHeight();

    return Pair.of(textWidth, textHeight);
  }

  private void addRubricCategory(final Document document,
                                 final Element tableBody,
                                 final int fontSize,
                                 final GoalGroup goalGroup,
                                 final @Nullable SubjectiveScore score) {
    final List<Goal> goals = goalGroup.getGoals().stream().filter(Predicate.not(AbstractGoal::isComputed))
                                      .map(Goal.class::cast).collect(Collectors.toList());
    final String goalGroupTitle = goalGroup.getTitle();
    addRubricCategory(document, tableBody, fontSize, goalGroupTitle, goals, score);
  }

  private void addRubricCategory(final Document document,
                                 final Element tableBody,
                                 final int fontSize,
                                 final String goalGroupTitle,
                                 final List<Goal> goals,
                                 final @Nullable SubjectiveScore score) {

    final Map<String, Double> standardSubScores = null == score ? Collections.emptyMap() : score.getStandardSubScores();
    final Map<String, String> goalComments = null == score ? Collections.emptyMap() : score.getGoalComments();

    // This is the 90 degree turned title for the left side of the table
    final Element goalGroupCell = createGoalGroupCell(document, fontSize, goalGroupTitle);
    FOPUtils.addBottomBorder(goalGroupCell, 1);
    FOPUtils.addRightBorder(goalGroupCell, 1);
    // This is the total number of columns for this table. Each subsection of
    // the table is 2 rows (colored title row, description row)
    goalGroupCell.setAttribute("number-rows-spanned", String.valueOf(goals.size()
        * 2));

    boolean firstRow = true;
    for (final Goal goal : goals) {
      final Element instructionsRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
      tableBody.appendChild(instructionsRow);

      final List<RubricRange> sortedRubricRanges = goal.getRubric();

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
      topicArea.appendChild(document.createTextNode(goal.getTitle()));

      if (goal.isRequired()) {
        final Element required = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
        topicBlock.appendChild(required);
        required.setAttribute("color", "red");
        required.appendChild(document.createTextNode(" *"));
      }

      final Element topicInstructions = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                                 goal.getDescription());
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

      if (goal.isEnumerated()) {
        throw new IllegalArgumentException("Enumerated goals aren't supported");
      }

      final double goalScore;
      if (null == score) {
        // will compare to false for all ranges
        goalScore = Double.NaN;
      } else {
        goalScore = standardSubScores.get(goal.getName());
      }

      for (final RubricRange rubricRange : sortedRubricRanges) {
        final boolean checked = rubricRange.getMin() <= goalScore
            && goalScore <= rubricRange.getMax();

        final String goalComment;
        if (null != score
            && sortedRubricRanges.get(sortedRubricRanges.size()
                - 1).equals(rubricRange)) {
          // last range is where the goal comment is output
          final String rawComment = goalComments.get(goal.getName());
          if (null != rawComment) {
            final String trimmed = rawComment.trim();
            if (trimmed.isBlank()) {
              goalComment = null;
            } else {
              goalComment = trimmed;
            }
          } else {
            goalComment = null;
          }
        } else {
          goalComment = null;
        }

        final Element rangeCell = createRubricRangeCell(document, rubricRange, checked, goalComment);

        rubricRow.appendChild(rangeCell);
        rangeCell.setAttribute("padding-top", RUBRIC_TABLE_PADDING);
        rangeCell.setAttribute("padding-left", RUBRIC_TABLE_PADDING);
        FOPUtils.addBottomBorder(rangeCell, 1);
        FOPUtils.addRightBorder(rangeCell, 1);
      }

    } // foreach row

  }

  private Element createRubricRangeCell(final Document document,
                                        final RubricRange rubricRange,
                                        final boolean checked,
                                        final @Nullable String goalComment) {

    final Element rangeCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);

    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    rangeCell.appendChild(block);
    block.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_CENTER);

    // add checkbox
    final Element inlineCheckbox = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    block.appendChild(inlineCheckbox);
    FOPUtils.addBorders(inlineCheckbox, 1, 1, 1, 1);
    if (checked) {
      inlineCheckbox.appendChild(document.createTextNode("X"));
    } else {
      inlineCheckbox.appendChild(document.createTextNode(String.valueOf(Utilities.NON_BREAKING_SPACE).repeat(4)));
    }

    // add rubric description, if there is one
    final String rawShortDescription = rubricRange.getShortDescription();
    if (null != rawShortDescription) {
      final String shortDescription = rawShortDescription.trim().replaceAll("\\s+", " ");
      if (!shortDescription.isEmpty()) {
        final StringBuilder rubricDescription = new StringBuilder();
        rubricDescription.append(" ");
        rubricDescription.append(shortDescription);
        block.appendChild(document.createTextNode(rubricDescription.toString()));
      }
    }

    if (null != goalComment) {
      // add judges comments
      final Element commentBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      block.appendChild(commentBlock);
      commentBlock.setAttribute("font-style", "italic");
      commentBlock.setAttribute("font-weight", "bold");
      commentBlock.appendChild(document.createTextNode(goalComment));
    }

    return rangeCell;
  }

  private Element createRubricHeaderRow(final Document document,
                                        final Element tableBody) {
    final Element headerRow = FOPUtils.createTableRow(document);
    tableBody.appendChild(headerRow);
    headerRow.setAttribute("font-size", "10pt");
    headerRow.setAttribute("font-weight", "bold");

    // goal group
    final Element goalGroup = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "");
    headerRow.appendChild(goalGroup);
    FOPUtils.addBottomBorder(goalGroup, 1);

    for (final String title : masterRubricRangeTitles) {
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

  /**
   * @param height height of comments section in inches, if a score is specified,
   *          this is ignored
   */
  private Element createCommentsBlock(final Document document,
                                      final double height,
                                      final @Nullable SubjectiveScore score) {
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
    final Element rowElement = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
    tableBody.appendChild(rowElement);

    final String commentGreatJob;
    final String commentThinkAbout;
    if (null == score) {
      rowElement.setAttribute("height", String.format("%fin", height));
      commentGreatJob = String.valueOf(Utilities.NON_BREAKING_SPACE);
      commentThinkAbout = String.valueOf(Utilities.NON_BREAKING_SPACE);
    } else {
      commentGreatJob = score.getCommentGreatJob();
      commentThinkAbout = score.getCommentThinkAbout();

      rowElement.setAttribute("font-style", "italic");
      rowElement.setAttribute("font-weight", "bold");
    }

    final Element left = FOPUtils.createTableCell(document, null, commentGreatJob == null ? "" : commentGreatJob);
    rowElement.appendChild(left);
    FOPUtils.addRightBorder(left, 1);
    left.setAttribute("padding-right", RUBRIC_TABLE_PADDING);
    left.setAttribute("padding-left", RUBRIC_TABLE_PADDING);

    final Element right = FOPUtils.createTableCell(document, null, commentThinkAbout == null ? "" : commentThinkAbout);
    rowElement.appendChild(right);
    right.setAttribute("padding-right", RUBRIC_TABLE_PADDING);
    right.setAttribute("padding-left", RUBRIC_TABLE_PADDING);

    if (null == score) {
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
    }

    return commentsTable;
  }

}
