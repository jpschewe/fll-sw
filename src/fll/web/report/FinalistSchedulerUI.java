/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.Team;
import fll.Utilities;
import fll.db.Queries;
import fll.util.ScoreUtils;

/**
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class FinalistSchedulerUI extends HttpServlet {

  private static final String CATEGORIES_KEY = "categories";

  private static final String FINALISTS_KEY = "finalists";

  private static final Logger LOG = Logger.getLogger(FinalistSchedulerUI.class);

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    doGet(request, response);
  }

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final Connection connection = (Connection)application.getAttribute("connection");
    final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
    final HttpSession session = request.getSession();

    // initialize the session
    if(null != request.getParameter("init") || null == session.getAttribute(CATEGORIES_KEY)) {
      // no categories cache, need to compute them
      final List<Element> categories = new LinkedList<Element>();
      final NodeList subjectiveCategoryElements = challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");
      for(int i = 0; i < subjectiveCategoryElements.getLength(); i++) {
        final Element subjectiveElement = (Element)subjectiveCategoryElements.item(i);
        categories.add(subjectiveElement);
      }
      session.setAttribute(CATEGORIES_KEY, categories);
      if(LOG.isDebugEnabled()) {
        LOG.debug("Initialized categories to: " + categories);
      }
    }
    // can't type inside the session, but we know the type
    @SuppressWarnings("unchecked")
    final List<Element> categories = (List<Element>)session.getAttribute(CATEGORIES_KEY);

    if(null != request.getParameter("init") || null == session.getAttribute(FINALISTS_KEY)) {
      session.setAttribute(FINALISTS_KEY, new HashMap<String, Collection<Integer>>());
      if(LOG.isDebugEnabled()) {
        LOG.debug("Initialized finalists");
      }
    }
    // can't type inside the session, but we know the type
    @SuppressWarnings("unchecked")
    final Map<String, Collection<Integer>> finalists = (Map<String, Collection<Integer>>)session.getAttribute(FINALISTS_KEY);
    // done with initialization of session

    // process request parameters
    if(null != request.getParameter("create-category")) {
      if(null == request.getParameter("new-category")) {
        LOG.error("No category specified");
        session.setAttribute("message", "<p class='error'>You must specify a category name</p>");
        response.sendRedirect(response.encodeRedirectURL("promptForCategoryName.jsp"));
        return;
      } else {
        final String categoryName = request.getParameter("new-category");
        if(finalists.containsKey(categoryName)) {
          LOG.error("duplicate category: " + categoryName);
          session.setAttribute("message", "<p class='error'>The category name <i>" + categoryName
              + "</i> has already been specified, please pick another one</p>");
          response.sendRedirect(response.encodeRedirectURL("promptForCategoryName.jsp"));
          return;
        } else {
          displayFinalistForm(response, connection, categoryName);
          return;
        }
      }
    } else if(null != request.getParameter("submit-finalists")) {
      // get the finalists list
      final Collection<Integer> finalistNums = new LinkedList<Integer>();
      for(final String finalistNumStr : request.getParameterValues("finalist")) {
        try {
          finalistNums.add(Integer.valueOf(finalistNumStr));
        } catch(final NumberFormatException e) {
          throw new RuntimeException("Internal error, cannot parse number '" + finalistNumStr + "'", e);
        }
      }
      finalists.put(request.getParameter("category-title"), finalistNums);
    } else if(null != request.getParameter("done")) {
      displayFinalistSchedule(request, response, connection, finalists);

      // clear out session and return
      session.removeAttribute(CATEGORIES_KEY);
      session.removeAttribute(FINALISTS_KEY);

      return;
    }
    // done with request parameters

    if(!categories.isEmpty()) {
      // remove the first category off the list and set the session attribute
      // back
      final Element subjectiveElement = categories.remove(0);
      session.setAttribute(CATEGORIES_KEY, categories);

      displayFinalistFormForCategory(response, connection, subjectiveElement);

    } else {
      // prompt user for name of new category or done
      if(LOG.isDebugEnabled()) {
        LOG.debug("done with subjective categories, asking for new categories");
      }
      response.sendRedirect(response.encodeRedirectURL("promptForCategoryName.jsp"));
      return;
    }
  }

  private void displayFinalistSchedule(final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final Connection connection,
                                       final Map<String, Collection<Integer>> finalists) throws IOException {
    // call schedule and display results
    final List<Map<String, Integer>> schedule = ScoreUtils.scheduleFinalists(finalists);
    if(LOG.isDebugEnabled()) {
      LOG.debug("Finalists schedule is: " + schedule);
    }

    // compute the list of all categories
    final List<String> finalistCategories = new LinkedList<String>();
    for(final Map<String, Integer> timeSlot : schedule) {
      for(final String catName : timeSlot.keySet()) {
        if(!finalistCategories.contains(catName)) {
          finalistCategories.add(catName);
        }
      }
    }

    // write out table of finalists
    final PrintWriter writer = response.getWriter();
    writer.write("<html><body>");
    writer.write("<h1>Finalists Schedule</h1>");
    writer.write("<table border='1'>");

    writer.write("<tr><th>Time Slot</th>");
    for(final String catName : finalistCategories) {
      writer.write("<th>" + catName + "</th>");
    }
    writer.write("</tr>");

    int slot = 1;
    for(final Map<String, Integer> timeSlot : schedule) {
      writer.write("<tr>");
      writer.write("<td>" + slot + "</td>");

      for(final String catName : finalistCategories) {
        writer.write("<td>");
        if(timeSlot.containsKey(catName)) {
          final Integer teamNumber = timeSlot.get(catName);
          try {
            final Team team = Team.getTeamFromDatabase(connection, teamNumber);
            writer.write(team.getTeamNumber() + " - " + team.getTeamName());
          } catch(final SQLException e) {
            throw new RuntimeException("Error getting information for team " + teamNumber, e);
          }
        } else {
          writer.write("&nbsp;");
        }
        writer.write("</td>");
      }

      writer.write("</tr>");

      ++slot;
    }

    writer.write("</table>");
    writer.write("</body></html>");
  }

  /**
   * Display the finalist form for a known subjective category.
   * 
   * @param response
   * @param connection
   * @param subjectiveElement
   * @throws IOException
   */
  private void displayFinalistFormForCategory(final HttpServletResponse response, final Connection connection, final Element subjectiveElement)
      throws IOException {

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      // display the teams by division and score group
      final String categoryTitle = subjectiveElement.getAttribute("title");
      final String categoryName = subjectiveElement.getAttribute("name");
      final String currentTournament = Queries.getCurrentTournament(connection);
      final PrintWriter writer = response.getWriter();
      writer.write("<html><body>");
      writer.write("<h1>Choose finalists for " + categoryTitle + "</h1>");
      writer.write("<hr/>");

      writer.write("<form method='post' action='FinalistSchedulerUI'>");
      writer.write("<input type='hidden' name='category-title' value='" + categoryTitle + "'/>");
      // foreach division
      for(String division : Queries.getDivisions(connection)) {

        final Map<String, Collection<Integer>> scoreGroups = Queries.computeScoreGroups(connection, currentTournament, division, categoryName);
        if(LOG.isDebugEnabled()) {
          LOG.debug("Found score groups for category " + categoryName + " division " + division + ": " + scoreGroups);
        }

        for(String scoreGroup : scoreGroups.keySet()) {
          writer.write("<h3>" + categoryTitle + " Division: " + division + " Score Group: " + scoreGroup + "</h3>");
          writer.write("<table border='0'>");
          writer.write("<tr><th>Finalist</th><th colspan='3'>Team # / Organization / Team Name</th><th>Scaled Score</th></tr>");

          final String teamSelect = StringUtils.join(scoreGroups.get(scoreGroup).iterator(), ", ");
          prep = connection.prepareStatement("SELECT Teams.TeamNumber,Teams.Organization,Teams.TeamName,FinalScores." + categoryName
              + " FROM Teams, FinalScores WHERE FinalScores.TeamNumber IN ( " + teamSelect
              + ") AND Teams.TeamNumber = FinalScores.TeamNumber AND FinalScores.Tournament = ? ORDER BY FinalScores." + categoryName + " DESC");
          prep.setString(1, currentTournament);
          rs = prep.executeQuery();
          while(rs.next()) {
            final String teamNum = rs.getString(1);
            final String org = rs.getString(2);
            final String name = rs.getString(3);
            final double score = rs.getDouble(4);
            final boolean scoreWasNull = rs.wasNull();
            writer.write("<tr>");
            writer.write("<td><input type='checkbox' name='finalist' value='" + teamNum + "'/></td>");

            writer.write("<td>");
            if(null == teamNum) {
              writer.write("");
            } else {
              writer.write(teamNum);
            }
            writer.write("</td>");
            writer.write("<td>");
            if(null == org) {
              writer.write("");
            } else {
              writer.write(org);
            }
            writer.write("</td>");
            writer.write("<td>");
            if(null == name) {
              writer.write("");
            } else {
              writer.write(name);
            }
            writer.write("</td>");
            if(!scoreWasNull) {
              writer.write("<td>");
              writer.write(Utilities.NUMBER_FORMAT_INSTANCE.format(score));
            } else {
              writer.write("<td align='center' class='warn'>No Score");
            }
            writer.write("</td>");
            writer.write("</tr>");
          } // results
          writer.write("</table");
        } // score groups

      } // divisions

      writer.write("<input type='submit' name='submit-finalists' value='Submit'>");
      writer.write("</form>");
      writer.write("</body></html>");

    } catch(final SQLException e) {
      throw new RuntimeException("Error getting finalist data", e);
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(prep);
    }
  }

  /**
   * Display the finalist form for a newly created category
   * 
   * @param response
   * @param connection
   * @param categoryTitle
   *          the name/title of the new category
   * @throws IOException
   */
  private void displayFinalistForm(final HttpServletResponse response, final Connection connection, final String categoryTitle) throws IOException {

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      // display the teams by division and score group
      final PrintWriter writer = response.getWriter();
      writer.write("<html><body>");
      writer.write("<h1>Choose finalists for " + categoryTitle + "</h1>");
      writer.write("<hr/>");

      writer.write("<form method='post' action='FinalistSchedulerUI'>");
      writer.write("<input type='hidden' name='category-title' value='" + categoryTitle + "'/>");
      // foreach division
      for(String division : Queries.getDivisions(connection)) {

        writer.write("<h3>" + categoryTitle + " Division: " + division + "</h3>");
        writer.write("<table border='0'>");
        writer.write("<tr><th>Finalist</th><th colspan='3'>Team # / Organization / Team Name</th></tr>");

        prep = connection.prepareStatement("SELECT Teams.TeamNumber,Teams.Organization,Teams.TeamName"
            + " FROM Teams, current_tournament_teams WHERE Teams.TeamNumber = current_tournament_teams.TeamNumber"
            + " AND current_tournament_teams.event_division = ?");
        prep.setString(1, division);
        rs = prep.executeQuery();
        while(rs.next()) {
          final String teamNum = rs.getString(1);
          final String org = rs.getString(2);
          final String name = rs.getString(3);
          writer.write("<tr>");
          writer.write("<td><input type='checkbox' name='finalist' value='" + teamNum + "'/></td>");

          writer.write("<td>");
          if(null == teamNum) {
            writer.write("");
          } else {
            writer.write(teamNum);
          }
          writer.write("</td>");
          writer.write("<td>");
          if(null == org) {
            writer.write("");
          } else {
            writer.write(org);
          }
          writer.write("</td>");
          writer.write("<td>");
          if(null == name) {
            writer.write("");
          } else {
            writer.write(name);
          }
          writer.write("</td>");
          writer.write("</tr>");
        } // results
        writer.write("</table");

      } // divisions

      writer.write("<input type='submit' name='submit-finalists' value='Submit'>");
      writer.write("</form>");
      writer.write("</body></html>");

    } catch(final SQLException e) {
      throw new RuntimeException("Error getting finalist data", e);
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(prep);
    }
  }

}
