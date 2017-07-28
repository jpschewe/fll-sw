/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools.displaySystem;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.db.GlobalParameters;
import fll.flltools.MhubHandler;
import fll.flltools.MhubParameters;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;

/**
 * Handle updating the display system based on the what is show on the default
 * display.
 */
@WebListener
public class DisplaySystemHandler implements ServletContextListener {

  private static final Logger LOGGER = LogUtils.getLogger();

  private boolean listDisplayed = false;

  private ExecutorService executor = null;

  private boolean running = false;

  @Override
  public void contextInitialized(@Nonnull final ServletContextEvent event) {
    if (null != executor) {
      throw new IllegalStateException("Handler is already executing, cannot start again");
    }

    executor = Executors.newSingleThreadExecutor();
    executor.submit(() -> execute(event.getServletContext()));
  }

  @Override
  public void contextDestroyed(@Nonnull final ServletContextEvent ignored) {
    running = false;
    executor.shutdown();
    executor = null;
  }

  private void execute(final ServletContext application) {
    running = true;

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Duration flipRate = Duration.ofSeconds(GlobalParameters.getIntGlobalParameter(connection,
                                                                                          GlobalParameters.DIVISION_FLIP_RATE));

      while (running) {
        final String hostname = MhubParameters.getHostname(connection);
        final MhubHandler handler = MhubHandler.getInstance();
        if (null != hostname
            && handler != null) {

          final DisplayInfo displayInfo = DisplayInfo.findOrCreateDefaultDisplay(application);
          if (displayInfo.isScoreboard()
              && !listDisplayed) {
            listDisplayed = true;
            final fll.flltools.BaseMessage msg = new fll.flltools.displaySystem.list.Show();
            handler.sendMesasge(msg);
          } else if (!displayInfo.isScoreboard()
              && listDisplayed) {
            listDisplayed = false;
            final fll.flltools.BaseMessage msg = new fll.flltools.displaySystem.list.Hide();
            handler.sendMesasge(msg);
          }

          // FIXME keep track of which display is showing and cycle through the
          // options

        }

        // wait for a bit
        try {
          Thread.sleep(flipRate.toMillis());
        } catch (final InterruptedException e) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Got interrupted, assuming should wake up and possibly exit", e);
          }
        }
      } // while running
    } catch (final SQLException e) {
      LOGGER.error("Error talking to the database, mhub display system handler terminated", e);
    }
  }

}
