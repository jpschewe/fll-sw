/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.JudgeInformation;
import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Get the information needed to edit judges and store it in the session.
 */
@WebServlet("/admin/GatherJudgeInformation")
public class GatherJudgeInformation extends BaseFLLServlet {

  /**
   * Key used to find the judges in the session. The type of the value is a
   * Collection of JudgeInformation.
   */
  public static final String JUDGES_KEY = "JUDGES";

  /**
   * Map of String (category db name) to String (category display name).
   */
  public static final String CATEGORIES_KEY = "CATEGORIES";

  /**
   * Collection of String with all stations that judges can be assigned to.
   */
  public static final String STATIONS_KEY = "JUDGING_STATIONS";

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * String used for all divisions when assigning judges.
   */
  public static final String ALL_DIVISIONS = "All";

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of GatherJudgeInformation.processRequest");
    }

    final StringBuilder message = new StringBuilder();
    final DataSource datasource = ApplicationAttributes.getDataSource();
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final int tournament = Queries.getCurrentTournament(connection);
      final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

      final List<Element> subjectiveCategories = new NodelistElementCollectionAdapter(
                                                                                      challengeDocument.getDocumentElement()
                                                                                                       .getElementsByTagName("subjectiveCategory")).asList();

      if (!TournamentSchedule.scheduleExistsInDatabase(connection, tournament)) {
        message.append("<p class='warning'>You have not loaded a schedule. If you intend to do so you should go back to the <a href='index.jsp'>admin page</a> and do this before assigning judges.</p>");
      }

      if (checkForEnteredSubjectiveScores(connection, subjectiveCategories, tournament)) {
        message.append("<p class='error'>Subjective scores have already been entered for this tournament, changing the judges may cause some scores to be deleted</p>");
      }

      session.setAttribute(JUDGES_KEY, gatherJudges(connection, tournament));

      session.setAttribute(STATIONS_KEY, gatherJudgingStations(connection, tournament));

      session.setAttribute(CATEGORIES_KEY, gatherCategories(subjectiveCategories));

      response.sendRedirect(response.encodeRedirectURL("judges.jsp"));

      session.setAttribute(SessionAttributes.MESSAGE, message.toString());

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }
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

  private List<String> gatherJudgingStations(final Connection connection,
                                             final int tournament) throws SQLException {
    final List<String> stations = Queries.getJudgingStations(connection, tournament);

    return stations;
  }

  private Map<String, String> gatherCategories(final List<Element> subjectiveCategories) {
    final Map<String, String> categories = new HashMap<String, String>();
    for (final Element element : subjectiveCategories) {
      final String categoryName = element.getAttribute("name");
      final String categoryTitle = element.getAttribute("title");
      categories.put(categoryName, categoryTitle);
    }
    return categories;
  }

  private Collection<JudgeInformation> gatherJudges(final Connection connection,
                                                    final int tournament) throws SQLException {
    Collection<JudgeInformation> judges = new LinkedList<JudgeInformation>();

    ResultSet rs = null;
    PreparedStatement stmt = null;
    try {
      stmt = connection.prepareStatement("SELECT id, category, station FROM Judges WHERE Tournament = ?");
      stmt.setInt(1, tournament);
      rs = stmt.executeQuery();
      while (rs.next()) {
        final String id = rs.getString(1);
        final String category = rs.getString(2);
        final String station = rs.getString(3);
        final JudgeInformation judge = new JudgeInformation(id, category, station);
        judges.add(judge);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }

    return judges;
  }

}
