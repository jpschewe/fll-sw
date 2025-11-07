/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import fll.Utilities;
import fll.util.FLLInternalException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Take care of initializing some variables in the servlet context.
 */
@WebListener
public class FLLContextListener implements ServletContextListener {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    final ServletContext application = event.getServletContext();
    if (null == application) {
      throw new FLLInternalException("Got null servlet context inside contextInitialized, this is odd");
    }

    // setup the character encoding
    application.setRequestCharacterEncoding(Utilities.DEFAULT_CHARSET);
    application.setResponseCharacterEncoding(Utilities.DEFAULT_CHARSET);

    initDataSource(application);

    // set some default text
    application.setAttribute(ApplicationAttributes.SCORE_PAGE_TEXT, "");
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    final ServletContext application = event.getServletContext();

    // shutdown the database
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    if (null != datasource) {
      try (Connection connection = datasource.getConnection()) {
        try (Statement stmt = connection.createStatement()) {
          stmt.executeUpdate("SHUTDOWN COMPACT");
        } catch (final SQLException e) {
          LOGGER.error("Error shutting down the database", e);
        }
      } catch (final SQLException e) {
        LOGGER.error("Error getting connection to shutdown the database", e);
      }
    }
  }

  private static void initDataSource(final ServletContext application) {
    final String database = application.getRealPath("/WEB-INF/flldb");

    // initialize the datasource
    if (null == ApplicationAttributes.getAttribute(application, ApplicationAttributes.DATASOURCE, DataSource.class)) {
      LOGGER.trace("Datasource not available, creating");

      final DataSource datasource = Utilities.createFileDataSource(database);
      application.setAttribute(ApplicationAttributes.DATASOURCE, datasource);

      // make sure that the database has started everything by doing a query on
      // the database
      try (Connection connection = datasource.getConnection()) {
        Utilities.testDatabaseInitialized(connection);
      } catch (final SQLException e) {
        throw new FLLInternalException("Got an error starting up the database", e);
      }
    }
  }

}
