/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Queue to keep track of performance scores that need entering. Enter them with
 * a pool of threads.
 * 
 * @author jpschewe
 */
/* package */class ScoreEntryQueue {

  private static final Logger LOG = Logger.getLogger(ScoreEntryQueue.class);

  /**
   * Create the queue.
   * 
   * @param numThreads the number of threads to use
   * @param testDataConn where to get the data from
   * @param performanceElement the challenge descriptor
   * @param testTournament the tournament to enter scores for
   */
  public ScoreEntryQueue(final int numThreads, final Connection testDataConn, final Element performanceElement, final String testTournament) {
    _testDataConn = testDataConn;
    _performanceElement = performanceElement;
    _testTournament = testTournament;

    for (int i = 0; i < numThreads; ++i) {
      final Thread t = new Thread(new Runnable() {
        public void run() {
          final Thread thread = Thread.currentThread();
          for (;;) {
            Data data = null;
            synchronized (_queue) {
              while (_queue.isEmpty()) {
                try {
                  _queue.wait();
                } catch (final InterruptedException ie) {
                  LOG.warn("Got interrupted", ie);
                }
              }
              data = _queue.remove(0);
              if (data == FINISHED) {
                // requeue so all threads will finish
                _queue.add(data);
                _queue.notifyAll();
                return;
              }
            }

            final int teamNumber = data.getTeamNumber();
            final int runNumber = data.getRunNumber();

            // return data object for later use
            synchronized (_freeData) {
              _freeData.add(data);
            }

            enterPerformanceScore(teamNumber, runNumber);
            synchronized (_inProcess) {
              _inProcess.remove(thread);
              _inProcess.notifyAll();
            }
          }
        }
      });
      t.start();
    }
  }

  /**
   * Shutdown the queue.
   */
  public void shutdown() {
    synchronized (_queue) {
      _queue.add(FINISHED);
      _queue.notifyAll();
    }
  }

  public void queuePerformanceScore(final int runNumber, final int teamNumber) {
    synchronized (_queue) {
      final Data data = getFreeData();
      data.setRunNumber(runNumber);
      data.setTeamNumber(teamNumber);
      _queue.add(data);
      _queue.notifyAll();
    }
  }

  /**
   * Wait for the queue to empty and for everything to be processed.
   */
  public void waitForQueueToFinish() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("waitForQueueToFinish:Top");
    }
    // wait for the queue to empty
    synchronized (_queue) {
      while (!_queue.isEmpty()) {
        try {
          _queue.wait();
        } catch (final InterruptedException ie) {
          LOG.warn("Got interrupted", ie);
        }
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("waitForQueueToFinish:Queue is empty, waiting for processes: "
          + _inProcess.size());
    }
    // now wait for the processes to finish, this order works
    // only because we assume that the queue is filled first
    // and then the data is processed and the test waits
    // until everything is processed before adding anything
    // else to the queue
    synchronized (_inProcess) {
      while (!_inProcess.isEmpty()) {
        try {
          _inProcess.wait();
        } catch (final InterruptedException ie) {
          LOG.warn("Got interrupted", ie);
        }
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("waitForQueueToFinish:Bottom");
    }
  }

  private void enterPerformanceScore(final int teamNumber, final int runNumber) {
    try {
      synchronized (_inProcess) {
        _inProcess.add(Thread.currentThread());
      }

      FullTournamentTest.enterPerformanceScore(_testDataConn, _performanceElement, _testTournament, runNumber, teamNumber);
    } catch (final SQLException sqle) {
      LOG.error("Got exception", sqle);
      Assert.fail("Got exception: "
          + sqle.getMessage());
    } catch (final IOException ioe) {
      LOG.error("Got exception", ioe);
      Assert.fail("Got exception: "
          + ioe.getMessage());
    } catch (final SAXException se) {
      LOG.error("Got exception", se);
      Assert.fail("Got exception: "
          + se.getMessage());
    } catch (final ParseException pe) {
      LOG.error("Got exception", pe);
      Assert.fail("Got exception: "
          + pe.getMessage());
    }
  }

  /**
   * Get the next free data object, or create a new one if one isn't available.
   * 
   * @return
   */
  private Data getFreeData() {
    synchronized (_freeData) {
      if (!_freeData.isEmpty()) {
        return _freeData.remove(0);
      } else {
        return new Data();
      }
    }
  }

  private final Connection _testDataConn;

  private final Element _performanceElement;

  private final String _testTournament;

  private final List<Data> _queue = new LinkedList<Data>();

  private final List<Data> _freeData = new LinkedList<Data>();

  private final Set<Thread> _inProcess = new HashSet<Thread>();

  private static final Data FINISHED = new Data();

  /**
   * Which data to be submitted.
   */
  private static final class Data {
    private int _runNumber;

    public int getRunNumber() {
      return _runNumber;
    }

    public void setRunNumber(final int v) {
      _runNumber = v;
    }

    private int _teamNumber;

    public int getTeamNumber() {
      return _teamNumber;
    }

    public void setTeamNumber(final int v) {
      _teamNumber = v;
    }
  }

}
