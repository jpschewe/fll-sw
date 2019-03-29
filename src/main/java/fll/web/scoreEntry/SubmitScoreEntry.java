/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;



import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;

import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;

/**
 * Submit performance scores.
 */
@WebServlet("/scoreEntry/SubmitScoreEntry")
public class SubmitScoreEntry extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      if (null != request.getParameter("delete")) {
        Queries.deletePerformanceScore(connection, request);
      } else if (null != request.getParameter("EditFlag")) {
        final int rowsUpdated = Queries.updatePerformanceScore(challengeDescription, connection, request);
        if (0 == rowsUpdated) {
          throw new FLLInternalException("No rows updated - did the score get deleted?");
        } else if (rowsUpdated > 1) {
          throw new FLLInternalException("Updated multiple rows!");
        }
      } else {
        Queries.insertPerformanceScore(challengeDescription, connection, request);
      }

      response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new FLLRuntimeException(e);
    } catch (final ParseException e) {
      LOGGER.error(e, e);
      throw new FLLInternalException("Cannot parse request parameters: "
          + e.getMessage(), e);
    }

  }

}
