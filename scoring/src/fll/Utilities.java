/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;

import org.apache.log4j.Logger;
import org.hsqldb.Server;

/**
 * Some handy utilties.
 * 
 * @version $Revision$
 */
public final class Utilities {

  private static final Logger LOG = Logger.getLogger(Utilities.class);
  
  /**
   * Single instance of the default NumberFormat instance to save on overhead
   */
  public static final NumberFormat NUMBER_FORMAT_INSTANCE = NumberFormat.getInstance();
  
  private Utilities() {
  }

  /**
   * Check to see if the database files can be written to.  Will check each of
   * the files that HSQLDB uses for the database and ensure they're all
   * readable and writable.  If there are any problems an exception will be
   * thrown.
   */
  public static void testHSQLDB(final String baseFilename) {
    final File baseFile = new File(baseFilename);
    final File dir = baseFile.getParentFile();
    if(!dir.isDirectory()) {
      throw new RuntimeException("Database directory " + dir.getAbsolutePath() + " is not a directory");
    }
    if(!dir.canWrite()) {
      throw new RuntimeException("Database directory " + dir.getAbsolutePath() + " is not writable");
    }
    if(!dir.canRead()) {
      throw new RuntimeException("Database directory " + dir.getAbsolutePath() + " is not readable");
    }      
    
    final String[] extensions = new String[] {
      ".properties",
      ".script",
      ".log",
      ".data",
      ".backup",
    };
    for(String extension : extensions) {
      final File file = new File(baseFilename + extension);
      if(file.exists() && !file.canWrite()) {
        throw new RuntimeException("Database file " + file.getAbsolutePath() + " exists and is not writable");
      }
      if(file.exists() && !file.canRead()) {
        throw new RuntimeException("Database file " + file.getAbsolutePath() + " exists and is not readable");
      }
    }
    
  }

  /**
   * @return the URL to use to connect to the database.
   */
  public static String getDBURLString(final String database) {
    // do this when getting the URL so it gets called from the JSP's as well
    testHSQLDB(database);
    if(LOG.isDebugEnabled()) {
      LOG.debug("URL: jdbc:hsqldb:file:" + database + ";shutdown=true");
    }
    return "jdbc:hsqldb:file:" + database + ";shutdown=true";
  }

  /**
   * Get the name of the database driver class.
   */
  public static String getDBDriverName() {
    return "org.hsqldb.jdbcDriver";
  }
  
  /**
   * Calls createDBConnection with fll as username and password
   *
   * @see #createDBConnection(String, String)
   */
  public static Connection createDBConnection() throws RuntimeException {
    return createDBConnection("fll", "fll");
  }
  
  /**
   * Calls createDBConnection with fll as the database
   *
   * @see #createDBConnection(String, String, String)
   * @throws RuntimeException on an error
   */
  public static Connection createDBConnection(final String username,
                                              final String password)
    throws RuntimeException {
    return createDBConnection(username, password, "fll");
  }
  
  /**
   * Creates a database connection.
   *
   * @param username username to use
   * @param password password to use
   * @param database name of the database to connect to
   * @throws RuntimeException on an error
   */
  public static Connection createDBConnection(final String username,
                                              final String password,
                                              final String database)
    throws RuntimeException {
    // create connection to database and puke if anything goes wrong

    try{
      // register the driver
      Class.forName(getDBDriverName()).newInstance();
    } catch(final ClassNotFoundException e){
      throw new RuntimeException("Unable to load driver.", e);
    } catch(final InstantiationException ie) {
      throw new RuntimeException("Unable to load driver.", ie);
    } catch(final IllegalAccessException iae) {
      throw new RuntimeException("Unable to load driver.", iae);
    }

    Connection connection = null;
    final String myURL = "jdbc:hsqldb:file:" + database + ";shutdown=true";
    if(LOG.isDebugEnabled()) {
      LOG.debug("myURL: " + myURL);
    }
    try {
      connection = DriverManager.getConnection(myURL);
    } catch(final SQLException sqle) {
      throw new RuntimeException("Unable to create connection: " + sqle.getMessage()
                                 + " database: " + database
                                 + " user: " + username);
    }

    if(Boolean.getBoolean("inside.test")) {
      if(!_testServerStarted) {
        if(LOG.isInfoEnabled()) {
          LOG.info("Starting database server for testing");
        }
        // TODO This still isn't working quite right when run from inside Eclipse, when run from the commandline forcing the parameter it's fine
        final Server server = new Server();
        server.setPort(9042);
        server.setDatabasePath(0, database);
        server.setDatabaseName(0, "fll");
        server.setNoSystemExit(true);
        final PrintWriter output = new PrintWriter(System.out);
        server.setErrWriter(output);
        server.setLogWriter(output);
        server.setTrace(true);
        server.start();
//        final Thread dbThread = new Thread(new Runnable() {
//          public void run() {
//            org.hsqldb.Server.main(new String[] {
//                "-port", "9042",
//                "-database.0", database,
//                "-dbname.0", "fll",
//                "-no_system_exit", "true",
//            });
//          }});
//        dbThread.setDaemon(true);
//        dbThread.start();
        _testServerStarted = true;
      }
    }
    
    return connection;
  }
  /**
   * Ensure that we only start the test database server once
   */
  private static boolean _testServerStarted = false;
  public static boolean isTestServerStarted() { return _testServerStarted; } 

  /**
   * Close stmt and ignore SQLExceptions.  This is useful in a finally so that
   * all of the finally block gets executed.  Handles null.
   */
  public static void closeStatement(final Statement stmt) {
    try {
      if(null != stmt) {
        stmt.close();
      }
    } catch(final SQLException sqle) {
      //ignore
      LOG.debug(sqle);
    }
  }

  /**
   * Close prep and ignore SQLExceptions.  This is useful in a finally so that
   * all of the finally block gets executed.  Handles null.
   */
  public static void closePreparedStatement(final PreparedStatement prep) {
    try {
      if(null != prep) {
        prep.close();
      }
    } catch(final SQLException sqle) {
      //ignore
      LOG.debug(sqle);
    }
  }
  
  /**
   * Close rs and ignore SQLExceptions.  This is useful in a finally so that
   * all of the finally block gets executed.  Handles null.
   */
  public static void closeResultSet(final ResultSet rs) {
    try {
      if(null != rs) {
        rs.close();
      }
    } catch(final SQLException sqle) {
      //ignore
      LOG.debug(sqle);
    }
  }

  /**
   * Close connection and ignore SQLExceptions.  This is useful in a finally
   * so that all of the finally block gets executed.  Handles null.
   */
  public static void closeConnection(final Connection connection) {
    try {
      if(null != connection) {
        connection.close();
      }
    } catch(final SQLException sqle) {
      //ignore
      LOG.debug(sqle);
    }
  }

  /**
   * Equals call that handles null without a NullPointerException.
   */
  public static boolean safeEquals(final Object o1,
                                   final Object o2) {
    if(o1 == o2) {
      return true;
    } else if(null == o1 && null != o2) {
      return false;
    } else {
      return o1.equals(o2);
    }
  }
}
