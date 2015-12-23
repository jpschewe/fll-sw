/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Tournament;
import fll.util.LogUtils;
import fll.web.SessionAttributes;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Populate page context for selectTournament.jsp.
 */
public class SelectTournament {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void populateContext(final HttpSession session,
                                     final PageContext page) {

    final DataSource importDataSource = SessionAttributes.getNonNullAttribute(session, "dbimport", DataSource.class);

    Connection connection = null;
    try {
      connection = importDataSource.getConnection();

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      page.setAttribute("tournaments", tournaments);

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

}
