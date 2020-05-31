/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.awt.Color;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.util.FP;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.playoff.DatabaseTeamScore;
import fll.xml.ChallengeDescription;
import fll.xml.Goal;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;
import fll.xml.ScoreType;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.WinnerType;
import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Final computed scores report.
 */
@WebServlet("/report/FinalComputedScores")
public final class FinalComputedScores extends BaseFLLServlet {

  /**
   * If 2 scores are within this amount of each other they are
   * considered a tie.
   */
  public static final double TIE_TOLERANCE = 1E-6;

  private static final double HEADER_MARGIN_INCHES = 0.8;

  private static final double FOOTER_MARGIN_INCHES = 0.5;

  private static final FOPUtils.Margins PAGE_MARGINS = new FOPUtils.Margins(0.2, 0.2, 0.5, 0.5);

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/FinalComputedScores")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentID);

      final int percentageHurdle = TournamentParameters.getPerformanceAdvancementPercentage(connection, tournamentID);
      final double performanceHurdle;
      if (percentageHurdle > 0
          && percentageHurdle < 100) {
        // set to a realistic value
        performanceHurdle = percentageHurdle
            / 100.0;
      } else {
        performanceHurdle = 0;
      }

      final double standardMean = GlobalParameters.getStandardizedMean(connection);
      final double standardSigma = GlobalParameters.getStandardizedSigma(connection);

      final Set<Integer> bestTeams = determineTeamsMeetingPerformanceHurdle(performanceHurdle, connection, tournamentID,
                                                                            challengeDescription.getWinner());

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=finalComputedScores.pdf");

      final String challengeTitle = challengeDescription.getTitle();

      try {
        final Document document = generateReport(connection, challengeDescription, challengeTitle, tournament,
                                                 bestTeams, percentageHurdle, standardMean, standardSigma);
        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the final scoresPDF", e);
      }

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Determine which teams meet the performance hurdle.
   * This is computed per division and stored in mTeamsMeetingPerformanceHurdle.
   *
   * @param performanceHurdle the percentage hurdle as a floating point number
   *          between 0 and 1. Outside this range causes the return value to be
   *          empty.
   * @return the set of teams that have a good enough performance score
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Winner criteria determines the sort")
  private Set<Integer> determineTeamsMeetingPerformanceHurdle(final double performanceHurdle,
                                                              final Connection connection,
                                                              final int tournament,
                                                              final WinnerType winnerCriteria)
      throws SQLException {

    final Set<Integer> bestTeams = new HashSet<>();
    if (performanceHurdle <= 0
        || performanceHurdle >= 1) {
      return bestTeams;
    }

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT TeamNumber FROM performance_seeding_max, TournamentTeams" //
          + " WHERE performance_seeding_max.TeamNumber = TournamentTeams.TeamNumber" //
          + "  AND TournamentTeams.Tournament = ?" //
          + "  AND TournamentTeams.event_division = ?" //
          + " ORDER by performance_seeding_max.score "
          + winnerCriteria.getSortString());
      prep.setInt(1, tournament);

      for (final String division : Queries.getAwardGroups(connection)) {
        final Set<Integer> teamNumbers = Queries.getTeamNumbersInEventDivision(connection, tournament, division);
        final int numTeams = teamNumbers.size();
        final int hurdle = (int) Math.floor(numTeams
            * performanceHurdle);

        prep.setString(2, division);

        int count = 0;
        rs = prep.executeQuery();
        while (count < hurdle
            && rs.next()) {
          final int teamNumber = rs.getInt(1);
          bestTeams.add(teamNumber);
          ++count;
        }
        SQLFunctions.close(rs);
        rs = null;
      } // foreach division

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    return bestTeams;
  }

  private static final Color TOP_SCORE_BACKGROUND = Color.LIGHT_GRAY;

  /**
   * Generate the actual report.
   */
  private Document generateReport(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final String challengeTitle,
                                  final Tournament tournament,
                                  final Set<Integer> bestTeams,
                                  final int percentageHurdle,
                                  final double standardMean,
                                  final double standardSigma)
      throws SQLException, IOException {
    if (tournament.getTournamentID() != Queries.getCurrentTournament(connection)) {
      throw new FLLRuntimeException("Cannot generate final score report for a tournament other than the current tournament");
    }

    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    FOPUtils.createSimplePageMaster(document, layoutMasterSet, pageMasterName, FOPUtils.PAGE_LETTER_SIZE, PAGE_MARGINS,
                                    HEADER_MARGIN_INCHES, FOOTER_MARGIN_INCHES);

    final Element legend = createLegend(document, percentageHurdle, standardMean, standardSigma);

    for (final String awardGroup : Queries.getAwardGroups(connection)) {
      final Element pageSequence = createAwardGroupPageSequence(connection, document, challengeDescription,
                                                                challengeTitle, winnerCriteria, tournament,
                                                                pageMasterName, legend, bestTeams, awardGroup);
      rootElement.appendChild(pageSequence);

    }

    return document;
  }

  private Element createAwardGroupPageSequence(final Connection connection,
                                               final Document document,
                                               final ChallengeDescription challengeDescription,
                                               final String challengeTitle,
                                               final WinnerType winnerCriteria,
                                               final Tournament tournament,
                                               final String pageMasterName,
                                               final Element legend,
                                               final Set<Integer> bestTeams,
                                               final String awardGroup)
      throws SQLException {
    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);

    final Element headerContent = createAwardGroupHeader(document, challengeTitle, tournament, awardGroup);

    final Element header = FOPUtils.createXslFoElement(document, FOPUtils.STATIC_CONTENT_TAG);
    pageSequence.appendChild(header);
    header.setAttribute("flow-name", "xsl-region-before");
    header.appendChild(headerContent);

    pageSequence.appendChild(legend.cloneNode(true));

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);
    final SubjectiveScoreCategory[] subjectiveCategories = challengeDescription.getSubjectiveCategories()
                                                                               .toArray(new SubjectiveScoreCategory[0]);

    // Figure out how many subjective categories have weights > 0.
    final int nonZeroWeights = (int) challengeDescription.getSubjectiveCategories().stream()
                                                         .filter(c -> c.getWeight() > 0).count();

    final Element divTable = FOPUtils.createBasicTable(document);
    documentBody.appendChild(divTable);
    divTable.setAttribute("font-size", "8pt");

    divTable.appendChild(FOPUtils.createTableColumn(document, 30)); // org / team
    divTable.appendChild(FOPUtils.createTableColumn(document, 10)); // judging group
    divTable.appendChild(FOPUtils.createTableColumn(document, 10)); // weight

    for (int i = 0; i < nonZeroWeights; i++) {
      divTable.appendChild(FOPUtils.createTableColumn(document, 15));
    }

    divTable.appendChild(FOPUtils.createTableColumn(document, 15)); // performance
    divTable.appendChild(FOPUtils.createTableColumn(document, 15)); // overall

    final Element tableHeader = createTableHeader(document, subjectiveCategories, challengeDescription);
    divTable.appendChild(tableHeader);

    final Element tableBody = writeScores(connection, document, subjectiveCategories,
                                          challengeDescription.getPerformance(), awardGroup, winnerCriteria, tournament,
                                          bestTeams);
    divTable.appendChild(tableBody);

    return pageSequence;
  }

  private Element createLegend(Document document,
                               final int percentageHurdle,
                               final double standardMean,
                               final double standardSigma) {
    final Element staticContent = FOPUtils.createXslFoElement(document, FOPUtils.STATIC_CONTENT_TAG);
    staticContent.setAttribute("flow-name", "xsl-region-after");

    staticContent.setAttribute("text-align", FOPUtils.TEXT_ALIGN_RIGHT);
    staticContent.setAttribute("font-size", "10pt");

    if (percentageHurdle > 0
        && percentageHurdle < 100) {
      final Element hurdleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      final String hurdleText = String.format("* - teams in the top %d%% of performance scores\n", percentageHurdle);
      hurdleBlock.appendChild(document.createTextNode(hurdleText));
      staticContent.appendChild(hurdleBlock);
    }

    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    staticContent.appendChild(block);

    final String legendText = String.format("bold score - top team in a category & judging group (rank)\n%.2f == average ; %.2f = 1 standard deviation\n@ - zero score on required goal",
                                            standardMean, standardSigma);
    block.appendChild(document.createTextNode(legendText));

    return staticContent;
  }

  /**
   * @return {team number -> rank}
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Winner criteria determines the sort")
  private Map<Integer, Integer> gatherRankedPerformanceTeams(final Connection connection,
                                                             final WinnerType winnerCriteria,
                                                             final Tournament tournament,
                                                             final String awawrdGroup)
      throws SQLException {
    final Map<Integer, Integer> rankedTeams = new HashMap<>();

    // 1 - tournament
    // 2 - award group
    // 3 - category
    // 4 - goal group
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT final_scores.team_number, final_scores.final_score"
            + " FROM final_scores, TournamentTeams" //
            + " WHERE final_scores.Tournament = ?" //
            + " AND TournamentTeams.Tournament = final_scores.tournament" //
            + " AND TournamentTeams.event_division = ?"//
            + " AND TournamentTeams.TeamNumber = final_scores.team_number"//
            + " AND final_scores.category = ?" //
            + " AND final_scores.goal_group = ?" //
            + " ORDER BY final_scores.final_score "
            + winnerCriteria.getSortString())) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, awawrdGroup);
      prep.setString(3, PerformanceScoreCategory.CATEGORY_NAME);
      prep.setString(4, "");

      int numTied = 1;
      int rank = 0;
      double prevScore = Double.NaN;
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          double score = rs.getDouble(2);
          if (rs.wasNull()) {
            score = Double.NaN;
          }

          if (!FP.equals(score, prevScore, TIE_TOLERANCE)) {
            rank += numTied;
            numTied = 1;
          } else {
            ++numTied;
          }

          rankedTeams.put(teamNumber, rank);

          prevScore = score;
        } // foreach result
      } // result set
    } // prepared statement

    return rankedTeams;
  }

  /**
   * Used with
   * {@link FinalComputedScores#iterateOverSubjectiveScores(Connection, SubjectiveScoreCategory, WinnerType, Tournament, String, String, SubjectiveScoreVisitor)}.
   */
  @FunctionalInterface
  public interface SubjectiveScoreVisitor {
    /**
     * @param teamNumber the number of the team
     * @param score the score of the team
     * @param rank the rank of the team in the judging group
     */
    void visit(int teamNumber,
               double score,
               int rank);
  }

  /**
   * Iterate over the standardized scores for a subjective category.
   *
   * @param connection database connection
   * @param category the category to select scores for
   * @param winnerCriteria who is the winner
   * @param tournament which tournament to get scores for
   * @param awardGroup which award group
   * @param judgingStation which judging station
   * @param visitor called with the data
   * @throws SQLException if a database error occurs
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "winner criteria determines the sort")
  public static void iterateOverSubjectiveScores(final Connection connection,
                                                 final SubjectiveScoreCategory category,
                                                 final WinnerType winnerCriteria,
                                                 final Tournament tournament,
                                                 final String awardGroup,
                                                 final String judgingStation,
                                                 final SubjectiveScoreVisitor visitor)
      throws SQLException {

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT final_scores.team_number, final_scores.final_score"//
            + " FROM final_scores, TournamentTeams" //
            + " WHERE final_scores.tournament = ?" //
            + " AND TournamentTeams.Tournament = final_scores.tournament" //
            + " AND TournamentTeams.event_division = ?"//
            + " AND TournamentTeams.TeamNumber = final_scores.team_number"//
            + " AND TournamentTeams.judging_station = ?" //
            + " AND final_scores.category = ?" //
            + " AND final_scores.goal_group = ?" //
            + " ORDER BY final_scores.final_score"
            + " "
            + winnerCriteria.getSortString())) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, awardGroup);
      prep.setString(3, judgingStation);
      prep.setString(4, category.getName());
      prep.setString(5, "");

      int numTied = 1;
      int rank = 0;
      double prevScore = Double.NaN;
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          double score = rs.getDouble(2);
          if (rs.wasNull()) {
            score = Double.NaN;
          }

          if (!FP.equals(score, prevScore, TIE_TOLERANCE)) {
            rank += numTied;
            numTied = 1;
          } else {
            ++numTied;
          }

          visitor.visit(teamNumber, score, rank);

          prevScore = score;
        }

      } // try ResultSet
    } // try PreparedStatment
  }

  /**
   * @return category -> Judging Group -> team number -> rank
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name and winner criteria determines the sort")
  private Map<ScoreCategory, Map<String, Map<Integer, Integer>>> gatherRankedSubjectiveTeams(final Connection connection,
                                                                                             final SubjectiveScoreCategory[] subjectiveCategories,
                                                                                             final WinnerType winnerCriteria,
                                                                                             final Tournament tournament,
                                                                                             final String awardGroup)
      throws SQLException {
    final Map<ScoreCategory, Map<String, Map<Integer, Integer>>> retval = new HashMap<>();
    final List<String> judgingStations = Queries.getJudgingStations(connection, tournament.getTournamentID());

    for (final SubjectiveScoreCategory category : subjectiveCategories) {
      final Map<String, Map<Integer, Integer>> categoryRanks = new HashMap<>();

      for (final String judgingStation : judgingStations) {

        final Map<Integer, Integer> rankedTeams = new HashMap<>();

        iterateOverSubjectiveScores(connection, category, winnerCriteria, tournament, awardGroup, judgingStation,
                                    (teamNumber,
                                     score,
                                     rank) -> {
                                      rankedTeams.put(teamNumber, rank);
                                    });

        categoryRanks.put(judgingStation, rankedTeams);

      } // foreach judging station

      retval.put(category, categoryRanks);

    } // foreach category

    return retval;
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table name")
  private Element writeScores(final Connection connection,
                              Document document,
                              final SubjectiveScoreCategory[] subjectiveCategories,
                              final PerformanceScoreCategory performanceCategory,
                              final String awardGroup,
                              final WinnerType winnerCriteria,
                              final Tournament tournament,
                              final Set<Integer> bestTeams)
      throws SQLException {

    final Element tableBody = FOPUtils.createXslFoElement(document, "table-body");

    final Map<ScoreCategory, Map<String, Map<Integer, Integer>>> teamSubjectiveRanks = gatherRankedSubjectiveTeams(connection,
                                                                                                                   subjectiveCategories,
                                                                                                                   winnerCriteria,
                                                                                                                   tournament,
                                                                                                                   awardGroup);

    final Map<Integer, Integer> teamPerformanceRanks = gatherRankedPerformanceTeams(connection, winnerCriteria,
                                                                                    tournament, awardGroup);

    try (
        PreparedStatement overallPrep = connection.prepareStatement("SELECT Teams.Organization, Teams.TeamName, Teams.TeamNumber, overall_score, current_tournament_teams.judging_station" //
            + " FROM overall_scores, Teams, current_tournament_teams WHERE overall_scores.tournament = ?"//
            + " AND current_tournament_teams.event_division = ?"//
            + " AND current_tournament_teams.TeamNumber = Teams.TeamNumber" //
            + " AND current_tournament_teams.TeamNumber = overall_scores.team_number" //
            + " ORDER BY overall_scores.overall_score "
            + winnerCriteria.getSortString() //
            + ", Teams.TeamNumber" //
        )) {
      overallPrep.setInt(1, tournament.getTournamentID());
      overallPrep.setString(2, awardGroup);

      try (ResultSet overallResult = overallPrep.executeQuery()) {
        while (overallResult.next()) {
          final int teamNumber = overallResult.getInt(3);
          final String organization = overallResult.getString(1);
          final String teamName = overallResult.getString(2);
          final String judgingGroup = overallResult.getString(5);

          final double overallScore;
          final double ts = overallResult.getDouble(4);
          if (overallResult.wasNull()) {
            overallScore = Double.NaN;
          } else {
            overallScore = ts;
          }

          final Element row1 = FOPUtils.createXslFoElement(document, "table-row");
          tableBody.appendChild(row1);

          // The first row of the team table...
          // First column is organization name
          row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, organization));

          row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, judgingGroup));

          row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Raw:"));

          insertRawSubjectiveScoreColumns(connection, tournament, winnerCriteria.getSortString(), document,
                                          subjectiveCategories, teamNumber, row1);

          insertRawPerformanceScore(connection, performanceCategory.getScoreType(), document, teamNumber, row1);

          // The "Overall score" column is not filled in for raw scores
          row1.appendChild(FOPUtils.createTableCell(document, null, ""));

          // The second row of the team table...
          final Element row2 = FOPUtils.createXslFoElement(document, "table-row");
          tableBody.appendChild(row2);

          // First column contains the team # and name
          final String row2BorderColor = "gray";
          final double row2BorderWidth = 0.5;

          final Element teamNameCol = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                               String.format("%d %s", teamNumber, teamName));
          row2.appendChild(teamNameCol);
          FOPUtils.addBottomBorder(teamNameCol, row2BorderWidth, row2BorderColor);

          /// judging group is empty in the second row
          final Element blankCell = FOPUtils.createTableCell(document, null, "");
          row2.appendChild(blankCell);
          FOPUtils.addBottomBorder(blankCell, row2BorderWidth, row2BorderColor);

          // "Scaled:"
          final Element scaledCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Scaled:");
          row2.appendChild(scaledCell);
          FOPUtils.addBottomBorder(scaledCell, row2BorderWidth, row2BorderColor);

          // Next, one column containing the scaled score for each subjective
          // category with weight > 0
          for (final ScoreCategory category : subjectiveCategories) {
            final Map<String, Map<Integer, Integer>> catRanks = teamSubjectiveRanks.get(category);
            final Map<Integer, Integer> judgingRanks = catRanks.get(judgingGroup);

            final double catWeight = category.getWeight();
            if (catWeight > 0.0) {
              insertCategoryScaledScore(connection, tournament, document, teamNumber, row2, row2BorderWidth,
                                        row2BorderColor, category, judgingRanks);
            } // non-zero category weight
          } // foreach category

          // 2nd to last column has the scaled performance score
          insertCategoryScaledScore(connection, tournament, document, teamNumber, row2, row2BorderWidth,
                                    row2BorderColor, performanceCategory, teamPerformanceRanks);

          // Last column contains the overall scaled score
          final Element overallCell;
          if (Double.isNaN(overallScore)) {
            overallCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "No Score");
            overallCell.setAttribute("color", "red");
          } else {
            final String overallScoreSuffix;
            if (bestTeams.contains(teamNumber)) {
              overallScoreSuffix = String.format("%1$s*", Utilities.NON_BREAKING_SPACE);
            } else {
              overallScoreSuffix = String.format("%1$s%1$s", Utilities.NON_BREAKING_SPACE);
            }

            overallCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                   Utilities.getFloatingPointNumberFormat().format(overallScore)
                                                       + overallScoreSuffix);
          }
          row2.appendChild(overallCell);
          FOPUtils.addBottomBorder(overallCell, row2BorderWidth, row2BorderColor);
        } // foreach score result
      } // ResultSet
    } // PreparedStatement

    return tableBody;
  }

  private void insertCategoryScaledScore(final Connection connection,
                                         final Tournament tournament,
                                         final Document document,
                                         final int teamNumber,
                                         final Element row,
                                         final double borderWidth,
                                         final String borderColor,
                                         final ScoreCategory category,
                                         final Map<Integer, Integer> rankInCategory)
      throws SQLException {
    try (
        PreparedStatement finalScorePrep = connection.prepareStatement("SELECT final_score FROM final_scores WHERE category = ? AND goal_group = ? AND tournament = ? AND team_number = ?")) {
      finalScorePrep.setString(1, category.getName());
      finalScorePrep.setString(2, "");
      finalScorePrep.setInt(3, tournament.getTournamentID());
      finalScorePrep.setInt(4, teamNumber);
      try (ResultSet finalScoreResult = finalScorePrep.executeQuery()) {

        final double scaledScore;
        if (finalScoreResult.next()) {
          final double v = finalScoreResult.getDouble(1);
          if (finalScoreResult.wasNull()) {
            scaledScore = Double.NaN;
          } else {
            scaledScore = v;
          }
        } else {
          scaledScore = Double.NaN;
        }

        final int rank;
        if (rankInCategory.containsKey(teamNumber)) {
          rank = rankInCategory.get(teamNumber);
        } else {
          rank = -1;
        }

        final String rankText;
        if (-1 == rank) {
          rankText = String.format("%1$s%1$s%1$s%1$s%1$s", Utilities.NON_BREAKING_SPACE);
        } else {
          rankText = String.format("%1$s(%2$d)", Utilities.NON_BREAKING_SPACE, rank);
        }

        final String overallScoreText;
        if (Double.isNaN(scaledScore)) {
          overallScoreText = "No Score";
        } else {
          overallScoreText = Utilities.getFloatingPointNumberFormat().format(scaledScore);
        }

        final String scoreText = overallScoreText
            + rankText;

        final Element subjCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, scoreText);
        row.appendChild(subjCell);
        if (1 == rank) {
          subjCell.setAttribute("font-weight", "bold");
          subjCell.setAttribute("background-color", FOPUtils.renderColor(TOP_SCORE_BACKGROUND));
        }
        FOPUtils.addBottomBorder(subjCell, borderWidth, borderColor);
        if (Double.isNaN(scaledScore)) {
          subjCell.setAttribute("color", "red");
        }

      } // finalScoreResult
    } // finalScorePrep
  }

  private void insertRawPerformanceScore(final Connection connection,
                                         final ScoreType performanceScoreType,
                                         final Document document,
                                         final int teamNumber,
                                         final Element row)
      throws SQLException {
    try (PreparedStatement scorePrep = connection.prepareStatement("SELECT score FROM performance_seeding_max"
        + " WHERE TeamNumber = ?")) {

      // Column for the highest performance score of the seeding rounds
      scorePrep.setInt(1, teamNumber);

      try (ResultSet rawScoreRS = scorePrep.executeQuery()) {
        final double rawScore;
        if (rawScoreRS.next()) {
          final double v = rawScoreRS.getDouble(1);
          if (rawScoreRS.wasNull()) {
            rawScore = Double.NaN;
          } else {
            rawScore = v;
          }
        } else {
          rawScore = Double.NaN;
        }

        final String text;
        if (Double.isNaN(rawScore)) {
          text = "No Score";
        } else {
          text = Utilities.getFormatForScoreType(performanceScoreType).format(rawScore);
        }

        final Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, text);
        row.appendChild(cell);
        if (Double.isNaN(rawScore)) {
          cell.setAttribute("color", "red");
        }

      } // ResultSet
    } // PreparedStatement
  }

  private Element createTableHeader(final Document document,
                                    final SubjectiveScoreCategory[] subjectiveCategories,
                                    final ChallengeDescription challengeDescription) {

    final Element tableHeader = FOPUtils.createXslFoElement(document, "table-header");

    final Element row1 = FOPUtils.createXslFoElement(document, "table-row");
    tableHeader.appendChild(row1);
    row1.setAttribute("font-weight", "bold");

    row1.appendChild(FOPUtils.createTableCell(document, null, "Organization"));

    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Judging Group"));

    row1.appendChild(FOPUtils.createTableCell(document, null, "")); // weight/raw&scaled

    for (final SubjectiveScoreCategory category : subjectiveCategories) {
      final double weight = category.getWeight();
      if (weight > 0.0) {
        final String catTitle = category.getTitle();

        row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, catTitle));
      }
    }

    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Performance"));

    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Overall Score"));

    // row 2 needs a bottom border, the border is added to each cell

    final Element row2 = FOPUtils.createXslFoElement(document, "table-row");
    tableHeader.appendChild(row2);

    final Element teamNumCell = FOPUtils.createTableCell(document, null, "Team # / Team Name");
    row2.appendChild(teamNumCell);
    FOPUtils.addBottomBorder(teamNumCell, 1);
    teamNumCell.setAttribute("font-weight", "bold");

    final Element blankCell1 = FOPUtils.createTableCell(document, null, "");
    row2.appendChild(blankCell1);
    FOPUtils.addBottomBorder(blankCell1, 1);

    final Element weightCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Weight:");
    FOPUtils.addBottomBorder(weightCell, 1);
    row2.appendChild(weightCell);

    for (int cat = 0; cat < subjectiveCategories.length; cat++) {
      final ScoreCategory category = subjectiveCategories[cat];
      if (category.getWeight() > 0.0) {
        final Element catCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                         Double.toString(category.getWeight()));
        FOPUtils.addBottomBorder(catCell, 1);
        row2.appendChild(catCell);
      }
    }

    final PerformanceScoreCategory performanceElement = challengeDescription.getPerformance();
    final Element perfCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                      Double.toString(performanceElement.getWeight()));
    FOPUtils.addBottomBorder(perfCell, 1);
    row2.appendChild(perfCell);

    final Element blankCell2 = FOPUtils.createTableCell(document, null, "");
    FOPUtils.addBottomBorder(blankCell2, 1);
    row2.appendChild(blankCell2); // under overall score

    return tableHeader;
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Winner type is used to determine sort order")
  private void insertRawSubjectiveScoreColumns(final Connection connection,
                                               final Tournament tournament,
                                               final String ascDesc,
                                               final Document document,
                                               final SubjectiveScoreCategory[] subjectiveCategories,
                                               final int teamNumber,
                                               final Element row)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT computed_total"
        + " FROM subjective_computed_scores"
        + " WHERE team_number = ? AND tournament = ? AND category = ? AND goal_group = ? ORDER BY computed_total "
        + ascDesc)) {
      prep.setString(4, ""); // goal group empty will give score for whole category

      // Next, one column containing the raw score for each subjective
      // category with weight > 0
      for (final SubjectiveScoreCategory catElement : subjectiveCategories) {
        final double catWeight = catElement.getWeight();
        if (catWeight > 0.0) {
          final String catName = catElement.getName();
          prep.setInt(1, teamNumber);
          prep.setInt(2, tournament.getTournamentID());
          prep.setString(3, catName);
          try (ResultSet rs = prep.executeQuery()) {
            boolean scoreSeen = false;
            final StringBuilder rawScoreText = new StringBuilder();
            while (rs.next()) {
              final double v = rs.getDouble(1);
              if (!rs.wasNull()) {
                if (scoreSeen) {
                  rawScoreText.append(", ");
                } else {
                  scoreSeen = true;
                }
                rawScoreText.append(Utilities.getFormatForScoreType(catElement.getScoreType()).format(v));
              }
            }

            final String scoreText;
            if (!scoreSeen) {
              scoreText = "No Score";
            } else {
              final boolean zeroInRequiredGoal = checkZeroInRequiredGoal(connection, tournament, catElement,
                                                                         teamNumber);
              if (zeroInRequiredGoal) {
                rawScoreText.append(" @");
              }
              scoreText = rawScoreText.toString();
            }

            final Element subjCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, scoreText);
            if (!scoreSeen) {
              subjCell.setAttribute("color", "red");
            }
            row.appendChild(subjCell);

          } // ResultSet
        } // category weight greater than 0
      } // foreach subjective category
    } // PreparedStatement
  }

  /**
   * Check if there is a zero score for a required goal in the specified category.
   *
   * @param connection the database connection
   * @param tournament which tournament is being worked with
   * @param category the category to check
   * @param teamNumber the team to check
   * @return true if there is a zero in a required goal
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  public static boolean checkZeroInRequiredGoal(final Connection connection,
                                                final Tournament tournament,
                                                final ScoreCategory category,
                                                final int teamNumber)
      throws SQLException {
    final Set<Goal> requiredGoals = category.getAllGoals().stream().filter(g -> g instanceof Goal).map(g -> (Goal) g)
                                            .filter(Goal::isRequired).collect(Collectors.toSet());

    if (!requiredGoals.isEmpty()) {
      boolean zeroInRequiredGoal = false;

      try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
          + category.getName()
          + " WHERE TeamNumber = ? AND Tournament = ?")) {
        prep.setInt(1, teamNumber);
        prep.setInt(2, tournament.getTournamentID());
        try (ResultSet rs = prep.executeQuery()) {
          while (!zeroInRequiredGoal
              && rs.next()) {
            try (DatabaseTeamScore score = new DatabaseTeamScore(teamNumber, rs)) {

              final Iterator<Goal> iter = requiredGoals.iterator();
              while (!zeroInRequiredGoal
                  && iter.hasNext()) {
                final Goal goal = iter.next();
                final double goalScore = score.getRawScore(goal.getName());
                if (FP.equals(0, goalScore, TIE_TOLERANCE)) {
                  zeroInRequiredGoal = true;
                }
              }

            } // score
          }

        } // result set
      } // prepared statement

      return zeroInRequiredGoal;
    } else {
      // no required goals
      return false;
    }
  }

  private Element createAwardGroupHeader(final Document document,
                                         final String challengeTitle,
                                         final Tournament tournament,
                                         final String division) {
    final String sidePadding = "3pt";
    final double borderWidth = 0.5;

    final Element header = FOPUtils.createBasicTable(document);
    header.appendChild(FOPUtils.createTableColumn(document, 1));
    header.appendChild(FOPUtils.createTableColumn(document, 1));
    FOPUtils.addBorders(header, borderWidth, borderWidth, borderWidth, borderWidth);

    header.setAttribute("font-family", "Times");
    header.setAttribute("font-size", "12pt");

    final Element tableBody = FOPUtils.createXslFoElement(document, "table-body");
    header.appendChild(tableBody);

    final Element row1 = FOPUtils.createXslFoElement(document, "table-row");
    tableBody.appendChild(row1);

    final Element challengeTitleCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, challengeTitle);
    row1.appendChild(challengeTitleCell);
    challengeTitleCell.setAttribute("padding-left", sidePadding);

    final StringBuilder title = new StringBuilder();
    if (null != tournament.getDate()) {
      title.append(AwardsReport.DATE_FORMATTER.format(tournament.getDate())
          + " - ");
    }
    title.append(tournament.getDescription());
    final Element titleCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, title.toString());
    row1.appendChild(titleCell);
    titleCell.setAttribute("padding-right", sidePadding);

    final Element row2 = FOPUtils.createXslFoElement(document, "table-row");
    tableBody.appendChild(row2);

    final Element finalScoresCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                             "Final Computed Scores");
    row2.appendChild(finalScoresCell);
    finalScoresCell.setAttribute("padding-left", sidePadding);

    final Element awardGroupCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Award Group: "
        + division);
    row2.appendChild(awardGroupCell);
    awardGroupCell.setAttribute("padding-right", sidePadding);

    return header;
  }

} // class FinalComputedScores
