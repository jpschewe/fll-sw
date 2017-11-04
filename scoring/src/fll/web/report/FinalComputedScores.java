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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.util.FP;
import fll.util.LogUtils;
import fll.util.PdfUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;
import fll.xml.ScoreType;
import fll.xml.WinnerType;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Final computed scores report.
 */
@WebServlet("/report/FinalComputedScores")
public final class FinalComputedScores extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

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
      final FooterHandler pageHandler = new FooterHandler(percentageHurdle, standardMean, standardSigma);

      generateReport(connection, response.getOutputStream(), challengeDescription, challengeTitle, tournament,
                     pageHandler, bestTeams);

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
                              final PdfPageEventHelper pageHandler,
                              final Set<Integer> bestTeams)
      throws SQLException, IOException {
    if (tournament.getTournamentID() != Queries.getCurrentTournament(connection)) {
      throw new FLLRuntimeException("Cannot generate final score report for a tournament other than the current tournament");
    }

    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final TournamentSchedule schedule;
    if (TournamentSchedule.scheduleExistsInDatabase(connection, tournament.getTournamentID())) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Found a schedule for tournament: "
            + tournament);
      }
      schedule = new TournamentSchedule(connection, tournament.getTournamentID());
    } else {
      schedule = null;
    }

    try {
      // This creates our new PDF document and declares it to be in portrait
      // orientation
      final Document pdfDoc = PdfUtils.createPortraitPdfDoc(out, pageHandler);

      final Iterator<String> agIter = Queries.getAwardGroups(connection).iterator();
      while (agIter.hasNext()) {
        final String awardGroup = agIter.next();

        final ScoreCategory[] subjectiveCategories = challengeDescription.getSubjectiveCategories()
                                                                         .toArray(new ScoreCategory[0]);

        // Figure out how many subjective categories have weights > 0.
        final double[] weights = new double[subjectiveCategories.length];
        int nonZeroWeights = 0;
        for (int catIndex = 0; catIndex < subjectiveCategories.length; catIndex++) {
          weights[catIndex] = subjectiveCategories[catIndex].getWeight();
          if (weights[catIndex] > 0.0) {
            nonZeroWeights++;
          }
        }
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

        writeColumnHeaders(schedule, weights, subjectiveCategories, relativeWidths, challengeDescription, divTable);

        writeScores(connection, subjectiveCategories, challengeDescription.getPerformance().getScoreType(), weights,
                    relativeWidths, awardGroup, winnerCriteria, tournament, divTable, bestTeams);

        // Add the division table to the document
        pdfDoc.add(divTable);

        // If there is another division to process, start it on a new page
        if (agIter.hasNext()) {
          pdfDoc.newPage();
        }
      }

      pdfDoc.close();
    } catch (final ParseException pe) {
      throw new RuntimeException("Error parsing category weight!", pe);
    } catch (final DocumentException de) {
      throw new RuntimeException("Error creating PDF document!", de);
    }
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

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT FinalScores.TeamNumber, FinalScores.performance"
          + " FROM FinalScores, TournamentTeams" //
          + " WHERE FinalScores.Tournament = ?" //
          + " AND TournamentTeams.Tournament = FinalScores.Tournament" //
          + " AND TournamentTeams.event_division = ?"//
          + " AND TournamentTeams.TeamNumber = FinalScores.TeamNumber"//
          + " ORDER BY FinalScores.performance "
          + winnerCriteria.getSortString());
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, awawrdGroup);

      int numTied = 1;
      int rank = 0;
      double prevScore = Double.NaN;
      rs = prep.executeQuery();
      while (rs.next()) {
        final int teamNumber = rs.getInt(1);
        double score = rs.getDouble(2);
        if (rs.wasNull()) {
          score = Double.NaN;
        }

        if (!FP.equals(score, prevScore, 1E-6)) {
          rank += numTied;
          numTied = 1;
        } else {
          ++numTied;
        }

        rankedTeams.put(teamNumber, rank);

        prevScore = score;
      }

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    return rankedTeams;
  }

  /**
   * @return category -> {Judging Group -> {team number -> rank}}
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name and winner criteria determines the sort")
  private Map<ScoreCategory, Map<String, Map<Integer, Integer>>> gatherRankedSubjectiveTeams(final Connection connection,
                                                                                             final ScoreCategory[] subjectiveCategories,
                                                                                             final WinnerType winnerCriteria,
                                                                                             final Tournament tournament,
                                                                                             final String awardGroup)
      throws SQLException {
    final Map<ScoreCategory, Map<String, Map<Integer, Integer>>> retval = new HashMap<>();
    final List<String> judgingStations = Queries.getJudgingStations(connection, tournament.getTournamentID());

    for (int cat = 0; cat < subjectiveCategories.length; cat++) {
      final String catName = subjectiveCategories[cat].getName();

      final Map<String, Map<Integer, Integer>> categoryRanks = new HashMap<>();

      try (final PreparedStatement prep = connection.prepareStatement("SELECT FinalScores.TeamNumber, FinalScores."
          + catName //
          + " FROM FinalScores, TournamentTeams" //
          + " WHERE FinalScores.Tournament = ?" //
          + " AND TournamentTeams.Tournament = FinalScores.Tournament" //
          + " AND TournamentTeams.event_division = ?"//
          + " AND TournamentTeams.TeamNumber = FinalScores.TeamNumber"//
          + " AND TournamentTeams.judging_station = ?" //
          + " ORDER BY FinalScores."
          + catName
          + " "
          + winnerCriteria.getSortString())) {
        prep.setInt(1, tournament.getTournamentID());
        prep.setString(2, awardGroup);

        for (final String judgingStation : judgingStations) {
          prep.setString(3, judgingStation);

          final Map<Integer, Integer> rankedTeams = new HashMap<>();

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

              if (!FP.equals(score, prevScore, 1E-6)) {
                rank += numTied;
                numTied = 1;
              } else {
                ++numTied;
              }

              rankedTeams.put(teamNumber, rank);

              prevScore = score;
            }

            categoryRanks.put(judgingStation, rankedTeams);

          } // try ResultSet
        } // foreach judging station

        retval.put(subjectiveCategories[cat], categoryRanks);

      } // try PreparedStatement
    } // foreach category

    return retval;

  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table name")
  private void writeScores(final Connection connection,
                           final ScoreCategory[] subjectiveCategories,
                           final ScoreType performanceScoreType,
                           final double[] weights,
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

    ResultSet rawScoreRS = null;
    PreparedStatement teamPrep = null;
    ResultSet teamsRS = null;
    PreparedStatement scorePrep = null;
    try {
      final StringBuilder query = new StringBuilder();
      query.append("SELECT Teams.Organization,Teams.TeamName,Teams.TeamNumber,FinalScores.OverallScore,FinalScores.performance,current_tournament_teams.judging_station");
      for (int cat = 0; cat < subjectiveCategories.length; cat++) {
        if (weights[cat] > 0.0) {
          final String catName = subjectiveCategories[cat].getName();
          query.append(",FinalScores."
              + catName);
        }
      }
      query.append(" FROM Teams,FinalScores,current_tournament_teams");
      query.append(" WHERE FinalScores.TeamNumber = Teams.TeamNumber");
      query.append(" AND FinalScores.Tournament = ?");
      query.append(" AND current_tournament_teams.event_division = ?");
      query.append(" AND current_tournament_teams.TeamNumber = Teams.TeamNumber");
      query.append(" ORDER BY FinalScores.OverallScore "
          + winnerCriteria.getSortString()
          + ", Teams.TeamNumber");
      teamPrep = connection.prepareStatement(query.toString());
      teamPrep.setInt(1, tournament.getTournamentID());
      teamPrep.setString(2, awardGroup);
      teamsRS = teamPrep.executeQuery();

      scorePrep = connection.prepareStatement("SELECT score FROM performance_seeding_max"
          + " WHERE TeamNumber = ?");

      while (teamsRS.next()) {
        final int teamNumber = teamsRS.getInt(3);
        final String organization = teamsRS.getString(1);
        final String teamName = teamsRS.getString(2);
        final String judgingGroup = teamsRS.getString(6);

        final double totalScore;
        final double ts = teamsRS.getDouble(4);
        if (teamsRS.wasNull()) {
          totalScore = Double.NaN;
        } else {
          totalScore = ts;
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

        insertRawScoreColumns(connection, tournament, winnerCriteria.getSortString(), subjectiveCategories, weights,
                              teamNumber, curteam);

        // Column for the highest performance score of the seeding rounds
        scorePrep.setInt(1, teamNumber);
        rawScoreRS = scorePrep.executeQuery();
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
        PdfPCell pCell = new PdfPCell(scorePhrase);
        pCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        pCell.setBorder(0);
        curteam.addCell(pCell);
        rawScoreRS.close();

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
        for (int cat = 0; cat < subjectiveCategories.length; cat++) {
          final Map<String, Map<Integer, Integer>> catRanks = teamSubjectiveRanks.get(subjectiveCategories[cat]);

          final double catWeight = weights[cat];
          if (catWeight > 0.0) {
            final double scaledScore;
            final double v = teamsRS.getDouble(6
                + cat
                + 1);
            if (teamsRS.wasNull()) {
              scaledScore = Double.NaN;
            } else {
              scaledScore = v;
            }

            final Map<Integer, Integer> judgingRanks = catRanks.get(judgingGroup);

            Font scoreFont;
            final String rankText;
            if (judgingRanks.containsKey(teamNumber)) {
              final int rank = judgingRanks.get(teamNumber);
              rankText = String.format("%1$s(%2$d)", Utilities.NON_BREAKING_SPACE, rank);
              if (1 == rank) {
                scoreFont = ARIAL_8PT_BOLD;
              } else {
                scoreFont = ARIAL_8PT_NORMAL;
              }
            } else {
              rankText = String.format("%1$s%1$s%1$s%1$s%1$s", Utilities.NON_BREAKING_SPACE);
              scoreFont = ARIAL_8PT_NORMAL;
            }

            final String scoreText;
            if (Double.isNaN(scaledScore)) {
              scoreText = "No Score"
                  + rankText;
              scoreFont = ARIAL_8PT_NORMAL_RED;
            } else {
              scoreText = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(scaledScore)
                  + rankText;
            }

            final PdfPCell subjCell = new PdfPCell(new Phrase(scoreText, scoreFont));
            subjCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            subjCell.setBorder(0);
            curteam.addCell(subjCell);
          }
        } // foreach category

        // 2nd to last column has the scaled performance score
        {
          Font scoreFont;
          final String rankText;
          if (teamPerformanceRanks.containsKey(teamNumber)) {
            final int rank = teamPerformanceRanks.get(teamNumber);
            rankText = String.format("%1$s(%2$d)", Utilities.NON_BREAKING_SPACE, rank);
            if (1 == rank) {
              scoreFont = ARIAL_8PT_BOLD;
            } else {
              scoreFont = ARIAL_8PT_NORMAL;
            }
          } else {
            rankText = String.format("%1$s%1$s%1$s%1$s%1$s", Utilities.NON_BREAKING_SPACE);
            scoreFont = ARIAL_8PT_NORMAL;
          }

          final double scaledScore;
          final double v = teamsRS.getDouble(5);
          if (teamsRS.wasNull()) {
            scaledScore = Double.NaN;
          } else {
            scaledScore = v;
          }

          final String scaledScoreStr;
          if (Double.isNaN(scaledScore)) {
            scoreFont = ARIAL_8PT_NORMAL_RED;
            scaledScoreStr = "No Score"
                + rankText;
          } else {
            scaledScoreStr = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(scaledScore)
                + rankText;
          }

          pCell = new PdfPCell(new Phrase(scaledScoreStr, scoreFont));
          pCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
          pCell.setBorder(0);
          curteam.addCell(pCell);
        } // performance score

        // Last column contains the overall scaled score
        final String overallScoreSuffix;
        if (bestTeams.contains(teamNumber)) {
          overallScoreSuffix = String.format("%1$s*", Utilities.NON_BREAKING_SPACE);
        } else {
          overallScoreSuffix = String.format("%1$s%1$s", Utilities.NON_BREAKING_SPACE);
        }

        pCell = new PdfPCell((Double.isNaN(totalScore) ? new Phrase("No Score"
            + overallScoreSuffix, ARIAL_8PT_NORMAL_RED)
            : new Phrase(Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(totalScore)
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
      }

      teamsRS.close();

    } finally {
      SQLFunctions.close(teamsRS);
      SQLFunctions.close(teamPrep);
      SQLFunctions.close(rawScoreRS);
      SQLFunctions.close(scorePrep);
    }

  }

  /**
   * @throws ParseException
   */
  private void writeColumnHeaders(final TournamentSchedule schedule,
                                  final double[] weights,
                                  final ScoreCategory[] subjectiveCategories,
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
    if (null != schedule) {
      final Paragraph judgingGroup = new Paragraph("Judging", ARIAL_8PT_BOLD);
      judgingGroup.add(Chunk.NEWLINE);
      judgingGroup.add(new Chunk("Group"));
      final PdfPCell osCell = new PdfPCell(judgingGroup);
      osCell.setBorder(0);
      osCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
      osCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
      divTable.addCell(osCell);
    }

    divTable.addCell(""); // weight/raw&scaled

    for (int cat = 0; cat < subjectiveCategories.length; cat++) {
      if (weights[cat] > 0.0) {
        final String catTitle = subjectiveCategories[cat].getTitle();

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
    if (null != schedule) {
      wCell.setColspan(2);
    }
    wCell.setBorder(0);
    wCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
    divTable.addCell(wCell);

    final PdfPCell[] wCells = new PdfPCell[subjectiveCategories.length];
    final Paragraph[] wPars = new Paragraph[subjectiveCategories.length];
    for (int cat = 0; cat < subjectiveCategories.length; cat++) {
      if (weights[cat] > 0.0) {
        wPars[cat] = new Paragraph(Double.toString(weights[cat]), ARIAL_8PT_NORMAL);
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

    PdfPCell blankCell = new PdfPCell();
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
  private void insertRawScoreColumns(final Connection connection,
                                     final Tournament tournament,
                                     final String ascDesc,
                                     final ScoreCategory[] subjectiveCategories,
                                     final double[] weights,
                                     final int teamNumber,
                                     final PdfPTable curteam)
      throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      // Next, one column containing the raw score for each subjective
      // category with weight > 0
      for (int catIndex = 0; catIndex < subjectiveCategories.length; catIndex++) {
        final ScoreCategory catElement = subjectiveCategories[catIndex];
        final double catWeight = weights[catIndex];
        if (catWeight > 0.0) {
          final String catName = catElement.getName();
          prep = connection.prepareStatement("SELECT ComputedTotal"
              + " FROM "
              + catName
              + " WHERE TeamNumber = ? AND Tournament = ? ORDER BY ComputedTotal "
              + ascDesc);
          prep.setInt(1, teamNumber);
          prep.setInt(2, tournament.getTournamentID());
          rs = prep.executeQuery();
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
          final PdfPCell subjCell = new PdfPCell((!scoreSeen ? new Phrase("No Score", ARIAL_8PT_NORMAL_RED)
              : new Phrase(rawScoreText.toString(), ARIAL_8PT_NORMAL)));
          subjCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
          subjCell.setBorder(0);
          curteam.addCell(subjCell);
          rs.close();
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
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

  private static class FooterHandler extends PdfPageEventHelper {

    private PdfTemplate _tpl;

    private BaseFont _headerFooterFont;

    private final String _legendText;

    /**
     * @param percentageHurdle percentage as an integer between 0 and 100
     */
    public FooterHandler(final int percentageHurdle,
                         final double standardMean,
                         final double standardSigma) {
      final String hurdleText;
      if (percentageHurdle > 0
          && percentageHurdle < 100) {
        hurdleText = String.format("* - teams in the top %d%% of performance scores, ", percentageHurdle);
      } else {
        hurdleText = "";
      }

      _legendText = String.format("%sbold - top team in a category & judging group, %.2f == average ; %.2f = 1 standard deviation",
                                  hurdleText, standardMean, standardSigma);
    }

    @Override
    public void onOpenDocument(final PdfWriter writer,
                               final Document document) {
      _headerFooterFont = TIMES_12PT_NORMAL.getBaseFont();

      // initialization of the footer template
      _tpl = writer.getDirectContent().createTemplate(100, 100);
      _tpl.setBoundingBox(new Rectangle(-20, -20, 100, 100));
    }

    @Override
    public void onEndPage(final PdfWriter writer,
                          final Document document) {
      final PdfContentByte cb = writer.getDirectContent();
      cb.saveState();

      // compose the footer

      final float textSize = _headerFooterFont.getWidthPoint(_legendText, 12);
      final float textBase = document.bottom()
          - 20;
      cb.beginText();
      cb.setFontAndSize(_headerFooterFont, 12);

      final float adjust = _headerFooterFont.getWidthPoint("0", 12);
      cb.setTextMatrix(document.right()
          - textSize
          - adjust, textBase);
      cb.showText(_legendText);
      cb.endText();
      cb.addTemplate(_tpl, document.right()
          - adjust, textBase);

      cb.restoreState();
    }

  } // class FooterHandler

} // class FinalComputedScores
