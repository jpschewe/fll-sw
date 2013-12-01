/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;

/**
 * 
 */
@WebServlet("/playoff/LimitTableAssignments")
public class LimitTableAssignments extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    Connection connection = null;
    PreparedStatement delete = null;
    PreparedStatement insert = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final int tournament = Queries.getCurrentTournament(connection);

      final String division = request.getParameter("division");
      if (null == division) {
        throw new FLLRuntimeException("Division parameter cannot be null");
      }

      delete = connection.prepareStatement("DELETE FROM table_division" //
          + " WHERE tournament = ?" //
          + " AND playoff_division = ?"//
      );
      delete.setInt(1, tournament);
      delete.setString(2, division);
      delete.executeUpdate();

      insert = connection.prepareStatement("INSERT INTO table_division (tournament, playoff_division, table_id) VALUES (?, ?, ?)");
      insert.setInt(1, tournament);
      insert.setString(2, division);
      for (final String idStr : request.getParameterValues("tables")) {
        final int id = Integer.valueOf(idStr);
        insert.setInt(3, id);
        insert.executeUpdate();
      }

    } catch (final SQLException e) {
      final String errorMessage = "There was an error talking to the database";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    } finally {
      SQLFunctions.close(delete);
      SQLFunctions.close(insert);
      SQLFunctions.close(connection);
    }
    
    //FIXME set form parameters division, firstRound, lastRound
    response.sendRedirect(response.encodeRedirectURL("scoregenbrackets.jsp"));

  }

}
