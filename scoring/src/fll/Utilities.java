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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import net.mtu.eggplant.io.LogWriter;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.jdbc.jdbcDataSource;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import fll.db.DataSourceSpy;
import fll.util.LogUtils;

/**
 * Some handy utilities.
 */
public final class Utilities {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Single instance of the default NumberFormat instance to save on overhead
   * and to use for consistency of formatting.
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
   * {@link au.com.bytecode.opencsv.CSVWriter#writeAll(ResultSet, boolean)} with
   * includeColumnNames set to true. This method assumes that the table to be
   * created does not exist, an error will be reported if it does.
   * 
   * @param connection the database connection to create the table within
   * @param tablename name of the table to create
   * @param reader where to read the data from, a {@link CSVReader} will be
   *          created from this
   * @throws SQLException if there is an error putting data in the database
   * @throws IOException if there is an error reading the data
   * @throws RuntimeException if the first line cannot be read
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                                                             "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Generate columns based upon file loaded")
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
      for (final String columnName : line) {
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
      SQLFunctions.close(stmt);

      // load each line into a row in the table
      prep = connection.prepareStatement(insertPrepSQL.append(valuesSQL).toString());
      while (null != (line = csvreader.readNext())) {
        for (int i = 0; i < line.length; ++i) {
          prep.setString(i + 1, line[i]);
        }
        prep.executeUpdate();
      }

    } finally {
      SQLFunctions.close(stmt);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check to see if the database files can be written to. Will check each of
   * the files that HSQLDB uses for the database and ensure they're all readable
   * and writable. If there are any problems a {@link RuntimeException} will be
   * thrown.
   * 
   * @return true if the database is ok
   * @throws RuntimeException on any error
   */
  public static boolean testHSQLDB(final String baseFilename) {
    final File baseFile = new File(baseFilename);
    final File dir = baseFile.getParentFile();
    if (null == dir) {
      LOGGER.warn("There is no parent file for "
          + baseFile.getAbsolutePath());
      return false;
    }
    if (!dir.isDirectory()) {
      LOGGER.warn("Database directory "
          + dir.getAbsolutePath() + " is not a directory");
      return false;
    }
    if (!dir.canWrite()) {
      LOGGER.warn("Database directory "
          + dir.getAbsolutePath() + " is not writable");
      return false;
    }
    if (!dir.canRead()) {
      LOGGER.warn("Database directory "
          + dir.getAbsolutePath() + " is not readable");
      return false;
    }

    for (final String extension : HSQL_DB_EXTENSIONS) {
      final File file = new File(baseFilename
          + extension);
      if (file.exists()
          && !file.canWrite()) {
        LOGGER.warn("Database file "
            + file.getAbsolutePath() + " exists and is not writable");
        return false;
      }
      if (file.exists()
          && !file.canRead()) {
        LOGGER.warn("Database file "
            + file.getAbsolutePath() + " exists and is not readable");
        return false;
      }
    }

    return true;
  }

  /**
   * Extensions used by HSQL for it's database files. These extensions include
   * the dot.
   */
  public static final Collection<String> HSQL_DB_EXTENSIONS = Collections
                                                                         .unmodifiableCollection(Arrays
                                                                                                       .asList(new String[] {
                                                                                                                             ".properties",
                                                                                                                             ".script",
                                                                                                                             ".log",
                                                                                                                             ".data",
                                                                                                                             ".backup",
                                                                                                                             "", }));

  /**
   * Test that the database behind the connection is initialized. Checks for the
   * existance of the TournamentParameters tables.
   * 
   * @param connection the connection to check
   * @return true if the database is initialized
   */
  public static boolean testDatabaseInitialized(final Connection connection) throws SQLException {
    ResultSet rs = null;
    try {
      // get list of tables that already exist
      final DatabaseMetaData metadata = connection.getMetaData();
      rs = metadata.getTables(null, null, "%", null);
      while (rs.next()) {
        if ("tournament_parameters".toLowerCase().equals(rs.getString(3).toLowerCase())) {
          return true;
        }
      }
      return false;
    } finally {
      SQLFunctions.close(rs);
    }
  }

  /**
   * Get the name of the database driver class.
   */
  public static String getDBDriverName() {
    if (Boolean.getBoolean("inside.test")) {
      return "net.sf.log4jdbc.DriverSpy";
    } else {
      return "org.hsqldb.jdbcDriver";
    }
  }

  /**
   * Load the database driver and throw a RuntimeException if there is an error.
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

  private static Server _testDatabaseServer = null;

  private static final Object TEST_DATABASE_SERVER_LOCK = new Object();

  /**
   * Create a datasource for the specified database
   * 
   * @param database the database to connect to
   * @return a datasource
   */
  public static DataSource createDataSource(final String database) {
    final String myURL;
    myURL = "jdbc:hsqldb:file:"
        + database + ";shutdown=true";
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("myURL: "
          + myURL);
    }
    return createDataSource(database, myURL);
  }

  /**
   * Create a {@link DataSource} attached to the specified database with the
   * specified URL.
   * 
   * @param database the name of the database, for debugging
   * @param myURL the URL to the database
   * @return the DataSource
   */
  public static DataSource createDataSource(final String database,
                                            final String myURL) {
    final jdbcDataSource dataSource = new jdbcDataSource();
    dataSource.setDatabase(myURL);
    dataSource.setUser("sa");

    // startup test database server
    if (Boolean.getBoolean("inside.test")) {
      synchronized (TEST_DATABASE_SERVER_LOCK) {
        if (null == _testDatabaseServer) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Configuring test database server");
          }
          _testDatabaseServer = new Server();
          _testDatabaseServer.setPort(9042);
          _testDatabaseServer.setDatabasePath(0, database);
          _testDatabaseServer.setDatabaseName(0, "fll");
          _testDatabaseServer.setNoSystemExit(true);
          _testDatabaseServer.setErrWriter(new PrintWriter(new LogWriter(LoggerFactory.getLogger("database"),
                                                                         LogWriter.LogLevel.ERROR)));
          _testDatabaseServer.setLogWriter(new PrintWriter(new LogWriter(LoggerFactory.getLogger("database"),
                                                                         LogWriter.LogLevel.INFO)));
          _testDatabaseServer.setTrace(true);
        }
        if (1 != _testDatabaseServer.getState()) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting database server for testing");
          }
          _testDatabaseServer.start();
        }
      }
      
      System.setProperty("log4jdbc.enabled", "true");
      final DataSourceSpy debugDatasource = new DataSourceSpy(dataSource);      
      return debugDatasource;
    } else {
      return dataSource;
    }
  }

  /**
   * Filter used to select only graphics files
   */
  private static final FilenameFilter GRAPHICS_FILTER = new FilenameFilter() {
    public boolean accept(final File dir,
                          final String name) {
      final String lowerName = name.toLowerCase();
      if (lowerName.endsWith(".png")
          || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif")) {
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

  public static void buildGraphicFileList(final String p,
                                          final File[] d,
                                          final List<String> f) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("buildGraphicFileList("
          + p + "," + Arrays.toString(d) + "," + f.toString() + ")");
    }
    for (final File element : d) {
      final String np = (p.length() == 0 ? p : p
          + "/")
          + element.getName();
      final String[] files = element.list(GRAPHICS_FILTER);
      if (files != null) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("files: "
              + Arrays.toString(files));
        }
        java.util.Arrays.sort(files);
        for (final String file : files) {
          f.add(np
              + "/" + file);
        }
      } else {
        LOGGER.debug("files: null");
      }
      final File[] dirs = element.listFiles(DIRFILTER);
      if (dirs != null) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("dirs: "
              + Arrays.toString(dirs));
        }
        java.util.Arrays.sort(dirs);
        buildGraphicFileList(np, dirs, f);
      } else {
        LOGGER.debug("dirs: null");
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("f: "
          + f.toString());
    }
  }

  /**
   * Add a display name to the list of known displays. Synchronized to avoid
   * race conditions.
   * 
   * @param application
   * @param name
   */
  public static synchronized void appendDisplayName(final ServletContext application,
                                                    final String name) {
    // ServletContext isn't type safe
    @SuppressWarnings("unchecked")
    Set<String> displayNames = (Set<String>) application.getAttribute("displayNames");
    if (null == displayNames) {
      displayNames = new HashSet<String>();
    }
    displayNames.add(name);
    application.setAttribute("displayNames", displayNames);
  }

  /**
   * Determine the extension given a filename
   * 
   * @param filename the filename
   * @return the extension, or null if there isn't one
   */
  public static String determineExtension(final String filename) {
    final int dotIndex = filename.lastIndexOf('.');
    final String extension;
    if (-1 != dotIndex) {
      extension = filename.substring(dotIndex);
    } else {
      extension = null;
    }
    return extension;
  }
}
