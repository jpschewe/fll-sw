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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.db.TableInformation;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.BracketSortType;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Work with bracket_parameters.jsp.
 */
@WebServlet("/playoff/BracketParameters")
public class BracketParameters extends BaseFLLServlet {

  private static Logger LOGGER = LogUtils.getLogger();

  /**
   * Setup variables needed for the page.
   * <ul>
   * <li>sortOptions - BracketSortType[]</li>
   * <li>defaultSort - BracketSortType</li>
   * <li>tableInfo - List&lt;TableInformation&gt;</li>
   * </ul>
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    pageContext.setAttribute("sortOptions", BracketSortType.class.getEnumConstants());
    pageContext.setAttribute("defaultSort", BracketSortType.SEEDING);

    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final int currentTournament = data.getCurrentTournament().getTournamentID();

      final List<TableInformation> tableInfo = TableInformation.getTournamentTableInformation(connection,
                                                                                              currentTournament,
                                                                                              data.getBracket());
      pageContext.setAttribute("tableInfo", tableInfo);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    Connection connection = null;
    PreparedStatement delete = null;
    PreparedStatement insert = null;
    try {
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
      connection = datasource.getConnection();

      delete = connection.prepareStatement("DELETE FROM table_division" //
          + " WHERE tournament = ?" //
          + " AND playoff_division = ?"//
      );
      delete.setInt(1, data.getCurrentTournament().getTournamentID());
      delete.setString(2, data.getBracket());
      delete.executeUpdate();

      insert = connection.prepareStatement("INSERT INTO table_division (tournament, playoff_division, table_id) VALUES (?, ?, ?)");
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

      session.setAttribute(PlayoffIndex.SESSION_DATA, data);

    } catch (final SQLException e) {
      final String errorMessage = "There was an error talking to the database";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    } finally {
      SQLFunctions.close(delete);
      SQLFunctions.close(insert);
      SQLFunctions.close(connection);
    }

    response.sendRedirect(response.encodeRedirectURL("InitializeBrackets"));
  }
}
