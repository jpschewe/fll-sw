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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.db.Queries;
import fll.scheduler.ConstraintViolation;
import fll.scheduler.SchedParams;
import fll.scheduler.ScheduleChecker;
import fll.scheduler.ScheduleParseException;
import fll.scheduler.SubjectiveStation;
import fll.scheduler.TournamentSchedule;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Read the uploaded file, then check for constraint
 * violations. Stores the schedule in
 * {@link UploadScheduleData#setSchedule(TournamentSchedule)}.
 */
@WebServlet("/schedule/CheckViolations")
public class CheckViolations extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);
    final File scheduleFile = uploadScheduleData.getScheduleFile();
    Objects.requireNonNull(scheduleFile);

    final String sheetName = uploadScheduleData.getSelectedSheet();
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      // if uploadSchedule_subjectiveHeaders is set, then use this as the list
      // of subjective headers
      List<SubjectiveStation> subjectiveStations = uploadScheduleData.getSubjectiveStations();

      final boolean isCsvFile = !ExcelCellReader.isExcelFile(scheduleFile);

      if (null == subjectiveStations) {
        // get unused headers

        try (final CellFileReader reader = CellFileReader.createCellReader(scheduleFile, sheetName)) {
          final ColumnInformation columnInfo = TournamentSchedule.findColumns(reader, new LinkedList<String>());
          // stream.close();
          if (!columnInfo.getUnusedColumns().isEmpty()) {
            uploadScheduleData.setUnusedHeaders(columnInfo.getUnusedColumns());
            WebUtils.sendRedirect(application, response, "/schedule/chooseSubjectiveHeaders.jsp");
            return;
          } else {
            subjectiveStations = Collections.emptyList();
          }
        }
      }

      final String fullname = scheduleFile.getName();
      final int dotIndex = fullname.lastIndexOf('.');
      final String name;
      if (-1 != dotIndex) {
        name = fullname.substring(0, dotIndex);
      } else {
        name = fullname;
      }
      final List<String> subjectiveHeaders = new LinkedList<String>();
      for (final SubjectiveStation station : subjectiveStations) {
        subjectiveHeaders.add(station.getName());
      }
      final TournamentSchedule schedule;
      if (isCsvFile) {
        schedule = new TournamentSchedule(name, scheduleFile, subjectiveHeaders);
      } else {
        try (final InputStream stream = new FileInputStream(scheduleFile)) {
          schedule = new TournamentSchedule(name, stream, sheetName, subjectiveHeaders);
        }
      }
      uploadScheduleData.setSchedule(schedule);

      final int tournamentID = Queries.getCurrentTournament(connection);
      final Collection<ConstraintViolation> violations = schedule.compareWithDatabase(connection, tournamentID);
      final SchedParams schedParams = new SchedParams(subjectiveStations, SchedParams.DEFAULT_PERFORMANCE_MINUTES,
                                                      SchedParams.MINIMUM_CHANGETIME_MINUTES,
                                                      SchedParams.MINIMUM_PERFORMANCE_CHANGETIME_MINUTES);
      final ScheduleChecker checker = new ScheduleChecker(schedParams, schedule);
      violations.addAll(checker.verifySchedule());
      if (violations.isEmpty()) {
        WebUtils.sendRedirect(application, response, "promptForEventDivisions.jsp");
        return;
      } else {
        uploadScheduleData.setViolations(violations);
        for (final ConstraintViolation violation : violations) {
          if (ConstraintViolation.Type.HARD == violation.getType()) {
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
    } catch (final SQLException e) {
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
    } finally {
      SQLFunctions.close(connection);

      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }

}
