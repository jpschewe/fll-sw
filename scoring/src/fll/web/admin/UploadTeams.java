/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import fll.Utilities;

import fll.web.debug.DebugHttpSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;


/**
 * Java code for uploading team data to the database.  Called from
 * filterTeams.jsp and columnSelection.jsp.
 *
 * @version $Revision$
 */
final public class UploadTeams {

  /**
   * For debugging only
   */
  public static void main(final String[] args) {
    try {
      final File file = new File("/home/jpschewe/projects/fll/code/2002/results/teams-20021023.csv");
      final Connection connection = Utilities.createDBConnection("disk", "fll_admin", "fll_admin");
      parseFile(file, connection, new DebugHttpSession());
//       final String line = "team #	State	Region	Div #	School/Organization	Team Name	Girls	Boys	Minority	Num Medals	Paid	Coach	E-mail	Phone #";
//       System.out.println(splitLine(line));
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  private UploadTeams() {
     
  }

  /**
   * Take file and parse it as tab delimited.
   *
   * @param file the file that the teams are in
   * @param connection the database connection to connect to
   * @param session used to store the list of columns in a format suitable for
   * use in selection form elements.  The attribute name is
   * columnSelectOptions.
   * @throws SQLException if an error occurs talking to the database
   * @throws IOException if an error occurs reading from file 
   */
  public static void parseFile(final File file,
                               final Connection connection,
                               final HttpSession session) throws SQLException, IOException {
    //parse file into table AllTeams
    final BufferedReader reader = new BufferedReader(new FileReader(file));

    //stores <option value='columnName'>columnName</option> for each column
    final StringBuffer selectOptions = new StringBuffer();

    //parse out the first line as the names of the columns
    final List columnNames = splitLine(reader.readLine());

    //build the SQL for inserting a row into the temporary table 
    final StringBuffer insertPrepSQL = new StringBuffer();
    insertPrepSQL.append("INSERT INTO AllTeams VALUES(");

    //build the SQL for creating the temporary tables
    final StringBuffer createTable = new StringBuffer();
    createTable.append("CREATE TABLE AllTeams (");
    final StringBuffer createFilteredTable = new StringBuffer();
    createFilteredTable.append("CREATE TABLE FilteredTeams (");

    //iterate over each column name and append to appropriate buffers
    boolean first = true;
    final List columnNamesSeen = new LinkedList();
    final Iterator headerIter = columnNames.iterator();
    while(headerIter.hasNext()) {
      final String header = (String)headerIter.next();
      final String columnName = sanitizeColumnName(header);
      //System.out.println("header: " + header
      //                   + " columnName: " + columnName);
      if(columnNamesSeen.contains(columnName)) {
        throw new RuntimeException("Duplicate column name found: " + columnName);
      } else {
        columnNamesSeen.add(columnName);
      }

      if(first) {
        first = false;
      } else {
        createTable.append(", ");
        createFilteredTable.append(", ");
        insertPrepSQL.append(", ");
      }
      createTable.append(columnName + " TEXT");
      createFilteredTable.append(columnName + " TEXT");
      insertPrepSQL.append("?");
      selectOptions.append("<option value='" + columnName + "'>" + columnName + "</option>");
    }
    createTable.append(")");
    createFilteredTable.append(")");
    insertPrepSQL.append(")");

    PreparedStatement insertPrep = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate("DROP TABLE IF EXISTS AllTeams"); //make sure the table doesn't  yet exist
      //System.out.println("creating table: " + createTable.toString());
      stmt.executeUpdate(createTable.toString()); //create AllTeams
      
      stmt.executeUpdate("DROP TABLE IF EXISTS FilteredTeams"); //make sure the table doesn't  yet exist
      stmt.executeUpdate(createFilteredTable.toString()); //create FilteredTeams

      insertPrep = connection.prepareStatement(insertPrepSQL.toString());

      //loop over the rest of the rows and insert them into the temporary table
      String line = reader.readLine();
      for(int lineCounter=0; null != line; line = reader.readLine(), lineCounter++) {
        final Iterator valueIter = splitLine(line).iterator();
        for(int column=1; valueIter.hasNext(); column++) {
          final String value = (String)valueIter.next();
          insertPrep.setString(column, value);
        }
        insertPrep.executeUpdate();
      }
    } finally {
      Utilities.closePreparedStatement(insertPrep);
      Utilities.closeStatement(stmt);
    }

    //save this for other pages to use
    session.setAttribute("columnSelectOptions", selectOptions.toString());
  }

  /**
   * Splits the line on tabs.  Empty columns are stored as null.  Other
   * columns are all trimmed.
   *
   * @return List of columns in line as Strings
   */
  private static List splitLine(final String line) {
    final List retval = new ArrayList();
    final StringBuffer field = new StringBuffer();
    for(int position=0; position < line.length(); position++) {
      if(line.charAt(position) ==  '\t') {
        if(0 == field.length()) {
          retval.add(null);
        } else {
          retval.add(field.toString().trim());
          //only reset size if something is there
          field.setLength(0);
        }
      } else {
        field.append(line.charAt(position));
      }
    }
    //add the last column
    if(field.length() > 0) {
      retval.add(field.toString().trim());
    }
    return retval;
  }

  /**
   * Apply the set of filters currently stored in session.
   *
   * @param connection the database connection
   * @param request the request where the filters are stored
   * @return the number of rows that match
   * @throws SQLException if an error occurs talking to the database
   */
  public static long applyFilters(final Connection connection,
                                  final HttpServletRequest request) throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      
      final StringBuffer whereClause = new StringBuffer();
      int filterCount = 0;
      String filterColumn = request.getParameter("filterColumn" + filterCount);
      String filterText = request.getParameter("filterText" + filterCount);
      String filterDelete = request.getParameter("filterDelete" + filterCount);
      while(null != filterColumn) {
        if(!"".equals(filterText) && !"1".equals(filterDelete)) {
          if(whereClause.length() > 0) {
            whereClause.append(" AND ");
          }
          whereClause.append(filterColumn + " LIKE '" + filterText.trim() + "'");
        }
        
        filterCount++;
        filterColumn = request.getParameter("filterColumn" + filterCount);
        filterText = request.getParameter("filterText" + filterCount);
        filterDelete = request.getParameter("filterDelete" + filterCount);
      }

      String sql = "SELECT COUNT(*) FROM AllTeams";
      if(whereClause.length() > 0) {
        sql += " WHERE " + whereClause.toString();
      }
      rs = stmt.executeQuery(sql);
      if(rs.next()) {
        return rs.getLong(1);
      } else {
        throw new RuntimeException("Internal error, can't get count");
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Copy teams out of AllTeams into FilteredTeams applying the filters in
   * request
   */
  public static void copyFilteredTeams(final Connection connection,
                                       final HttpServletRequest request) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      
      final StringBuffer whereClause = new StringBuffer();
      int filterCount = 0;
      String filterColumn = request.getParameter("filterColumn" + filterCount);
      String filterText = request.getParameter("filterText" + filterCount);
      String filterDelete = request.getParameter("filterDelete" + filterCount);
      while(null != filterColumn) {
        if(!"".equals(filterText) && !"1".equals(filterDelete)) {
          if(whereClause.length() > 0) {
            whereClause.append(" AND ");
          }
          whereClause.append(filterColumn + " LIKE '" + filterText.trim() + "'");
        }
        
        filterCount++;
        filterColumn = request.getParameter("filterColumn" + filterCount);
        filterText = request.getParameter("filterText" + filterCount);
        filterDelete = request.getParameter("filterDelete" + filterCount);
      }

      //first clean out FilteredTeams
      stmt.executeUpdate("DELETE FROM FilteredTeams");

      //do the copy
      String sql = "INSERT INTO FilteredTeams SELECT * FROM AllTeams";
      if(whereClause.length() > 0) {
        sql += " WHERE " + whereClause.toString();
      }
      stmt.executeUpdate(sql);
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Verify the team information.  Used with verifyTeams.jsp
   * <ul>
   *   <li>Make sure that the column for TeamNumber is specified</li>
   *   <li>Make sure that the TeamNumbers are unique - just use count</li>
   *   <li>Generate the regions table off of the Regions column?</li>
   *   <li>Delete all rows from the Teams table</li>
   *   <li>Copy the teams into the Teams table</li>
   * </ul>
   *
   * @param connection connection with admin priviliges to DB
   * @param request used to get the information about which columns are mapped
   * to which
   * @param response used to redirect pages for errors
   * @param session used to store error messages
   * @param out used to output information/messages to user user about the
   * verification
   * @return if successful
   * @throws SQLException on error talking to DB
   * @throws IOException on error writing to webpage 
   */
  public static boolean verifyTeams(final Connection connection,
                                    final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final HttpSession session,
                                    final JspWriter out)
    throws SQLException, IOException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();

      rs = stmt.executeQuery("SELECT COUNT(*) FROM FilteredTeams");
      rs.next();
      final int numRowsInFiltered = rs.getInt(1);
      rs.close();
      
      final StringBuffer dbColumns = new StringBuffer();
      final StringBuffer dataColumns = new StringBuffer();

      final String teamNumberColumn = request.getParameter("TeamNumber");
      if(null == teamNumberColumn || "".equals(teamNumberColumn)) {
        //Error, redirect back to teamColumnSelection.jsp with error message
        session.setAttribute("errorMessage", "You must specify a column for TeamNumber");
        response.sendRedirect(response.encodeRedirectURL("teamColumnSelection.jsp"));
        return false;
      }
      
      //always have TeamNumber
      dbColumns.append("TeamNumber");
      dataColumns.append(teamNumberColumn);
      
      final Enumeration paramIter = request.getParameterNames();
      while(paramIter.hasMoreElements()) {
        final String parameter = (String)paramIter.nextElement();
        if(null != parameter
           && !"".equals(parameter)
           && !"TeamNumber".equals(parameter)) {
          final String value = request.getParameter(parameter);
          if(null != value && !"".equals(value)) {
            dbColumns.append(", " + parameter);
            dataColumns.append(", " + value);
          }
        }
      }

      //clean out Teams table first
      stmt.executeUpdate("DELETE FROM Teams");
      final String copySQL = "INSERT INTO Teams (" + dbColumns.toString() + ") SELECT " + dataColumns.toString() + " FROM FilteredTeams";
      final int numRowsInserted = stmt.executeUpdate(copySQL);
      
      if(numRowsInserted != numRowsInFiltered) {
        //Error
        out.println("<font color='red'>Error, row counts do not match.<br>");
        out.println("Rows after applying filters: " + numRowsInFiltered + "<br>");
        out.println("Rows in the teams table after inserting data: " + numRowsInserted + "<br>");
        out.println("This probably means that the column you specified as the TeamNumber column is not unique.<br>");
        out.println("Use the back button in your browser to correct this.<br>");
        out.println("Or go back and check your input file for errors.<br></font>");
        return false;
      }

      return true;
    } finally {
      Utilities.closeStatement(stmt);
      Utilities.closeResultSet(rs);
    }
  }

  /**
   * Create a string that's a valid column name.
   *
   * <ul>
   *   <li>Replace '#' with '_'</li>
   *   <li>Replace ' ' with '_'</li>
   *   <li>Replace '/' with '_'</li>
   *   <li>Replace '-' with '_'</li>
   *   <li>Replace null or empty string with EMPTYHEADER1 where number
   *   increments each time found</li>
   *   <li>Replace '?' with '_'</li>
   *   <li>Replace the string 'constraint' with 'CONSTRAINT_'</li>
   * </ul>
   */
  private static String sanitizeColumnName(final String str) {
    if(null == str || "".equals(str)) {
      return "EMPTYHEADER" + EMPTY_HEADER_COUNT++;
    } else if("constraint".equalsIgnoreCase(str)) {
      return "CONSTRAINT_";
    } else {
      String ret = str;
      final Matcher illegalCharMatcher = ILLEGAL_CHAR_PATTERN.matcher(ret);
      ret = illegalCharMatcher.replaceAll("_");
      
      return ret;
    }
  }

  private static int EMPTY_HEADER_COUNT = 0;
  private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[ #?/-]");
  
}
