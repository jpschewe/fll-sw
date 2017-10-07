/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FP;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.playoff.DatabaseTeamScore;
import fll.web.playoff.TeamScore;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.EnumeratedValue;
import fll.xml.PerformanceScoreCategory;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Report displaying the details of performance scores for each team.
 */
@WebServlet("/report/PerformanceScoreReport")
public class PerformanceScoreReport extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);

  private static final Font HEADER_FONT = TITLE_FONT;

  private static final Font SCORE_FONT = FontFactory.getFont(FontFactory.TIMES, 10, Font.NORMAL);

  private static final Font BEST_SCORE_FONT = FontFactory.getFont(FontFactory.TIMES, 10, Font.BOLD);

  private static final String REPORT_TITLE = "Performance Score Report";

  @Override
  protected void processRequest(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application,
                                HttpSession session)
      throws IOException, ServletException {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      final int tournamentId = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);
      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID());

      // create simple doc and write to a ByteArrayOutputStream
      final Document document = new Document(PageSize.LETTER, 36, 36, 72, 36);
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final PdfWriter writer = PdfWriter.getInstance(document, baos);
      final PerformanceScoreReportPageEventHandler headerHandler = new PerformanceScoreReportPageEventHandler(HEADER_FONT,
                                                                                                              REPORT_TITLE,
                                                                                                              challengeDescription.getTitle(),
                                                                                                              tournament.getDescription());
      writer.setPageEvent(headerHandler);

      document.open();

      document.addTitle(REPORT_TITLE);

      final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection);
      if (teams.isEmpty()) {
        final Paragraph para = new Paragraph();
        para.add(Chunk.NEWLINE);
        para.add(new Chunk("No teams in the tournament."));
        document.add(para);
      } else {
        for (Map.Entry<Integer, TournamentTeam> entry : teams.entrySet()) {
          headerHandler.setTeamInfo(entry.getValue());

          outputTeam(connection, document, tournament, challengeDescription, numSeedingRounds, entry.getValue());

          document.add(Chunk.NEXTPAGE);
        }
      }

      document.close();

      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=performanceScoreReport.pdf");
      // the content length is needed for MSIE!!!
      response.setContentLength(baos.size());
      // write ByteArrayOutputStream to the ServletOutputStream
      final ServletOutputStream out = response.getOutputStream();
      baos.writeTo(out);
      out.flush();

    } catch (final SQLException e) {
      LOG.error(e, e);
      throw new RuntimeException(e);
    } catch (final DocumentException e) {
      LOG.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  private void outputTeam(final Connection connection,
                          final Document document,
                          final Tournament tournament,
                          final ChallengeDescription challenge,
                          final int numSeedingRounds,
                          final TournamentTeam team)
      throws DocumentException, SQLException {
    // output first row for header
    final PdfPTable table = new PdfPTable(numSeedingRounds
        + 1);
    table.addCell("");
    for (int runNumber = 1; runNumber <= numSeedingRounds; ++runNumber) {

      table.addCell(new Phrase("Run "
          + runNumber, HEADER_FONT));
    }

    final PerformanceScoreCategory performance = challenge.getPerformance();

    final TeamScore[] scores = getScores(connection, tournament, team, numSeedingRounds);
    for (final AbstractGoal goal : performance.getGoals()) {
      final double bestScore = bestScoreForGoal(scores, goal);

      final StringBuilder goalTitle = new StringBuilder();
      goalTitle.append(goal.getTitle());
      if (goal.isComputed()) {
        goalTitle.append(" (computed)");
      }
      table.addCell(new Phrase(goalTitle.toString(), HEADER_FONT));

      for (final TeamScore score : scores) {
        if (!score.scoreExists()
            || score.isBye()
            || score.isNoShow()) {
          table.addCell("");
        } else {
          final double computedValue = goal.getComputedScore(score);

          final StringBuilder cellStr = new StringBuilder();
          if (!goal.isComputed()) {
            if (goal.isEnumerated()) {
              final String enumValue = score.getEnumRawScore(goal.getName());
              boolean found = false;
              for (final EnumeratedValue ev : goal.getValues()) {
                if (ev.getValue().equals(enumValue)) {
                  cellStr.append(ev.getTitle()
                      + " -> ");
                  found = true;
                  break;
                }
              }
              if (!found) {
                LOG.warn("Could not find enumerated title for "
                    + enumValue);
                cellStr.append(enumValue
                    + " -> ");
              }
            } else {
              if (goal.isYesNo()) {
                if (FP.greaterThan(score.getRawScore(goal.getName()), 0, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
                  cellStr.append("Yes -> ");
                } else {
                  cellStr.append("No -> ");
                }
              } else {
                final double rawValue = goal.getRawScore(score);
                cellStr.append(Utilities.getFormatForScoreType(goal.getScoreType()).format(rawValue)
                    + " -> ");
              }
            } // not enumerated
          } // not computed

          cellStr.append(Utilities.getFormatForScoreType(performance.getScoreType()).format(computedValue));
          if (FP.equals(bestScore, computedValue, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
            table.addCell(new Phrase(cellStr.toString(), BEST_SCORE_FONT));
          } else {
            table.addCell(new Phrase(cellStr.toString(), SCORE_FONT));
          }
        } // exists, non-bye, non-no show

      } // foreach score
    } // foreach goal

    // totals
    table.addCell(new Phrase("Total", HEADER_FONT));
    final double bestTotalScore = bestTotalScore(performance, scores);
    for (final TeamScore score : scores) {
      if (!score.scoreExists()) {
        table.addCell("");
      } else if (score.isBye()) {
        table.addCell(new Phrase("Bye", SCORE_FONT));
      } else if (score.isNoShow()) {
        table.addCell(new Phrase("No Show", SCORE_FONT));
      } else {
        final double totalScore = performance.evaluate(score);

        if (FP.equals(bestTotalScore, totalScore, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
          table.addCell(new Phrase(Utilities.getFormatForScoreType(performance.getScoreType()).format(totalScore),
                                   BEST_SCORE_FONT));
        } else {
          table.addCell(new Phrase(Utilities.getFormatForScoreType(performance.getScoreType()).format(totalScore),
                                   SCORE_FONT));
        }
      }

    }

    document.add(table);

    final Paragraph definitionPara = new Paragraph();
    definitionPara.add(Chunk.NEWLINE);
    definitionPara.add(new Chunk("The team's top score for each goal and overall are in bold."));
    document.add(definitionPara);
  }

  /**
   * @return best total score
   */
  private double bestTotalScore(final PerformanceScoreCategory performance,
                                final TeamScore[] scores) {
    double bestScore = Double.MAX_VALUE
        * -1;
    for (final TeamScore score : scores) {
      final double computedValue = performance.evaluate(score);
      bestScore = Math.max(bestScore, computedValue);
    }
    return bestScore;
  }

  /**
   * @return the best score for the specified goal
   */
  private double bestScoreForGoal(final TeamScore[] scores,
                                  final AbstractGoal goal) {
    double bestScore = Double.MAX_VALUE
        * -1;
    for (final TeamScore score : scores) {
      final double computedValue = goal.getComputedScore(score);
      bestScore = Math.max(bestScore, computedValue);
    }
    return bestScore;
  }

  private TeamScore[] getScores(final Connection connection,
                                final Tournament tournament,
                                final TournamentTeam team,
                                final int numSeedingRounds)
      throws SQLException {
    final TeamScore[] scores = new TeamScore[numSeedingRounds];
    for (int runNumber = 1; runNumber <= numSeedingRounds; ++runNumber) {
      scores[runNumber
          - 1] = new DatabaseTeamScore(GenerateDB.PERFORMANCE_TABLE_NAME, tournament.getTournamentID(),
                                       team.getTeamNumber(), runNumber, connection);
    }
    return scores;
  }

  private static final class PerformanceScoreReportPageEventHandler extends PdfPageEventHelper {
    /**
     * @param headerFont font to use for the footer
     * @param reportTitle title of the report
     * @param challengeTitle title of the challenge
     * @param tournament the tournament name
     */
    public PerformanceScoreReportPageEventHandler(final Font font,
                                                  final String reportTitle,
                                                  final String challengeTitle,
                                                  final String tournament) {
      _font = font;
      _reportTitle = reportTitle;
      _tournament = tournament;
      _challengeTitle = challengeTitle;
      _formattedDate = DateFormat.getDateInstance().format(new Date());
    }

    /**
     * Set team information for header
     */
    public void setTeamInfo(final TournamentTeam value) {
      _team = value;
    }

    private TournamentTeam _team = null;

    private final String _reportTitle;

    private final String _formattedDate;

    private final String _tournament;

    private final String _challengeTitle;

    private final Font _font;

    @Override
    // initialization of the header table
    public void onEndPage(final PdfWriter writer,
                          final Document document) {
      final PdfPTable header = new PdfPTable(2);
      final Phrase p = new Phrase();
      final Chunk ck = new Chunk(_challengeTitle
          + "\n"
          + _reportTitle, _font);
      p.add(ck);
      header.getDefaultCell().setBorderWidth(0);
      header.addCell(p);
      header.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      header.addCell(new Phrase(new Chunk("Tournament: "
          + _tournament
          + "\nDate: "
          + _formattedDate, _font)));

      // horizontal line
      final PdfPCell blankCell = new PdfPCell();
      blankCell.setBorder(0);
      blankCell.setBorderWidthTop(1.0f);
      blankCell.setColspan(2);
      header.addCell(blankCell);

      if (null != _team) {
        // team information
        final Paragraph para = new Paragraph();
        para.add(new Chunk("Team #"
            + _team.getTeamNumber()
            + " "
            + _team.getTeamName()
            + " / "
            + _team.getOrganization(), TITLE_FONT));
        para.add(Chunk.NEWLINE);
        para.add(new Chunk("Award Group: "
            + _team.getAwardGroup(), TITLE_FONT));
        para.add(Chunk.NEWLINE);

        final PdfPCell teamInformation = new PdfPCell(para);
        teamInformation.setBorder(0);
        teamInformation.setColspan(2);

        header.addCell(teamInformation);
      }

      header.setTotalWidth(document.right()
          - document.left());

      final PdfContentByte cb = writer.getDirectContent();
      cb.saveState();
      header.writeSelectedRows(0, -1, document.left(), document.getPageSize().getHeight()
          - 10, cb);
      cb.restoreState();
    }

  }

}
