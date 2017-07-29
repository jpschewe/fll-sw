/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Utilities;
import fll.flltools.MhubMessageHandler;
import fll.util.LogUtils;

/**
 * Take care of initializing some variables in the servlet context.
 */
@WebListener
public class FLLContextListener implements ServletContextListener {

  private static final Logger LOGGER = LogUtils.getLogger();

  private MhubMessageHandler mhubMessageHandler = null;

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    final ServletContext application = event.getServletContext();
    if (null == application) {
      LOGGER.error("Got null servlet context inside contextInitialized, this is odd");
      return;
    }

    initDataSource(application);

    // set some default text
    application.setAttribute(ApplicationAttributes.SCORE_PAGE_TEXT, "");

    // make sure the default display object exists
    DisplayInfo.getDisplayInformation(application);

    mhubMessageHandler = new MhubMessageHandler(application);
    mhubMessageHandler.start();

  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    final ServletContext application = event.getServletContext();

    if (null != mhubMessageHandler) {
      mhubMessageHandler.shutdown();
    }

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

    Utilities.unloadDBDriver();
  }

  private static void initDataSource(final ServletContext application) {
    final String database = application.getRealPath("/WEB-INF/flldb");

    // initialize the datasource
    if (null == ApplicationAttributes.getDataSource(application)) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Datasource not available, creating");
      }
      final DataSource datasource = Utilities.createFileDataSource(database);
      application.setAttribute(ApplicationAttributes.DATASOURCE, datasource);

      // make sure that the database has started everything by doing a query on
      // the database
      try (Connection connection = datasource.getConnection()) {
        Utilities.testDatabaseInitialized(connection);
      } catch (final SQLException e) {
        LOGGER.error("Got an error starting up the database, this is probably not good. Maybe it will work itself out",
                     e);
      }
    }
  }

}
