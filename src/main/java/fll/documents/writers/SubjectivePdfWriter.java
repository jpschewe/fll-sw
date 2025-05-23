package fll.documents.writers;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.SubjectiveScore;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.UserImages;
import fll.Utilities;
import fll.scheduler.SubjectiveTime;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.tomcat.TomcatLauncher;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.util.FOPUtils.Margins;
import fll.util.HSLColor;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.Goal;
import fll.xml.GoalElement;
import fll.xml.GoalGroup;
import fll.xml.NonNumericCategory;
import fll.xml.RubricRange;
import fll.xml.SubjectiveGoalRef;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Write subjective sheets as PDFs.
 */
public final class SubjectivePdfWriter {
  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String CORNER_RADIUS = "5pt";

  private final ChallengeDescription description;

  private final String tournamentName;

  private final Color sheetColor;

  private final SubjectiveScoreCategory scoreCategory;

  private final String gearEmptyBase64;

  private final String gearFilledBase64;

  private static final class RubricMetaData {

    /**
     * @param title title
     * @param shortDescription short description if all goals have the same short
     *          description otherwise null
     */
    RubricMetaData(final String title,
                   final @Nullable String shortDescription) {
      this.title = title;
      this.shortDescription = shortDescription;
    }

    private final String title;

    public String getTitle() {
      return title;
    }

    private final @Nullable String shortDescription;

    /**
     * @return if not null, then all goals have this short description
     */
    public @Nullable String getShortDescription() {
      return shortDescription;
    }
  }

  private final List<RubricMetaData> masterRubricRangeMetaData;

  /**
   * Virtual score categories that reference this category.
   */
  private final Set<VirtualSubjectiveScoreCategory> virtualReferences;

  private static List<RubricMetaData> computeRubricRangeTitles(final SubjectiveScoreCategory category) {
    List<RubricMetaData> masterRubricRangeMetaData = null;

    // Go through the sheet (ScoreCategory) and put all the rows (abstractGoal)
    // into the right tables (GoalGRoup)
    for (final AbstractGoal abstractGoal : category.getAllGoals()) {
      if (abstractGoal instanceof Goal) {
        final Goal goal = (Goal) abstractGoal;

        if (null == masterRubricRangeMetaData) {
          masterRubricRangeMetaData = new LinkedList<>();
          for (final RubricRange range : goal.getRubric()) {
            final boolean allSameShortDescription = allShortDescriptionsSame(category, range.getTitle());
            final RubricMetaData meta;
            if (allSameShortDescription) {
              meta = new RubricMetaData(range.getTitle(), range.getShortDescription());
            } else {
              meta = new RubricMetaData(range.getTitle(), null);
            }
            masterRubricRangeMetaData.add(meta);
          }
        } else {
          // check that the titles match
          final List<RubricRange> rubric = goal.getRubric();
          if (rubric.size() != masterRubricRangeMetaData.size()) {
            throw new FLLRuntimeException("Rubric range titles not consistent across all goals in score category: "
                + category.getTitle());
          }

          final Iterator<RubricMetaData> metaIter = masterRubricRangeMetaData.iterator();
          final Iterator<RubricRange> rubricIter = goal.getRubric().iterator();
          while (metaIter.hasNext()
              && rubricIter.hasNext()) {
            final RubricMetaData meta = metaIter.next();
            final RubricRange range = rubricIter.next();
            if (!meta.getTitle().equals(range.getTitle())) {
              throw new FLLRuntimeException("Rubric range titles not consistent across all goals in score category: "
                  + category.getTitle());
            }
          }
        }
      } // goal
    }

    if (null == masterRubricRangeMetaData) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(masterRubricRangeMetaData);
    }
  }

  /**
   * @param description the challenge description
   * @param scoreCategory category to generate the sheet for
   * @param tournamentName the name of the tournament to display on the sheets
   * @throws FLLInternalException if the rubric titles are not consistent across
   *           the goals in the category
   */
  private SubjectivePdfWriter(final ChallengeDescription description,
                              final String tournamentName,
                              final SubjectiveScoreCategory scoreCategory) {
    this.description = description;
    this.tournamentName = tournamentName;
    this.scoreCategory = scoreCategory;
    this.masterRubricRangeMetaData = computeRubricRangeTitles(scoreCategory);
    final Set<VirtualSubjectiveScoreCategory> virtualReferences = new HashSet<>();
    for (VirtualSubjectiveScoreCategory virt : description.getVirtualSubjectiveCategories()) {
      final boolean referenced = virt.getGoalReferences().stream()
                                     .anyMatch(ref -> ref.getCategory().equals(scoreCategory));
      if (referenced) {
        virtualReferences.add(virt);
      }
    }
    this.virtualReferences = Collections.unmodifiableSet(virtualReferences);
    this.gearEmptyBase64 = getGearEmptyImageAsBase64();
    this.gearFilledBase64 = getGearFilledImageAsBase64();

    // uses hard coded constants to make the colors look like FIRST and default
    // to blue.
    switch (scoreCategory.getName()) {
    case SubjectiveConstants.CORE_VALUES_NAME:
      sheetColor = new Color(0xf9, 0xd0, 0xc9);
      break;
    case SubjectiveConstants.PROJECT_NAME:
      sheetColor = new Color(0xd2, 0xda, 0xef);
      break;
    case SubjectiveConstants.ROBOT_DESIGN_NAME:
    case SubjectiveConstants.PROGRAMMING_NAME:
    default:
      sheetColor = new Color(0xd6, 0xea, 0xd7);
      break;
    }

  }

  private static String getGearEmptyImageAsBase64() {
    final Base64.Encoder encoder = Base64.getEncoder();

    final @Nullable Path webroot = TomcatLauncher.findWebappRoot(TomcatLauncher.getClassesPath());
    if (null == webroot) {
      throw new FLLInternalException("Cannot find web root when looking for gear-empty.png");
    }

    try (InputStream input = Files.newInputStream(webroot.resolve("images").resolve("gear-empty.png"))) {
      if (null == input) {
        throw new FLLInternalException("Cannot find gear empty image");
      }

      final String encoded = encoder.encodeToString(IOUtils.toByteArray(input));
      return encoded;
    } catch (final IOException e) {
      throw new FLLInternalException("Unable to read gear empty image", e);
    }
  }

  private static String getGearFilledImageAsBase64() {
    final Base64.Encoder encoder = Base64.getEncoder();

    final ClassLoader loader = castNonNull(UserImages.class.getClassLoader());
    try (InputStream input = loader.getResourceAsStream("fll/resources/documents/gear-filled.png")) {
      if (null == input) {
        throw new FLLInternalException("Cannot find gear empty image");
      }

      final String encoded = encoder.encodeToString(IOUtils.toByteArray(input));
      return encoded;
    } catch (final IOException e) {
      throw new FLLInternalException("Unable to read gear empty image", e);
    }
  }

  private static String getHeaderImageAsBase64() {
    final Base64.Encoder encoder = Base64.getEncoder();

    final Path fllSubjectiveLogo = UserImages.getImagesPath().resolve(UserImages.FLL_SUBJECTIVE_LOGO_FILENAME);
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      if (!Files.exists(fllSubjectiveLogo)) {
        throw new FLLInternalException("Cannot find FLL Subjective Logo: "
            + fllSubjectiveLogo.toAbsolutePath().toString());
      }

      Files.copy(fllSubjectiveLogo, output);

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
                               final @Nullable LocalTime scheduledTime,
                               final @Nullable SubjectiveScore score) {
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
    imageGraphic.setAttribute("content-width", "105px");
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
    categoryTeamNumberBlock.setAttribute("text-align-last", "justify");

    final Element categoryTitle = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    categoryTeamNumberBlock.appendChild(categoryTitle);
    categoryTitle.setAttribute("font-size", "20pt");
    categoryTitle.appendChild(document.createTextNode(scoreCategory.getTitle()));

    final Element teamNumberEle = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    categoryTeamNumberBlock.appendChild(teamNumberEle);
    teamNumberEle.setAttribute("font-size", "12pt");
    teamNumberEle.appendChild(document.createTextNode(String.format("    Team Number: %s",
                                                                    Team.NULL_TEAM_NUMBER == teamNumber ? ""
                                                                        : Integer.toString(teamNumber))));

    final String scheduledTimeStr;
    if (Team.NULL_TEAM_NUMBER == teamNumber) {
      scheduledTimeStr = "";
    } else if (null == scheduledTime) {
      scheduledTimeStr = "N/A";
    } else {
      scheduledTimeStr = TournamentSchedule.humanFormatTime(scheduledTime);
    }
    final Element timeCell = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                            String.format("Time: %s",
                                                                          StringUtils.leftPad(scheduledTimeStr, 5,
                                                                                              Utilities.NON_BREAKING_SPACE)));
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

    final Element tournamentNameJudgeInitialsCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
    row2.appendChild(tournamentNameJudgeInitialsCell);

    final Element tournamentNameJudgeInitialsContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    tournamentNameJudgeInitialsCell.appendChild(tournamentNameJudgeInitialsContainer);
    tournamentNameJudgeInitialsContainer.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_RIGHT);

    final Element tournamentNameElement = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    tournamentNameJudgeInitialsContainer.appendChild(tournamentNameElement);
    tournamentNameElement.setAttribute("font-size", "6pt");
    tournamentNameElement.setAttribute("font-style", "italic");
    tournamentNameElement.appendChild(document.createTextNode(tournamentName));

    // space before the judge initials
    final Element spacer = FOPUtils.createXslFoElement(document, FOPUtils.LEADER_TAG);
    spacer.setAttribute("leader-pattern", "space");
    tournamentNameJudgeInitialsContainer.appendChild(spacer);

    final String judgeInitials = getJudgeInitials(score);
    final Element judgesInitialsElement = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    tournamentNameJudgeInitialsContainer.appendChild(judgesInitialsElement);
    judgesInitialsElement.setAttribute("font-size", "6pt");
    judgesInitialsElement.setAttribute("font-weight", "normal");
    judgesInitialsElement.setAttribute("font-style", "italic");
    judgesInitialsElement.appendChild(document.createTextNode(judgeInitials));

    addInstructions(document, header, score);

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

  private void addInstructions(final Document document,
                               final Element header,
                               final @Nullable SubjectiveScore score) {
    final Element directionsContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    // "Instructions\n in bold"
    final Element instructionsText = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    directionsContainer.appendChild(instructionsText);
    instructionsText.setAttribute("font-weight", "bold");
    instructionsText.appendChild(document.createTextNode("Instructions"));

    // "instructions text"
    final Element directionsBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    directionsContainer.appendChild(directionsBlock);
    directionsBlock.appendChild(document.createTextNode(scoreCategory.getScoreSheetInstructions()));

    final List<SubjectiveScoreCategory.Nominates> categoriesCanNominate = scoreCategory.getNominates();
    if (!categoriesCanNominate.isEmpty()) {
      final Element instructionsTable = FOPUtils.createBasicTable(document);
      header.appendChild(instructionsTable);
      instructionsTable.appendChild(FOPUtils.createTableColumn(document, 25)); // instructions
      instructionsTable.appendChild(FOPUtils.createTableColumn(document, 25)); // categories and checkbox
      instructionsTable.appendChild(FOPUtils.createTableColumn(document, 50)); // category descriptions

      final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
      instructionsTable.appendChild(tableBody);

      final Element row1 = FOPUtils.createTableRow(document);
      tableBody.appendChild(row1);

      final Element directionsCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
      row1.appendChild(directionsCell);
      directionsCell.appendChild(directionsContainer);
      directionsCell.setAttribute("number-rows-spanned", String.valueOf(categoriesCanNominate.size()
          + 1));
      directionsCell.setAttribute("padding-right", "5pt");

      final String verticalPadding = "3pt";

      final Element nominateInstructions = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                                    "If the team is a candidate for one of these awards, please tick the appropriate box:");
      row1.appendChild(nominateInstructions);
      // span checkbox and description
      nominateInstructions.setAttribute("number-columns-spanned", "2");
      nominateInstructions.setAttribute("padding-bottom", verticalPadding);

      // row per award, top border if not first
      boolean first = true;
      for (final SubjectiveScoreCategory.Nominates nominates : categoriesCanNominate) {
        final Element row = FOPUtils.createTableRow(document);
        tableBody.appendChild(row);

        final NonNumericCategory category = description.getNonNumericCategoryByTitle(nominates.getNonNumericCategoryTitle());
        if (null == category) {
          throw new FLLInternalException("There is no non-numeric category with title "
              + nominates.getNonNumericCategoryTitle()
              + ". This should have been caught when parsing the challenge description");
        }

        final Element checkboxCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
        row.appendChild(checkboxCell);
        checkboxCell.setAttribute("padding-top", verticalPadding);
        checkboxCell.setAttribute("padding-bottom", verticalPadding);
        checkboxCell.setAttribute("display-align", "center");

        final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        checkboxCell.appendChild(block);
        block.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_LEFT);

        // add checkbox
        final Element inlineCheckbox = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
        block.appendChild(inlineCheckbox);
        FOPUtils.addBorders(inlineCheckbox, 1, 1, 1, 1);
        if (null != score
            && score.getNonNumericNominations().contains(category.getTitle())) {
          inlineCheckbox.appendChild(document.createTextNode(String.format("%cX%c", Utilities.NON_BREAKING_SPACE,
                                                                           Utilities.NON_BREAKING_SPACE)));
        } else {
          inlineCheckbox.appendChild(document.createTextNode(Utilities.NON_BREAKING_SPACE_STRING.repeat(4)));
        }
        final Element title = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
        block.appendChild(title);
        title.setAttribute("font-weight", "bold");
        title.appendChild(document.createTextNode(" "
            + category.getTitle()));

        final Element descriptionCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                                 category.getDescription());
        row.appendChild(descriptionCell);
        descriptionCell.setAttribute("padding-top", verticalPadding);
        descriptionCell.setAttribute("padding-bottom", verticalPadding);
        descriptionCell.setAttribute("display-align", "center");

        if (first) {
          first = false;
        } else {
          FOPUtils.addTopBorder(checkboxCell, 1);
          FOPUtils.addTopBorder(descriptionCell, 1);
        }
      }

    } else {
      // add the instructions to the header
      header.appendChild(directionsContainer);
    }
  }

  private static int[] getTableColumnInformation(final List<RubricMetaData> rubricMetaDta) {
    final int[] colWidths = rubricMetaDta.stream().mapToInt(meta -> {
      if (meta.getTitle().length() <= 2) {
        // likely "ND", save some space
        return 4;
      } else {
        return 23;
      }
    }).toArray();

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

      try (PDDocument testPdf = Loader.loadPDF(out.toByteArray())) {
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
  @SuppressFBWarnings(value = "FL_FLOATS_AS_LOOP_COUNTERS", justification = "comment height is a floating point number, precision is ok")
  private static Pair<Integer, Double> determineParameters(final ChallengeDescription description,
                                                           final String tournamentName,
                                                           final SubjectiveScoreCategory category) {

    final TeamScheduleInfo teamInfo = new TeamScheduleInfo(TournamentTeam.SAMPLE_TEAM);

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
  public static void createDocumentForSchedule(final OutputStream stream,
                                               final ChallengeDescription description,
                                               final String tournamentName,
                                               final SubjectiveScoreCategory category,
                                               final @Nullable String schedulerColumn,
                                               final List<TeamScheduleInfo> schedule)
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
   * @param connection database connection
   * @param tournament tournament
   * @param stream where to write the document
   * @param description the challenge description
   * @param category the category to write
   * @param scores the team scores to populate the sheet with
   * @throws IOException if there is an error writing the document to
   *           {@code stream}
   * @throws SQLException if there is an error reading from the database
   */
  public static void createDocumentForScores(final Connection connection,
                                             final Tournament tournament,
                                             final OutputStream stream,
                                             final ChallengeDescription description,
                                             final SubjectiveScoreCategory category,
                                             final Collection<SubjectiveScore> scores)
      throws IOException, SQLException {

    final Pair<Integer, Double> parameters = determineParameters(description, tournament.getName(), category);
    final int pointSize = parameters.getLeft();
    final double commentHeight = parameters.getRight();

    LOGGER.debug("Point size: {} comment height: {}", pointSize, commentHeight);

    final SubjectivePdfWriter writer = new SubjectivePdfWriter(description, tournament.getName(), category);

    try {
      final Document document = writer.createDocumentForScores(connection, tournament, scores, pointSize);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();
      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the subjective schedule PDF."
          + " category: "
          + category.getName(), e);
    }
  }

  private Document createDocumentForScores(final Connection connection,
                                           final Tournament tournament,
                                           final Collection<SubjectiveScore> scores,
                                           final int pointSize)
      throws SQLException {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();
    final Element documentBody = createBaseDocument(document, pointSize);

    final int[] columnWidths = getTableColumnInformation(masterRubricRangeMetaData);

    if (scores.isEmpty()) {
      final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      documentBody.appendChild(block);
      block.appendChild(document.createTextNode("No scores for category "
          + scoreCategory.getTitle()));
    } else {
      for (final SubjectiveScore score : scores) {
        final TournamentTeam team = TournamentTeam.getTournamentTeamFromDatabase(connection, tournament,
                                                                                 score.getTeamNumber());

        final Element sheet = createSheet(document, team.getTeamNumber(), team.getTeamName(), team.getAwardGroup(),
                                          null, pointSize, Double.NaN /* ignored when there is a score */, columnWidths,
                                          score);
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

    final int[] columnWidths = getTableColumnInformation(masterRubricRangeMetaData);

    // Go through all of the team schedules and put them all into a pdf
    for (final TeamScheduleInfo teamInfo : schedule) {
      final int teamNumber = teamInfo.getTeamNumber();
      final String teamName = teamInfo.getTeamName();
      final String awardGroup = teamInfo.getAwardGroup();
      final LocalTime scheduledTime;
      if (null == scheduleColumn) {
        scheduledTime = null;
      } else {
        final SubjectiveTime stime = teamInfo.getSubjectiveTimeByName(scheduleColumn);
        if (null == stime) {
          scheduledTime = null;
        } else {
          scheduledTime = stime.getTime();
        }
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
    sheet.setAttribute("font-size", String.format("%dpt", fontSize));

    final Element header = createHeader(document, teamNumber, teamName, awardGroup, scheduledTime, score);
    sheet.appendChild(header);
    header.setAttribute("space-after", "5");

    final Element rubric = createRubric(document, fontSize, columnWidths, score);
    sheet.appendChild(rubric);

    final Element comments = createCommentsBlock(document, commentHeight, score);
    sheet.appendChild(comments);
    comments.setAttribute("space-before", "3");

    if (null != score
        && score.getNoShow()) {
      // add last so that it's on top of everything
      sheet.appendChild(FOPUtils.createWatermark(document, "NO SHOW", WATERMARK_OPACITY));
    }

    return sheet;
  }

  private static Optional<String> findRangeShortDescriptionWithTitle(final String title,
                                                                     final Goal goal) {
    return goal.getRubric().stream().filter(r -> title.equals(r.getTitle())).map(RubricRange::getShortDescription)
               .findFirst();
  }

  /**
   * Check that the rubric ranges with {@code title} in all goals have the same
   * {@link RubricRange#getShortDescription()}.
   * 
   * @param title the title to look for
   * @return true if all of the short descriptions are the same
   */
  private static boolean allShortDescriptionsSame(final SubjectiveScoreCategory scoreCategory,
                                                  final String title) {
    return scoreCategory.getAllGoals().stream()//
                        .filter(Goal.class::isInstance)//
                        .map(Goal.class::cast)//
                        .map(g -> findRangeShortDescriptionWithTitle(title, g))//
                        .filter(Optional::isPresent) //
                        .map(Optional::get) //
                        .distinct()//
                        .count() <= 1;
  }

  private Element createRubric(final Document document,
                               final int fontSize,
                               final int[] columnWidths,
                               final @Nullable SubjectiveScore score) {
    final Element rubric = FOPUtils.createBasicTable(document);
    rubric.setAttribute("border-collapse", "separate");

    for (final int width : columnWidths) {
      rubric.appendChild(FOPUtils.createTableColumn(document, width));
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    rubric.appendChild(tableBody);

    tableBody.appendChild(createRubricHeaderRow(document, tableBody));

    List<Element> lastRowCells = null;
    for (final GoalElement ge : scoreCategory.getGoalElements()) {
      if (ge.isGoalGroup()) {
        final GoalGroup goalGroup = (GoalGroup) ge;
        lastRowCells = addRubricGoalGroup(document, tableBody, columnWidths.length, fontSize, goalGroup);

        for (final AbstractGoal abstractGoal : goalGroup.getGoals()) {
          if (!abstractGoal.isComputed()) {
            final Goal goal = (Goal) abstractGoal;
            lastRowCells = addRubricGoal(document, tableBody, goal, score);
          }
        }
      } else if (ge.isGoal()) {
        final AbstractGoal abstractGoal = (AbstractGoal) ge;
        if (!abstractGoal.isComputed()) {
          final Goal goal = (Goal) abstractGoal;
          lastRowCells = addRubricGoal(document, tableBody, goal, score);
        }
      } else {
        throw new FLLInternalException("Unexpected goal element type: "
            + ge.getClass());
      }
    }

    if (null != lastRowCells) {
      boolean first = true;
      Element lastCell = null;
      for (final Element cell : lastRowCells) {
        FOPUtils.addBottomBorder(cell, 1);
        if (first) {
          cell.setAttribute(String.format("%s:border-after-start-radius", FOPUtils.XSL_FOX_PREFIX), CORNER_RADIUS);
          first = false;
        }
        lastCell = cell;
      }
      if (null != lastCell) {
        lastCell.setAttribute(String.format("%s:border-after-end-radius", FOPUtils.XSL_FOX_PREFIX), CORNER_RADIUS);
      }
    }

    return rubric;
  }

  private List<Element> addRubricGoalGroup(final Document document,
                                           final Element tableBody,
                                           final int numColumns,
                                           final int fontSize,
                                           final GoalGroup goalGroup) {

    final String backgroundColor = String.format("#%02x%02x%02x", sheetColor.getRed(), sheetColor.getGreen(),
                                                 sheetColor.getBlue());

    final Element row = FOPUtils.createTableRow(document);
    tableBody.appendChild(row);

    // This is the title row with the background color
    final Element cell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
    row.appendChild(cell);
    FOPUtils.addTopBorder(cell, 1);

    cell.setAttribute("background-color", backgroundColor);
    cell.setAttribute("number-columns-spanned", String.valueOf(numColumns));
    cell.setAttribute("padding-left", RUBRIC_TABLE_PADDING);
    cell.setAttribute("padding-top", RUBRIC_TABLE_PADDING);
    FOPUtils.addLeftBorder(cell, 1);
    FOPUtils.addTopBorder(cell, 1);
    FOPUtils.addRightBorder(cell, 1);

    final Element goalGroupTitleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    cell.appendChild(goalGroupTitleBlock);
    goalGroupTitleBlock.setAttribute("font-weight", "bold");
    goalGroupTitleBlock.setAttribute("font-size", String.valueOf(fontSize
        + 2));
    goalGroupTitleBlock.appendChild(document.createTextNode(goalGroup.getTitle()));

    final String goalGroupDescription = goalGroup.getDescription();
    if (!StringUtils.isBlank(goalGroupDescription)) {
      final Element goalGroupDescriptionBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      cell.appendChild(goalGroupDescriptionBlock);
      goalGroupDescriptionBlock.setAttribute("font-size", String.valueOf(fontSize));
      goalGroupDescriptionBlock.appendChild(document.createTextNode(goalGroupDescription));
    }

    return Collections.singletonList(cell);
  }

  private static final String RUBRIC_TABLE_PADDING = "2pt";

  private List<Element> addRubricGoal(final Document document,
                                      final Element tableBody,
                                      final Goal goal,
                                      final @Nullable SubjectiveScore score) {

    final Map<String, Double> standardSubScores = null == score ? Collections.emptyMap() : score.getStandardSubScores();
    final Map<String, String> goalComments = null == score ? Collections.emptyMap() : score.getGoalComments();

    final List<RubricRange> sortedRubricRanges = goal.getRubric();

    // These are the cells with the descriptions for each level of
    // accomplishment
    final Element rubricRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
    tableBody.appendChild(rubricRow);

    if (goal.isEnumerated()) {
      throw new IllegalArgumentException("Enumerated goals aren't supported");
    }

    final double goalScore;
    if (null == score
        || score.getNoShow()) {
      // will compare to false for all ranges
      goalScore = Double.NaN;
    } else {
      final String goalName = goal.getName();
      if (standardSubScores.containsKey(goalName)) {
        goalScore = standardSubScores.get(goalName);
      } else {
        goalScore = Double.NaN;
      }
    }

    final List<Element> cells = new LinkedList<>();
    Element lastCell = null;
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

      boolean virtualReferenced = false;
      for (VirtualSubjectiveScoreCategory virt : description.getVirtualSubjectiveCategories()) {
        for (SubjectiveGoalRef ref : virt.getGoalReferences()) {
          if (ref.getCategory().equals(scoreCategory)
              && ref.getGoal().equals(goal)) {
            virtualReferenced = true;
            break;
          }
        }
        if (virtualReferenced) {
          break;
        }
      }
      final Element rangeCell = createRubricRangeCell(document, rubricRange, virtualReferenced, checked, goalComment);

      rubricRow.appendChild(rangeCell);
      rangeCell.setAttribute("padding-top", RUBRIC_TABLE_PADDING);
      rangeCell.setAttribute("padding-left", RUBRIC_TABLE_PADDING);
      FOPUtils.addLeftBorder(rangeCell, 1);
      FOPUtils.addTopBorder(rangeCell, 1);

      cells.add(rangeCell);
      lastCell = rangeCell;
    }
    if (null != lastCell) {
      FOPUtils.addRightBorder(lastCell, 1);
    }

    return cells;
  }

  private Element createRubricRangeCell(final Document document,
                                        final RubricRange rubricRange,
                                        final boolean virtualReferenced,
                                        final boolean checked,
                                        final @Nullable String goalComment) {

    final Element rangeCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);

    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    rangeCell.appendChild(block);
    block.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_LEFT);

    // add checkbox
    if (virtualReferenced) {
      final Element imageGraphic = FOPUtils.createXslFoElement(document, "external-graphic");
      block.appendChild(imageGraphic);
      imageGraphic.setAttribute("src", String.format("url('data:image/png;base64,%s')",
                                                     checked ? gearFilledBase64 : gearEmptyBase64));
    } else {
      final Element inlineCheckbox = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
      block.appendChild(inlineCheckbox);
      FOPUtils.addBorders(inlineCheckbox, 1, 1, 1, 1);
      if (checked) {
        inlineCheckbox.appendChild(document.createTextNode(String.format("%cX%c", Utilities.NON_BREAKING_SPACE,
                                                                         Utilities.NON_BREAKING_SPACE)));

      } else {
        inlineCheckbox.appendChild(document.createTextNode(Utilities.NON_BREAKING_SPACE_STRING.repeat(4)));
      }
    }

    // add rubric description, if there is one
    final Optional<RubricMetaData> metaData = masterRubricRangeMetaData.stream()
                                                                       .filter(md -> md.getTitle()
                                                                                       .equals(rubricRange.getTitle()))
                                                                       .findFirst();
    if (!metaData.isPresent()) {
      throw new FLLInternalException("Cannot find rubric metadata for range with title "
          + rubricRange.getTitle());
    }

    if (null == metaData.get().getShortDescription()) {
      block.appendChild(document.createTextNode(" "
          + rubricRange.getShortDescription()));
    }

    if (null != goalComment) {
      // add judges comments
      Arrays.stream(goalComment.split("\\r?\\n")).forEach(comment -> {
        final Element commentBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        block.appendChild(commentBlock);
        commentBlock.setAttribute("font-style", "italic");
        commentBlock.setAttribute("font-weight", "bold");
        commentBlock.appendChild(document.createTextNode(comment));
      });
    }

    return rangeCell;
  }

  private static final float LIGHTEN_PERCENTAGE = 5;

  private static final float DARKEN_PERCENTAGE = 15;

  private Element createRubricHeaderRow(final Document document,
                                        final Element tableBody) {
    final Element headerRow = FOPUtils.createTableRow(document);
    tableBody.appendChild(headerRow);
    headerRow.setAttribute("font-size", "10pt");

    int index = 1;
    Color columnColor = new HSLColor(sheetColor).adjustTone(LIGHTEN_PERCENTAGE);
    Element lastCell = null;
    boolean first = true;
    for (final RubricMetaData rubricMetaData : masterRubricRangeMetaData) {
      final String columnColorStr = String.format("#%02x%02x%02x", columnColor.getRed(), columnColor.getGreen(),
                                                  columnColor.getBlue());

      final Element cell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
      headerRow.appendChild(cell);
      final Element blockContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
      cell.appendChild(blockContainer);
      cell.setAttribute("background-color", columnColorStr);

      final Element titleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      blockContainer.appendChild(titleBlock);
      titleBlock.appendChild(document.createTextNode(rubricMetaData.getTitle()));
      titleBlock.setAttribute("font-weight", "bold");

      final String shortDescription = rubricMetaData.getShortDescription();
      if (null != shortDescription) {
        final Element descriptionBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        blockContainer.appendChild(descriptionBlock);
        descriptionBlock.appendChild(document.createTextNode(shortDescription));
      }

      final Element numberBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      blockContainer.appendChild(numberBlock);
      numberBlock.appendChild(document.createTextNode(String.valueOf(index)));
      numberBlock.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_CENTER);
      numberBlock.setAttribute("font-weight", "bold");

      FOPUtils.addLeftBorder(cell, 1);
      FOPUtils.addTopBorder(cell, 1);
      if (first) {
        cell.setAttribute(String.format("%s:border-before-start-radius", FOPUtils.XSL_FOX_PREFIX), CORNER_RADIUS);
        first = false;
      }

      lastCell = cell;
      columnColor = new HSLColor(columnColor).adjustShade(DARKEN_PERCENTAGE);
      index = index
          + 1;
    }
    if (null != lastCell) {
      lastCell.setAttribute(String.format("%s:border-before-end-radius", FOPUtils.XSL_FOX_PREFIX), CORNER_RADIUS);
      FOPUtils.addRightBorder(lastCell, 1);
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
    final String virtualCategoryNames = virtualReferences.stream().map(VirtualSubjectiveScoreCategory::getTitle)
                                                         .collect(Collectors.joining(","));
    final Element commentsTable = FOPUtils.createBasicTable(document);

    commentsTable.appendChild(FOPUtils.createTableColumn(document, 1));
    commentsTable.appendChild(FOPUtils.createTableColumn(document, 1));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    commentsTable.appendChild(tableBody);

    final Element commentsLabelRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
    tableBody.appendChild(commentsLabelRow);

    final Element commentsLabel;
    if (virtualReferences.isEmpty()) {
      commentsLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Feedback Comments");
    } else {
      commentsLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                               String.format("Feedback Comments - also applies to %s",
                                                             virtualCategoryNames));
    }
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
      commentGreatJob = Utilities.NON_BREAKING_SPACE_STRING;
      commentThinkAbout = Utilities.NON_BREAKING_SPACE_STRING;
    } else {
      final String greatJobRaw = score.getCommentGreatJob();
      if (null == greatJobRaw) {
        commentGreatJob = Utilities.NON_BREAKING_SPACE_STRING;
      } else {
        commentGreatJob = greatJobRaw;
      }

      final String thinkAboutRaw = score.getCommentThinkAbout();
      if (null == thinkAboutRaw) {
        commentThinkAbout = Utilities.NON_BREAKING_SPACE_STRING;
      } else {
        commentThinkAbout = thinkAboutRaw;
      }

      rowElement.setAttribute("font-style", "italic");
      rowElement.setAttribute("font-weight", "bold");
    }

    final Element left = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
    rowElement.appendChild(left);
    FOPUtils.addRightBorder(left, 1);
    left.setAttribute("padding-right", RUBRIC_TABLE_PADDING);
    left.setAttribute("padding-left", RUBRIC_TABLE_PADDING);
    Arrays.stream(commentGreatJob.split("\\r?\\n")).forEach(comment -> {
      final Element commentBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      left.appendChild(commentBlock);
      commentBlock.appendChild(document.createTextNode(comment));
    });

    final Element right = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
    rowElement.appendChild(right);
    right.setAttribute("padding-right", RUBRIC_TABLE_PADDING);
    right.setAttribute("padding-left", RUBRIC_TABLE_PADDING);
    Arrays.stream(commentThinkAbout.split("\\r?\\n")).forEach(comment -> {
      final Element commentBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      right.appendChild(commentBlock);
      commentBlock.appendChild(document.createTextNode(comment));
    });

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

    if (!virtualReferences.isEmpty()) {
      final Element blankRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
      tableBody.appendChild(blankRow);
      final Element blankCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                         Utilities.NON_BREAKING_SPACE_STRING);
      blankRow.appendChild(blankCell);
      blankCell.setAttribute("number-columns-spanned", "2");

      final String legendText = String.format("%c Criteria on this page with this style of check box count dually toward %s and %s awards rankings",
                                              Utilities.NON_BREAKING_SPACE, scoreCategory.getTitle(),
                                              virtualCategoryNames);
      final Element legendRow = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_ROW_TAG);
      tableBody.appendChild(legendRow);

      final Element legendCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
      legendRow.appendChild(legendCell);
      legendCell.setAttribute("number-columns-spanned", "2");

      final Element legendContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
      legendCell.appendChild(legendContainer);

      final Element legendBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      legendContainer.appendChild(legendBlock);

      final Element imageGraphic = FOPUtils.createXslFoElement(document, "external-graphic");
      legendBlock.appendChild(imageGraphic);
      imageGraphic.setAttribute("src", String.format("url('data:image/png;base64,%s')", gearEmptyBase64));

      legendBlock.appendChild(document.createTextNode(legendText));
    }
    return commentsTable;
  }

  private static final float WATERMARK_OPACITY = 0.5f;

  private static String getJudgeInitials(final @Nullable SubjectiveScore score) {
    if (null == score) {
      return "";
    } else {
      final String judge = score.getJudge();
      final String initials = Arrays.stream(judge.split(" ")).map(s -> s.substring(0, 1)).collect(Collectors.joining());
      return initials;
    }

  }

}
