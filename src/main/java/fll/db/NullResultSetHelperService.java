package fll.db;

import java.io.IOException;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;

import org.apache.commons.text.TextStringBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.opencsv.ResultSetHelper;

/**
 * Helper class for processing JDBC ResultSet objects.
 * This class uses {@link #getNullString()} as the value for NULL and prepends
 * this
 * to all string values that are not null yet evaluate to this so that one can
 * properly read the file back into the database.
 * <p>
 * Based heavily upon com.opencsv.ResultSetHelperService
 * </p>
 */
public class NullResultSetHelperService implements ResultSetHelper {

  private static final String DEFAULT_DATE_FORMAT = "dd-MMM-yyyy";

  private static final String DEFAULT_TIMESTAMP_FORMAT = "dd-MMM-yyyy HH:mm:ss";

  private String dateFormat = DEFAULT_DATE_FORMAT;

  private String dateTimeFormat = DEFAULT_TIMESTAMP_FORMAT;

  private @Nullable NumberFormat integerFormat = null;

  private @Nullable NumberFormat floatingPointFormat = null;

  private final String nullString;

  public final String getNullString() {
    return this.nullString;
  }

  /**
   * @param nullString the string to use to represent null values, cannot be null
   */
  public NullResultSetHelperService(final String nullString) {
    this.nullString = nullString;
  }

  /**
   * Set a default date format pattern that will be used by the service.
   *
   * @param dateFormat Desired date format
   */
  public void setDateFormat(final String dateFormat) {
    this.dateFormat = dateFormat;
  }

  /**
   * Set a default date time format pattern that will be used by the service.
   *
   * @param dateTimeFormat Desired date time format
   */
  public void setDateTimeFormat(final String dateTimeFormat) {
    this.dateTimeFormat = dateTimeFormat;
  }

  /**
   * Set a default number formatter for floating point numbers that will be used
   * by the service.
   *
   * @param format Desired number format. Should not be null
   */
  public void setIntegerFormat(final NumberFormat format) {
    this.integerFormat = format;
  }

  /**
   * Set a default number formatter for integer numbers that will be used by the
   * service.
   *
   * @param format Desired number format. Should not be null
   */
  public void setFloatingPointFormat(final NumberFormat format) {
    this.floatingPointFormat = format;
  }

  @Override
  public String[] getColumnNames(final ResultSet rs) throws SQLException {
    final ResultSetMetaData metadata = rs.getMetaData();
    final String[] nameArray = new String[metadata.getColumnCount()];
    for (int i = 0; i < metadata.getColumnCount(); i++) {
      nameArray[i] = metadata.getColumnLabel(i
          + 1);
    }
    return nameArray;
  }

  @Override
  public String[] getColumnValues(final ResultSet rs) throws SQLException, IOException {
    return this.getColumnValues(rs, false, dateFormat, dateTimeFormat);
  }

  @Override
  public String[] getColumnValues(final ResultSet rs,
                                  final boolean trim)
      throws SQLException, IOException {
    return this.getColumnValues(rs, trim, dateFormat, dateTimeFormat);
  }

  @Override
  public String[] getColumnValues(final ResultSet rs,
                                  final boolean trim,
                                  final String dateFormatString,
                                  final String timeFormatString)
      throws SQLException, IOException {
    final ResultSetMetaData metadata = rs.getMetaData();
    final String[] valueArray = new String[metadata.getColumnCount()];
    for (int i = 1; i <= metadata.getColumnCount(); i++) {
      valueArray[i
          - 1] = getColumnValue(rs, metadata.getColumnType(i), i, trim, dateFormatString, timeFormatString);
    }
    return valueArray;
  }

  /**
   * The formatted timestamp.
   * 
   * @param timestamp Timestamp read from resultset
   * @param timestampFormatString Format string
   * @return Formatted time stamp.
   */
  protected @Nullable String handleTimestamp(final @Nullable Timestamp timestamp,
                                             final String timestampFormatString) {
    SimpleDateFormat timeFormat = new SimpleDateFormat(timestampFormatString);
    return timestamp == null ? null : timeFormat.format(timestamp);
  }

  private String getColumnValue(final ResultSet rs,
                                final int colType,
                                final int colIndex,
                                final boolean trim,
                                final String dateFormatString,
                                final String timestampFormatString)
      throws SQLException, IOException {

    @Nullable
    String value;

    switch (colType) {
    case Types.BOOLEAN:
      value = Objects.toString(rs.getBoolean(colIndex));
      break;
    case Types.NCLOB:
      value = handleNClob(rs, colIndex);
      break;
    case Types.CLOB:
      value = handleClob(rs, colIndex);
      break;
    case Types.BIGINT:
      value = applyFormatter(integerFormat, rs.getBigDecimal(colIndex));
      break;
    case Types.DECIMAL:
    case Types.REAL:
    case Types.NUMERIC:
      value = applyFormatter(floatingPointFormat, rs.getBigDecimal(colIndex));
      break;
    case Types.DOUBLE:
      value = applyFormatter(floatingPointFormat, rs.getDouble(colIndex));
      break;
    case Types.FLOAT:
      value = applyFormatter(floatingPointFormat, rs.getFloat(colIndex));
      break;
    case Types.INTEGER:
    case Types.TINYINT:
    case Types.SMALLINT:
      value = applyFormatter(integerFormat, rs.getInt(colIndex));
      break;
    case Types.DATE:
      value = handleDate(rs, colIndex, dateFormatString);
      break;
    case Types.TIME:
      value = Objects.toString(rs.getTime(colIndex), null);
      break;
    case Types.TIMESTAMP:
      value = handleTimestamp(rs.getTimestamp(colIndex), timestampFormatString);
      break;
    case Types.NVARCHAR:
    case Types.NCHAR:
    case Types.LONGNVARCHAR:
      value = handleNVarChar(rs, colIndex, trim);
      break;
    case Types.LONGVARCHAR:
    case Types.VARCHAR:
    case Types.CHAR:
      value = handleVarChar(rs, colIndex, trim);
      break;
    default:
      // This takes care of Types.BIT, Types.JAVA_OBJECT, and anything
      // unknown.
      value = Objects.toString(rs.getObject(colIndex), null);
    }

    if (rs.wasNull()
        || value == null) {
      value = getNullString();
    } else if (value.equals(getNullString())) {
      // prepend the null string as an escape
      value = getNullString()
          + value;
    }

    return value;
  }

  private @Nullable String applyFormatter(final @Nullable NumberFormat formatter,
                                          final @Nullable Number value) {
    if (value != null
        && formatter != null) {
      return formatter.format(value);
    }
    return Objects.toString(value, null);
  }

  /**
   * retrieves the data from an VarChar in a result set.
   *
   * @param rs - result set
   * @param colIndex - column location of the data in the result set
   * @param trim - should the value be trimmed before being returned
   * @return a string representing the VarChar from the result set
   * @throws SQLException
   */
  protected @Nullable String handleVarChar(final ResultSet rs,
                                           final int colIndex,
                                           final boolean trim)
      throws SQLException {
    String value;
    @Nullable
    String columnValue = rs.getString(colIndex);
    if (trim
        && columnValue != null) {
      value = columnValue.trim();
    } else {
      value = columnValue;
    }
    return value;
  }

  /**
   * retrieves the data from an NVarChar in a result set.
   *
   * @param rs - result set
   * @param colIndex - column location of the data in the result set
   * @param trim - should the value be trimmed before being returned
   * @return a string representing the NVarChar from the result set
   * @throws SQLException
   */
  protected @Nullable String handleNVarChar(final ResultSet rs,
                                            final int colIndex,
                                            final boolean trim)
      throws SQLException {
    String value;
    String nColumnValue = rs.getNString(colIndex);
    if (trim
        && nColumnValue != null) {
      value = nColumnValue.trim();
    } else {
      value = nColumnValue;
    }
    return value;
  }

  /**
   * retrieves an date from a result set.
   *
   * @param rs - result set
   * @param colIndex - column location of the data in the result set
   * @param dateFormatString - desired format of the date
   * @return - a string representing the data from the result set in the format
   *         set in dateFomratString.
   * @throws SQLException
   */
  protected @Nullable String handleDate(final ResultSet rs,
                                        final int colIndex,
                                        final String dateFormatString)
      throws SQLException {
    final Date date = rs.getDate(colIndex);
    if (date != null) {
      SimpleDateFormat df = new SimpleDateFormat(dateFormatString);
      return df.format(date);
    } else {
      return null;
    }
  }

  /**
   * retrieves the data out of a CLOB.
   *
   * @param rs - result set
   * @param colIndex - column location of the data in the result set
   * @return the data in the Clob as a string.
   * @throws SQLException
   * @throws IOException
   */
  protected @Nullable String handleClob(final ResultSet rs,
                                        final int colIndex)
      throws SQLException, IOException {
    final Clob c = rs.getClob(colIndex);
    if (c != null) {
      final TextStringBuilder sb = new TextStringBuilder();
      sb.readFrom(c.getCharacterStream());
      return sb.toString();
    } else {
      return null;
    }
  }

  /**
   * retrieves the data out of a NCLOB.
   *
   * @param rs - result set
   * @param colIndex - column location of the data in the result set
   * @return the data in the NCLOB as a string.
   * @throws SQLException
   * @throws IOException
   */
  protected @Nullable String handleNClob(final ResultSet rs,
                                         final int colIndex)
      throws SQLException, IOException {
    final NClob nc = rs.getNClob(colIndex);
    if (nc != null) {
      final TextStringBuilder sb = new TextStringBuilder();
      sb.readFrom(nc.getCharacterStream());
      return sb.toString();
    } else {
      return null;
    }
  }
}
