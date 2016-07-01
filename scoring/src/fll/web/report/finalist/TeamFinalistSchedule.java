/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report.finalist;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.report.ReportPageEventHandler;
import fll.xml.ChallengeDescription;

/**
 * Generate a PDF with one page per team any finalist schedule.
 * 
 * @author jpschewe
 */
@WebServlet("/report/finalist/TeamFinalistSchedule")
public class TeamFinalistSchedule extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

  private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);

  private static final Font HEADER_FONT = TITLE_FONT;

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      // create simple doc and write to a ByteArrayOutputStream
      final Document document = new Document(PageSize.LETTER);
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final PdfWriter writer = PdfWriter.getInstance(document, baos);
      writer.setPageEvent(new ReportPageEventHandler(HEADER_FONT, "Finalist Callback Schedule",
                                                     challengeDescription.getTitle(),
                                                     Queries.getCurrentTournamentName(connection)));

      document.open();

      document.addTitle("Ranking Report");

      // add content
      final int currentTournament = Queries.getCurrentTournament(connection);

      final Collection<String> divisions = FinalistSchedule.getAllDivisions(connection, currentTournament);
      final Collection<FinalistSchedule> schedules = new LinkedList<FinalistSchedule>();
      for (final String division : divisions) {
        final FinalistSchedule schedule = new FinalistSchedule(connection, currentTournament, division);
        schedules.add(schedule);
      }

      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection, currentTournament);
      final List<Integer> teamNumbers = new LinkedList<Integer>(tournamentTeams.keySet());
      Collections.sort(teamNumbers);

      for (final int teamNum : teamNumbers) {
        final TournamentTeam team = tournamentTeams.get(teamNum);

        for (final FinalistSchedule schedule : schedules) {
          final List<FinalistDBRow> finalistTimes = schedule.getScheduleForTeam(teamNum);
          final Map<String, String> rooms = schedule.getRooms();

          if (!finalistTimes.isEmpty()) {

            final Paragraph para = new Paragraph();
            para.add(Chunk.NEWLINE);
            para.add(new Chunk("Finalist times for Team "
                + teamNum, TITLE_FONT));
            para.add(Chunk.NEWLINE);
            para.add(new Chunk(team.getTeamName()
                + " / " + team.getOrganization(), TITLE_FONT));
            para.add(Chunk.NEWLINE);
            para.add(new Chunk("Award Group: "
                + team.getAwardGroup(), TITLE_FONT));
            para.add(Chunk.NEWLINE);
            para.add(Chunk.NEWLINE);

            final PdfPTable table = new PdfPTable(3);
            // table.getDefaultCell().setBorder(0);
            table.setWidthPercentage(100);

            table.addCell(new Phrase(new Chunk("Time", HEADER_FONT)));
            table.addCell(new Phrase(new Chunk("Room", HEADER_FONT)));
            table.addCell(new Phrase(new Chunk("Category", HEADER_FONT)));

            for (final FinalistDBRow row : finalistTimes) {
              final String categoryName = row.getCategoryName();
              String room = rooms.get(categoryName);
              if (null == room) {
                room = "";
              }

              table.addCell(new Phrase(String.format("%d:%02d", row.getHour(), row.getMinute()), VALUE_FONT));
              table.addCell(new Phrase(new Chunk(room, VALUE_FONT)));
              table.addCell(new Phrase(new Chunk(categoryName, VALUE_FONT)));

            } // foreach row

            para.add(table);

            document.add(para);
            document.add(Chunk.NEXTPAGE);

          } // non-empty list of teams

        } // foreach schedule

      } // foreach team

      document.close();

      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=teamFinalistSchedule.pdf");
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

}
