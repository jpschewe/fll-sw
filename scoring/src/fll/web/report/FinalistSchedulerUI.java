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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.util.ScoreUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

/**
 * Handle scheduling of finalists.
 * 
 */
@WebServlet("/report/FinalistSchedulerUI")
public class FinalistSchedulerUI extends BaseFLLServlet {

  private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
    @Override 
    protected DateFormat initialValue() {
      return new SimpleDateFormat("HH:mm");
    }
  };
  
  /**
   * Key into session that contains the division finalists Map.
   */
  private static final String DIVISION_FINALISTS_KEY = "divisionFinalists";
  
  /**
   * Key into session that contains the names for the categories.
   */
  private static final String CATEGORIES_KEY = "subjectiveCategories";

  /**
   * Key into session that contains the "extra" categories and the finalists in
   * those categories.
   */
  private static final String EXTRA_CATEGORIES_FINALISTS_KEY = "extraCategories";

  private static final Logger LOG = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = SessionAttributes.getDataSource(session);
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

    try {

      final Connection connection = datasource.getConnection();

      // initialize the session
      if (null != request.getParameter("init")
          || null == session.getAttribute(CATEGORIES_KEY)) {
        // no categories cache, need to compute them
        final List<String> subjectiveCategories = new LinkedList<String>();
        for (final Element subjectiveElement : new NodelistElementCollectionAdapter(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
          final String categoryTitle = subjectiveElement.getAttribute("title");
          subjectiveCategories.add(categoryTitle);
        }
        session.setAttribute(CATEGORIES_KEY, subjectiveCategories);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Initialized subjectiveCategories to: "
              + subjectiveCategories);
        }
      }
      // can't type inside the session, but we know the type
      @SuppressWarnings("unchecked")
      final List<String> categories = (List<String>) session.getAttribute(CATEGORIES_KEY);

      if (null != request.getParameter("init")
          || null == session.getAttribute(EXTRA_CATEGORIES_FINALISTS_KEY)) {
        session.setAttribute(EXTRA_CATEGORIES_FINALISTS_KEY, new HashMap<String, Map<String, List<Integer>>>());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Initialized extra categories");
        }
      }
      // can't type inside the session, but we know the type
      @SuppressWarnings("unchecked")
      final Map<String, List<Integer>> extraCategoryFinalists = (Map<String, List<Integer>>) session.getAttribute(EXTRA_CATEGORIES_FINALISTS_KEY);
      // done with initialization of session

      // process request parameters
      if (null != request.getParameter("num-finalists")) {
        // just store the number of finalists and prompt for extra categories
        final int numFinalists = Integer.valueOf(request.getParameter("num-finalists"));
        session.setAttribute("numFinalists", numFinalists);

        // store the division
        final String division = request.getParameter("division");
        if (null == division) {
          throw new RuntimeException("Missing expected parameter 'division'");
        }
        session.setAttribute("division", division);

        response.sendRedirect(response.encodeRedirectURL("promptForCategoryName.jsp"));
        return;
      } else if (null != request.getParameter("create-category")) {
        if (null == request.getParameter("new-category")) {
          LOG.error("No category specified");
          session.setAttribute("message", "<p class='error'>You must specify a category name</p>");
          response.sendRedirect(response.encodeRedirectURL("promptForCategoryName.jsp"));
          return;
        } else {
          final String categoryName = request.getParameter("new-category");
          if (categories.contains(categoryName)) {
            LOG.error("duplicate category: "
                + categoryName);
            session.setAttribute("message", "<p class='error'>The category name <i>"
                + categoryName + "</i> has already been specified, please pick another one</p>");
            response.sendRedirect(response.encodeRedirectURL("promptForCategoryName.jsp"));
            return;
          } else {
            categories.add(categoryName);

            final int numFinalists = SessionAttributes.getNonNullAttribute(session, "numFinalists", Integer.class);

            final List<Integer> finalistNums = new LinkedList<Integer>();
            for (int i = 1; i <= numFinalists; ++i) {
              final String numStr = request.getParameter("finalist-"
                  + i);
              if (null != numStr
                  && !"".equals(numStr)) {
                try {
                  finalistNums.add(Integer.valueOf(numStr));
                } catch (final NumberFormatException e) {
                  throw new RuntimeException("Internal error, cannot parse number '"
                      + numStr + "'", e);
                }
              }
            } // numFinalists

            extraCategoryFinalists.put(categoryName, finalistNums);

            response.sendRedirect(response.encodeRedirectURL("promptForCategoryName.jsp"));
            return;
          } // ok to add category
        } // new category to add
      } else if (null != request.getParameter("done")) {
        final int numFinalists = SessionAttributes.getNonNullAttribute(session, "numFinalists", Integer.class);
        final String division = SessionAttributes.getNonNullAttribute(session, "division", String.class);
        response.setContentType("text/html");
        displayProposedFinalists(response, connection, challengeDocument, numFinalists, division, extraCategoryFinalists, new ColorChooser());
        return;
      } else if (null != request.getParameter("submit-finalists")) {
        // store the data
        final Map<String, Collection<Integer>> divisionFinalists = new HashMap<String, Collection<Integer>>();
        for (final String categoryTitle : categories) {
          final Collection<Integer> catFinalists = new LinkedList<Integer>();
          final String[] teamNumsStr = request.getParameterValues(categoryTitle);
          for (final String teamNumStr : teamNumsStr) {
            catFinalists.add(Integer.valueOf(teamNumStr));
          }
          divisionFinalists.put(categoryTitle, catFinalists);
        }
        session.setAttribute(DIVISION_FINALISTS_KEY, divisionFinalists);

        // prompt for times
        response.sendRedirect(response.encodeRedirectURL("promptForFinalistTimes.jsp"));        
        return;
      } else if(null != request.getParameter("submit-times")) {
        final String hourStr = request.getParameter("hour");
        if(null == hourStr) {
          throw new FLLRuntimeException("Missing 'hour' parameter");
        }
        final int hour = Utilities.NUMBER_FORMAT_INSTANCE.parse(hourStr).intValue();
        if(hour < 0 || hour > 24) {
          throw new FLLRuntimeException("Hour must be between 0 and 24");
        }
        
        final String minuteStr = request.getParameter("minute");
        if(null == minuteStr) {
          throw new FLLRuntimeException("Missing 'minute' parameter");
        }
        final int minute = Utilities.NUMBER_FORMAT_INSTANCE.parse(minuteStr).intValue();
        if(minute < 0 || minute > 59) {
          throw new FLLRuntimeException("minute must be between 0 and 59");
        }
        final Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, hour);
        start.set(Calendar.MINUTE, minute);
        
        final String intervalStr = request.getParameter("interval");
        if(null == intervalStr) {
          throw new FLLRuntimeException("Missing 'interval' parameter");
        }
        final int interval = Utilities.NUMBER_FORMAT_INSTANCE.parse(intervalStr).intValue();
        if(interval < 1) {
          throw new FLLRuntimeException("interval must be greater than 0");
        }
        
        // display the schedule
        final String division = SessionAttributes.getNonNullAttribute(session, "division", String.class);
        response.setContentType("text/html");
        displayFinalistSchedule(session, response, connection, division, categories, start, interval);

        // clear out session and return
        session.removeAttribute(CATEGORIES_KEY);
        session.removeAttribute(EXTRA_CATEGORIES_FINALISTS_KEY);

        return;
      }
      // done with request parameters
    } catch (final SQLException sqle) {
      throw new FLLRuntimeException(sqle);
    } catch (final ParseException e) {
      throw new FLLRuntimeException(e);
    }

    // send them back to pick a number
    LOG.warn("No parameters, sending user back to get num finalists");
    response.sendRedirect(response.encodeRedirectURL("promptForNumFinalists.jsp"));
  }

  private void displayFinalistSchedule(final HttpSession session,
                                       final HttpServletResponse response,
                                       final Connection connection,
                                       final String division,
                                       final List<String> categories,
                                       final Calendar start,
                                       final int interval) throws IOException {

    final Formatter formatter = new Formatter(response.getWriter());
    formatter.format("<html><body>");
    formatter.format("<h1>Finalists Schedule</h1>");

    formatter.format("<h2>Division: %s</h2>", division);

    // can't type inside the session, but we know the type
    @SuppressWarnings("unchecked")
    final Map<String, Collection<Integer>> divisionFinalists = (Map<String, Collection<Integer>>)SessionAttributes.getAttribute(session, DIVISION_FINALISTS_KEY, Map.class);

    // call schedule
    final List<Map<String, Integer>> schedule = ScoreUtils.scheduleFinalists(divisionFinalists);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Division: "
          + division + " finalists schedule is: " + schedule);
    }

    // write out table of finalists
    formatter.format("<table border='1'>");

    formatter.format("<tr><th>Time Slot</th>");
    for (final String categoryTitle : categories) {
      formatter.format("<th>%s</th>", categoryTitle);
    }
    formatter.format("</tr>");
    
    for (final Map<String, Integer> timeSlot : schedule) {
      formatter.format("<tr>");
      formatter.format("<td>%s</td>", DATE_FORMAT.get().format(start.getTime()));

      for (final String categoryTitle : categories) {
        formatter.format("<td>");
        if (timeSlot.containsKey(categoryTitle)) {
          final Integer teamNumber = timeSlot.get(categoryTitle);
          try {
            final Team team = Team.getTeamFromDatabase(connection, teamNumber);
            formatter.format("%s - %s", team.getTeamNumber(), team.getTeamName());
          } catch (final SQLException e) {
            throw new RuntimeException("Error getting information for team "
                + teamNumber, e);
          }
        } else {
          formatter.format("&nbsp;");
        }
        formatter.format("</td>");
      }

      formatter.format("</tr>");

      // increment for next row
      start.add(Calendar.MINUTE, interval);
    }

    formatter.format("</table>");

    formatter.format("</body></html>");
  }
  
  /**
   * Display the proposed finalists and allow the user to pick which ones to
   * actually schedule.
   * 
   * @param response
   * @param connection
   * @param numFinalists
   * @param finalists map of divisions to map of extra category names to list of
   *          team numbers
   * @throws IOException
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { 
  "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table name")
  private void displayProposedFinalists(final HttpServletResponse response,
                                        final Connection connection,
                                        final Document challengeDocument,
                                        final int numFinalists,
                                        final String division,
                                        final Map<String, List<Integer>> divisionFinalists,
                                        final ColorChooser colorChooser) throws IOException {
    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);
    final String ascDesc = WinnerType.HIGH == winnerCriteria ? "DESC" : "ASC";

    final PrintWriter writer = response.getWriter();

    final Formatter formatter = new Formatter();

    // map of teams to HTML color names
    final Map<Integer, String> teamColors = new HashMap<Integer, String>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    PreparedStatement rawPrep = null;
    ResultSet rawRS = null;
    try {
      formatter.format("<h1>Choose finalists to be scheduled</h1>");
      formatter.format("<hr/>");

      formatter
               .format("<p>Below are the proposed finalists, please choose at least one team per category per division that should be called back for finalist judging");
      formatter.format("<form action='FinalistSchedulerUI' method='POST'>");

      // display the teams by division and score group
      final int currentTournament = Queries.getCurrentTournament(connection);

      formatter.format("<h2>Division: %s</h2>", division);

      for (final Element subjectiveElement : new NodelistElementCollectionAdapter(challengeDocument.getDocumentElement()
                                                                       .getElementsByTagName("subjectiveCategory"))) {
        final String categoryTitle = subjectiveElement.getAttribute("title");
        final String categoryName = subjectiveElement.getAttribute("name");


        final Map<String, Collection<Integer>> scoreGroups = Queries.computeScoreGroups(connection, currentTournament, division, categoryName);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Found score groups for category "
              + categoryName + " division " + division + ": " + scoreGroups);
        }

        formatter.format("<table border='1'><tbody>");
        formatter.format("<tr><th colspan='6'>%s</th></tr>", categoryTitle);
        formatter.format("<tr><th>Score Group</th><th>Team #</th><th>Team Name</th><th>Finalist?</th><th>Combined</th><th>Standardized</th><th>Raw</th></tr>");

        for(final Map.Entry<String, Collection<Integer>> entry : scoreGroups.entrySet()) {
          final String scoreGroup = entry.getKey();
          final String teamSelect = StringUtils.join(entry.getValue().iterator(), ", ");
          prep = connection.prepareStatement("SELECT Teams.TeamNumber, FinalScores." + categoryName
              + " FROM Teams, FinalScores WHERE FinalScores.TeamNumber IN ( " + teamSelect
              + ") AND Teams.TeamNumber = FinalScores.TeamNumber AND FinalScores.Tournament = ? ORDER BY FinalScores." + categoryName + " " + ascDesc
              + " LIMIT " + numFinalists);
          prep.setInt(1, currentTournament);
          rawPrep = connection.prepareStatement("SELECT StandardizedScore,ComputedTotal FROM " + categoryName + " WHERE TeamNumber = ? AND Tournament = ?");
          rawPrep.setInt(2, currentTournament);
          
          boolean first = true;
          rs = prep.executeQuery();
          while (rs.next()) {
            final int teamNum = rs.getInt(1);            
            final double finalScore = rs.getDouble(2);
            final boolean finalScoreNull = rs.wasNull();
            if (teamColors.containsKey(teamNum)) {
              teamColors.put(teamNum, colorChooser.getNextAvailableTeamColor());
            } else {
              teamColors.put(teamNum, null);
            }
            
            rawPrep.setInt(1, teamNum);
            rawRS = rawPrep.executeQuery();
            final StringBuilder standardized = new StringBuilder();
            final StringBuilder raw = new StringBuilder();
            boolean rawFirst = true;
            boolean standardizedFirst = true;
            while(rawRS.next()) {              
              final double standardizedScore = rawRS.getDouble(1);
              if(!rawRS.wasNull()) {
                if(standardizedFirst) {
                  standardizedFirst = false;
                } else {
                  standardized.append(",");
                }
                standardized.append(Utilities.NUMBER_FORMAT_INSTANCE.format(standardizedScore));
              }
              final double rawScore = rawRS.getDouble(2);
              if(!rawRS.wasNull()) {
                if(rawFirst) {
                  rawFirst = false;
                } else {
                  raw.append(",");
                }
                raw.append(Utilities.NUMBER_FORMAT_INSTANCE.format(rawScore));
              }
            }
            
            try {
              final Team team = Team.getTeamFromDatabase(connection, teamNum);
              formatter.format("<tr class='team-%s'><td>%s</td><td>%s</td><td>%s</td><td><input type='checkbox' name='%s' value='%s' %s/><td>%s</td><td>%s</td><td>%s</td></tr>", teamNum, scoreGroup,
                               teamNum, team.getTeamName(), categoryTitle, teamNum, first ? "checked" : "", finalScoreNull ? "" : Utilities.NUMBER_FORMAT_INSTANCE.format(finalScore), standardized.toString(), raw.toString());
            } catch (final SQLException e) {
              throw new RuntimeException("Error getting information for team "
                  + teamNum, e);
            }

            
            if(first) {
              first = false;
            }
          }
        } // score groups
        formatter.format("</tbody></table>");
      } // subjective categories

      // other categories
      for (final Map.Entry<String, List<Integer>> entry : divisionFinalists.entrySet()) {
        final String categoryTitle = entry.getKey();
        final List<Integer> teams = entry.getValue();

        formatter.format("<table border='1'><tbody>");
        formatter.format("<tr><th colspan='2'>%s</th></tr>", categoryTitle);
        formatter.format("<tr><th>Team #</th><th>Finalist?</th></tr>");
        boolean first = true;
        for (final Integer teamNum : teams) {
          if (teamColors.containsKey(teamNum)) {
            teamColors.put(teamNum, colorChooser.getNextAvailableTeamColor());
          } else {
            teamColors.put(teamNum, null);
          }

          formatter.format("<tr class='team-%s'><td>%s</td><td><input type='checkbox' name='%s' value='%s' %s/></tr>", teamNum, teamNum,
                           categoryTitle, teamNum, first ? "checked" : "");
          if(first) {
            first = false;
          }
        }
        formatter.format("</tbody></table>");
      } // other categories

      writer.write("<html><head>");
      writer.write("<title>Finalist Scheduling</title>");
      writer.write("<link rel='stylesheet' type='text/css' href='/fll-sw/style/style.jsp' />");
      writer.write("</head>");
      // write out style sheet for teams
      writer.write("<style type='text/css'>");
      for (final Map.Entry<Integer, String> entry : teamColors.entrySet()) {
        final String color = entry.getValue();
        if (null != color) {
          final int teamNum = entry.getKey();
          writer.write("tr.team-"
              + teamNum + " td { background-color: " + color + "; color: black; }");
        }
      }
      writer.write("</style>");
      writer.write(formatter.toString());
      writer.write("<input type='submit' name='submit-finalists' value='Submit'>");
      writer.write("</form>");
      writer.write("</body></html>");

    } catch (final SQLException e) {
      throw new RuntimeException("Error getting finalist data", e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
      SQLFunctions.close(rawRS);
      SQLFunctions.close(rawPrep);
    }
  }

  /**
   * Class to encapsulate choosing of colors for the scheduler.
   */
  private static final class ColorChooser {
    /**
     * Get the next color available to use for the background of proposed
     * finalists.
     * 
     * @return HTML color code
     */
    public String getNextAvailableTeamColor() {
      if (_teamColorIndex >= _teamColors.length) {
        LOG.error("Ran out of team colors at index "
            + _teamColorIndex);
        _teamColorIndex = 0;
      }
      final String color = _teamColors[_teamColorIndex];
      ++_teamColorIndex;
      return color;
    }

    private int _teamColorIndex = 0;

    private final String[] _teamColors = { "#CD5555", "#EE0000", "#FF6347", "#00FFAA", "#D19275", "#00B2EE", "#FF7D40", "#FFDAB9", "#FCB514", "#FFEC8B",
                                          "#C8F526", "#7CFC00", "#4AC948", "#62B1F6", "#AAAAFF", "#8470FF", "#AB82FF", "#A020F0", "#E066FF", "#DB70DB",
                                          "#FF00CC", "#FF34B3", "#FF0066", "#FF0033", "#FF030D", };

  }
  
}
