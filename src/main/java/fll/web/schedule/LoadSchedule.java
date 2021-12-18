/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.scheduler.ScheduleParseException;
import fll.scheduler.SubjectiveStation;
import fll.scheduler.TournamentSchedule;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Load the schedule.
 */
@WebServlet("/schedule/LoadSchedule")
public class LoadSchedule extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    try {
      if (!uploadScheduleData.isSubjectiveStationsSet()) {
        final File scheduleFile = uploadScheduleData.getScheduleFile();

        final String sheetName = uploadScheduleData.getSelectedSheet();

        try (CellFileReader reader = CellFileReader.createCellReader(scheduleFile, sheetName)) {
          final ColumnInformation columnInfo = TournamentSchedule.findColumns(reader, new LinkedList<String>());
          if (!columnInfo.getUnusedColumns().isEmpty()) {
            uploadScheduleData.setUnusedHeaders(columnInfo.getUnusedColumns());
            WebUtils.sendRedirect(application, response, "/schedule/chooseSubjectiveHeaders.jsp");
            return;
          } else {
            uploadScheduleData.setSubjectiveStations(Collections.emptyList());
          }
        }
      }

      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      loadSchedule(uploadScheduleData, datasource);

      WebUtils.sendRedirect(application, response, "CheckMissingTeams");
    } catch (final InvalidFormatException e) {
      final String message = "Error parsing schedule spreadsheet";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }

  private void loadSchedule(final UploadScheduleData uploadScheduleData,
                            final DataSource datasource) {

    final File scheduleFile = uploadScheduleData.getScheduleFile();

    final String sheetName = uploadScheduleData.getSelectedSheet();
    try (Connection connection = datasource.getConnection()) {

      final String fullname = scheduleFile.getName();
      final int dotIndex = fullname.lastIndexOf('.');
      final String name;
      if (-1 != dotIndex) {
        name = fullname.substring(0, dotIndex);
      } else {
        name = fullname;
      }

      final List<SubjectiveStation> subjectiveStations = uploadScheduleData.getSubjectiveStations();

      final List<String> subjectiveHeaders = new LinkedList<>();
      for (final SubjectiveStation station : subjectiveStations) {
        subjectiveHeaders.add(station.getName());
      }
      final TournamentSchedule schedule = new TournamentSchedule(name,
                                                                 CellFileReader.createCellReader(scheduleFile,
                                                                                                 sheetName),
                                                                 subjectiveHeaders);
      uploadScheduleData.setSchedule(schedule);

      if (!scheduleFile.delete()) {
        scheduleFile.deleteOnExit();
      }

    } catch (final SQLException e) {
      final String message = "Error talking to the database";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } catch (final ParseException e) {
      final String message = "Error parsing schedule";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } catch (final ScheduleParseException e) {
      final String message = "Error parsing schedule";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } catch (final InvalidFormatException e) {
      final String message = "Error parsing schedule spreadsheet";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } catch (final IOException e) {
      final String message = "Error reading schedule spreadsheet";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    }
  }

}
