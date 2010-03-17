/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


import org.apache.log4j.Logger;


/**
 * Some utilities for writing tests.
 */
public final class TestUtils {

  private static final Logger LOG = Logger.getLogger(TestUtils.class);

  /**
   * Root URL for the software with trailing slash.
   */
  public static final String URL_ROOT = "http://localhost:9080/fll-sw/";

  private TestUtils() {
    // no instances
  }

  /**
   * Creates a database connection to the test database started on localhost
   * inside tomcat.
   * 
   * @param username username to use
   * @param password password to use
   * @throws RuntimeException on an error
   */
  public static Connection createTestDBConnection() throws RuntimeException {
    // create connection to database and puke if anything goes wrong
    // register the driver
    try {
      Class.forName("org.hsqldb.jdbcDriver").newInstance();
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException("Unable to load driver.", e);
    } catch (final InstantiationException ie) {
      throw new RuntimeException("Unable to load driver.", ie);
    } catch (final IllegalAccessException iae) {
      throw new RuntimeException("Unable to load driver.", iae);
    }

    Connection connection = null;
    final String myURL = "jdbc:hsqldb:hsql://localhost:9042/fll";
    if (LOG.isDebugEnabled()) {
      LOG.debug("created test database connection myURL: "
          + myURL);
    }
    try {
      connection = DriverManager.getConnection(myURL);
    } catch (final SQLException sqle) {
      throw new RuntimeException("Unable to create connection: "
          + sqle.getMessage() + " URL: " + myURL);
    }

    return connection;
  }

  /**
   * Delete all files that would be associated with the specified database. 
   */
  public static void deleteDatabase(final String database) {
    for(final String extension : Utilities.HSQL_DB_EXTENSIONS) {
      final String filename = database + extension;
      final File file = new File(filename);
      if(file.exists()) {
        if(!file.delete()) {
          file.deleteOnExit();
        }
      }
    }
  }
}
