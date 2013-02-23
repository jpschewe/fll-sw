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
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

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
import fll.Utilities;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.playoff.Playoff;
import fll.xml.ChallengeDescription;

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
                                HttpSession session) throws IOException, ServletException {
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
      writer.setPageEvent(new PageEventHandler(challengeDescription.getTitle(), tournament.getName()));

      document.open();

      document.addTitle("Playoff Report");

      final List<String> playoffDivisions = Playoff.getPlayoffDivisions(connection, tournament.getTournamentID());
      for (final String division : playoffDivisions) {

        Paragraph para = processDivision(connection, tournament, division);
        document.add(para);
      }

      document.close();

      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=rankingReport.pdf");
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
                                    final String division) throws SQLException {
    PreparedStatement teamPrep = null;
    ResultSet teamResult = null;
    PreparedStatement scorePrep = null;
    ResultSet scoreResult = null;
    try {
      final Paragraph para = new Paragraph();
      para.add(Chunk.NEWLINE);
      para.add(new Chunk("Results for division "
          + division, TITLE_FONT));
      para.add(Chunk.NEWLINE);

      final int maxRun = Playoff.getMaxRunNumber(connection, tournament.getTournamentID(), division);

      if (maxRun < 1) {
        para.add("Cannot determine max run number for this division. This is an internal error");
      } else {
        teamPrep = connection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName FROM PlayoffData, Teams" //
            + " WHERE PlayoffData.Tournament = ?" //
            + " AND PlayoffData.event_division = ?" + " AND PlayoffData.run_number = ?" //
            + " AND Teams.TeamNumber = PlayoffData.team");
        teamPrep.setInt(1, tournament.getTournamentID());
        teamPrep.setString(2, division);

        scorePrep = connection.prepareStatement("SELECT ComputedTotal FROM Performance" //
            + " WHERE Tournament = ?" //
            + " AND RunNumber = ?"//
            + " AND TeamNumber = ?" //
        );
        scorePrep.setInt(1, tournament.getTournamentID());

        // figure out the last 2 teams
        teamPrep.setInt(3, maxRun - 1);
        scorePrep.setInt(2, maxRun - 1);
        teamResult = teamPrep.executeQuery();
        while (teamResult.next()) {
          final int teamNumber = teamResult.getInt(1);
          final String teamName = teamResult.getString(2);

          scorePrep.setInt(3, teamNumber);
          scoreResult = scorePrep.executeQuery();
          final String scoreStr;
          if (scoreResult.next()) {
            final double score = scoreResult.getDouble(1);
            scoreStr = Utilities.NUMBER_FORMAT_INSTANCE.format(score);
          } else {
            scoreStr = "Unknown";
          }

          para.add(String.format("Team %d - %s with a score of %s", teamNumber, teamName, scoreStr));
          para.add(Chunk.NEWLINE);

          SQLFunctions.close(scoreResult);
          scoreResult = null;
        }
        SQLFunctions.close(teamResult);
        teamResult = null;

        // show the winner
        teamPrep.setInt(3, maxRun);
        teamResult = teamPrep.executeQuery();
        if (teamResult.next()) {
          final int teamNumber = teamResult.getInt(1);
          final String teamName = teamResult.getString(2);

          para.add(String.format("The winner is team %d - %s", teamNumber, teamName));
        } else {
          para.add("The winner has not been determined yet");
        }
        para.add(Chunk.NEWLINE);

      }

      return para;
    } finally {
      SQLFunctions.close(teamResult);
      SQLFunctions.close(teamPrep);
      SQLFunctions.close(scorePrep);
      SQLFunctions.close(scoreResult);
    }
  }

  /**
   * Be able to initialize the header table at the end of a page.
   */
  private static final class PageEventHandler extends PdfPageEventHelper {
    public PageEventHandler(final String challengeTitle,
                            final String tournament) {
      _tournament = tournament;
      _challengeTitle = challengeTitle;
      _formattedDate = DateFormat.getDateInstance().format(new Date());
    }

    private final String _formattedDate;

    private final String _tournament;

    private final String _challengeTitle;

    @Override
    // initialization of the header table
    public void onEndPage(final PdfWriter writer,
                          final Document document) {
      final PdfPTable header = new PdfPTable(2);
      final Phrase p = new Phrase();
      final Chunk ck = new Chunk(_challengeTitle
          + "\nPlayoff Winners", HEADER_FONT);
      p.add(ck);
      header.getDefaultCell().setBorderWidth(0);
      header.addCell(p);
      header.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      header.addCell(new Phrase(new Chunk("Tournament: "
          + _tournament + "\nDate: " + _formattedDate, HEADER_FONT)));
      final PdfPCell blankCell = new PdfPCell();
      blankCell.setBorder(0);
      blankCell.setBorderWidthTop(1.0f);
      blankCell.setColspan(2);
      header.addCell(blankCell);

      final PdfContentByte cb = writer.getDirectContent();
      cb.saveState();
      header.setTotalWidth(document.right()
          - document.left());
      header.writeSelectedRows(0, -1, document.left(), document.getPageSize().getHeight() - 10, cb);
      cb.restoreState();
    }

  }
}
