/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.db.Queries;
import fll.scheduler.ConstraintViolation;
import fll.scheduler.ScheduleParseException;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Read uploadSchedule_file and uploadSchedule_sheet, then check for constraint
 * violations. Stores the schedule in uploadSchedule_schedule, type
 * {@link TournamentSchedule}.
 * 
 * @web.servlet name="CheckViolations"
 * @web.servlet-mapping url-pattern="/schedule/CheckViolations"
 */
public class CheckViolations extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final File scheduleFile = SessionAttributes.getNonNullAttribute(session, "uploadSchedule_file", File.class);
    final String sheetName = SessionAttributes.getNonNullAttribute(session, "uploadSchedule_sheet", String.class);
    try {
      final InputStream stream = new FileInputStream(scheduleFile);
      final TournamentSchedule schedule = new TournamentSchedule(stream, sheetName);
      session.setAttribute("uploadSchedule_schedule", schedule);
      
      final DataSource datasource = SessionAttributes.getDataSource(session);
      final Connection connection = datasource.getConnection();
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Collection<ConstraintViolation> violations = schedule.compareWithDatabase(connection, tournamentID);
      violations.addAll(schedule.verifySchedule());
      if(violations.isEmpty()) {
        WebUtils.sendRedirect(application, response, "/schedule/CommitSchedule");
        return;
      } else {
        session.setAttribute("uploadSchedule_violations", violations);
        for(final ConstraintViolation violation : violations) {
          if(violation.isHard()) {
            WebUtils.sendRedirect(application, response, "/schedule/displayHardViolations.jsp");
            return;
          }
        }
        WebUtils.sendRedirect(application, response, "/schedule/displaySoftViolations.jsp");
        return;
      }
      
    } catch (final ParseException e) {
      final String message = "Error parsing schedule";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } catch(final SQLException e) {
      final String message = "Error talking to the database";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } catch (final InvalidFormatException e) {
      final String message = "Error parsing schedule spreadsheet";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } catch (final ScheduleParseException e) {
      final String message = "Error parsing schedule";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    }
  }

}
