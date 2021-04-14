/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.db.TableInformation;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.BracketSortType;

/**
 * Work with bracket_parameters.jsp.
 */
@WebServlet("/playoff/BracketParameters")
public class BracketParameters extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Setup variables needed for the page.
   * <ul>
   * <li>sortOptions - BracketSortType[]</li>
   * <li>defaultSort - BracketSortType</li>
   * <li>tableInfo - List&lt;TableInformation&gt;</li>
   * </ul>
   * 
   * @param application the application context
   * @param session the session context
   * @param pageContext the page context
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    pageContext.setAttribute("sortOptions", BracketSortType.class.getEnumConstants());
    pageContext.setAttribute("defaultSort", BracketSortType.SEEDING);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final int currentTournament = data.getCurrentTournament().getTournamentID();

      final String bracket = data.getBracket();
      if (null == bracket) {
        throw new FLLRuntimeException("Playoff session data has a null bracket");
      }

      final List<TableInformation> tableInfo = TableInformation.getTournamentTableInformation(connection,
                                                                                              currentTournament,
                                                                                              bracket);
      pageContext.setAttribute("tableInfo", tableInfo);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }
  }

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

    final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                          PlayoffSessionData.class);

    final String sortStr = request.getParameter("sort");
    if (null == sortStr
        || "".equals(sortStr)) {
      throw new FLLRuntimeException("Missing parameter 'sort'");
    }

    final BracketSortType sort = BracketSortType.valueOf(sortStr);

    data.setSort(sort);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      try (PreparedStatement delete = connection.prepareStatement("DELETE FROM table_division" //
          + " WHERE tournament = ?" //
          + " AND playoff_division = ?"//
      )) {
        delete.setInt(1, data.getCurrentTournament().getTournamentID());
        delete.setString(2, data.getBracket());
        delete.executeUpdate();
      }

      try (
          PreparedStatement insert = connection.prepareStatement("INSERT INTO table_division (tournament, playoff_division, table_id) VALUES (?, ?, ?)")) {
        insert.setInt(1, data.getCurrentTournament().getTournamentID());
        insert.setString(2, data.getBracket());
        final String[] tables = request.getParameterValues("tables");
        if (null != tables) {
          for (final String idStr : tables) {
            final int id = Integer.parseInt(idStr);
            insert.setInt(3, id);
            insert.executeUpdate();
          }
        }
      }

      session.setAttribute(PlayoffIndex.SESSION_DATA, data);

    } catch (final SQLException e) {
      final String errorMessage = "There was an error talking to the database";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    }

    response.sendRedirect(response.encodeRedirectURL("InitializeBrackets"));
  }
}
