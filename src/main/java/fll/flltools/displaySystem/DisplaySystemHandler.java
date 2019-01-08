/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools.displaySystem;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.flltools.MhubMessageHandler;
import fll.flltools.MhubParameters;
import fll.flltools.displaySystem.list.SetArray;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;
import fll.web.scoreboard.Last8;
import fll.web.scoreboard.Top10;

/**
 * Handle updating the display system based on the what is show on the default
 * display.
 */
@ThreadSafe
public class DisplaySystemHandler extends Thread {

  private static final Logger LOGGER = LogUtils.getLogger();

  private boolean listDisplayed = false;

  private boolean running = false;

  private final ServletContext application;

  private final MhubMessageHandler messageHandler;

  /**
   * Construct the display handler, but doesn't start it
   * 
   * @param messageHandler used to send messages
   * @param application used to get the database connection and current display
   *          state
   */
  public DisplaySystemHandler(@Nonnull final MhubMessageHandler messageHandler,
                              @Nonnull final ServletContext application) {
    this.messageHandler = messageHandler;
    this.application = application;
  }

  /**
   * Stop the thread. If called from another thread, wait for this thread to
   * stop.
   */
  public void shutdown() {
    running = false;
    this.interrupt(); // stop any sleep
    if (Thread.currentThread() != this) {
      try {
        this.join();
      } catch (final InterruptedException e) {
        LOGGER.warn("Interrupted waiting for shutdown, exiting", e);
      }
    }
  }

  @Override
  public void run() {
    running = true;

    Duration flipRate = Duration.ofSeconds(GlobalParameters.DIVISION_FLIP_RATE_DEFAULT);

    int currentAwardGroupIndex = -1;
    boolean showingMostRecent = false;

    while (running) {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      if (null != datasource) {
        try (Connection connection = datasource.getConnection()) {
          if (Utilities.testDatabaseInitialized(connection)) {

            flipRate = Duration.ofSeconds(GlobalParameters.getIntGlobalParameter(connection,
                                                                                 GlobalParameters.DIVISION_FLIP_RATE));

            final String hostname = MhubParameters.getHostname(connection);
            if (null != hostname) {

              final DisplayInfo displayInfo = DisplayInfo.findOrCreateDefaultDisplay(application);
              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Found default display. Scoreboard? "
                    + displayInfo.isScoreboard());
              }

              final String displayNode = MhubParameters.getDisplayNode(connection);

              if (displayInfo.isScoreboard()
                  && !listDisplayed) {
                final fll.flltools.BaseMessage msg = new fll.flltools.displaySystem.list.Show(displayNode);

                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace("Sending show message");
                }

                if (messageHandler.sendMesasge(msg)) {
                  listDisplayed = true;
                } else {
                  LOGGER.warn("Could not send show message");
                }

              } else if (!displayInfo.isScoreboard()
                  && listDisplayed) {
                final fll.flltools.BaseMessage msg = new fll.flltools.displaySystem.list.Hide(displayNode);

                if (LOGGER.isTraceEnabled()) {
                  LOGGER.trace("Sending show message");
                }

                if (messageHandler.sendMesasge(msg)) {
                  listDisplayed = false;
                } else {
                  LOGGER.warn("Could not send hide message");
                }
              }

              if (listDisplayed) {

                final SetArray.Payload payload;
                final List<String> awardGroups = Queries.getAwardGroups(connection);

                if (!showingMostRecent
                    || awardGroups.isEmpty()) {
                  // display most recent

                  payload = Last8.getTableAsList(application);

                  showingMostRecent = true;
                  currentAwardGroupIndex = -1;
                } else {
                  // show top 10 for an award group
                  ++currentAwardGroupIndex;
                  if (currentAwardGroupIndex >= awardGroups.size()) {
                    currentAwardGroupIndex = 0;
                  }

                  final String awardGroupName = awardGroups.get(currentAwardGroupIndex);

                  payload = Top10.getTableAsList(application, awardGroupName);

                  showingMostRecent = false;
                }

                final SetArray msg = new SetArray();
                msg.setPayload(payload);
                if (!messageHandler.sendMesasge(msg)) {
                  LOGGER.warn("Unable to send setArray message");
                }
              }

            } // non-null hostname

          } // the database is initialized

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
