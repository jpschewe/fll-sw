/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
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

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FLLRuntimeException;
import fll.util.FP;
import fll.util.PdfUtils;
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

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/FinalComputedScores")) {
      return;
    }

    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

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

      generateReport(connection, response.getOutputStream(), challengeDescription, challengeTitle, tournament,
                     bestTeams, percentageHurdle, standardMean, standardSigma);

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
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

  private static final Font ARIAL_8PT_BOLD = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD);

  private static final Font ARIAL_8PT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);

  private static final Font ARIAL_8PT_NORMAL_RED = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL,
                                                                       BaseColor.RED);

  private static final Font TIMES_12PT_NORMAL = FontFactory.getFont(FontFactory.TIMES, 12, Font.NORMAL);

  /**
   * Generate the actual report.
   */
  private void generateReport(final Connection connection,
                              final OutputStream out,
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

    try {
      // This creates our new PDF document and declares it to be in portrait
      // orientation
      final Document pdfDoc = PdfUtils.createPortraitPdfDoc(out, null);

      final Iterator<String> agIter = Queries.getAwardGroups(connection).iterator();
      while (agIter.hasNext()) {
        final String awardGroup = agIter.next();

        final SubjectiveScoreCategory[] subjectiveCategories = challengeDescription.getSubjectiveCategories()
                                                                                   .toArray(new SubjectiveScoreCategory[0]);

        // Figure out how many subjective categories have weights > 0.
        final int nonZeroWeights = (int) challengeDescription.getSubjectiveCategories().stream()
                                                             .filter(c -> c.getWeight() > 0).count();

        // Array of relative widths for the columns of the score page
        // Array length varies with the number of subjective scores weighted >
        // 0.
        final int numColumnsLeftOfSubjective = 3;
        final int numColumnsRightOfSubjective = 2;
        final float[] relativeWidths = new float[numColumnsLeftOfSubjective
            + nonZeroWeights
            + numColumnsRightOfSubjective];
        relativeWidths[0] = 3f;
        relativeWidths[1] = 1.0f;
        relativeWidths[2] = 1.0f;
        relativeWidths[relativeWidths.length
            - numColumnsRightOfSubjective] = 1.5f;
        relativeWidths[relativeWidths.length
            - numColumnsRightOfSubjective
            + 1] = 1.5f;
        for (int i = numColumnsLeftOfSubjective; i < numColumnsLeftOfSubjective
            + nonZeroWeights; i++) {
          relativeWidths[i] = 1.5f;
        }

        // Create a table to hold all the scores for this division
        final PdfPTable divTable = new PdfPTable(relativeWidths);
        divTable.getDefaultCell().setBorder(0);
        divTable.setWidthPercentage(100);

        final PdfPTable header = createHeader(challengeTitle, tournament.getDescription(), awardGroup);
        final PdfPCell headerCell = new PdfPCell(header);
        headerCell.setColspan(relativeWidths.length);
        divTable.addCell(headerCell);

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("num relative widths: "
              + relativeWidths.length);
          for (int i = 0; i < relativeWidths.length; ++i) {
            LOGGER.trace("\twidth["
                + i
                + "] = "
                + relativeWidths[i]);
          }
        }

        writeColumnHeaders(subjectiveCategories, relativeWidths, challengeDescription, divTable);

        writeScores(connection, subjectiveCategories, challengeDescription.getPerformance(), relativeWidths, awardGroup,
                    winnerCriteria, tournament, divTable, bestTeams);

        // Add the division table to the document
        pdfDoc.add(divTable);

        // If there is another division to process, start it on a new page
        if (agIter.hasNext()) {
          pdfDoc.newPage();
        }
      }

      addLegend(pdfDoc, percentageHurdle, standardMean, standardSigma);

      pdfDoc.close();
    } catch (final ParseException pe) {
      throw new RuntimeException("Error parsing category weight!", pe);
    } catch (final DocumentException de) {
      throw new RuntimeException("Error creating PDF document!", de);
    }
  }

  private void addLegend(final Document pdf,
                         final int percentageHurdle,
                         final double standardMean,
                         final double standardSigma)
      throws DocumentException {
    final String hurdleText;
    if (percentageHurdle > 0
        && percentageHurdle < 100) {
      hurdleText = String.format("* - teams in the top %d%% of performance scores\n", percentageHurdle);
    } else {
      hurdleText = "";
    }

    final String legendText = String.format("%sbold score - top team in a category & judging group (rank)\n%.2f == average ; %.2f = 1 standard deviation\n@ - zero score on required goal",
                                            hurdleText, standardMean, standardSigma);
    final Phrase phrase = new Phrase(legendText, TIMES_12PT_NORMAL);
    pdf.add(phrase);
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
        final PreparedStatement prep = connection.prepareStatement("SELECT final_scores.team_number, final_scores.final_score"//
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
      try (final ResultSet rs = prep.executeQuery()) {
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
  private void writeScores(final Connection connection,
                           final SubjectiveScoreCategory[] subjectiveCategories,
                           final PerformanceScoreCategory performanceCategory,
                           final float[] relativeWidths,
                           final String awardGroup,
                           final WinnerType winnerCriteria,
                           final Tournament tournament,
                           final PdfPTable divTable,
                           final Set<Integer> bestTeams)
      throws SQLException {

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

          // ///////////////////////////////////////////////////////////////////
          // Build a table of data for this team
          // ///////////////////////////////////////////////////////////////////
          final PdfPTable curteam = new PdfPTable(relativeWidths);
          curteam.getDefaultCell().setBorder(0);

          // The first row of the team table...
          // First column is organization name
          final PdfPCell teamCol = new PdfPCell(new Phrase(organization, ARIAL_8PT_NORMAL));
          teamCol.setBorder(0);
          teamCol.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
          curteam.addCell(teamCol);
          curteam.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

          final PdfPCell judgeGroupCell = new PdfPCell(new Phrase(judgingGroup, ARIAL_8PT_NORMAL));
          judgeGroupCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
          judgeGroupCell.setBorder(0);
          curteam.addCell(judgeGroupCell);

          // Second column is "Raw:"
          final PdfPCell rawLabel = new PdfPCell(new Phrase("Raw:", ARIAL_8PT_NORMAL));
          rawLabel.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
          rawLabel.setBorder(0);
          curteam.addCell(rawLabel);

          insertRawSubjectiveScoreColumns(connection, tournament, winnerCriteria.getSortString(), subjectiveCategories,
                                          teamNumber, curteam);

          insertRawPerformanceScore(connection, performanceCategory.getScoreType(), teamNumber, curteam);

          // The "Overall score" column is not filled in for raw scores
          curteam.addCell("");

          // The second row of the team table...
          // First column contains the team # and name
          final PdfPCell teamNameCol = new PdfPCell(new Phrase(Integer.toString(teamNumber)
              + " "
              + teamName, ARIAL_8PT_NORMAL));
          teamNameCol.setBorder(0);
          teamNameCol.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
          curteam.addCell(teamNameCol);

          // Second column contains "Scaled:"
          final PdfPCell scaledCell = new PdfPCell(new Phrase("Scaled:", ARIAL_8PT_NORMAL));
          scaledCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
          scaledCell.setBorder(0);
          scaledCell.setColspan(2);
          curteam.addCell(scaledCell);

          // Next, one column containing the scaled score for each subjective
          // category with weight > 0
          for (final ScoreCategory category : subjectiveCategories) {
            final Map<String, Map<Integer, Integer>> catRanks = teamSubjectiveRanks.get(category);
            final Map<Integer, Integer> judgingRanks = catRanks.get(judgingGroup);

            final double catWeight = category.getWeight();
            if (catWeight > 0.0) {
              insertCategoryScaledScore(connection, tournament, teamNumber, curteam, category, judgingRanks);
            } // non-zero category weight
          } // foreach category

          // 2nd to last column has the scaled performance score
          insertCategoryScaledScore(connection, tournament, teamNumber, curteam, performanceCategory,
                                    teamPerformanceRanks);

          // Last column contains the overall scaled score
          final String overallScoreSuffix;
          if (bestTeams.contains(teamNumber)) {
            overallScoreSuffix = String.format("%1$s*", Utilities.NON_BREAKING_SPACE);
          } else {
            overallScoreSuffix = String.format("%1$s%1$s", Utilities.NON_BREAKING_SPACE);
          }

          final PdfPCell pCell = new PdfPCell((Double.isNaN(overallScore) ? new Phrase("No Score"
              + overallScoreSuffix, ARIAL_8PT_NORMAL_RED)
              : new Phrase(Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(overallScore)
                  + overallScoreSuffix, ARIAL_8PT_NORMAL)));
          pCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
          pCell.setBorder(0);
          curteam.addCell(pCell);

          // This is an empty row in the team table that is added to put a
          // horizontal rule under the team's score in the display
          final PdfPCell blankCell = new PdfPCell();
          blankCell.setBorder(0);
          blankCell.setBorderWidthBottom(0.5f);
          blankCell.setBorderColorBottom(BaseColor.GRAY);
          blankCell.setColspan(relativeWidths.length);
          curteam.addCell(blankCell);

          // Create a new cell and add it to the division table - this cell will
          // contain the entire team table we just built above
          final PdfPCell curteamCell = new PdfPCell(curteam);
          curteamCell.setBorder(0);
          curteamCell.setColspan(relativeWidths.length);
          divTable.addCell(curteamCell);
        } // foreach score result
      } // ResultSet
    } // PreparedStatement

  }

  private void insertCategoryScaledScore(final Connection connection,
                                         final Tournament tournament,
                                         final int teamNumber,
                                         final PdfPTable curteam,
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

        final Font scoreFont;
        if (1 == rank) {
          scoreFont = ARIAL_8PT_BOLD;
        } else {
          scoreFont = ARIAL_8PT_NORMAL;
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
          overallScoreText = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(scaledScore);
        }

        final String scoreText = overallScoreText
            + rankText;

        final PdfPCell subjCell = new PdfPCell(new Phrase(scoreText, scoreFont));
        subjCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        subjCell.setBorder(0);
        curteam.addCell(subjCell);
      } // finalScoreResult
    } // finalScorePrep
  }

  private void insertRawPerformanceScore(final Connection connection,
                                         final ScoreType performanceScoreType,
                                         final int teamNumber,
                                         final PdfPTable curteam)
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
        final Phrase scorePhrase;
        if (Double.isNaN(rawScore)) {
          scorePhrase = new Phrase("No Score", ARIAL_8PT_NORMAL_RED);
        } else {
          scorePhrase = new Phrase(Utilities.getFormatForScoreType(performanceScoreType).format(rawScore),
                                   ARIAL_8PT_NORMAL);
        }
        final PdfPCell pCell = new PdfPCell(scorePhrase);
        pCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        pCell.setBorder(0);
        curteam.addCell(pCell);
      } // ResultSet
    } // PreparedStatement
  }

  /**
   * @throws ParseException
   */
  private void writeColumnHeaders(final SubjectiveScoreCategory[] subjectiveCategories,
                                  final float[] relativeWidths,
                                  final ChallengeDescription challengeDescription,
                                  final PdfPTable divTable)
      throws ParseException {

    // /////////////////////////////////////////////////////////////////////
    // Write the table column headers
    // /////////////////////////////////////////////////////////////////////
    // team information
    final PdfPCell organizationCell = new PdfPCell(new Phrase("Organization", ARIAL_8PT_BOLD));
    organizationCell.setBorder(0);
    organizationCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
    divTable.addCell(organizationCell);

    // judging group
    final Paragraph judgingGroup = new Paragraph("Judging", ARIAL_8PT_BOLD);
    judgingGroup.add(Chunk.NEWLINE);
    judgingGroup.add(new Chunk("Group"));
    final PdfPCell judgeGroupCell = new PdfPCell(judgingGroup);
    judgeGroupCell.setBorder(0);
    judgeGroupCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
    judgeGroupCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
    divTable.addCell(judgeGroupCell);

    divTable.addCell(""); // weight/raw&scaled

    for (final SubjectiveScoreCategory category : subjectiveCategories) {
      final double weight = category.getWeight();
      if (weight > 0.0) {
        final String catTitle = category.getTitle();

        final Paragraph catPar = new Paragraph(catTitle, ARIAL_8PT_BOLD);
        final PdfPCell catCell = new PdfPCell(catPar);
        catCell.setBorder(0);
        catCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        catCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
        divTable.addCell(catCell);
      }
    }

    final Paragraph perfPar = new Paragraph("Performance", ARIAL_8PT_BOLD);
    final PdfPCell perfCell = new PdfPCell(perfPar);
    perfCell.setBorder(0);
    perfCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
    perfCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
    divTable.addCell(perfCell);

    final Paragraph overallScore = new Paragraph("Overall", ARIAL_8PT_BOLD);
    overallScore.add(Chunk.NEWLINE);
    overallScore.add(new Chunk("Score"));
    final PdfPCell osCell = new PdfPCell(overallScore);
    osCell.setBorder(0);
    osCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
    osCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
    divTable.addCell(osCell);

    // /////////////////////////////////////////////////////////////////////
    // Write a table row with the relative weights of the subjective scores
    // /////////////////////////////////////////////////////////////////////

    final PdfPCell teamCell = new PdfPCell(new Phrase("Team # / Team Name", ARIAL_8PT_BOLD));
    teamCell.setBorder(0);
    teamCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
    divTable.addCell(teamCell);

    final Paragraph wPar = new Paragraph("Weight:", ARIAL_8PT_NORMAL);
    final PdfPCell wCell = new PdfPCell(wPar);
    wCell.setColspan(2);
    wCell.setBorder(0);
    wCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
    divTable.addCell(wCell);

    final PdfPCell[] wCells = new PdfPCell[subjectiveCategories.length];
    final Paragraph[] wPars = new Paragraph[subjectiveCategories.length];
    for (int cat = 0; cat < subjectiveCategories.length; cat++) {
      final ScoreCategory category = subjectiveCategories[cat];
      if (category.getWeight() > 0.0) {
        wPars[cat] = new Paragraph(Double.toString(category.getWeight()), ARIAL_8PT_NORMAL);
        wCells[cat] = new PdfPCell(wPars[cat]);
        wCells[cat].setBorder(0);
        wCells[cat].setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        divTable.addCell(wCells[cat]);
      }
    }

    final PerformanceScoreCategory performanceElement = challengeDescription.getPerformance();
    final double perfWeight = performanceElement.getWeight();
    final Paragraph perfWeightPar = new Paragraph(Double.toString(perfWeight), ARIAL_8PT_NORMAL);
    final PdfPCell perfWeightCell = new PdfPCell(perfWeightPar);
    perfWeightCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
    perfWeightCell.setBorder(0);
    divTable.addCell(perfWeightCell);

    divTable.addCell("");

    final PdfPCell blankCell = new PdfPCell();
    blankCell.setBorder(0);
    blankCell.setBorderWidthBottom(1.0f);
    blankCell.setColspan(relativeWidths.length);
    divTable.addCell(blankCell);

    // Cause the first 4 rows to be repeated on
    // each page - 1 row for box header, 2 rows text headers and 1 for
    // the horizontal line.
    divTable.setHeaderRows(4);
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Winner type is used to determine sort order")
  private void insertRawSubjectiveScoreColumns(final Connection connection,
                                               final Tournament tournament,
                                               final String ascDesc,
                                               final SubjectiveScoreCategory[] subjectiveCategories,
                                               final int teamNumber,
                                               final PdfPTable curteam)
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

            final Font scoreFont;
            final String scoreText;
            if (!scoreSeen) {
              scoreFont = ARIAL_8PT_NORMAL_RED;
              scoreText = "No Score";
            } else {
              final boolean zeroInRequiredGoal = checkZeroInRequiredGoal(connection, tournament, catElement,
                                                                         teamNumber);
              if (zeroInRequiredGoal) {
                rawScoreText.append(" @");
              }
              scoreFont = ARIAL_8PT_NORMAL;
              scoreText = rawScoreText.toString();
            }
            final PdfPCell subjCell = new PdfPCell(new Phrase(scoreText, scoreFont));
            subjCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            subjCell.setBorder(0);
            curteam.addCell(subjCell);
          } // ResultSet
        } // category weight greater than 0
      } // foreach subjective category
    } // PreparedStatement
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  private static boolean checkZeroInRequiredGoal(final Connection connection,
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

  private PdfPTable createHeader(final String challengeTitle,
                                 final String tournamentName,
                                 final String division) {
    // initialization of the header table
    final PdfPTable header = new PdfPTable(2);

    final Phrase p = new Phrase();
    p.add(new Chunk(challengeTitle, TIMES_12PT_NORMAL));
    p.add(Chunk.NEWLINE);
    p.add(new Chunk("Final Computed Scores", TIMES_12PT_NORMAL));
    header.getDefaultCell().setBorderWidth(0);
    header.addCell(p);
    header.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);

    final Phrase p2 = new Phrase();
    p2.add(new Chunk("Tournament: "
        + tournamentName, TIMES_12PT_NORMAL));
    p2.add(Chunk.NEWLINE);
    p2.add(new Chunk("Award Group: "
        + division, TIMES_12PT_NORMAL));
    header.addCell(p2);

    return header;
  }

} // class FinalComputedScores
