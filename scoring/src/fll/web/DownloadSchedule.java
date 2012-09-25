/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

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
import org.w3c.dom.Element;

import fll.db.Queries;
import fll.scheduler.PerformanceTime;
import fll.scheduler.TeamScheduleInfo;
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

      final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

      final Element top = document.createElement("schedule");
      document.appendChild(top);
      top.setAttribute("xsi:noNamespaceSchemaLocation", "http://fll-sw.sourceforge.net/schedule.xsd");
      top.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

      for (final TeamScheduleInfo si : schedule.getSchedule()) {
        final Element team = document.createElement("team");
        top.appendChild(team);
        team.setAttribute("number", String.valueOf(si.getTeamNumber()));
        team.setAttribute("judging_station", si.getJudgingStation());

        for (final String subjName : si.getKnownSubjectiveStations()) {
          final Date time = si.getSubjectiveTimeByName(subjName).getTime();
          final Element subjective = document.createElement("subjective");
          team.appendChild(subjective);
          subjective.setAttribute("name", subjName);
          subjective.setAttribute("time", fll.xml.XMLUtils.XML_TIME_FORMAT.get().format(time));
        }

        for (int round = 0; round < si.getNumberOfRounds(); ++round) {
          final PerformanceTime perfTime = si.getPerf(round);
          final Element perf = document.createElement("performance");
          team.appendChild(perf);
          perf.setAttribute("round", String.valueOf(round + 1));
          perf.setAttribute("table_color", perfTime.getTable());
          perf.setAttribute("table_side", String.valueOf(perfTime.getSide()));
          perf.setAttribute("time", fll.xml.XMLUtils.XML_TIME_FORMAT.get().format(perfTime.getTime()));
        }
      }

      response.reset();
      response.setContentType("text/xml");
      response.setHeader("Content-Disposition", "schedule.xml");
      XMLUtils.writeXML(document, response.getWriter(), "UTF-8");

    } catch (final SQLException sqle) {
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error getting schedule from the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }
  }

}
