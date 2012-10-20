/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.XMLUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;

import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.LogUtils;

/**
 * Download the schedule for the current tournament.
 */
@WebServlet("/schedule.xml")
public class DownloadSchedule extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);
      final TournamentSchedule schedule = new TournamentSchedule(connection, currentTournament);

      final Document document = schedule.createXML();

      final ProcessingInstruction stylesheet = document.createProcessingInstruction("xml-stylesheet", "type='text/css' href='schedule.css'");
      document.insertBefore(stylesheet, document.getDocumentElement());
      
      response.reset();
      response.setContentType("text/xml");
      response.setHeader("Content-Disposition", "filename=schedule.xml");
      XMLUtils.writeXML(document, response.getWriter(), "UTF-8");

    } catch (final SQLException sqle) {
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error getting schedule from the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }
  }

}
