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
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hsqldb.jdbc.JDBCDataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fll.util.FLLRuntimeException;
import fll.xml.ScoreType;

/**
 * Some handy utilities.
 */
public final class Utilities {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final FloatingPointNumberFormat FLOATING_POINT_NUMBER_FORMAT_INSTANCE = new FloatingPointNumberFormat();

  @SuppressWarnings("nullness:type.argument") // initialValue returns non-null
  private static final class FloatingPointNumberFormat extends ThreadLocal<NumberFormat> {
    @Override
    protected NumberFormat initialValue() {
      final NumberFormat format = NumberFormat.getInstance();
      // setup the number format instance to be 2 decimal places
      format.setMaximumFractionDigits(2);
      format.setMinimumFractionDigits(2);
      return format;
    }
  }

  /**
   * Single instance of the floating point NumberFormat instance to save
   * on overhead and to use for consistency of formatting.
   * Has 2 decimal digits.
   * 
   * @return single instance
   */
  public static NumberFormat getFloatingPointNumberFormat() {
    return FLOATING_POINT_NUMBER_FORMAT_INSTANCE.get();
  }

  private static final XmlFloatingPointNumberFormat XML_FLOATING_POINT_NUMBER_FORMAT_INSTANCE = new XmlFloatingPointNumberFormat();

  @SuppressWarnings("nullness:type.argument") // initialValue returns non-null
  private static final class XmlFloatingPointNumberFormat extends ThreadLocal<NumberFormat> {
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

  private static final IntegerNumberFormat INTEGER_NUMBER_FORMAT_INSTANCE = new IntegerNumberFormat();

  @SuppressWarnings("nullness:type.argument") // initialValue returns non-null
  private static final class IntegerNumberFormat extends ThreadLocal<NumberFormat> {
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
    final String desiredParam = "tournament_parameters".toLowerCase();
    final DatabaseMetaData metadata = connection.getMetaData();
    try (ResultSet rs = metadata.getTables(null, null, "%", null)) {
      while (rs.next()) {
        final String value = rs.getString(3);
        if (null != value
            && desiredParam.equals(value.toLowerCase())) {
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
  public static @Nullable String determineExtension(final String filename) {
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
    return new ObjectMapper() //
                             .registerModule(new Jdk8Module()) //
                             .registerModule(new JavaTimeModule()) //
                             .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //
    ;
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

  /**
   * Get a non-null class loader. This starts with
   * {@link Thread#getContextClassLoader()} and then checks
   * {@link Class#getClassLoader()} for this class and if that is null returns
   * {@link ClassLoader#getSystemClassLoader()}.
   * 
   * @return non-null class loader
   */
  public static ClassLoader getClassLoader() {
    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    if (null != contextClassLoader) {
      return contextClassLoader;
    }

    final ClassLoader classClassLoader = Utilities.getClassLoader();
    if (null != classClassLoader) {
      return classClassLoader;
    }

    return ClassLoader.getSystemClassLoader();
  }

  /**
   * Convert the current row of {@code rs} to a map for later use.
   * 
   * @param rs the {@link ResultSet} to read, must have next already called to be
   *          at the row to convert
   * @return a new map of the data. The keys are the column names. The map keys
   *         are case insensitve so that the map behaves like an SQL database.
   * @throws SQLException on a database error
   */
  public static Map<String, @Nullable Object> resultSetRowToMap(final ResultSet rs) throws SQLException {
    final ResultSetMetaData md = rs.getMetaData();
    final int columns = md.getColumnCount();
    final Map<String, @Nullable Object> row = new CaseInsensitiveMap<>();
    for (int i = 1; i <= columns; i++) {
      row.put(md.getColumnName(i), rs.getObject(i));
    }
    return row;
  }

  /**
   * @param value the string
   * @return if the value is null, the empty string otherwise the value
   */
  public static String stringValueOrEmpty(final @Nullable String value) {
    if (null == value) {
      return "";
    } else {
      return value;
    }
  }

}
