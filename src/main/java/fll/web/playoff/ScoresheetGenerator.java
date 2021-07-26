/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.Version;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.util.FP;
import fll.web.WebUtils;
import fll.web.report.FinalComputedScores;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.EnumeratedValue;
import fll.xml.GoalElement;
import fll.xml.GoalGroup;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * @author Dan Churchill
 */
public class ScoresheetGenerator {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String LONG_BLANK = "_________________________";

  private static final String SHORT_BLANK = "______";

  private static final float WATERMARK_OPACITY = 0.2f;

  private final String tournamentName;

  private final ChallengeDescription description;

  /**
   * Create PDF with the specified number of sheets.
   * Initially all sheets
   * are empty. They should be filled in using the set methods.
   *
   * @param numSheets the number of sheets on a page
   * @param tournamentName the name of the tournament to display
   * @param description the challenge description to get the goals from
   */
  public ScoresheetGenerator(final int numSheets,
                             final ChallengeDescription description,
                             final String tournamentName) {
    this.tournamentName = tournamentName;
    this.numSheets = numSheets;
    this.description = Objects.requireNonNull(description);
    initializeArrays();

    for (int i = 0; i < this.numSheets; i++) {
      this.table[i] = SHORT_BLANK;
      this.name[i] = LONG_BLANK;
      this.round[i] = SHORT_BLANK;
      this.number[i] = null;
      this.time[i] = null;
      this.divisionLabel[i] = AWARD_GROUP_LABEL;
      this.division[i] = SHORT_BLANK;
    }
  }

  /**
   * Generate blank score sheets. One is regular and one is practice.
   * 
   * @param description the challenge description
   */
  public ScoresheetGenerator(final ChallengeDescription description) {
    this.tournamentName = "Example";
    this.description = Objects.requireNonNull(description);

    // must have been called asking for blank
    this.numSheets = 2;
    initializeArrays();

    for (int i = 0; i < numSheets; i++) {
      this.table[i] = SHORT_BLANK;
      this.name[i] = LONG_BLANK;
      this.round[i] = Utilities.isOdd(i) ? "Practice" : SHORT_BLANK;
      this.divisionLabel[i] = AWARD_GROUP_LABEL;
      this.division[i] = SHORT_BLANK;
      this.number[i] = null;
      this.time[i] = null;
      this.isPractice[i] = Utilities.isOdd(i);
    }
  }

  /**
   * Create a new ScoresheetGenerator object populated with form header data
   * provided in the given servlet request. The request should contain the
   * attributes listed below (this matches the
   * expected format of the request returned):
   * <ul>
   * <li><b>"numMatches"</b> - The number of matches in the form.
   * <li><b>"checkX"</b> - Present only for matches that should be printed.
   * <li><b>"roundX"</b> - For X = 1, 2, ... numMatches. Playoff round number
   * for scoresheet X.
   * <li><b>"teamAX"</b> - For X = 1, 2, ... numMatches. Team number of team on
   * table A for match X.
   * <li><b>"tableAX"</b> - For X = 1, 2, ... numTeams. Table assignment for
   * table A on match X.
   * <li><b>"teamBX"</b> - For X = 1, 2, ... numMatches. Team number of team on
   * table B for match X.
   * <li><b>"tableBX"</b> - For X = 1, 2, ... numTeams. Table assignment for
   * table B on match X.
   * </ul>
   * If any of the above objects don't exist in the Map, blank lines will be
   * placed in the generated form for that field. This also updates the
   * PlayoffData table in the assumption that the created scoresheet will be
   * printed, so table assignments are stored into the database and the match is
   * marked as printed. It is very questionable whether this is where this
   * should happen, but I don't feel like breaking it out.
   * 
   * @param request used to get information about award group and number of
   *          matches
   * @param connection where to find database information
   * @param tournament the tournament ID
   * @param description description of the challenge
   * @throws SQLException on a database error
   */
  public ScoresheetGenerator(final HttpServletRequest request,
                             final Connection connection,
                             final int tournament,
                             final ChallengeDescription description)
      throws SQLException {
    this.description = Objects.requireNonNull(description);
    final Tournament tournamentObj = Tournament.findTournamentByID(connection, tournament);
    this.tournamentName = tournamentObj.getName();
    final ScoreType performanceScoreType = description.getPerformance().getScoreType();

    final String numMatchesStr = WebUtils.getNonNullRequestParameter(request, "numMatches");

    final String division = WebUtils.getNonNullRequestParameter(request, "division");

    // called with specific sheets to print
    final int numMatches = Integer.parseInt(numMatchesStr);

    // ignore slot index 0 since the parameters are 1-based
    final boolean[] checkedMatches = new boolean[numMatches
        + 1];
    int checkedMatchCount = 0;
    // Build array of out how many matches we are printing
    for (int i = 1; i <= numMatches; i++) {
      final String checkX = "print"
          + i;
      checkedMatches[i] = !StringUtils.isBlank(request.getParameter(checkX));
      if (checkedMatches[i]) {
        checkedMatchCount++;
      }
    }

    if (checkedMatchCount == 0) {
      throw new FLLRuntimeException("No matches were found checked. Please go back and select the checkboxes for the scoresheets that you want to print");
    }

    this.numSheets = checkedMatchCount
        * 2;

    initializeArrays();

    // Loop through checked matches, populate data, and update database to
    // track
    // printed status and remember assigned tables.
    try (
        PreparedStatement updatePrep = connection.prepareStatement("UPDATE PlayoffData SET Printed=true, AssignedTable=?"
            + " WHERE event_division=? AND Tournament=? AND PlayoffRound=? AND Team=?")) {
      // could do division here, too, but since getting it from Team object,
      // will defer to same place as other
      updatePrep.setInt(3, tournament);

      int j = 0;
      for (int i = 1; i <= numMatches; i++) {
        if (checkedMatches[i]) {
          final String round = WebUtils.getNonNullRequestParameter(request, "round"
              + i);
          final int playoffRound = Integer.parseInt(round);

          // Get teamA info
          final Team teamA = Team.getTeamFromDatabase(connection,
                                                      Integer.parseInt(WebUtils.getNonNullRequestParameter(request,
                                                                                                           "teamA"
                                                                                                               + i)));
          this.name[j] = teamA.getTrimmedTeamName();
          this.number[j] = teamA.getTeamNumber();
          this.round[j] = "Round P"
              + round;
          this.table[j] = WebUtils.getNonNullRequestParameter(request, "tableA"
              + i);

          final int performanceRunA = Playoff.getRunNumber(connection, division, teamA.getTeamNumber(), playoffRound);
          this.divisionLabel[j] = HEAD_TO_HEAD_LABEL;
          this.division[j] = division;
          final int bracketA = Playoff.getBracketNumber(connection, tournament, teamA.getTeamNumber(), performanceRunA);
          final String bracketALabel = String.format("Match %d", bracketA);
          this.time[j] = bracketALabel;

          updatePrep.setString(1, table[j]);
          updatePrep.setString(2, division);
          updatePrep.setInt(4, playoffRound);
          updatePrep.setInt(5, teamA.getTeamNumber());
          if (updatePrep.executeUpdate() < 1) {
            LOGGER.warn(String.format("Could not update playoff table and print flags for team: %s playoff round: %s playoff bracket: %s",
                                      teamA.getTeamNumber(), playoffRound, division));
          } else {
            // update the brackets with the table name
            H2HUpdateWebSocket.updateBracket(connection, performanceScoreType, division, teamA, performanceRunA);
          }
          j++;

          // Get teamB info
          final Team teamB = Team.getTeamFromDatabase(connection,
                                                      Integer.parseInt(WebUtils.getNonNullRequestParameter(request,
                                                                                                           "teamB"
                                                                                                               + i)));
          this.name[j] = teamB.getTrimmedTeamName();
          this.number[j] = teamB.getTeamNumber();
          this.round[j] = "Round P"
              + round;
          this.table[j] = WebUtils.getNonNullRequestParameter(request, "tableB"
              + i);

          final int performanceRunB = Playoff.getRunNumber(connection, division, teamB.getTeamNumber(), playoffRound);
          this.divisionLabel[j] = HEAD_TO_HEAD_LABEL;
          this.division[j] = division;
          final int bracketB = Playoff.getBracketNumber(connection, tournament, teamB.getTeamNumber(), performanceRunB);
          final String bracketBLabel = String.format("Match %d", bracketB);
          this.time[j] = bracketBLabel;

          updatePrep.setString(1, table[j]);
          updatePrep.setString(2, division);
          updatePrep.setInt(4, playoffRound);
          updatePrep.setInt(5, teamB.getTeamNumber());
          if (updatePrep.executeUpdate() < 1) {
            LOGGER.warn(String.format("Could not update playoff table and print flags for team: %s playoff round: %s playoff bracket: %s",
                                      teamB.getTeamNumber(), playoffRound, division));
          } else {
            // update the brackets with the table name
            H2HUpdateWebSocket.updateBracket(connection, performanceScoreType, division, teamB, performanceRunB);
          }
          j++;
        }
      }
    }
  }

  /**
   * Private support function to create new data arrays for the scoresheet
   * information. IMPORTANT!!! The value of {@link #numSheets} must be set
   * before the
   * call to this method is made.
   */
  @EnsuresNonNull({ "table", "name", "round", "number", "divisionLabel", "division", "time", "isPractice" })
  private void initializeArrays(@UnderInitialization ScoresheetGenerator this) {
    table = new String[numSheets];
    name = new String[numSheets];
    round = new String[numSheets];
    number = new Integer[numSheets];
    divisionLabel = new String[numSheets];
    division = new String[numSheets];
    time = new String[numSheets];
    isPractice = new boolean[numSheets];
  }

  /**
   * @param out where to write the PDF
   * @throws IOException if there is a problem writing to the output
   */
  public void writeFile(final OutputStream out) throws IOException {

    try {
      final Document performanceDoc = createDocument();
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, performanceDoc, out);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private Document createDocument() {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);
    rootElement.setAttribute("font-family", "Helvetica");

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element footer = FOPUtils.createCopyrightFooter(document, description);
    if (null != footer) {
      pageSequence.appendChild(footer);
    }

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element titleHeader = createTitleBlock(document);
    final Element goalsTable = createGoalsTable(document);
    final Element checkBlock = createCheckBlock(document);
    final Element practiceWatermark = FOPUtils.createWatermark(document, "PRACTICE", WATERMARK_OPACITY);

    for (int sheetIndex = 0; sheetIndex < numSheets; sheetIndex++) {
      final Element sheet = createScoreSheet(document, titleHeader, goalsTable, checkBlock, practiceWatermark,
                                             sheetIndex);
      documentBody.appendChild(sheet);
    }

    return document;
  }

  private Element createScoreSheet(final Document document,
                                   final Element titleHeader,
                                   final Element goalsTable,
                                   final Element checkBlock,
                                   final Element practiceWatermark,
                                   final int sheetIndex) {
    final Element sheet = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    sheet.setAttribute("page-break-after", "always");

    if (isPractice[sheetIndex]) {
      sheet.appendChild(practiceWatermark.cloneNode(true));
    }

    sheet.appendChild(titleHeader.cloneNode(true));

    final Element teamInfoContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    sheet.appendChild(teamInfoContainer);
    teamInfoContainer.setAttribute("padding-bottom", "10pt");
    teamInfoContainer.setAttribute("padding-top", "10pt");
    FOPUtils.addBottomBorder(teamInfoContainer, 1.0);

    final Element teamInfo = createTeamInfoBlock(document, sheetIndex);
    teamInfoContainer.appendChild(teamInfo);

    final Element goalsTableContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    sheet.appendChild(goalsTableContainer);
    goalsTableContainer.setAttribute("padding-bottom", "10pt");

    goalsTableContainer.appendChild(goalsTable.cloneNode(true));

    sheet.appendChild(checkBlock.cloneNode(true));

    return sheet;
  }

  private Element createTeamInfoBlock(final Document document,
                                      final int sheetIndex) {
    final Element table = FOPUtils.createBasicTable(document);
    table.setAttribute("font-size", "10pt");

    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 10));
    table.appendChild(FOPUtils.createTableColumn(document, 9));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final Element row1 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row1);

    final Element timeLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Time:");
    row1.appendChild(timeLabel);
    timeLabel.setAttribute("padding-top", "2pt");
    timeLabel.setAttribute("padding-bottom", "2pt");

    final Element timeValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                       null == time[sheetIndex] ? SHORT_BLANK : time[sheetIndex]);
    row1.appendChild(timeValue);
    timeValue.setAttribute("font-family", "Courier");
    timeValue.setAttribute("padding-top", "2pt");
    timeValue.setAttribute("padding-bottom", "2pt");

    final Element tableLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Table:");
    row1.appendChild(tableLabel);
    tableLabel.setAttribute("padding-top", "2pt");
    tableLabel.setAttribute("padding-bottom", "2pt");

    final Element tableValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                        null == this.table[sheetIndex] ? SHORT_BLANK
                                                            : this.table[sheetIndex]);
    tableValue.setAttribute("font-family", "Courier");
    tableValue.setAttribute("padding-top", "2pt");
    tableValue.setAttribute("padding-bottom", "2pt");
    row1.appendChild(tableValue);

    final Element roundLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Round:");
    row1.appendChild(roundLabel);
    roundLabel.setAttribute("padding-top", "2pt");
    roundLabel.setAttribute("padding-bottom", "2pt");

    final Element roundValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                        null == round[sheetIndex] ? SHORT_BLANK : round[sheetIndex]);
    roundValue.setAttribute("font-family", "Courier");
    roundValue.setAttribute("padding-top", "2pt");
    roundValue.setAttribute("padding-bottom", "2pt");
    row1.appendChild(roundValue);

    final Element refSig = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Ref ____");
    row1.appendChild(refSig);
    refSig.setAttribute("font-size", "8pt");
    refSig.setAttribute("padding-top", "2pt");
    refSig.setAttribute("padding-bottom", "2pt");

    final Element row2 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row2);

    final Element teamNumberLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Team #:");
    row2.appendChild(teamNumberLabel);
    teamNumberLabel.setAttribute("padding-top", "2pt");
    teamNumberLabel.setAttribute("padding-bottom", "2pt");

    final Element teamNumberValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                             null == number[sheetIndex] ? SHORT_BLANK
                                                                 : String.valueOf(number[sheetIndex]));
    teamNumberValue.setAttribute("font-family", "Courier");
    row2.appendChild(teamNumberValue);
    teamNumberValue.setAttribute("padding-top", "2pt");
    teamNumberValue.setAttribute("padding-bottom", "2pt");

    final Element awardGroupLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                             divisionLabel[sheetIndex]);
    awardGroupLabel.setAttribute("number-columns-spanned", "2");
    awardGroupLabel.setAttribute("padding-top", "2pt");
    awardGroupLabel.setAttribute("padding-bottom", "2pt");
    row2.appendChild(awardGroupLabel);

    final Element awardGroupValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                             null == division[sheetIndex] ? SHORT_BLANK
                                                                 : division[sheetIndex]);
    awardGroupValue.setAttribute("font-family", "Courier");
    awardGroupValue.setAttribute("number-columns-spanned", "2");
    awardGroupValue.setAttribute("padding-top", "2pt");
    awardGroupValue.setAttribute("padding-bottom", "2pt");
    row2.appendChild(awardGroupValue);

    final Element teamSig = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Team ____");
    row2.appendChild(teamSig);
    teamSig.setAttribute("font-size", "8pt");
    teamSig.setAttribute("padding-top", "2pt");
    teamSig.setAttribute("padding-bottom", "2pt");

    final Element row3 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row3);

    final Element teamNameLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Team Name:");
    row3.appendChild(teamNameLabel);
    teamNameLabel.setAttribute("number-columns-spanned", "2");
    teamNameLabel.setAttribute("padding-top", "2pt");
    teamNameLabel.setAttribute("padding-bottom", "2pt");

    final Element teamNameValue = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                                 null == name[sheetIndex] ? LONG_BLANK
                                                                     : name[sheetIndex]);
    row3.appendChild(teamNameValue);
    teamNameValue.setAttribute("number-columns-spanned", "4");
    teamNameValue.setAttribute("font-family", "Courier");
    teamNameValue.setAttribute("padding-top", "2pt");
    teamNameValue.setAttribute("padding-bottom", "2pt");

    final Element tournament = FOPUtils.createNoWrapTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                              null == tournamentName ? "___" : tournamentName);
    row3.appendChild(tournament);
    tournament.setAttribute("font-size", "6pt");
    tournament.setAttribute("font-style", "italic");
    tournament.setAttribute("padding-top", "2pt");
    tournament.setAttribute("padding-bottom", "2pt");

    return table;
  }

  private Element createTitleBlock(final Document document) {
    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.setAttribute("background-color", "black");

    final Element title = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.appendChild(title);
    title.setAttribute("font-size", "14pt");
    title.setAttribute("font-weight", "bold");
    title.setAttribute("text-align", "center");
    title.setAttribute("color", "white");
    title.appendChild(document.createTextNode(description.getTitle()));

    final Element version = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.appendChild(version);
    version.setAttribute("font-size", "8pt");
    version.setAttribute("text-align", "center");
    version.setAttribute("color", "white");

    final StringBuilder versionText = new StringBuilder();
    versionText.append("SW version: ");
    versionText.append(Version.getVersion());
    if (null != description.getRevision()) {
      versionText.append(" Descriptor revision: ");
      versionText.append(description.getRevision());
    }
    version.appendChild(document.createTextNode(versionText.toString()));

    return block;
  }

  private Element createCheckBlock(final Document document) {
    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.setAttribute("font-size", "8pt");
    block.setAttribute("text-align", "center");
    // ensure that the text takes up the whole line
    block.setAttribute("text-align-last", "justify");

    block.appendChild(document.createTextNode("Data Entry Score _______"));

    // add leader to take up the space between the 2 blocks of text
    final Element space = FOPUtils.createHorizontalSpace(document);
    block.appendChild(space);

    block.appendChild(document.createTextNode("2nd Check Initials _______"));

    return block;
  }

  private Element createGoalsTable(final Document document) {

    final PerformanceScoreCategory performanceElement = description.getPerformance();

    final Element goalsTable = FOPUtils.createBasicTable(document);
    goalsTable.setAttribute("font-size", "10pt");
    goalsTable.setAttribute("font-family", "Helvetica");
    goalsTable.appendChild(FOPUtils.createTableColumn(document, 4));
    goalsTable.appendChild(FOPUtils.createTableColumn(document, 48));
    goalsTable.appendChild(FOPUtils.createTableColumn(document, 48));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    goalsTable.appendChild(tableBody);

    boolean firstRow = true;
    for (final GoalElement ge : performanceElement.getGoalElements()) {
      if (ge.isGoal()) {
        final AbstractGoal goal = (AbstractGoal) ge;
        if (!goal.isComputed()) {
          final Element row = outputGoal(document, goal, "", null);
          tableBody.appendChild(row);

          if (firstRow) {
            FOPUtils.addTopBorder(row, 1);
            firstRow = false;
          }
        }
      } else if (ge.isGoalGroup()) {
        final GoalGroup group = (GoalGroup) ge;
        outputGoalGroup(document, group, tableBody);

        firstRow = false;
      } else {
        throw new FLLInternalException("Unknown goal element type: "
            + ge.getClass());
      }
    } // foreach goal element

    return goalsTable;
  }

  private void outputGoalGroup(final Document document,
                               final GoalGroup group,
                               final Element tableBody) {
    final String goalGroupTitle = group.getTitle();
    final List<AbstractGoal> nonComputedGoals = group.getGoals().stream()
                                                     .filter(Predicate.not(AbstractGoal::isComputed))
                                                     .collect(Collectors.toList());
    final Element row = FOPUtils.createTableRow(document);
    FOPUtils.addTopBorder(row, 1);

    if (!StringUtils.isBlank(goalGroupTitle)) {
      final int categoryRowSpan = nonComputedGoals.size();

      // One should be able to just use the reference-orientation property on the
      // text cell. However when this is done the cells aren't properly sized and the
      // text gets put in the wrong place.
      //
      // Jon Schewe sent an email to the Apache FOP list 5/9/2020 and didn't find an
      // answer.
      // http://mail-archives.apache.org/mod_mbox/xmlgraphics-fop-users/202005.mbox/%3Cd8da02c550c0271943a651c13d7218377efc7137.camel%40mtu.net%3E
      // Bug https://issues.apache.org/jira/browse/FOP-2946 is open for this
      final Element categoryCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
      row.appendChild(categoryCell);
      categoryCell.setAttribute("number-rows-spanned", String.valueOf(categoryRowSpan));

      final Element categoryCellContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
      categoryCell.appendChild(categoryCellContainer);
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
      text.setAttribute("style", "fill: black; font-family:Helvetica; font-size: 12px; font-style:normal;");
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
      tspan.appendChild(document.createTextNode(goalGroupTitle));
    } // non-blank goal group title

    boolean firstGoal = true;
    for (final AbstractGoal goal : nonComputedGoals) {
      final Element rowToAppendTo;
      if (firstGoal) {
        rowToAppendTo = row;
        firstGoal = false;
      } else {
        rowToAppendTo = null;
      }
      final Element goalRow = outputGoal(document, goal, goalGroupTitle, rowToAppendTo);
      tableBody.appendChild(goalRow);
    }
  }

  /**
   * @param document used to create elements
   * @param goal the goal to output
   * @param goalGroupTitle the goal group that this goal is a member of, if blank
   *          or null then the goal is not in a goal group
   * @param rowToAppendTo if not null, then use this row rather than creating a
   *          new one
   * @return the row to add to the table body
   */
  private Element outputGoal(final Document document,
                             final AbstractGoal goal,
                             final @Nullable String goalGroupTitle,
                             final @Nullable Element rowToAppendTo) {
    final Element row;
    if (null == rowToAppendTo) {
      row = FOPUtils.createTableRow(document);
    } else {
      row = rowToAppendTo;
    }

    // This is the text for the left hand "label" cell
    final Element goalLabel = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, goal.getTitle());
    row.appendChild(goalLabel);
    goalLabel.setAttribute("padding-top", "2pt");
    goalLabel.setAttribute("padding-bottom", "2pt");
    goalLabel.setAttribute("padding-right", "9pt");

    if (StringUtils.isBlank(goalGroupTitle)) {
      // category column and goal label column
      goalLabel.setAttribute("number-columns-spanned", "2");
    }

    // define the value cell
    final double min = goal.getMin();
    final String minStr = FP.equals(min, Math.round(min), FinalComputedScores.TIE_TOLERANCE) ? String.valueOf((int) min)
        : String.valueOf(min);
    final double max = goal.getMax();
    final String maxStr = FP.equals(max, Math.round(max), FinalComputedScores.TIE_TOLERANCE) ? String.valueOf((int) max)
        : String.valueOf(max);

    // If element has child nodes, then we have an enumerated list
    // of choices. Otherwise it is either yes/no or a numeric field.
    final Element goalValue;
    if (goal.isEnumerated()) {
      final StringBuilder choices = new StringBuilder();

      // replace spaces with "no-break" spaces
      boolean first = true;
      final List<EnumeratedValue> values = goal.getSortedValues();
      for (final EnumeratedValue value : values) {
        if (!first) {
          choices.append(" / ");
        } else {
          first = false;
        }
        choices.append(value.getTitle().toUpperCase().replace(' ', Utilities.NON_BREAKING_SPACE));
      }
      goalValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, choices.toString());
    } else {
      if (goal.isYesNo()) {
        // order of yes/no needs to match ScoreEntry.generateYesNoButtons
        goalValue = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, "NO / YES");
      } else {
        goalValue = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);

        final String range = "("
            + minStr
            + " - "
            + maxStr
            + ")";
        final Element t = FOPUtils.createBasicTable(document);
        goalValue.appendChild(t);
        t.setAttribute("width", "100pt");
        t.setAttribute("font-size", "8pt");
        t.appendChild(FOPUtils.createTableColumn(document, 1));
        t.appendChild(FOPUtils.createTableColumn(document, 1));

        final Element tBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
        t.appendChild(tBody);

        final Element tRow = FOPUtils.createTableRow(document);
        tBody.appendChild(tRow);

        final Element r = FOPUtils.createTableCell(document, null, "");
        tRow.appendChild(r);
        FOPUtils.addBorders(r, 1, 1, 1, 1);

        final Element q = FOPUtils.createTableCell(document, null, range);
        tRow.appendChild(q);
        FOPUtils.addBorders(q, 1, 1, 1, 1);
      }
    }
    goalValue.setAttribute("font-family", "Courier");
    goalValue.setAttribute("padding-top", "2pt");
    goalValue.setAttribute("padding-bottom", "2pt");
    row.appendChild(goalValue);

    return row;
  }

  private final int numSheets;

  private String[] table;

  private String[] name;

  private String[] round;

  private @Nullable Integer[] number;

  private static final String HEAD_TO_HEAD_LABEL = "Head to head Bracket:";

  /**
   * Label used for award groups.
   */
  public static final String AWARD_GROUP_LABEL = "Award Group:";

  private String[] divisionLabel;

  private String[] division;

  private @Nullable String[] time;

  private boolean[] isPractice;

  /**
   * Sets the table label for scoresheet with index i.
   *
   * @param i The 0-based index of the scoresheet to which to assign this table
   *          label.
   * @param table A string with the table label for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setTable(final int i,
                       final String table)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + numSheets);
    }
    this.table[i] = table;
  }

  /**
   * Sets the division for scoresheet with index i.
   *
   * @param i The 0-based index of the scoresheet to which to assign this table
   *          label.
   * @param divisionLabel the label to use for this division should be
   *          {@link #HEAD_TO_HEAD_LABEL} or {@link #AWARD_GROUP_LABEL}
   * @param division the string to display in the division section of the score
   *          sheet
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setDivision(final int i,
                          final String divisionLabel,
                          final String division)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + numSheets);
    }
    this.divisionLabel[i] = divisionLabel;
    this.division[i] = division;
  }

  /**
   * Sets the team name for scoresheet with index i.
   *
   * @param i The 0-based index of the scoresheet to which to assign this team
   *          name.
   * @param name A string with the team name for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setName(final int i,
                      final String name)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + numSheets);
    }
    this.name[i] = name;
  }

  /**
   * Sets the team number for scoresheet with index i.
   *
   * @param i The 0-based index of the scoresheet to which to assign this team
   *          number.
   * @param number A string with the team number for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setNumber(final int i,
                        final Integer number)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + numSheets);
    }
    this.number[i] = number;
  }

  /**
   * Sets the time for scoresheet with index i.
   *
   * @param i The 0-based index of the scoresheet to which to assign this time.
   * @param time the time for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setTime(final int i,
                      final LocalTime time)
      throws IllegalArgumentException {
    setTime(i, TournamentSchedule.formatTime(time));
  }

  /**
   * Puts an arbitrary string in the time field.
   *
   * @param i The 0-based index of the scoresheet to which to assign this time.
   * @param time the time for the specified scoresheet.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  private void setTime(final int i,
                       final String time)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + numSheets);
    }
    this.time[i] = time;
  }

  /**
   * Sets the round number descriptor for scoresheet with index i.
   *
   * @param i The 0-based index of the scoresheet to which to assign this round
   *          number.
   * @param round A string with the round number descriptor for the specified
   *          scoresheet. Should be a 1-based number.
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setRound(final int i,
                       final String round)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + numSheets);
    }
    this.round[i] = round;
  }

  /**
   * Sets if the score sheet for the specified index is a practice round.
   *
   * @param i The 0-based index of the score sheet
   * @param isPractice true if this is a practice round
   * @throws IllegalArgumentException Thrown if the index is out of valid range.
   */
  public void setPractice(final int i,
                          final boolean isPractice)
      throws IllegalArgumentException {
    if (i < 0) {
      throw new IllegalArgumentException("Index must not be < 0");
    }
    if (i >= numSheets) {
      throw new IllegalArgumentException("Index must be < "
          + numSheets);
    }
    this.isPractice[i] = isPractice;
  }

  /**
   * Used by {@link WatermarkHandler}.
   *
   * @return If index is out of bounds, return false.
   */
  /* package */ boolean isPractice(final int index) {
    if (index >= isPractice.length) {
      return false;
    } else {
      return isPractice[index];
    }
  }

}
