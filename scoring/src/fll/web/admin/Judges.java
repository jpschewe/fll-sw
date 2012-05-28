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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.JudgeInformation;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.xml.XMLUtils;

/**
 * Java code used in judges.jsp
 */
public final class Judges {

  private Judges() {

  }

  /**
   * Generate the judges page
   */
  public static void generatePage(final JspWriter out,
                                  final ServletContext application,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) throws SQLException, IOException, ParseException {
    final HttpSession session = request.getSession();
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
    final DataSource datasource = SessionAttributes.getDataSource(session);
    final Connection connection = datasource.getConnection();
    final int tournament = Queries.getCurrentTournament(connection);

    final List<Element> subjectiveCategories = new NodelistElementCollectionAdapter(
                                                                                    challengeDocument.getDocumentElement()
                                                                                                     .getElementsByTagName("subjectiveCategory")).asList();

    // count the number of rows present
    int rowIndex = 0;
    while (null != request.getParameter("cat"
        + (rowIndex + 1))) {
      ++rowIndex;
      out.println("<!-- found a row "
          + rowIndex + "-->");
    }

    final int numRows = rowIndex + 1;

    String errorString = null;
    if (null != request.getParameter("finished")) {
      errorString = generateVerifyTable(out, subjectiveCategories, request, challengeDocument);
      if (null == errorString) {
        // everything is good
        return;
      }
    }

    if (null != errorString) {
      out.println("<p id='error' class='error'>"
          + errorString + "</p>");
    }

    // get list of divisions and add "All" as a possible value
    final List<String> divisions = Queries.getEventDivisions(connection);
    divisions.add(0, GatherJudgeInformation.ALL_DIVISIONS);

    out.println("<p>Judges ID's must be unique.  They can be just the name of the judge.  Keep in mind that this ID needs to be entered on the judging forms.  There must be at least 1 judge for each category.</p>");

    //FIXME get this into the JSP
    if (checkForEnteredSubjectiveScores(connection, subjectiveCategories, tournament)) {
      out.println("<p class='error'>Subjective scores have already been entered for this tournament, changing the judges may cause some scores to be deleted</p>");
    }
    out.println("<table border='1' id='data'><tr><th>ID</th><th>Category</th><th>Division</th></tr>");

    int row = 0; // keep track of which row we're generating

    if (null == request.getParameter("cat0")) {
      // this is the first time the page has been visited so we need to read
      // the judges out of the DB
      ResultSet rs = null;
      PreparedStatement stmt = null;
      try {
        stmt = connection.prepareStatement("SELECT id, category, event_division FROM Judges WHERE Tournament = ?");
        stmt.setInt(1, tournament);
        rs = stmt.executeQuery();
        for (row = 0; rs.next(); row++) {
          final String id = rs.getString(1);
          final String category = rs.getString(2);
          final String division = rs.getString(3);
          final JudgeInformation judge = new JudgeInformation(id, category, division);
          generateRow(out, subjectiveCategories, divisions, row, judge);
        }
      } finally {
        SQLFunctions.close(rs);
        SQLFunctions.close(stmt);
      }
    } else {
      // need to walk the parameters to see what we've been passed
      String id = request.getParameter("id"
          + row);
      String category = request.getParameter("cat"
          + row);
      String division = request.getParameter("div"
          + row);
      while (null != category) {
        final JudgeInformation judge = new JudgeInformation(id, category, division);
        generateRow(out, subjectiveCategories, divisions, row, judge);

        row++;
        id = request.getParameter("id"
            + row);
        category = request.getParameter("cat"
            + row);
        division = request.getParameter("div"
            + row);
      }
    }

    // if there aren't enough judges in the database for each category,
    // generate some more
    final int tableRows = Math.max(Math.max(numRows, subjectiveCategories.size()), row);

    for (; row < tableRows; row++) {
      generateRow(out, subjectiveCategories, divisions, row, new JudgeInformation(null, null, null));
    }

    out.println("</table>");
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines the table name")
  private static boolean checkForEnteredSubjectiveScores(final Connection connection,
                                                         final List<Element> subjectiveCategories,
                                                         final int tournament) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      for (final Element category : subjectiveCategories) {
        final String categoryName = category.getAttribute("name");
        prep = connection.prepareStatement(String.format("SELECT * FROM %s WHERE Tournament = ?", categoryName));
        prep.setInt(1, tournament);
        rs = prep.executeQuery();
        if (rs.next()) {
          return true;
        }
      }
      return false;
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

  }

  /**
   * Generate a row in the judges table defaulting the form elemenets to the
   * given information.
   * 
   * @param out where to print
   * @param subjectiveCategories the possible categroies
   * @param divisions List of divisions as Strings, "All" is always the first
   *          element in the list
   * @param row the row being generated, used for naming form elements
   */
  private static void generateRow(final JspWriter out,
                                  final List<Element> subjectiveCategories,
                                  final List<String> divisions,
                                  final int row,
                                  final JudgeInformation judge) throws IOException {
    out.println("<tr>");
    out.print("  <td><input type='text' name='id"
        + row + "'");
    if (null != judge.getId()) {
      out.print(" value='"
          + judge.getId() + "'");
    }
    out.println("></td>");

    out.println("  <td><select name='cat"
        + row + "'>");
    for (final Element category : subjectiveCategories) {
      final String categoryName = category.getAttribute("name");
      final String categoryTitle = category.getAttribute("title");
      out.print("  <option value='"
          + categoryName + "'");
      if (categoryName.equals(judge.getCategory())) {
        out.print(" selected");
      }
      out.println(">"
          + categoryTitle + "</option>");
    }
    out.println("  </select></td>");

    out.println("  <td><select name='div"
        + row + "'>");
    for (final String div : divisions) {
      out.print("  <option value='"
          + div + "'");
      if (div.equals(judge.getDivision())) {
        out.print(" selected");
      }
      out.println(">"
          + div + "</option>");
    }
    out.println("  </select></td>");

    out.println("</tr>");
  }

  /**
   * Validate the list of judges in request and if ok generate a final table for
   * the user to check along with buttons for submit and cancel. If something is
   * wrong, return a useful message.
   * 
   * @return null if everything is ok, otherwise the error message
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "XSS_REQUEST_PARAMETER_TO_JSP_WRITER", justification = "Checking category name retrieved from request against valid category names")
  private static String generateVerifyTable(final JspWriter out,
                                            final List<Element> subjectiveCategories,
                                            final HttpServletRequest request,
                                            final Document challengeDocument) throws IOException, ParseException {
    // keep track of any errors
    final StringBuffer error = new StringBuffer();

    // keep track of which categories have judges
    final Map<String, Set<String>> hash = new HashMap<String, Set<String>>();

    // populate a hash where key is category name and value is an empty
    // Set. Use set so there are no duplicates
    for (final Element element : subjectiveCategories) {
      final String categoryName = element.getAttribute("name");
      hash.put(categoryName, new HashSet<String>());
    }

    // walk request and push judge id into the Set, if not null or empty,
    // in the value for each category in the hash.
    int row = 0;
    String id = request.getParameter("id"
        + row);
    String category = request.getParameter("cat"
        + row);
    while (null != category) {
      if (null != id) {
        id = id.trim();
        id = id.toUpperCase();
        if (id.length() > 0) {
          final Set<String> set = hash.get(category);
          set.add(id);
        }
      }

      row++;
      id = request.getParameter("id"
          + row);
      category = request.getParameter("cat"
          + row);
    }

    // now walk the keys of the hash and make sure that all values have a list
    // of size > 0, otherwise append an error to error.
    for (final Map.Entry<String, Set<String>> entry : hash.entrySet()) {
      final String categoryName = entry.getKey();
      final Set<String> set = entry.getValue();
      if (set.isEmpty()) {
        error.append("You must specify at least one judge for "
            + categoryName + "<br>");
      }
    }

    if (error.length() > 0) {
      return error.toString();
    } else {
      out.println("<form action='judges.jsp' method='POST' name='CommitJudges'>");

      out.println("<p>If everything looks ok, click Commit, otherwise click Cancel and you'll go back to the edit page.</p>");
      // generate final table with submit button
      out.println("<table border='1'><tr><th>ID</th><th>Category</th><th>Division</th></tr>");

      // walk request and put data in a table
      row = 0;
      id = request.getParameter("id"
          + row);
      category = request.getParameter("cat"
          + row);
      String division = request.getParameter("div"
          + row);
      while (null != category) {
        if (null != id
            && division != null && XMLUtils.isValidCategoryName(challengeDocument, category)) {
          id = id.trim();
          id = id.toUpperCase();
          if (id.length() > 0) {
            out.println("<tr>");
            out.println("  <td>"
                + id + " <input type='hidden' name='id" + row + "' value='" + id + "'></td>");
            out.println("  <td>"
                + category + " <input type='hidden' name='cat" + row + "' value='" + category + "'></td>");
            out.println("  <td>"
                + division + " <input type='hidden' name='div" + row + "' value='" + division + "'></td>");
            out.println("</tr>");

          }
        }

        row++;
        id = request.getParameter("id"
            + row);
        category = request.getParameter("cat"
            + row);
        division = request.getParameter("div"
            + row);
      }

      out.println("</table>");
      out.println("<input type='submit' name='commit' value='Commit'>");
      out.println("<input type='submit' name='cancel' value='Cancel'>");
      out.println("</form>");

      return null;
    }
  }

}
