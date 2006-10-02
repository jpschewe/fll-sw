/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Some utilities for writing tests. 
 *
 * @version $Revision$
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
   * Load test data into a database from the specified stream.
   *
   * @param connection the connection to the database
   * @param scriptStream a stream of SQL statements to load data from.
   * @throws IOException if there is a problem loading the data
   * @throws SQLException if there is an error in the script 
   */
  public static void loadSQLScript(final Connection connection,
                                   final InputStream scriptStream)
    throws IOException, SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      // read in the script file up to a semicolon and execute it
      final LineNumberReader reader = new LineNumberReader(new InputStreamReader(scriptStream));
      String line;
      final StringBuilder buffer = new StringBuilder();
      while(null != (line = reader.readLine())) {
        if(!line.startsWith("--")) {
          buffer.append(line);
          int semi;
          while( (semi = buffer.indexOf(";")) > 0) {
            final String scriptStmt = buffer.substring(0, semi);
            if(LOG.isDebugEnabled()) {
              LOG.debug("Line " + reader.getLineNumber() + ": " + scriptStmt);
            }
            stmt.executeUpdate(scriptStmt);
            buffer.delete(0, semi+1);
          }
        }
      }
      if(buffer.length() > 0) {
        LOG.warn("Extra data left in buffer, ignoring: " + buffer.toString());
      }
      reader.close();
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Creates a database connection to the test database started on localhost
   * inside tomcat.
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
