/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Utilities
 *
 * @version $Revision$
 */
public class WebTestUtils {
  
  private static final Logger LOG = Logger.getLogger(WebTestUtils.class);

  /**
   * Root URL for the software with trailing slash.
   */
  public static final String URL_ROOT = "http://localhost:9080/fll-sw/";
  
  private WebTestUtils() {
    // no instances
  }

  /**
   * Creates a database connection to the test database started on localhost.
   *
   * @param username username to use
   * @param password password to use
   * @throws RuntimeException on an error
   */
  public static Connection createDBConnection(final String username,
                                              final String password)
    throws RuntimeException {
    //create connection to database and puke if anything goes wrong
    //register the driver
    try{
      Class.forName("org.hsqldb.jdbcDriver").newInstance();
    } catch(final ClassNotFoundException e){
      throw new RuntimeException("Unable to load driver.", e);
    } catch(final InstantiationException ie) {
      throw new RuntimeException("Unable to load driver.", ie);
    } catch(final IllegalAccessException iae) {
      throw new RuntimeException("Unable to load driver.", iae);
    }

    Connection connection = null;
    final String myURL = "jdbc:hsqldb:hsql://localhost/fll";
    LOG.debug("myURL: " + myURL);
    try {
      connection = DriverManager.getConnection(myURL);
    } catch(final SQLException sqle) {
      throw new RuntimeException("Unable to create connection: " + sqle.getMessage()
                                 + " URL: " + myURL
                                 + " user: " + username);
    }
    
    return connection;
  }

  
}
