/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.hsqldb.Server;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

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
  static {
    // setup the number format instance to be 2 decimal places
    NUMBER_FORMAT_INSTANCE.setMaximumFractionDigits(2);
    NUMBER_FORMAT_INSTANCE.setMinimumFractionDigits(2);
  }

  private Utilities() {
  }

  /**
   * Load a CSV file into an SQL table. Assumes that the first line in the CSV
   * file specifies the column names. This method is meant as the inverse of
   * {@link CSVWriter#writeAll(ResultSet, boolean)} with includeColumnNames set
   * to true. This method assumes that the table to be created does not exist,
   * an error will be reported if it does.
   * 
   * @param connection
   *          the database connection to create the table within
   * @param tablename
   *          name of the table to create
   * @param reader
   *          where to read the data from, a {@link CSVReader} will be created
   *          from this
   * @throws SQLException
   *           if there is an error putting data in the database
   * @throws IOException
   *           if there is an error reading the data
   * @throws RuntimeException
   *           if the first line cannot be read
   */
  public static void loadCSVFile(final Connection connection,
                                 final String tablename,
                                 final Reader reader) throws IOException, SQLException {
    Statement stmt = null;
    PreparedStatement prep = null;
    try {
      final CSVReader csvreader = new CSVReader(reader);

      // read the header and create the table and create the
      final StringBuilder insertPrepSQL = new StringBuilder();
      insertPrepSQL.append("INSERT INTO ");
      insertPrepSQL.append(tablename);
      insertPrepSQL.append(" ( ");
      final StringBuilder valuesSQL = new StringBuilder();
      valuesSQL.append(" VALUES (");

      final StringBuilder createTable = new StringBuilder();
      createTable.append("CREATE TABLE ");
      createTable.append(tablename);
      createTable.append(" (");
      String[] line = csvreader.readNext();
      if (null == line) {
        throw new RuntimeException("Cannot find the header line");
      }
      stmt = connection.createStatement();
      boolean first = true;
      for (String columnName : line) {
        if (first) {
          first = false;
        } else {
          createTable.append(", ");
          insertPrepSQL.append(", ");
          valuesSQL.append(", ");
        }

        createTable.append(columnName);
        createTable.append(" longvarchar");
        insertPrepSQL.append(columnName);
        valuesSQL.append("?");
      }
      createTable.append(")");
      insertPrepSQL.append(")");
      valuesSQL.append(")");
      stmt.executeUpdate(createTable.toString());
      Utilities.closeStatement(stmt);

      // load each line into a row in the table
      prep = connection.prepareStatement(insertPrepSQL.append(valuesSQL).toString());
      while (null != (line = csvreader.readNext())) {
        for (int i = 0; i < line.length; ++i) {
          prep.setString(i + 1, line[i]);
        }
        prep.executeUpdate();
      }

    } finally {
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(prep);
    }
  }

  /**
   * Check to see if the database files can be written to. Will check each of
   * the files that HSQLDB uses for the database and ensure they're all readable
   * and writable. If there are any problems an exception will be thrown.
   */
  public static void testHSQLDB(final String baseFilename) {
    final File baseFile = new File(baseFilename);
    final File dir = baseFile.getParentFile();
    if (!dir.isDirectory()) {
      throw new RuntimeException("Database directory " + dir.getAbsolutePath()
          + " is not a directory");
    }
    if (!dir.canWrite()) {
      throw new RuntimeException("Database directory " + dir.getAbsolutePath() + " is not writable");
    }
    if (!dir.canRead()) {
      throw new RuntimeException("Database directory " + dir.getAbsolutePath() + " is not readable");
    }

    final String[] extensions = new String[] { ".properties", ".script", ".log", ".data",
        ".backup", };
    for (String extension : extensions) {
      final File file = new File(baseFilename + extension);
      if (file.exists() && !file.canWrite()) {
        throw new RuntimeException("Database file " + file.getAbsolutePath()
            + " exists and is not writable");
      }
      if (file.exists() && !file.canRead()) {
        throw new RuntimeException("Database file " + file.getAbsolutePath()
            + " exists and is not readable");
      }
    }

  }

  /**
   * @return the URL to use to connect to the database.
   */
  public static String getDBURLString(final String database) {
    // do this when getting the URL so it gets called from the JSP's as well
    testHSQLDB(database);
    if (LOG.isDebugEnabled()) {
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
   * Load the database driver and throw a RuntimeException if there is an error.
   *
   */
  public static void loadDBDriver() {
    try {
      // register the driver
      Class.forName(Utilities.getDBDriverName()).newInstance();
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException("Unable to load driver.", e);
    } catch (final InstantiationException ie) {
      throw new RuntimeException("Unable to load driver.", ie);
    } catch (final IllegalAccessException iae) {
      throw new RuntimeException("Unable to load driver.", iae);
    }
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
   * Calls createDBConnection with fll as username and password.
   * 
   * @param database the database to use
   * @see #createDBConnection(String, String)
   */
  public static Connection createDBConnection(final String database) throws RuntimeException {
    return createDBConnection("fll", "fll", database);
  }
  
  /**
   * Calls createDBConnection with fll as the database
   * 
   * @see #createDBConnection(String, String, String)
   * @throws RuntimeException
   *           on an error
   */
  public static Connection createDBConnection(final String username, final String password)
      throws RuntimeException {
    return createDBConnection(username, password, "fll");
  }

  /**
   * Creates a database connection.
   * 
   * @param username
   *          username to use
   * @param password
   *          password to use
   * @param database
   *          name of the database to connect to
   * @throws RuntimeException
   *           on an error
   */
  public static Connection createDBConnection(final String username,
                                              final String password,
                                              final String database) throws RuntimeException {
    // create connection to database and puke if anything goes wrong
    loadDBDriver();

    Connection connection = null;
    final String myURL = "jdbc:hsqldb:file:" + database + ";shutdown=true";
    if (LOG.isDebugEnabled()) {
      LOG.debug("myURL: " + myURL);
    }
    try {
      connection = DriverManager.getConnection(myURL);
    } catch (final SQLException sqle) {
      throw new RuntimeException("Unable to create connection: " + sqle.getMessage()
          + " database: " + database + " user: " + username);
    }

    if (Boolean.getBoolean("inside.test")) {
      if (!_testServerStarted) {
        if (LOG.isInfoEnabled()) {
          LOG.info("Starting database server for testing");
        }
        // TODO This still isn't working quite right when run from inside
        // Eclipse, when run from the commandline forcing the parameter it's
        // fine
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
        // final Thread dbThread = new Thread(new Runnable() {
        // public void run() {
        // org.hsqldb.Server.main(new String[] {
        // "-port", "9042",
        // "-database.0", database,
        // "-dbname.0", "fll",
        // "-no_system_exit", "true",
        // });
        // }});
        // dbThread.setDaemon(true);
        // dbThread.start();
        _testServerStarted = true;
      }
    }

    return connection;
  }

  /**
   * Ensure that we only start the test database server once
   */
  private static boolean _testServerStarted = false;

  public static boolean isTestServerStarted() {
    return _testServerStarted;
  }

  /**
   * Close stmt and ignore SQLExceptions. This is useful in a finally so that
   * all of the finally block gets executed. Handles null.
   */
  public static void closeStatement(final Statement stmt) {
    try {
      if (null != stmt) {
        stmt.close();
      }
    } catch (final SQLException sqle) {
      // ignore
      LOG.debug(sqle);
    }
  }

  /**
   * Close prep and ignore SQLExceptions. This is useful in a finally so that
   * all of the finally block gets executed. Handles null.
   */
  public static void closePreparedStatement(final PreparedStatement prep) {
    try {
      if (null != prep) {
        prep.close();
      }
    } catch (final SQLException sqle) {
      // ignore
      LOG.debug(sqle);
    }
  }

  /**
   * Close rs and ignore SQLExceptions. This is useful in a finally so that all
   * of the finally block gets executed. Handles null.
   */
  public static void closeResultSet(final ResultSet rs) {
    try {
      if (null != rs) {
        rs.close();
      }
    } catch (final SQLException sqle) {
      // ignore
      LOG.debug(sqle);
    }
  }

  /**
   * Close connection and ignore SQLExceptions. This is useful in a finally so
   * that all of the finally block gets executed. Handles null.
   */
  public static void closeConnection(final Connection connection) {
    try {
      if (null != connection) {
        connection.close();
      }
    } catch (final SQLException sqle) {
      // ignore
      LOG.debug(sqle);
    }
  }

  /**
   * Equals call that handles null without a NullPointerException.
   */
  public static boolean safeEquals(final Object o1, final Object o2) {
    if (o1 == o2) {
      return true;
    } else if (null == o1 && null != o2) {
      return false;
    } else {
      return o1.equals(o2);
    }
  }

  /**
   * Filter used to select only graphics files
   */
  private static final FilenameFilter GRAPHICS_FILTER = new FilenameFilter() {
    public boolean accept(final File dir, final String name) {
      if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
        return true;
      } else {
        return false;
      }
    }
  };

  /**
   * Filter used to select only directories
   */
  private static final FileFilter DIRFILTER = new FileFilter() {
    public boolean accept(final File f) {
      if (f.isDirectory()) {
        return true;
      } else {
        return false;
      }
    }
  };

  public static void buildGraphicFileList(final String p, final File[] d, final List<String> f) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("buildGraphicFileList(" + p + "," + Arrays.toString(d) + "," + f.toString() + ")");
    }
    for (int i = 0; i < d.length; i++) {
      String np = (p.length() == 0 ? p : p + "/") + d[i].getName();
      String[] files = d[i].list(GRAPHICS_FILTER);
      if (files != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("files: " + Arrays.toString(files));
        }
        java.util.Arrays.sort(files);
        for (int j = 0; j < files.length; j++) {
          f.add(np + "/" + files[j]);
        }
      } else {
        LOG.debug("files: null");
      }
      File[] dirs = d[i].listFiles(DIRFILTER);
      if (dirs != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("dirs: " + Arrays.toString(dirs));
        }
        java.util.Arrays.sort(dirs);
        buildGraphicFileList(np, dirs, f);
      } else {
        LOG.debug("dirs: null");
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("f: " + f.toString());
    }
  }
}
