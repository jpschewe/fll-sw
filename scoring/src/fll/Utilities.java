/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.text.NumberFormat;

/**
 * Some handy utilties.
 * 
 * @version $Revision$
 */
public class Utilities {

  /**
   * Single instance of the default NumberFormat instance to save on overhead
   */
  public static final NumberFormat NUMBER_FORMAT_INSTANCE = NumberFormat.getInstance();
  
  private Utilities() {
  }

  /**
   * Calls createDBConnection with fll as username and password
   *
   * @see #createDBConnection(String, String, String)
   */
  public static Connection createDBConnection(final String hostname) throws RuntimeException {
    return createDBConnection(hostname, "fll", "fll");
  }
  
  /**
   * Calls createDBConnection with fll as the database
   *
   * @see #createDBConnection(String, String, String, String)
   * @throws RuntimeException on an error
   */
  public static Connection createDBConnection(final String hostname,
                                              final String username,
                                              final String password)
    throws RuntimeException {
    return createDBConnection(hostname, username, password, "fll");
  }
  
  /**
   * Creates a database connection.
   *
   * @param hostname name of machine to connect to
   * @param username username to use
   * @param password password to use
   * @param database name of the database to connect to
   * @throws RuntimeException on an error
   */
  public static Connection createDBConnection(final String hostname,
                                              final String username,
                                              final String password,
                                              final String database)
    throws RuntimeException {
    //create connection to database and puke if anything goes wrong
    //register the driver
    try{
      Class.forName("org.gjt.mm.mysql.Driver").newInstance();
    } catch(final ClassNotFoundException e){
      throw new RuntimeException("Unable to load driver.");
    } catch(final InstantiationException ie) {
      throw new RuntimeException("Unable to load driver.");
    } catch(final IllegalAccessException iae) {
      throw new RuntimeException("Unable to load driver.");
    }

    Connection connection = null;
    final String myURL = "jdbc:mysql://" + hostname + "/" + database + "?user=" + username + "&password=" + password + "&autoReconnect=true";
    try {
      connection = DriverManager.getConnection(myURL);
    } catch(final SQLException sqle) {
      throw new RuntimeException("Unable to create connection: " + sqle.getMessage());
    }
    return connection;
  }

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
    }
  }
  
}
