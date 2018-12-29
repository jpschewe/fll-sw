/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.JudgeInformation;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

/**
 * GET: {judges}
 * POST: expects the data from GET and returns UploadResult
 */
@WebServlet("/api/Judges/*")
public class JudgesServlet extends HttpServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException, ServletException {
    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final ObjectMapper jsonMapper = new ObjectMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final int currentTournament = Queries.getCurrentTournament(connection);
      final Collection<JudgeInformation> judges = JudgeInformation.getJudges(connection, currentTournament);

      jsonMapper.writeValue(writer, judges);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic table based upon categories")
  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response) throws IOException, ServletException {
    final ObjectMapper jsonMapper = new ObjectMapper();
    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final ServletContext application = getServletContext();

    int numNewJudges = 0;
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    PreparedStatement insertJudge = null;
    try {
      connection = datasource.getConnection();

      final int currentTournament = Queries.getCurrentTournament(connection);

      final StringWriter debugWriter = new StringWriter();
      IOUtils.copy(request.getReader(), debugWriter);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Read data: "
            + debugWriter.toString());
      }

      final Reader reader = new StringReader(debugWriter.toString());

      final Collection<JudgeInformation> judges = jsonMapper.readValue(reader, JudgesTypeInformation.INSTANCE);

      final Collection<JudgeInformation> currentJudges = JudgeInformation.getJudges(connection, currentTournament);

      insertJudge = connection.prepareStatement("INSERT INTO Judges (id, category, Tournament, station) VALUES (?, ?, ?, ?)");
      insertJudge.setInt(3, currentTournament);

      for (final JudgeInformation judge : judges) {

        if (null != judge) {
          JudgeInformation found = null;
          for (final JudgeInformation cjudge : currentJudges) {
            if (ComparisonUtils.safeEquals(cjudge, judge)) {
              found = cjudge;
            }
          }

          if (null == found) {
            insertJudge.setString(1, judge.getId());
            insertJudge.setString(2, judge.getCategory());
            insertJudge.setString(4, judge.getGroup());
            insertJudge.executeUpdate();
            ++numNewJudges;
          }
        } // non-null judge
      } // foreach judge sent

      final UploadResult result = new UploadResult(true, "Successfully uploaded judges", numNewJudges);
      response.reset();
      jsonMapper.writeValue(writer, result);

    } catch (final SQLException e) {
      LOGGER.error("Error uploading judges", e);

      final UploadResult result = new UploadResult(false, e.getMessage(), numNewJudges);
      jsonMapper.writeValue(writer, result);
    } finally {
      SQLFunctions.close(insertJudge);
      SQLFunctions.close(connection);
    }

  }

  public static final class UploadResult {
    public UploadResult(final boolean success,
                        final String message,
                        final int numNewJudges) {
      mSuccess = success;
      mMessage = message;
      mNumNewJudges = numNewJudges;
    }

    private final boolean mSuccess;

    public boolean getSuccess() {
      return mSuccess;
    }

    private final String mMessage;

    public String getMessage() {
      return mMessage;
    }

    private final int mNumNewJudges;

    public int getNumNewJudges() {
      return mNumNewJudges;
    }

  }

  private static final class JudgesTypeInformation extends TypeReference<Collection<JudgeInformation>> {
    public static final JudgesTypeInformation INSTANCE = new JudgesTypeInformation();
  }

}
