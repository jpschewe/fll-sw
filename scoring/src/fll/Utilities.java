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
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.hsqldb.jdbc.jdbcDataSource;

import au.com.bytecode.opencsv.CSVReader;
import fll.db.ImportDB;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

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

  /**
   * Character set to use throughout the software. Currently set to UTF-8.
   */
  public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

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
                                 final Map<String, String> types,
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
      final String[] columnTypes = new String[line.length];
      for (int columnIndex = 0; columnIndex < line.length; ++columnIndex) {
        final String columnName = line[columnIndex].toLowerCase();
        if (columnIndex > 0) {
          createTable.append(", ");
          insertPrepSQL.append(", ");
          valuesSQL.append(", ");
        }
        String type = types.get(columnName);
        if (null == type) {
          type = "longvarchar";
        }
        if (type.equalsIgnoreCase("varchar")) {
          type = "varchar(255)";
        }
        columnTypes[columnIndex] = type;
        createTable.append(columnName);
        createTable.append(" "
            + type);
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
        for (int columnIndex = 0; columnIndex < line.length; ++columnIndex) {
          coerceData(line[columnIndex], columnTypes[columnIndex], prep, columnIndex + 1);
        }
        prep.executeUpdate();
      }

    } finally {
      SQLFunctions.close(stmt);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Convert data to type and put in prepared statement at index.
   * 
   * @param data the data as a string
   * @param type the sql type that the data is to be converted to
   * @param prep the prepared statement to insert into
   * @param index which index in the prepared statement to put the data in
   * @throws SQLException
   * @throws ParseException
   */
  private static void coerceData(final String data,
                                 final String type,
                                 final PreparedStatement prep,
                                 final int index) throws SQLException {
    final String typeLower = type.toLowerCase();
    if ("longvarchar".equals(typeLower)
        || typeLower.startsWith("varchar")) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.VARCHAR);
      } else {
        prep.setString(index, data);
      }
    } else if ("char".equals(typeLower)) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.CHAR);
      } else {
        prep.setString(index, data);
      }
    } else if ("integer".equals(typeLower)) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.INTEGER);
      } else {
        final long value = Long.valueOf(data);
        prep.setLong(index, value);
      }
    } else if ("float".equals(typeLower)) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.DOUBLE);
      } else {
        final double value = Double.valueOf(data);
        prep.setDouble(index, value);
      }
    } else if ("boolean".equals(typeLower)) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.BOOLEAN);
      } else {
        final boolean value = Boolean.valueOf(data);
        prep.setBoolean(index, value);
      }
    } else if ("time".equals(typeLower)) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.TIME);
      } else {
        try {
          final Date value = ImportDB.CSV_TIME_FORMATTER.get().parse(data);
          final Time time = new Time(value.getTime());
          prep.setTime(index, time);
        } catch (final ParseException e) {
          throw new FLLRuntimeException("Problem parsing time in database dump", e);
        }
      }
    } else if ("timestamp".equals(typeLower)) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.TIMESTAMP);
      } else {
        try {
          final Date value = ImportDB.CSV_TIMESTAMP_FORMATTER.get().parse(data);
          final Timestamp time = new Timestamp(value.getTime());
          prep.setTimestamp(index, time);
        } catch (final ParseException e) {
          throw new FLLRuntimeException("Problem parsing timestamp in database dump", e);
        }
      }
    } else {
      throw new FLLRuntimeException("Unhandled SQL data type '"
          + type + "'");
    }
  }

  /**
   * Extensions used by HSQL for it's database files. These extensions include
   * the dot.
   */
  public static final Collection<String> HSQL_DB_EXTENSIONS = Collections.unmodifiableCollection(Arrays.asList(new String[] {
                                                                                                                             ".properties",
                                                                                                                             ".script",
                                                                                                                             ".log",
                                                                                                                             ".data",
                                                                                                                             ".backup",
                                                                                                                             "", }));

  /**
   * Test that the database behind the connection is initialized. Checks for the
   * existence of the TournamentParameters tables.
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

  /**
   * Create a datasource for the specified database
   * 
   * @param database the database to connect to, assumed to be a filename
   * @return a datasource
   */
  public static DataSource createFileDataSource(final String database) {
    final String myURL;
    myURL = "jdbc:hsqldb:file:"
        + database;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("myURL: "
          + myURL);
    }
    return createDataSourceFromURL(myURL);
  }

  /**
   * Create a datasource for the specified memory database
   * 
   * @param database the database to connect to, assumed to be a filename
   * @return a datasource
   */
  public static DataSource createMemoryDataSource(final String database) {
    final String myURL;
    myURL = "jdbc:hsqldb:mem:"
        + database;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("myURL: "
          + myURL);
    }
    return createDataSourceFromURL(myURL);
  }

  /**
   * Create a {@link DataSource} attached database with the specified URL.
   * 
   * @param myURL the URL to the database
   * @return the DataSource
   */
  private static DataSource createDataSourceFromURL(final String myURL) {
    final jdbcDataSource dataSource = new jdbcDataSource();
    dataSource.setDatabase(myURL);
    dataSource.setUser("sa");

    return dataSource;
  }
  
  //TODO get datasource debugging back in at some point?
  // System.setProperty("log4jdbc.enabled", "true");
  // final DataSourceSpy debugDatasource = new DataSourceSpy(dataSource);
  // return debugDatasource;

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
                                          final Collection<String> f) {
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
   * Application attribute to hold names of all displays. Type is Set<String>
   */
  public static final String DISPLAY_NAMES_KEY = "displayNames";

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
    Set<String> displayNames = ApplicationAttributes.getAttribute(application, DISPLAY_NAMES_KEY, Set.class);
    if (null == displayNames) {
      displayNames = new HashSet<String>();
    }
    displayNames.add(name);
    application.setAttribute(DISPLAY_NAMES_KEY, displayNames);
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

  public static final int MINUTES_PER_HOUR = 60;

  /**
   * Convert hours to minutes.
   */
  public static int convertHoursToMinutes(final int hours) {
    return hours
        * MINUTES_PER_HOUR;
  }

  public static final long SECONDS_PER_MINUTE = 60;

  public static final long MILLISECONDS_PER_SECOND = 1000;

  public static long convertMinutesToSeconds(final long minutes) {
    return minutes
        * SECONDS_PER_MINUTE;
  }

  public static long convertMinutesToMilliseconds(final long minutes) {
    return convertMinutesToSeconds(minutes)
        * MILLISECONDS_PER_SECOND;
  }

  /**
   * Get the name of the file without the extension (if there is one).
   */
  public static String extractBasename(final File selectedFile) {
    final String name;
    final String fullname = selectedFile.getName();
    final int dotIndex = fullname.lastIndexOf('.');
    if (-1 != dotIndex) {
      name = fullname.substring(0, dotIndex);
    } else {
      name = fullname;
    }
    return name;
  }

  /**
   * Get the absolute name of the file without the extension (if there is one).
   */
  public static String extractAbsoluteBasename(final File selectedFile) {
    final String name;
    final String fullname = selectedFile.getAbsolutePath();
    final int dotIndex = fullname.lastIndexOf('.');
    if (-1 != dotIndex) {
      name = fullname.substring(0, dotIndex);
    } else {
      name = fullname;
    }
    return name;
  }

  /**
   * Read an integer property and fail if the property doesn't have a value or
   * doesn't parse a a number.
   * 
   * @param properties where to read the property from
   * @param property the property to read
   * @return the value
   * @throws NumberFormatException if there is a parse error
   * @throws NullPointerException if no value is found
   */
  public static int readIntProperty(final Properties properties,
                                    final String property) throws NumberFormatException, NullPointerException {
    final String value = properties.getProperty(property);
    if (null == value) {
      throw new NullPointerException("Property '"
          + property + "' doesn't have a value");
    }
    return Integer.valueOf(value.trim());
  }

  /**
   * If the string is longer than len, truncate it and append "..." TODO move to
   * JonsInfra
   * 
   * @param name the string
   * @param len the max length for the string
   * @return the string never longer than len characters
   */
  public static String trimString(final String name,
                                  final int len) {
    if (len <= 3) {
      throw new IllegalArgumentException("Length must be longer than 3 otherwise all strings are just '...'");
    }
    if (null == name) {
      return null;
    } else if (name.length() > len) {
      return name.substring(0, len - 3)
          + "...";
    } else {
      return name;
    }
  }
}
