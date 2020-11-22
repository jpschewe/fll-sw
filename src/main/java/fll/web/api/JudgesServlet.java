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
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.JudgeInformation;
import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;

/**
 * API access to judges.
 * GET: {judges}
 * POST: expects the data from GET and returns UploadResult
 */
@WebServlet("/api/Judges/*")
public class JudgesServlet extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.isJudge()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final int currentTournament = Queries.getCurrentTournament(connection);
      final Collection<JudgeInformation> judges = JudgeInformation.getJudges(connection, currentTournament);

      jsonMapper.writeValue(writer, judges);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic table based upon categories")
  @Override
  protected final void doPost(final HttpServletRequest request,
                              final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.isJudge()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    response.reset();
    response.setContentType("application/json");
    final PrintWriter writer = response.getWriter();

    final ServletContext application = getServletContext();

    int numNewJudges = 0;

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int currentTournament = Queries.getCurrentTournament(connection);

      final StringWriter debugWriter = new StringWriter();
      request.getReader().transferTo(debugWriter);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Read data: "
            + debugWriter.toString());
      }

      final Reader reader = new StringReader(debugWriter.toString());

      final Collection<JudgeInformation> judges = jsonMapper.readValue(reader, JudgesTypeInformation.INSTANCE);

      final Collection<JudgeInformation> currentJudges = JudgeInformation.getJudges(connection, currentTournament);
      LOGGER.trace("Current judges: {}", currentJudges);

      try (
          PreparedStatement insertJudge = connection.prepareStatement("INSERT INTO Judges (id, category, Tournament, station) VALUES (?, ?, ?, ?)")) {
        insertJudge.setInt(3, currentTournament);

        for (final JudgeInformation judge : judges) {
          if (null != judge) {
            JudgeInformation found = null;
            for (final JudgeInformation cjudge : currentJudges) {
              if (Objects.equals(cjudge, judge)) {
                found = cjudge;
                break;
              }
            }

            if (null == found) {
              LOGGER.trace("Adding judge: {}", judge.getId());

              insertJudge.setString(1, judge.getId());
              insertJudge.setString(2, judge.getCategory());
              insertJudge.setString(4, judge.getGroup());
              insertJudge.executeUpdate();
              ++numNewJudges;
            } // add judge
          } // non-null judge
        } // foreach judge sent

        final UploadResult result = new UploadResult(true, "Successfully uploaded judges", numNewJudges);
        response.reset();
        jsonMapper.writeValue(writer, result);
      } // prepared statement

    } catch (final SQLException e) {
      LOGGER.error("Error uploading judges", e);

      final UploadResult result = new UploadResult(false, e.getMessage(), numNewJudges);
      jsonMapper.writeValue(writer, result);
    }

  }

  /**
   * The result of an upload.
   */
  public static final class UploadResult {
    /**
     * @param success {@link #getSuccess()}
     * @param message {@link #getMessage()}
     * @param numNewJudges {@link #getNumNewJudges()}
     */
    public UploadResult(final boolean success,
                        final String message,
                        final int numNewJudges) {
      mSuccess = success;
      mMessage = message;
      mNumNewJudges = numNewJudges;
    }

    private final boolean mSuccess;

    /**
     * @return if the upload was successful
     */
    public boolean getSuccess() {
      return mSuccess;
    }

    private final String mMessage;

    /**
     * @return message for the user
     */
    public String getMessage() {
      return mMessage;
    }

    private final int mNumNewJudges;

    /**
     * @return number of new judges
     */
    public int getNumNewJudges() {
      return mNumNewJudges;
    }

  }

  private static final class JudgesTypeInformation extends TypeReference<Collection<JudgeInformation>> {
    public static final JudgesTypeInformation INSTANCE = new JudgesTypeInformation();
  }

}
