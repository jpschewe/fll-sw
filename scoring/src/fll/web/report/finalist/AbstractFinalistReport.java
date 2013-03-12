/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

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
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
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
import fll.xml.ChallengeDescription;

/**
 * Outputs the PDF showing times of finalist categories.
 */
abstract public class AbstractFinalistReport extends BaseFLLServlet {

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);

  private static final Font HEADER_FONT = TITLE_FONT;

  private static final Font SCHEDULE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);

  private static final Font SCHEDULE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final boolean showPrivate,
                                final HttpServletResponse response,
                                final ServletContext application) throws IOException, ServletException {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      // create simple doc and write to a ByteArrayOutputStream
      final Document document = new Document(PageSize.LETTER);
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final PdfWriter writer = PdfWriter.getInstance(document, baos);
      writer.setPageEvent(new PageEventHandler(challengeDescription.getTitle(),
                                               Queries.getCurrentTournamentName(connection), showPrivate));

      document.open();

      document.addTitle("Finalist Schedule");

      // add content
      final FinalistSchedule schedule = new FinalistSchedule(connection, Queries.getCurrentTournament(connection));

      for (final Map.Entry<String, Boolean> entry : schedule.getCategories().entrySet()) {
        if (showPrivate
            || entry.getValue()) {
          createCategoryPage(document, connection, entry.getKey(), schedule);
        }
      }

      document.close();

      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=finalist-schedule.pdf");
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
   * Create a page for the specified category.
   * 
   * @throws DocumentException
   * @throws SQLException
   */
  private void createCategoryPage(final Document document,
                                  final Connection connection,
                                  final String category,
                                  final FinalistSchedule schedule) throws DocumentException, SQLException {
    // header name
    final Paragraph para = new Paragraph();
    para.add(Chunk.NEWLINE);

    para.add(new Chunk(String.format("Finalist schedule for %s", category), TITLE_FONT));
    para.add(Chunk.NEWLINE);
    document.add(para);

    final PdfPTable schedTable = new PdfPTable(new float[] { 12.5f, 12.5f, 37.5f, 37.5f });
    schedTable.setWidthPercentage(100);
    schedTable.getDefaultCell().setBorder(0);
    schedTable.getDefaultCell().enableBorderSide(Rectangle.BOTTOM);
    schedTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

    schedTable.addCell(new Phrase("Time", SCHEDULE_HEADER_FONT));

    schedTable.addCell(new Phrase("Team #", SCHEDULE_HEADER_FONT));

    schedTable.addCell(new Phrase("Team Name", SCHEDULE_HEADER_FONT));

    schedTable.addCell(new Phrase("Organization", SCHEDULE_HEADER_FONT));

    // foreach information output
    for (final FinalistDBRow row : schedule.getScheduleForCategory(category)) {
      // time, number, name, organization
      schedTable.addCell(new Phrase(String.format("%d:%02d", row.getHour(), row.getMinute()), SCHEDULE_FONT));

      final Team team = Team.getTeamFromDatabase(connection, row.getTeamNumber());
      schedTable.addCell(new Phrase(String.valueOf(team.getTeamNumber()), SCHEDULE_FONT));

      schedTable.addCell(new Phrase(team.getTeamName(), SCHEDULE_FONT));

      schedTable.addCell(new Phrase(team.getOrganization(), SCHEDULE_FONT));

    }
    document.add(schedTable);

    document.add(Chunk.NEXTPAGE);
  }

  /**
   * Be able to initialize the header table at the end of a page.
   */
  private static final class PageEventHandler extends PdfPageEventHelper {
    public PageEventHandler(final String challengeTitle,
                            final String tournament,
                            final boolean showPrivate) {
      _tournament = tournament;
      _challengeTitle = challengeTitle;
      _formattedDate = DateFormat.getDateInstance().format(new Date());
      _showPrivate = showPrivate;
    }

    private final boolean _showPrivate;

    private final String _formattedDate;

    private final String _tournament;

    private final String _challengeTitle;

    @Override
    // initialization of the header table
    public void onEndPage(final PdfWriter writer,
                          final Document document) {
      final PdfPTable header = new PdfPTable(2);
      final Phrase p = new Phrase();
      final Chunk ck = new Chunk(String.format("%s%n %s Finalist Schedule", //
                                               _challengeTitle, //
                                               _showPrivate ? "Private" : ""), HEADER_FONT);
      p.add(ck);
      header.getDefaultCell().setBorderWidth(0);
      header.addCell(p);
      header.getDefaultCell().setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
      header.addCell(new Phrase(new Chunk(String.format("Tournament: %s %nDate: %s", _tournament, _formattedDate),
                                          HEADER_FONT)));
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
