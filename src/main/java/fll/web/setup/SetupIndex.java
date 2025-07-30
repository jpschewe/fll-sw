/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.setup;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Utilities;
import fll.web.ApplicationAttributes;
import fll.xml.DescriptionInfo;

/**
 * Utilities for /setup/index.jsp.
 */
public final class SetupIndex {

  private SetupIndex() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Populate the page context with information for the jsp.
   * Variables added to the pageContext:
   * <ul>
   * <li>descriptions - {@link List} of {@link DescriptionInfo} (sorted by
   * title)</li>
   * <li>dbinitialized - boolean if the database has been initialized</li>
   * </ul>
   * 
   * @param application application variable access
   * @param pageContext page variable access
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {

    final List<DescriptionInfo> descriptions = DescriptionInfo.getAllKnownChallengeDescriptionInfo();
    pageContext.setAttribute("descriptions", descriptions);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      pageContext.setAttribute("dbinitialized", Utilities.testDatabaseInitialized(connection));
    } catch (final SQLException sqle) {
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    }
  }

}
