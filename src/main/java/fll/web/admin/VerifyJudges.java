/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.PolyNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.JudgeInformation;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;

/**
 * Verify the judges information.
 */
@WebServlet("/admin/VerifyJudges")
public class VerifyJudges extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  @SuppressFBWarnings(value = "XSS_REQUEST_PARAMETER_TO_JSP_WRITER", justification = "Checking category name retrieved from request against valid category names")
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of VerifyJudges.processRequest");
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      // keep track of any errors
      final StringBuilder error = new StringBuilder();
      final StringBuilder warning = new StringBuilder();

      // populate a hash where key is category name and value is an empty
      // Set. Use set so there are no duplicates
      final List<SubjectiveScoreCategory> subjectiveCategories = challengeDescription.getSubjectiveCategories();

      final int numRows = WebUtils.getIntRequestParameter(request, "total_num_rows");
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Found num rows: "
            + numRows);
      }

      final Collection<JudgeInformation> judges = new LinkedList<>();

      // walk request and push judge id into the Set, if not null or empty,
      // in the value for each category in the hash.
      for (int row = 1; row <= numRows; ++row) {
        String id = request.getParameter("id"
            + row);
        final String category = WebUtils.getNonNullRequestParameter(request, "cat"
            + row);
        final String station = WebUtils.getNonNullRequestParameter(request, "station"
            + row);
        if (null != id) {
          id = sanitizeJudgeId(id);
          if (id.length() > 0) {
            final JudgeInformation judge = new JudgeInformation(id, category, station);
            judges.add(judge);
          }
        }
      }

      // check that each judging station has a judge for each category
      final Collection<String> judgingStations = Queries.getJudgingStations(connection, tournament);
      for (final String jstation : judgingStations) {
        for (final SubjectiveScoreCategory cat : subjectiveCategories) {
          final String categoryName = cat.getName();
          boolean found = false;

          for (final JudgeInformation judge : judges) {
            if (judge.getCategory().equals(categoryName)
                && judge.getGroup().equals(jstation)) {
              found = true;
            }
          }

          if (!found) {
            warning.append("You should specify at least one judge for category '"
                + categoryName
                + "' at judging station '"
                + jstation
                + "'<br/> This is OK if some judges will be entered through the web application.");
          }

        }

      }

      session.setAttribute(GatherJudgeInformation.JUDGES_KEY, judges);

      if (error.length() > 0) {
        final StringBuilder message = new StringBuilder();
        message.append("<p class='error' id='error'>");
        message.append(error);
        message.append("</p>");
        if (warning.length() > 0) {
          message.append("<p class='warning' id='warning'>");
          message.append(warning);
          message.append("</p>");
        }
        SessionAttributes.appendToMessage(session, message.toString());
        response.sendRedirect(response.encodeRedirectURL("judges.jsp"));
      } else {
        if (warning.length() > 0) {
          final StringBuilder message = new StringBuilder();
          message.append("<p class='warning' id='warning'>");
          message.append(warning);
          message.append("</p>");
          SessionAttributes.appendToMessage(session, message.toString());
        }
        response.sendRedirect(response.encodeRedirectURL("displayJudges.jsp"));
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

  }

  /**
   * Make sure that judge ID's don't contain characters that
   * will give us problems.
   */
  private @PolyNull String sanitizeJudgeId(final @PolyNull String id) {
    if (null == id) {
      return null;
    } else {
      String fixed = id.trim();
      fixed = fixed.toUpperCase();
      fixed = fixed.replaceAll("\"", "_");
      fixed = fixed.replaceAll("'", "_");
      return fixed;
    }
  }

}
