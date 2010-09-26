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
 * 
 * @version $Revision$
 */
public final class Judges {

  private static final Logger LOG = LogUtils.getLogger();

  private Judges() {

  }

  /**
   * Generate the judges page
   */
  public static void generatePage(final JspWriter out, final ServletContext application, final HttpServletRequest request, final HttpServletResponse response)
      throws SQLException, IOException, ParseException {
    final HttpSession session = request.getSession();
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
    final DataSource datasource = SessionAttributes.getDataSource(session);
    final Connection connection = datasource.getConnection();
    final int tournament = Queries.getCurrentTournament(connection);

    final List<Element> subjectiveCategories = new NodelistElementCollectionAdapter(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory")).asList();

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
    out.println("<form action='judges.jsp' method='POST' name='judges'>");

    String errorString = null;
    if (null != request.getParameter("finished")) {
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
      out.println("<p id='error'><font color='red'>"
          + errorString + "</font></p>");
    }

    // get list of divisions and add "All" as a possible value
    final List<String> divisions = Queries.getEventDivisions(connection);
    divisions.add(0, "All");

    out
       .println("<p>Judges ID's must be unique.  They can be just the name of the judge.  Keep in mind that this ID needs to be entered on the judging forms.  There must be at least 1 judge for each category.</p>");

    out.println("<table border='1'><tr><th>ID</th><th>Category</th><th>Division</th></tr>");

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
    out.println("<input type='text' name='num_rows' value='1' size='10'/>");
    out.println("<input type='submit' name='add_rows' value='Add Rows'/><br/>");
    out.println("<input type='submit' name='finished' value='Finished'/><br/>");

    out.println("</form>");
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
      out.print("  <option value='"
          + categoryName + "'");
      if (categoryName.equals(cat)) {
        out.print(" selected");
      }
      out.println(">"
          + categoryName + "</option>");
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="XSS_REQUEST_PARAMETER_TO_JSP_WRITER", justification="Checking category name retrieved from request against valid category names")
  private static String generateVerifyTable(final JspWriter out, final List<Element> subjectiveCategories, final HttpServletRequest request, final Document challengeDocument)
      throws IOException, ParseException {
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
    for(final Map.Entry<String, Set<String>> entry : hash.entrySet()) {
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
            && division != null
            && XMLUtils.isValidCategoryName(challengeDocument, category)) {
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
  private static void commitData(final HttpServletRequest request, final HttpServletResponse response, final HttpSession session, final Connection connection, final int tournament)
      throws SQLException, IOException {
    PreparedStatement prep = null;
    try {
      // delete old data in judges
      prep = connection.prepareStatement("DELETE FROM Judges where Tournament = ?");
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
      while (null != category) {
        prep.setString(1, id);
        prep.setString(2, category);
        prep.setString(3, division);
        prep.executeUpdate();

        row++;
        id = request.getParameter("id"
            + row);
        category = request.getParameter("cat"
            + row);
        division = request.getParameter("div"
            + row);
      }

    } finally {
      SQLFunctions.close(prep);
    }

    // finally redirect to index.jsp
    session.setAttribute(SessionAttributes.MESSAGE, "<p id='success'><i>Successfully assigned judges</i></p>");
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

}
