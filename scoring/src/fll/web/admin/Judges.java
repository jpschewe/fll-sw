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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.xml.XMLUtils;

/**
 * Java code used in judges.jsp
 */
public final class Judges {

  private static final Logger LOG = LogUtils.getLogger();

  private Judges() {

  }

  /**
   * String used for all divisions when assigning judges.
   */
  public static final String ALL_DIVISIONS = "All";

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
    if (null != request.getParameter("add_rows")) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<!-- adding another row to "
            + rowIndex + "-->");
      }
      try {
        final Integer numRows = Integer.valueOf(request.getParameter("num_rows"));
        rowIndex += numRows;
      } catch (final NumberFormatException nfe) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Got exception reading number of rows, defaulting to 1", nfe);
        }
        rowIndex += 1;
      }
    }

    final int numRows = rowIndex + 1;

    String errorString = null;
    if (null != request.getParameter("finished")) {
      out.println("<form action='judges.jsp' method='POST' name='judges'>");
      errorString = generateVerifyTable(out, subjectiveCategories, request, challengeDocument);
      if (null == errorString) {
        // everything is good
        out.println("</form>");
        return;
      }
    } else if (null != request.getParameter("commit")) {
      commitData(request, response, session, connection, Queries.getCurrentTournament(connection));
      return;
    }

    if (null != errorString) {
      out.println("<p id='error' class='error'>"
          + errorString + "</p>");
    }

    // get list of divisions and add "All" as a possible value
    final List<String> divisions = Queries.getEventDivisions(connection);
    divisions.add(0, ALL_DIVISIONS);

    out.println("<p>Judges ID's must be unique.  They can be just the name of the judge.  Keep in mind that this ID needs to be entered on the judging forms.  There must be at least 1 judge for each category.</p>");

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
          generateRow(out, subjectiveCategories, divisions, row, id, category, division);
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
        generateRow(out, subjectiveCategories, divisions, row, id, category, division);

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
      generateRow(out, subjectiveCategories, divisions, row, null, null, null);
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
   * @param id id of judge, can be null
   * @param cat category of judge, can be null
   * @param division division judge is scoring, can be null
   */
  private static void generateRow(final JspWriter out,
                                  final List<Element> subjectiveCategories,
                                  final List<String> divisions,
                                  final int row,
                                  final String id,
                                  final String cat,
                                  final String division) throws IOException {
    out.println("<tr>");
    out.print("  <td><input type='text' name='id"
        + row + "'");
    if (null != id) {
      out.print(" value='"
          + id + "'");
    }
    out.println("></td>");

    out.println("  <td><select name='cat"
        + row + "'>");
    for (final Element category : subjectiveCategories) {
      final String categoryName = category.getAttribute("name");
      final String categoryTitle = category.getAttribute("title");
      out.print("  <option value='"
          + categoryName + "'");
      if (categoryName.equals(cat)) {
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
      if (div.equals(division)) {
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

      return null;
    }
  }

  /**
   * Commit the subjective data from request to the database and redirect
   * response back to index.jsp.
   * 
   * @param tournament the current tournament
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines the table name")
  private static void commitData(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final HttpSession session,
                                 final Connection connection,
                                 final int tournament) throws SQLException, IOException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      // save old judge information
      final Set<JudgeInformation> oldJudgeInfo = new HashSet<JudgeInformation>();
      prep = connection.prepareStatement("SELECT id, category, event_division FROM Judges WHERE Tournament = ?");
      prep.setInt(1, tournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final String id = rs.getString(1);
        final String category = rs.getString(2);
        final String division = rs.getString(3);
        oldJudgeInfo.add(new JudgeInformation(id, category, division));
      }

      // delete old data in judges
      prep = connection.prepareStatement("DELETE FROM Judges where Tournament = ?");
      prep.setInt(1, tournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

      // walk request parameters and insert data into database
      prep = connection.prepareStatement("INSERT INTO Judges (id, category, event_division, Tournament) VALUES(?, ?, ?, ?)");
      prep.setInt(4, tournament);
      int row = 0;
      String id = request.getParameter("id"
          + row);
      String category = request.getParameter("cat"
          + row);
      String division = request.getParameter("div"
          + row);
      final Set<JudgeInformation> newJudgeInfo = new HashSet<JudgeInformation>();
      while (null != category) {
        prep.setString(1, id);
        prep.setString(2, category);
        prep.setString(3, division);
        prep.executeUpdate();
        final JudgeInformation info = new JudgeInformation(id, category, division);
        newJudgeInfo.add(info);

        row++;
        id = request.getParameter("id"
            + row);
        category = request.getParameter("cat"
            + row);
        division = request.getParameter("div"
            + row);
      }

      // figure out which ones are no longer there and remove all of their old
      // scores
      oldJudgeInfo.removeAll(newJudgeInfo);
      for (final JudgeInformation oldInfo : oldJudgeInfo) {
        prep = connection.prepareStatement(String.format("DELETE FROM %s WHERE Judge = ? AND Tournament = ?",
                                                         oldInfo.getCategory()));
        prep.setString(1, oldInfo.getId());
        prep.setInt(2, tournament);
        prep.executeUpdate();
      }

    } finally {
      SQLFunctions.close(prep);
      SQLFunctions.close(rs);
    }

    // finally redirect to index.jsp
    session.setAttribute(SessionAttributes.MESSAGE, "<p id='success'><i>Successfully assigned judges</i></p>");
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

  /**
   * Judge information for use in a collection.
   */
  private static final class JudgeInformation {
    private final String id;

    public String getId() {
      return id;
    }

    private final String category;

    public String getCategory() {
      return category;
    }

    private final String division;

    public String getDivision() {
      return division;
    }

    public JudgeInformation(final String id,
                            final String category,
                            final String division) {
      this.id = id;
      this.category = category;
      this.division = division;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (null == o) {
        return false;
      } else if (o == this) {
        return true;
      } else if (o.getClass().equals(JudgeInformation.class)) {
        final JudgeInformation other = (JudgeInformation) o;
        return getId().equals(other.getId())
            && getCategory().equals(other.getCategory()) && getDivision().equals(other.getDivision());
      } else {
        return false;
      }
    }
  }
}
