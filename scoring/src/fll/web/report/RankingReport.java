/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
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

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

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

import fll.Team;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * @author jpschewe
 */
@WebServlet("/report/RankingReport")
public class RankingReport extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

  private static final Font RANK_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);

  private static final Font RANK_VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);

  private static final Font HEADER_FONT = TITLE_FONT;

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource();
      connection = datasource.getConnection();
      final org.w3c.dom.Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

      // create simple doc and write to a ByteArrayOutputStream
      final Document document = new Document(PageSize.LETTER);
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final PdfWriter writer = PdfWriter.getInstance(document, baos);
      final Element root = challengeDocument.getDocumentElement();
      writer.setPageEvent(new PageEventHandler(root.getAttribute("title"), Queries.getCurrentTournamentName(connection)));

      document.open();

      document.addTitle("Ranking Report");

      // add content
      final Map<String, Map<Integer, Map<String, Integer>>> rankingMap = Queries.getTeamRankings(connection,
                                                                                                 challengeDocument);
      for (final Map.Entry<String, Map<Integer, Map<String, Integer>>> divEntry : rankingMap.entrySet()) {
        final String division = divEntry.getKey();
        for (final Map.Entry<Integer, Map<String, Integer>> teamEntry : divEntry.getValue().entrySet()) {
          final int teamNum = teamEntry.getKey();
          final Team team = Team.getTeamFromDatabase(connection, teamNum);
          final Paragraph para = new Paragraph();
          para.add(Chunk.NEWLINE);
          para.add(new Chunk("Ranks for Team "
              + teamNum, TITLE_FONT));
          para.add(Chunk.NEWLINE);
          para.add(new Chunk(team.getTeamName()
              + " / " + team.getOrganization(), TITLE_FONT));
          para.add(Chunk.NEWLINE);
          para.add(new Chunk("Division "
              + division, TITLE_FONT));
          para.add(Chunk.NEWLINE);
          para.add(new Chunk(
                             "Each team is ranked in each category in the judging group and division they were judged in (if mutiple judging groups exist). Performance and Overall score are ranked by division only. Team may have the same rank if they were tied or were in different judging groups. ",
                             RANK_VALUE_FONT));
          para.add(Chunk.NEWLINE);
          para.add(Chunk.NEWLINE);
          for (final Map.Entry<String, Integer> rankEntry : teamEntry.getValue().entrySet()) {
            para.add(new Chunk(rankEntry.getKey()
                + ": ", RANK_TITLE_FONT));

            final int rank = rankEntry.getValue();
            if (Queries.NO_SHOW_RANK == rank) {
              para.add(new Chunk("No Show", RANK_VALUE_FONT));
            } else {
              para.add(new Chunk(String.valueOf(rank), RANK_VALUE_FONT));
            }
            para.add(Chunk.NEWLINE);
          }
          document.add(para);
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
      response.setHeader("Content-Disposition", "filename=rankingReport.pdf");
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
          + "\nFinal Computed Rankings", HEADER_FONT);
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
