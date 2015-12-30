/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import net.mtu.eggplant.util.Pair;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Java code used in tables.jsp
 * 
 * @version $Revision$
 */
public final class Tables {

  private Tables() {

  }

  /**
   * Generate the tables page
   */
  public static void generatePage(final JspWriter out,
                                  final ServletContext application,
                                  final HttpSession session,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) throws SQLException, IOException, ParseException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final int tournament = Queries.getCurrentTournament(connection);
      final String submitButton = request.getParameter("submit");

      int rowIndex = 0;
      while (null != request.getParameter("SideA"
          + (rowIndex
              + 1))) {
        ++rowIndex;
        out.println("<!-- found a row "
            + rowIndex + "-->");
      }
      if ("Add Row".equals(submitButton)) {
        out.println("<!-- adding another row to "
            + rowIndex + "-->");
        ++rowIndex;
      }
      out.println("<!-- final count of rows is "
          + rowIndex + "-->");
      final int numRows = rowIndex
          + 1;

      out.println("<form action='tables.jsp' method='POST' name='tables'>");

      String errorString = null;
      if ("Finished".equals(submitButton)) {
        errorString = commitData(request, response, session, connection, Queries.getCurrentTournament(connection));
      }

      if (null == submitButton
          || "Add Row".equals(submitButton) || null != errorString) {
        if (null != errorString) {
          out.println("<p id='error'><font color='red'>"
              + errorString + "</font></p>");
        }

        out.println("<p>Table labels should be unique. These labels must occur in pairs, where a label refers to a single side of a table. E.g. If the skirt of a table was red on one side and green on the other, the labels could be Red and Green, but if the table was red all around they could be Red1 and Red2.</p>");

        out.println("<table border='1'><tr><th>Side A</th><th>Side B</th><th>Delete?</th></tr>");

        int row = 0; // keep track of which row we're generating

        if (null == request.getParameter("SideA0")) {
          // this is the first time the page has been visited so we need to read
          // the table labels out of the DB
          ResultSet rs = null;
          PreparedStatement stmt = null;
          try {
            stmt = connection.prepareStatement("SELECT SideA,SideB FROM tablenames WHERE Tournament = ? ORDER BY PairID");
            stmt.setInt(1, tournament);
            rs = stmt.executeQuery();
            for (row = 0; rs.next(); row++) {
              final String sideA = rs.getString(1);
              final String sideB = rs.getString(2);
              generateRow(out, row, sideA, sideB, "");
            }
          } finally {
            SQLFunctions.close(rs);
            SQLFunctions.close(stmt);
          }
        } else {
          // need to walk the parameters to see what we've been passed
          String sideA = request.getParameter("SideA"
              + row);
          String sideB = request.getParameter("SideB"
              + row);
          String delete = request.getParameter("delete"
              + row);
          while (null != sideA) {
            generateRow(out, row, sideA, sideB, delete);

            row++;
            sideA = request.getParameter("SideA"
                + row);
            sideB = request.getParameter("SideB"
                + row);
            delete = request.getParameter("delete"
                + row);
          }
        }

        final int tableRows = Math.max(numRows, row);
        for (; row < tableRows; row++) {
          generateRow(out, row, null, null, null);
        }

        out.println("</table>");
        out.println("<input type='submit' name='submit' id='add_row' value='Add Row'>");
        out.println("<input type='submit' name='submit' id='finished' value='Finished'>");
      }

      out.println("</form>");
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Generate a row in the judges table defaulting the form elemenets to the
   * given information.
   * 
   * @param out where to print element in the list
   * @param row the row being generated, used for naming form elements
   * @param sideA Label for side A of the table, can be null
   * @param sideB Label for side B of the table, can be null
   * @param delete Either "checked" or null, depending on whether the check box
   *          should initially be checked or not.
   */
  private static void generateRow(final JspWriter out,
                                  final int row,
                                  final String sideA,
                                  final String sideB,
                                  final String delete) throws IOException {
    out.println("<tr>");
    out.print("  <td><input type='text' name='SideA"
        + row + "'");
    if (null != sideA) {
      out.print(" value='"
          + sideA + "'");
    }
    out.println("></td>");
    out.print("  <td><input type='text' name='SideB"
        + row + "'");
    if (null != sideA) {
      out.print(" value='"
          + sideB + "'");
    }
    out.println("></td>");

    out.println("  <td><input type='checkbox' value='checked' name='delete"
        + row + "' " + (null == delete ? "" : delete) + ">");

    out.println("  </td>");

    out.println("</tr>");
  }

  /**
   * Replace the tables for a tournament.
   * 
   * @param connection the database connection
   * @param tournamentId the tournament id
   * @param tables the new tables, each pair is side 1 and side 2 of the table
   * @throws SQLException
   */
  public static void replaceTablesForTournament(final Connection connection,
                                                final int tournamentId,
                                                final List<Pair<String, String>> tables) throws SQLException {
    PreparedStatement deleteNames = null;
    PreparedStatement deleteInfo = null;
    PreparedStatement insert = null;
    try {
      // delete old data in tablenames
      deleteNames = connection.prepareStatement("DELETE FROM tablenames where Tournament = ?");
      deleteNames.setInt(1, tournamentId);
      deleteNames.executeUpdate();

      deleteInfo = connection.prepareStatement("DELETE FROM table_division where Tournament = ?");
      deleteInfo.setInt(1, tournamentId);
      deleteInfo.executeUpdate();

      insert = connection.prepareStatement("INSERT INTO tablenames (Tournament, PairID, SideA, SideB) VALUES(?, ?, ?, ?)");
      insert.setInt(1, tournamentId);
      int pairId = 0;
      for (final Pair<String, String> tableInfo : tables) {
        insert.setInt(2, pairId);
        insert.setString(3, tableInfo.getOne());
        insert.setString(4, tableInfo.getTwo());
        insert.executeUpdate();

        ++pairId;
      }

    } finally {
      SQLFunctions.close(deleteInfo);
      SQLFunctions.close(deleteNames);
      SQLFunctions.close(insert);
    }

  }

  /**
   * Commit the subjective data from request to the database and redirect
   * response back to index.jsp.
   * 
   * @param tournament the current tournament
   */
  private static String commitData(final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   final HttpSession session,
                                   final Connection connection,
                                   final int tournament) throws SQLException, IOException {
    final List<Pair<String, String>> tables = new LinkedList<>();

    int row = 0;
    String sideA = request.getParameter("SideA"
        + row);
    String sideB = request.getParameter("SideB"
        + row);
    String delete = request.getParameter("delete"
        + row);
    while (null != sideA) {
      if (null == delete
          || delete.equals("")) {
        // Don't put blank entries in the database
        if (!(sideA.trim().equals("")
            && sideB.trim().equals(""))) {
          tables.add(new Pair<>(sideA, sideB));
        }
      }

      row++;
      sideA = request.getParameter("SideA"
          + row);
      sideB = request.getParameter("SideB"
          + row);
      delete = request.getParameter("delete"
          + row);
    }

    replaceTablesForTournament(connection, tournament, tables);

    // finally redirect to index.jsp
    session.setAttribute(SessionAttributes.MESSAGE, "<p id='success'><i>Successfully assigned tables</i></p>");
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
    return null;
  }

  /**
   * Check if tables are assigned for the specified tournament.
   * 
   * @param connection database connection
   * @param tournamentID tournament ID
   * @return true if the tables have been assigned
   * @throws SQLException
   */
  static public boolean tablesAssigned(final Connection connection,
                                       final int tournamentID) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      boolean tablesAssigned = false;

      prep = connection.prepareStatement("SELECT COUNT(*) FROM tablenames WHERE Tournament = ?");
      prep.setInt(1, tournamentID);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int count = rs.getInt(1);
        tablesAssigned = count > 0;
      }
      return tablesAssigned;

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

}
