/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.scheduler.ScheduleParseException;
import fll.scheduler.SubjectiveStation;
import fll.scheduler.TournamentSchedule;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

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

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    try {
      final List<SubjectiveStation> subjectiveStations = uploadScheduleData.getSubjectiveStations();
      if (null == subjectiveStations) {
        final File scheduleFile = uploadScheduleData.getScheduleFile();
        Objects.requireNonNull(scheduleFile);

        final String sheetName = uploadScheduleData.getSelectedSheet();

        try (final CellFileReader reader = CellFileReader.createCellReader(scheduleFile, sheetName)) {
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
    Objects.requireNonNull(scheduleFile);

    final String sheetName = uploadScheduleData.getSelectedSheet();
    try (Connection connection = datasource.getConnection()) {

      final boolean isCsvFile = !ExcelCellReader.isExcelFile(scheduleFile);

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
      final TournamentSchedule schedule;
      if (isCsvFile) {
        schedule = new TournamentSchedule(name, scheduleFile, subjectiveHeaders);
      } else {
        try (final InputStream stream = new FileInputStream(scheduleFile)) {
          schedule = new TournamentSchedule(name, stream, sheetName, subjectiveHeaders);
        }
      }
      uploadScheduleData.setSchedule(schedule);

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
