/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.TableInformation;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * Helper code for choose-table.jsp.
 */
@WebServlet("/scoreEntry/ChooseTable")
public class ChooseTable extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param application get application variables
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final Collection<String> tables = TableInformation.getAllTableNames(connection, tournament);
      pageContext.setAttribute("tables", tables);

    } catch (final SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final String selectedTable = request.getParameter("table");
    LOGGER.trace("Selected table '{}'", selectedTable);

    if (null == selectedTable
        || "__all__".equals(selectedTable)) {
      session.removeAttribute("scoreEntrySelectedTable");
    } else {
      session.setAttribute("scoreEntrySelectedTable", selectedTable);
    }

    response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
  }

}
