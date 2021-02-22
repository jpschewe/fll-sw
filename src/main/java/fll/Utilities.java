/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.sql.DataSource;
import javax.swing.ImageIcon;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hsqldb.jdbc.JDBCDataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.ImportDB;
import fll.util.FLLRuntimeException;
import fll.xml.ScoreType;

/**
 * Some handy utilities.
 */
public final class Utilities {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final ThreadLocal<NumberFormat> FLOATING_POINT_NUMBER_FORMAT_INSTANCE = new ThreadLocal<NumberFormat>() {
    @Override
    protected NumberFormat initialValue() {
      final NumberFormat format = NumberFormat.getInstance();
      // setup the number format instance to be 2 decimal places
      format.setMaximumFractionDigits(2);
      format.setMinimumFractionDigits(2);
      return format;
    }
  };

  /**
   * @return Single instance of the floating point NumberFormat instance to save
   *         on
   *         overhead
   *         and to use for consistency of formatting.
   */
  public static NumberFormat getFloatingPointNumberFormat() {
    return FLOATING_POINT_NUMBER_FORMAT_INSTANCE.get();
  }

  private static final ThreadLocal<NumberFormat> XML_FLOATING_POINT_NUMBER_FORMAT_INSTANCE = new ThreadLocal<NumberFormat>() {
    @Override
    protected NumberFormat initialValue() {
      final NumberFormat format = NumberFormat.getInstance();
      // setup the number format instance to be 2 decimal places
      format.setMaximumFractionDigits(2);
      format.setMinimumFractionDigits(2);
      format.setGroupingUsed(false);
      return format;
    }
  };

  /**
   * @return Single instance of the floating point NumberFormat instance to save
   *         on
   *         overhead
   *         and to use for consistency of formatting. Compatible with XML
   *         floating point
   *         fields.
   */
  public static NumberFormat getXmlFloatingPointNumberFormat() {
    return XML_FLOATING_POINT_NUMBER_FORMAT_INSTANCE.get();
  }

  private static final ThreadLocal<NumberFormat> INTEGER_NUMBER_FORMAT_INSTANCE = new ThreadLocal<NumberFormat>() {
    @Override
    protected NumberFormat initialValue() {
      return NumberFormat.getIntegerInstance();
    }
  };

  /**
   * @return Single instance of the integer NumberFormat instance to save on
   *         overhead
   *         and to use for consistency of formatting.
   */
  public static NumberFormat getIntegerNumberFormat() {
    return INTEGER_NUMBER_FORMAT_INSTANCE.get();
  }

  /**
   * @param type the score type
   * @return the format object for the specified score type
   */
  public static NumberFormat getFormatForScoreType(final ScoreType type) {
    return type == ScoreType.FLOAT ? getFloatingPointNumberFormat() : getIntegerNumberFormat();
  }

  /**
   * Character set to use throughout the software. Currently set to UTF-8.
   */
  public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

  /**
   * Unicode character for a non-breaking space.
   */
  public static final char NON_BREAKING_SPACE = '\u00a0';

  private Utilities() {
  }

  /**
   * Load a CSV file into an SQL table. Assumes that the first line in the CSV
   * file specifies the column names. This method is meant as the inverse of
   * {@link com.opencsv.CSVWriter#writeAll(ResultSet, boolean)} with
   * includeColumnNames set to true. This method assumes that the table to be
   * created does not exist, an error will be reported if it does.
   *
   * @param connection the database connection to create the table within
   * @param tablename name of the table to create
   * @param reader where to read the data from, a {@link CSVReader} will be
   *          created from this
   * @param types column name to sql type mapping
   * @throws SQLException if there is an error putting data in the database
   * @throws IOException if there is an error reading the data
   * @throws RuntimeException if the first line cannot be read
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                                "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Generate columns based upon file loaded")
  public static void loadCSVFile(final Connection connection,
                                 final String tablename,
                                 final Map<String, String> types,
                                 final Reader reader)
      throws IOException, SQLException {
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

      final String[] columnTypes = new String[line.length];
      try (Statement stmt = connection.createStatement()) {
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
          // handle old dumps with no size
          if (type.equalsIgnoreCase("varchar")) {
            type = "varchar(255)";
          }
          // handle old dumps with no size
          if (type.equalsIgnoreCase("char")) {
            type = "char(255)";
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
      } // statement

      // load each line into a row in the table
      try (PreparedStatement prep = connection.prepareStatement(insertPrepSQL.append(valuesSQL).toString())) {
        while (null != (line = csvreader.readNext())) {
          for (int columnIndex = 0; columnIndex < line.length; ++columnIndex) {
            coerceData(line[columnIndex], columnTypes[columnIndex], prep, columnIndex
                + 1);
          }
          prep.executeUpdate();
        }
      } // prepared statement
    } catch (final CsvValidationException e) {
      throw new IOException("Error reading line of file", e);
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
  private static void coerceData(final @Nullable String data,
                                 final String type,
                                 final PreparedStatement prep,
                                 final int index)
      throws SQLException {
    final String typeLower = type.toLowerCase();
    if ("longvarchar".equals(typeLower)
        || typeLower.startsWith("varchar")) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.VARCHAR);
      } else {
        prep.setString(index, data);
      }
    } else if (typeLower.startsWith("char")) {
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
        final long value = Long.parseLong(data);
        prep.setLong(index, value);
      }
    } else if ("float".equals(typeLower)
        || "double".equals(typeLower)) {
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
    } else if ("date".equals(typeLower)) {
      if (null == data
          || "".equals(data.trim())) {
        prep.setNull(index, Types.DATE);
      } else {
        try {
          final java.util.Date value = ImportDB.CSV_DATE_FORMATTER.get().parse(data);
          prep.setDate(index, new java.sql.Date(value.getTime()));
        } catch (final ParseException e) {
          throw new FLLRuntimeException("Problem parsing date in database dump", e);
        }
      }
    } else {
      throw new FLLRuntimeException("Unhandled SQL data type '"
          + type
          + "'");
    }
  }

  /**
   * Extensions used by HSQL for it's database files. These extensions include
   * the dot.
   */
  public static final Collection<String> HSQL_DB_EXTENSIONS = Collections.unmodifiableCollection(Arrays.asList(new String[] { ".properties",
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
   * @throws SQLException on a database error
   */
  public static boolean testDatabaseInitialized(final Connection connection) throws SQLException {
    // get list of tables that already exist
    final DatabaseMetaData metadata = connection.getMetaData();
    try (ResultSet rs = metadata.getTables(null, null, "%", null)) {
      while (rs.next()) {
        if ("tournament_parameters".toLowerCase().equals(rs.getString(3).toLowerCase())) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Get the name of the database driver class.
   */
  private static Driver getDBDriver() {
    if (Boolean.getBoolean("inside.test")) {
      return new net.sf.log4jdbc.DriverSpy();
    } else {
      return new org.hsqldb.jdbcDriver();
    }
  }

  /**
   * Load the database driver and throw a RuntimeException if there is an error.
   */
  public static void loadDBDriver() {
    try {
      DriverManager.registerDriver(getDBDriver());
    } catch (final SQLException e) {
      throw new RuntimeException("Unable to register database driver", e);
    }
  }

  /**
   * Unload the database driver and throw a RuntimeException if there is an
   * error.
   */
  public static void unloadDBDriver() {
    try {
      DriverManager.deregisterDriver(getDBDriver());
    } catch (final SQLException e) {
      throw new RuntimeException("Unable to unload database driver", e);
    }
  }

  /**
   * Create a datasource for the specified database.
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
   * Create a datasource for the specified memory database.
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
    final JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setDatabase(myURL);
    dataSource.setUser("sa");

    return dataSource;
  }

  // TODO get datasource debugging back in at some point?
  // System.setProperty("log4jdbc.enabled", "true");
  // final DataSourceSpy debugDatasource = new DataSourceSpy(dataSource);
  // return debugDatasource;
  // if we want to debug we can execute "SET DATABASE SQL LOG LEVEL 3" and then
  // inspect flldb.sql.log

  /**
   * Filter used to select only graphics files
   */
  private static final FilenameFilter GRAPHICS_FILTER = (dir,
                                                         name) -> {
    final String lowerName = name.toLowerCase();
    return (lowerName.endsWith(".png")
        || lowerName.endsWith(".jpg")
        || lowerName.endsWith(".jpeg")
        || lowerName.endsWith(".gif"));
  };

  /**
   * Filter used to select only directories
   */
  private static final FileFilter DIRFILTER = f -> {
    return f.isDirectory();
  };

  /**
   * Find all graphic files in the specified directory.
   *
   * @param directory which directory to search in
   * @return paths to all files, sorted
   */
  public static List<String> getGraphicFiles(final File directory) {
    final List<String> logoFiles = new ArrayList<>();
    buildGraphicFileList("", logoFiles, directory);
    Collections.sort(logoFiles);
    return logoFiles;
  }

  /**
   * Build a list of file found in the specified directories. This
   * method will search recursively.
   *
   * @param prefix prefix path, this should be "" initially
   * @param directories directories to look in
   * @param output the list of image files (output parameter)
   */
  private static void buildGraphicFileList(final String prefix,
                                           final Collection<String> output,
                                           final File... directories) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("buildGraphicFileList("
          + prefix
          + ","
          + Arrays.toString(directories)
          + ","
          + output.toString()
          + ")");
    }
    for (final File element : directories) {
      final String np = (prefix.length() == 0 ? prefix
          : prefix
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
          output.add(np
              + "/"
              + file);
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
        buildGraphicFileList(np, output, dirs);
      } else {
        LOGGER.debug("dirs: null");
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("f: "
          + output.toString());
    }
  }

  /**
   * Determine the extension given a filename.
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

  /**
   * @param selectedFile the file to analyze
   * @return the name of the file without the extension (if there is one).
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
   * @param selectedFile the file to analyze
   * @return the absolute name of the file without the extension (if there is
   *         one).
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
                                    final String property)
      throws NumberFormatException, NullPointerException {
    final String value = properties.getProperty(property);
    if (null == value) {
      throw new NullPointerException("Property '"
          + property
          + "' doesn't have a value");
    }
    return Integer.parseInt(value.trim());
  }

  /**
   * Read an integer property and fail if the property doesn't parse
   * as a number.
   *
   * @param properties where to read the property from
   * @param property the property to read
   * @param defaultValue the value to use if the property doesn't exist
   * @return the value
   * @throws NumberFormatException if there is a parse error
   */
  public static int readIntProperty(final Properties properties,
                                    final String property,
                                    final int defaultValue)
      throws NumberFormatException {
    final String value = properties.getProperty(property);
    if (null == value) {
      return defaultValue;
    } else {
      return Integer.parseInt(value.trim());
    }
  }

  /**
   * Read a boolean property and fail if the property doesn't have a value.
   * "1" and "true" (case insensitive) are true, everything else is false.
   *
   * @param properties where to read the property from
   * @param property the property to read
   * @return the value
   * @throws NullPointerException if no value is found
   */
  public static boolean readBooleanProperty(final Properties properties,
                                            final String property)
      throws NullPointerException {
    final String value = properties.getProperty(property);
    if (null == value) {
      throw new NullPointerException("Property '"
          + property
          + "' doesn't have a value");
    } else {
      return "1".equals(value)
          || "true".equalsIgnoreCase(value);
    }
  }

  /**
   * Read a boolean property and fail if the property doesn't have a value.
   * "1" and "true" (case insensitive) are true, everything else is false.
   *
   * @param properties where to read the property from
   * @param property the property to read
   * @param defaultValue the value to use if the property is not found
   * @return the value
   */
  public static boolean readBooleanProperty(final Properties properties,
                                            final String property,
                                            final boolean defaultValue) {
    final String value = properties.getProperty(property);
    if (null == value) {
      return defaultValue;
    } else {
      return "1".equals(value)
          || "true".equalsIgnoreCase(value);
    }
  }

  /**
   * Check if a number is even.
   *
   * @param number the number to check
   * @return true if the number is even
   */
  public static boolean isEven(final int number) {
    return number
        % 2 == 0;
  }

  /**
   * Check if a number is odd.
   *
   * @param number the number to check
   * @return true if the number is odd
   */
  public static boolean isOdd(final int number) {
    return !isEven(number);
  }

  /**
   * Parse a string of the form [integer, integer, ...].
   *
   * @param str string to parse
   * @return array of integers
   * @throws FLLRuntimeException if the string cannot be parsed
   */
  public static int[] parseListOfIntegers(final @Nullable String str) {
    if (null == str
        || str.isEmpty()) {
      return new int[0];
    }

    final int lbracket = str.indexOf('[');
    if (-1 == lbracket) {
      throw new FLLRuntimeException("No '[' found in string: '"
          + str
          + "'");
    }
    final int rbracket = str.indexOf(']', lbracket);
    if (-1 == rbracket) {
      throw new FLLRuntimeException("No ']' found in string: '"
          + str
          + "'");
    }
    final String[] strings;
    if (lbracket
        + 1 == rbracket) {
      strings = new String[0];
    } else {
      strings = str.substring(lbracket
          + 1, rbracket).split(",");
    }

    final int[] values = new int[strings.length];
    for (int i = 0; i < strings.length; ++i) {
      values[i] = Integer.parseInt(strings[i].trim());
    }

    return values;
  }

  /**
   * Parse a string of the form [string, string, ...].
   *
   * @param str string to parse, handles null and empty
   * @return array of strings
   * @throws FLLRuntimeException if the string cannot be parsed
   */
  public static String[] parseListOfStrings(final @Nullable String str) {
    if (null == str
        || str.isEmpty()) {
      return new String[0];
    }

    final int lbracket = str.indexOf('[');
    if (-1 == lbracket) {
      throw new FLLRuntimeException("No '[' found in string: '"
          + str
          + "'");
    }
    final int rbracket = str.indexOf(']', lbracket);
    if (-1 == rbracket) {
      throw new FLLRuntimeException("No ']' found in string: '"
          + str
          + "'");
    }
    final String[] strings;
    if (lbracket
        + 1 == rbracket) {
      strings = new String[0];
    } else {
      strings = str.substring(lbracket
          + 1, rbracket).split(",");
    }

    for (int i = 0; i < strings.length; ++i) {
      strings[i] = strings[i].trim();
    }

    return strings;
  }

  /**
   * @param container the container to find component in
   * @param component the component to find
   * @return the index of component or -1 if not found
   */
  public static int getIndexOfComponent(final Container container,
                                        final Component component) {
    int index = -1;
    for (int i = 0; index < 0
        && i < container.getComponentCount(); ++i) {
      final Component c = container.getComponent(i);
      if (c == component) {
        index = i;
      }
    }
    return index;
  }

  /**
   * Create a standard JSON object mapper that understands our classes.
   * 
   * @return a new instance
   */
  public static ObjectMapper createJsonMapper() {
    return new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
  }

  /**
   * Check if two doubles are exactly equal.
   * 
   * @param one first value to compare
   * @param two second value to compare
   * @return if the two doubles are exactly equal
   * @see Double#doubleToLongBits(double)
   */
  public static boolean doubleExactEquals(final double one,
                                          final double two) {
    return Double.doubleToLongBits(one) == Double.doubleToLongBits(two);
  }

  /**
   * Get a stream from an iterator. From
   * https://www.geeksforgeeks.org/convert-an-iterator-to-stream-in-java/.
   * 
   * @param <T> the object type in the iterator
   * @param iterator the iterator
   * @return a stream over the iterator
   */
  public static <T> Stream<T> getStreamFromIterator(final Iterator<T> iterator) {

    // Convert the iterator to Spliterator
    final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);

    // Get a Sequential Stream from spliterator
    return StreamSupport.stream(spliterator, false);
  }

  /**
   * Used by JSON deserialization.
   */
  public static final class ListOfStringTypeInformation extends TypeReference<List<String>> {
    /** single instance. */
    public static final ListOfStringTypeInformation INSTANCE = new ListOfStringTypeInformation();
  }

  /**
   * Create an icon from the resource at path. This uses the
   * {@link Thread#getContextClassLoader()} if one exists. If not the classloader
   * of this class will be used to find the reference. If that is null, use the
   * system classloader.
   * 
   * @param path the path to load the icon from
   * @return the icon or null if the resource is not found
   */
  public static @Nullable ImageIcon getIcon(final String path) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (null == cl) {
      cl = Utilities.class.getClassLoader();
    }
    if (null == cl) {
      cl = ClassLoader.getSystemClassLoader();
    }

    final URL url = cl.getResource(path);
    if (null == url) {
      return null;
    } else {
      return new ImageIcon(url);
    }
  }

}
