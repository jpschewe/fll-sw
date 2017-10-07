/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

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
import com.itextpdf.text.pdf.PdfWriter;

import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.playoff.Playoff;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Report displaying which teams won each playoff bracket.
 */
@WebServlet("/report/PlayoffReport")
public class PlayoffReport extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);

  private static final Font HEADER_FONT = TITLE_FONT;

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

      final Tournament tournament = Tournament.findTournamentByID(connection, Queries.getCurrentTournament(connection));

      // create simple doc and write to a ByteArrayOutputStream
      final Document document = new Document(PageSize.LETTER);
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final PdfWriter writer = PdfWriter.getInstance(document, baos);
      writer.setPageEvent(new ReportPageEventHandler(HEADER_FONT, "Head to Head Winners",
                                                     challengeDescription.getTitle(), tournament.getDescription()));

      document.open();

      document.addTitle("Head to Head Report");

      final List<String> playoffDivisions = Playoff.getPlayoffBrackets(connection, tournament.getTournamentID());
      for (final String division : playoffDivisions) {

        Paragraph para = processDivision(connection, tournament, division,
                                         challengeDescription.getPerformance().getScoreType());
        document.add(para);
      }

      document.close();

      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=playoffReport.pdf");
      // the content length is needed for MSIE!!!
      response.setContentLength(baos.size());
      // write ByteArrayOutputStream to the ServletOutputStream
      final ServletOutputStream out = response.getOutputStream();
      baos.writeTo(out);
      out.flush();

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } catch (final DocumentException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Create the paragraph for the specified division.
   * 
   * @throws SQLException
   */
  private Paragraph processDivision(final Connection connection,
                                    final Tournament tournament,
                                    final String division,
                                    final ScoreType performanceScoreType)
      throws SQLException {
    PreparedStatement teamPrep = null;
    ResultSet teamResult = null;
    PreparedStatement scorePrep = null;
    ResultSet scoreResult = null;
    try {
      final Paragraph para = new Paragraph();
      para.add(Chunk.NEWLINE);
      para.add(new Chunk("Results for head to head bracket "
          + division, TITLE_FONT));
      para.add(Chunk.NEWLINE);

      final int maxRun = Playoff.getMaxPerformanceRound(connection, tournament.getTournamentID(), division);

      if (maxRun < 1) {
        para.add("Cannot determine max run number for this playoff bracket. This is an internal error");
      } else {
        teamPrep = connection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName, Teams.Organization" //
            + " FROM PlayoffData, Teams" //
            + " WHERE PlayoffData.Tournament = ?" //
            + " AND PlayoffData.event_division = ?" //
            + " AND PlayoffData.run_number = ?" //
            + " AND Teams.TeamNumber = PlayoffData.team"//
            + " ORDER BY PlayoffData.linenumber" //
        );
        teamPrep.setInt(1, tournament.getTournamentID());
        teamPrep.setString(2, division);

        scorePrep = connection.prepareStatement("SELECT ComputedTotal" //
            + " FROM Performance" //
            + " WHERE Tournament = ?" //
            + " AND TeamNumber = ?" //
            + " AND RunNumber = ?"//
        );

        // figure out the last teams
        final List<String> lastTeams = new LinkedList<>();

        teamPrep.setInt(3, maxRun
            - 1);
        scorePrep.setInt(1, tournament.getTournamentID());
        scorePrep.setInt(3, maxRun
            - 1);
        teamResult = teamPrep.executeQuery();
        while (teamResult.next()) {
          final int teamNumber = teamResult.getInt(1);
          final String teamName = teamResult.getString(2);
          final String organization = teamResult.getString(3);

          scorePrep.setInt(2, teamNumber);
          scoreResult = scorePrep.executeQuery();
          final String scoreStr;
          if (scoreResult.next()) {
            scoreStr = Utilities.getFormatForScoreType(performanceScoreType).format(scoreResult.getDouble(1));
          } else {
            scoreStr = "unknown";
          }

          lastTeams.add(String.format("Team %d from %s - %s with a score of %s", teamNumber, organization, teamName,
                                      scoreStr));

          SQLFunctions.close(scoreResult);
          scoreResult = null;
        }
        SQLFunctions.close(teamResult);
        teamResult = null;

        // determine the winners
        int lastTeamsIndex = 0;
        teamPrep.setInt(3, maxRun);
        teamResult = teamPrep.executeQuery();
        while (teamResult.next()) {
          final int teamNumber = teamResult.getInt(1);
          final String teamName = teamResult.getString(2);

          para.add(String.format("Competing for places %d and %d", lastTeamsIndex
              + 1, lastTeamsIndex
                  + 2));
          para.add(Chunk.NEWLINE);
          if (lastTeamsIndex < lastTeams.size()) {
            para.add(lastTeams.get(lastTeamsIndex));
          } else {
            para.add("Internal error, unknown team competing");
          }
          para.add(Chunk.NEWLINE);
          ++lastTeamsIndex;

          if (lastTeamsIndex < lastTeams.size()) {
            para.add(lastTeams.get(lastTeamsIndex));
          } else {
            para.add("Internal error, unknown team competing");
          }
          para.add(Chunk.NEWLINE);
          ++lastTeamsIndex;

          para.add(String.format("The winner is team %d - %s", teamNumber, teamName));
          para.add(Chunk.NEWLINE);
          para.add(Chunk.NEWLINE);
        }
      }

      return para;
    } finally {
      SQLFunctions.close(teamResult);
      SQLFunctions.close(teamPrep);
      SQLFunctions.close(scoreResult);
      SQLFunctions.close(scorePrep);
    }
  }

}
