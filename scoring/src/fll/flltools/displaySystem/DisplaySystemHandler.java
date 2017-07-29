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
import fll.flltools.MhubMessageHandler;
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

    Duration flipRate = Duration.ofSeconds(GlobalParameters.DIVISION_FLIP_RATE_DEFAULT);

    while (running) {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      if (null != datasource) {
        try (Connection connection = datasource.getConnection()) {
          flipRate = Duration.ofSeconds(GlobalParameters.getIntGlobalParameter(connection,
                                                                               GlobalParameters.DIVISION_FLIP_RATE));

          final String hostname = MhubParameters.getHostname(connection);
          final MhubMessageHandler handler = MhubMessageHandler.getInstance();
          if (null != hostname
              && null != handler) {

            final DisplayInfo displayInfo = DisplayInfo.findOrCreateDefaultDisplay(application);
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Found default display. Scoreboard? "
                  + displayInfo.isScoreboard());
            }

            if (displayInfo.isScoreboard()
                && !listDisplayed) {
              final fll.flltools.BaseMessage msg = new fll.flltools.displaySystem.list.Show();

              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Sending show message");
              }

              if (handler.sendMesasge(msg)) {
                listDisplayed = true;
              } else {
                LOGGER.warn("Could not send show message");
              }

            } else if (!displayInfo.isScoreboard()
                && listDisplayed) {
              final fll.flltools.BaseMessage msg = new fll.flltools.displaySystem.list.Hide();

              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Sending show message");
              }

              if (handler.sendMesasge(msg)) {
                listDisplayed = false;
              } else {
                LOGGER.warn("Could not send hide message");
              }
            }

            // FIXME keep track of which display is showing and cycle through
            // the
            // options

          } // non-null hostname and non-null handler

        } catch (final SQLException e) {
          LOGGER.error("Error talking to the database, will try again later", e);
        }
      } // have a datasource

      // wait for a bit
      try {
        Thread.sleep(flipRate.toMillis());
      } catch (final InterruptedException e) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Got interrupted, assuming should wake up and possibly exit", e);
        }
      }

    } // while running

  }

}
