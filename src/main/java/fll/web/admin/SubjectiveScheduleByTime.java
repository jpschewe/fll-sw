/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

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

import org.apache.log4j.Logger;

import com.itextpdf.text.DocumentException;

import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * @see TournamentSchedule#outputSubjectiveSchedulesByTime(java.io.OutputStream)
 */
@WebServlet("/admin/SubjectiveScheduleByTime")
public class SubjectiveScheduleByTime extends BaseFLLServlet {

  private static Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournamentID = Queries.getCurrentTournament(connection);

      if (!TournamentSchedule.scheduleExistsInDatabase(connection, currentTournamentID)) {
        session.setAttribute(SessionAttributes.MESSAGE,
                             "<p class='error'>There is no schedule for this tournament.</p>");
        WebUtils.sendRedirect(application, response, "/admin/index.jsp");
        return;
      }

      final TournamentSchedule schedule = new TournamentSchedule(connection, currentTournamentID);

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=subjectiveByTime.pdf");
      schedule.outputSubjectiveSchedulesByTime(response.getOutputStream());

    } catch (final DocumentException e) {
      LOGGER.error(e.getMessage(), e);
      throw new FLLInternalException("Got error writing schedule", e);
    } catch (final SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }
  }

}
