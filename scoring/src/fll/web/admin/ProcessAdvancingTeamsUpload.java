/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.Utilities;
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Process the uploaded data and forward to GatherAdvancementData.
 */
@WebServlet("/admin/ProcessAdvancingTeamsUpload")
public final class ProcessAdvancingTeamsUpload extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final StringBuilder message = new StringBuilder();
    final String advanceFile = SessionAttributes.getNonNullAttribute(session, "advanceFile", String.class);
    final File file = new File(advanceFile);
    Connection connection = null;
    try {
      if (!file.exists()
          || !file.canRead()) {
        throw new RuntimeException("Cannot read file: "
            + advanceFile);
      }

      final String teamNumberColumnName = request.getParameter("teamNumber");
      if (null == teamNumberColumnName) {
        throw new RuntimeException("Cannot find 'teamNumber' request parameter");
      }

      final String sheetName = SessionAttributes.getAttribute(session, "sheetName", String.class);

      final Collection<Integer> teams = processFile(file, sheetName, teamNumberColumnName);

      if (!file.delete()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Error deleting file, will need to wait until exit. Filename: "
              + file.getAbsolutePath());
        }
      }

      // process as if the user had selected these teams
      final DataSource datasource = SessionAttributes.getDataSource(session);
      connection = datasource.getConnection();
      GatherAdvancementData.processAdvancementData(response, session, false, connection, teams);
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error saving advancment data into the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving advancment data into the database", sqle);
    } catch (final ParseException e) {
      message.append("<p class='error'>Error saving team data into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving advancment data into the database", e);
    } catch (final Exception e) {
      message.append("<p class='error'>Error saving advancment data into the database: "
          + e.getMessage() + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving advancment data into the database", e);
    } finally {
      if (!file.delete()) {
        file.deleteOnExit();
      }
      SQLFunctions.close(connection);
    }
  }

  /**
   * Get the team numbers of advancing teams.
   * 
   * @throws InvalidFormatException
   */
  public static Collection<Integer> processFile(final File file,
                                                final String sheetName,
                                                final String teamNumberColumnName) throws SQLException, IOException,
      ParseException, InvalidFormatException {
    final CellFileReader reader;
    if (file.getName().endsWith(".xls")
        || file.getName().endsWith(".xslx")) {
      FileInputStream fis = null;
      try {
        fis = new FileInputStream(file);
        reader = new ExcelCellReader(fis, sheetName);
      } finally {
        if (null != fis) {
          fis.close();
        }
      }
    } else {
      // determine if the file is tab separated or comma separated, check the
      // first line for tabs and if they aren't found, assume it's comma
      // separated
      final BufferedReader breader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                                                                              Utilities.DEFAULT_CHARSET));
      try {
        final String firstLine = breader.readLine();
        if (null == firstLine) {
          LOGGER.error("Empty file");
          return Collections.emptyList();
        }
        if (firstLine.indexOf("\t") != -1) {
          reader = new CSVCellReader(file, '\t');
        } else {
          reader = new CSVCellReader(file);
        }
      } finally {
        breader.close();
      }
    }

    // parse out the first non-blank line as the names of the columns
    String[] columnNames = reader.readNext();
    while (columnNames.length < 1) {
      columnNames = reader.readNext();
    }

    int teamNumColumnIdx = 0;
    while (!teamNumberColumnName.equals(columnNames[teamNumColumnIdx])) {
      ++teamNumColumnIdx;
    }

    final Collection<Integer> teams = new LinkedList<Integer>();
    String[] data = reader.readNext();
    while (null != data) {
      if (teamNumColumnIdx < data.length) {
        final String teamNumStr = data[teamNumColumnIdx];
        if (null != teamNumStr
            && !"".equals(teamNumStr.trim())) {
          final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumStr).intValue();
          teams.add(teamNumber);
        }
      }

      data = reader.readNext();
    }
    return teams;
  }

}
