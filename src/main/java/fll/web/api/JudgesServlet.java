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
import java.util.Optional;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.JudgeInformation;
import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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

    if (!auth.isJudge()
        && !auth.isReportGenerator()) {
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

      final int numNewJudges = processJudges(connection, currentTournament, judges);

      final UploadResult result = new UploadResult(true, Optional.of("Successfully uploaded judges"), numNewJudges);
      response.reset();
      jsonMapper.writeValue(writer, result);

    } catch (final SQLException e) {
      LOGGER.error("Error uploading judges", e);

      final UploadResult result = new UploadResult(false, Optional.ofNullable(e.getMessage()), -1);
      jsonMapper.writeValue(writer, result);
    }
  }

  /**
   * Process uploaded judges. Add judges to the database that haven't been seen
   * yet.
   * 
   * @param connection database connection
   * @param currentTournament id for the tournament the judges are for
   * @param judges the judges
   * @return the number of new judges seen
   * @throws SQLException on a database error
   */
  public static int processJudges(final Connection connection,
                                  final int currentTournament,
                                  final Collection<JudgeInformation> judges)
      throws SQLException {
    int numNewJudges = 0;

    final Collection<JudgeInformation> currentJudges = JudgeInformation.getJudges(connection, currentTournament);
    LOGGER.trace("Current judges: {}", currentJudges);

    try (
        PreparedStatement insertJudge = connection.prepareStatement("INSERT INTO Judges (id, category, Tournament, station, final_scores) VALUES (?, ?, ?, ?, ?)");
        PreparedStatement updateJudge = connection.prepareStatement("UPDATE Judges SET final_scores = ? WHERE id = ? AND category = ? AND Tournament = ? AND station = ?")) {
      insertJudge.setInt(3, currentTournament);
      updateJudge.setInt(4, currentTournament);

      for (final JudgeInformation judge : judges) {
        if (null != judge) {
          if (!currentJudges.contains(judge)) {
            LOGGER.trace("Adding judge: {}", judge.getId());
            insertJudge.setString(1, judge.getId());
            insertJudge.setString(2, judge.getCategory());
            insertJudge.setString(4, judge.getGroup());
            insertJudge.setBoolean(5, judge.isFinalScores());
            insertJudge.executeUpdate();
            ++numNewJudges;
          } else {
            if (judge.isFinalScores()) {
              updateJudge.setBoolean(1, judge.isFinalScores());
              updateJudge.setString(2, judge.getId());
              updateJudge.setString(3, judge.getCategory());
              updateJudge.setString(5, judge.getGroup());
              updateJudge.executeUpdate();
            }
          }
        } // non-null judge
      } // foreach judge sent
    } // prepared statement

    return numNewJudges;
  }

  /**
   * The result of an upload.
   */
  public static final class UploadResult extends ApiResult {
    /**
     * @param success {@link #getSuccess()}
     * @param message {@link #getMessage()}
     * @param numNewJudges {@link #getNumNewJudges()}
     */
    public UploadResult(final boolean success,
                        final Optional<String> message,
                        final int numNewJudges) {
      super(success, message);
      mNumNewJudges = numNewJudges;
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
