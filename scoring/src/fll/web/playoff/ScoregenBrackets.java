/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.db.TableInformation;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

/**
 * Helpers for scoregenbrackets.jsp.
 */
public class ScoregenBrackets {
  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @param application application context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final HttpServletRequest request,
                                     final PageContext pageContext) {

    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      String division = request.getParameter("division");
      if (null == division) {
        division = (String) request.getAttribute("division");

        if (null == division) {
          throw new RuntimeException(
                                     "No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
        }
      }
      pageContext.setAttribute("division", division);

      final int currentTournament = Queries.getCurrentTournament(connection);

      final List<TableInformation> tableInfo = TableInformation.getTournamentTableInformation(connection,
                                                                                              currentTournament,
                                                                                              division);
      pageContext.setAttribute("tableInfo", tableInfo);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(connection);
    }

  }
}