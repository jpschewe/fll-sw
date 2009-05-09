/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fll.Team;
import fll.db.Queries;

/**
 * Generate some XML documents.
 * 
 * @version $Revision$
 */
public final class XMLUtils {

  private XMLUtils() {
  }

  /**
   * Create an XML document that represents the data in teams
   */
  public static Document createTeamsDocument(final Connection connection, final Collection<Team> teams) throws SQLException {
    final Document document = DOCUMENT_BUILDER.newDocument();
    final Element top = document.createElement("teams");
    document.appendChild(top);
    for (final Team team : teams) {
      final Element teamElement = document.createElement("team");
      teamElement.setAttribute("teamName", team.getTeamName());
      teamElement.setAttribute("teamNumber", String.valueOf(team.getTeamNumber()));
      teamElement.setAttribute("division", Queries.getEventDivision(connection, team.getTeamNumber()));
      teamElement.setAttribute("organization", team.getOrganization());
      teamElement.setAttribute("region", team.getRegion());
      top.appendChild(teamElement);
    }

    return document;
  }

  /**
   * Create a document to hold subject scores for the tournament described in
   * challengeDocument.
   * 
   * @param challengeDocument describes the tournament
   * @param teams the teams for this tournament
   * @param connection the database connection used to retrieve the judge
   *          information
   * @param tournament the tournament to generate the document for, used for
   *          deciding which set of judges to use
   * @return the document
   */
  public static Document createSubjectiveScoresDocument(final Document challengeDocument,
                                                        final Collection<Team> teams,
                                                        final Connection connection,
                                                        final String tournament) throws SQLException {
    ResultSet rs = null;
    ResultSet rs2 = null;
    PreparedStatement prep = null;
    PreparedStatement prep2 = null;
    try {
      prep = connection.prepareStatement("SELECT id, event_division FROM Judges WHERE category = ? AND Tournament = ?");
      prep.setString(2, tournament);

      final String currentTournament = Queries.getCurrentTournament(connection);

      final Document document = DOCUMENT_BUILDER.newDocument();
      final Element top = document.createElement("scores");
      document.appendChild(top);

      for (final Element categoryDescription : XMLUtils.filterToElements(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
        final String categoryName = categoryDescription.getAttribute("name");
        final Element categoryElement = document.createElement(categoryName);
        top.appendChild(categoryElement);

        prep.setString(1, categoryName);
        rs = prep.executeQuery();
        while (rs.next()) {
          final String judge = rs.getString(1);
          final String division = rs.getString(2);

          for (final Team team : teams) {
            final String teamDiv = Queries.getEventDivision(connection, team.getTeamNumber());
            if ("All".equals(division)
                || division.equals(teamDiv)) {
              final Element scoreElement = document.createElement("score");
              categoryElement.appendChild(scoreElement);

              scoreElement.setAttribute("teamName", team.getTeamName());
              scoreElement.setAttribute("teamNumber", String.valueOf(team.getTeamNumber()));
              scoreElement.setAttribute("division", teamDiv);
              scoreElement.setAttribute("organization", team.getOrganization());
              scoreElement.setAttribute("judge", judge);
              scoreElement.setAttribute("NoShow", "false");

              prep2 = connection.prepareStatement("SELECT * FROM "
                  + categoryName + " WHERE TeamNumber = ? AND Tournament = ? AND Judge = ?");
              prep2.setInt(1, team.getTeamNumber());
              prep2.setString(2, currentTournament);
              prep2.setString(3, judge);
              rs2 = prep2.executeQuery();
              if (rs2.next()) {
                for (final Element goalDescription : XMLUtils.filterToElements(categoryDescription.getElementsByTagName("goal"))) {
                  final String goalName = goalDescription.getAttribute("name");
                  final String value = rs2.getString(goalName);
                  if (!rs2.wasNull()) {
                    scoreElement.setAttribute(goalName, value);
                  }
                }
                scoreElement.setAttribute("NoShow", rs2.getString("NoShow"));
              }
            }
          }
        }
      }
      return document;
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeResultSet(rs2);
      SQLFunctions.closePreparedStatement(prep);
      SQLFunctions.closePreparedStatement(prep2);
    }
  }

  private static final DocumentBuilder DOCUMENT_BUILDER;

  // create basic document builder
  static {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DOCUMENT_BUILDER = factory.newDocumentBuilder();
      DOCUMENT_BUILDER.setErrorHandler(new ErrorHandler() {
        public void error(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void fatalError(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void warning(final SAXParseException spe) throws SAXParseException {
          System.err.println(spe.getMessage());
        }
      });
    } catch (final ParserConfigurationException pce) {
      throw new RuntimeException(pce.getMessage());
    }
  }

  /**
   * Parse xmlDoc an XML document. Just does basic parsing, no validity checks.
   */
  public static Document parseXMLDocument(final InputStream xmlDocStream) {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setIgnoringComments(true);
      factory.setIgnoringElementContentWhitespace(true);

      final DocumentBuilder parser = factory.newDocumentBuilder();
      parser.setErrorHandler(new ErrorHandler() {
        public void error(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void fatalError(final SAXParseException spe) throws SAXParseException {
          throw spe;
        }

        public void warning(final SAXParseException spe) throws SAXParseException {
          System.err.println(spe.getMessage());
        }
      });

      final Document document = parser.parse(xmlDocStream);
      return document;
    } catch (final ParserConfigurationException pce) {
      throw new RuntimeException(pce.getMessage());
    } catch (final SAXException se) {
      throw new RuntimeException(se.getMessage());
    } catch (final IOException ioe) {
      throw new RuntimeException(ioe.getMessage());
    }
  }

  /**
   * Find a subjective category by name.
   * 
   * @param challengeDocument the document to look in
   * @param name the name to look for
   * @return the element or null if one is not found
   */
  public static Element getSubjectiveCategoryByName(final Document challengeDocument, final String name) {
    for (final Element categoryElement : XMLUtils.filterToElements(challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory"))) {
      final String categoryName = categoryElement.getAttribute("name");
      if (categoryName.equals(name)) {
        return categoryElement;
      }
    }
    return null;
  }

  /**
   * Check if an element describes an enumerated goal or not.
   * 
   * @param element the goal element
   * @return if the element represents an enumerated goal
   */
  public static boolean isEnumeratedGoal(final Element element) {
    if (!"goal".equals(element.getNodeName())) {
      // not a goal element
      return false;
    }

    final List<Element> values = XMLUtils.filterToElements(element.getElementsByTagName("value"));
    return values.size() > 0;
  }

  /**
   * Check if an element describes a computed goal or not.
   * 
   * @param element the goal element
   * @return if the element represents a computed goal
   */
  public static boolean isComputedGoal(final Element element) {
    return "computedGoal".equals(element.getNodeName());
  }

  /**
   * Filter the {@link NodeList} to only Elements.
   * 
   * @param nodelist cannot be null
   * @return the list of {@link Element}s
   */
  public static List<Element> filterToElements(final NodeList nodelist) {
    final List<Element> retval = new ArrayList<Element>(nodelist.getLength());
    for (int i = 0; i < nodelist.getLength(); ++i) {
      final Node node = nodelist.item(i);
      if (node instanceof Element) {
        retval.add((Element) node);
      }
    }
    return retval;
  }

  /**
   * Get the bracket sort type from the document. If the attribute doesn't
   * exist, then return {@link BracketSortType#SEEDING}.
   */
  public static BracketSortType getBracketSort(final Document challengeDocument) {
    final Element root = challengeDocument.getDocumentElement();
    if (root.hasAttribute("bracketSort")) {
      final String sortStr = root.getAttribute("bracketSort");
      final BracketSortType sort = Enum.valueOf(BracketSortType.class, sortStr);
      if (null == sort) {
        return BracketSortType.SEEDING;
      } else {
        return sort;
      }
    } else {
      return BracketSortType.SEEDING;
    }
  }

  /**
   * Get the winner criteria for the tournament.
   */
  public static WinnerType getWinnerCriteria(final Document challengeDocument) {
    final Element root = challengeDocument.getDocumentElement();
    return getWinnerCriteria(root);
  }

  /**
   * Get the winner criteria for a particular element.
   */
  public static WinnerType getWinnerCriteria(final Element element) {
    if (element.hasAttribute("winner")) {
      final String str = element.getAttribute("winner");
      final String sortStr;
      if (null != str) {
        sortStr = str.toUpperCase();
      } else {
        sortStr = "HIGH";
      }
      final WinnerType sort = Enum.valueOf(WinnerType.class, sortStr);
      if (null == sort) {
        return WinnerType.HIGH;
      } else {
        return sort;
      }
    } else {
      return WinnerType.HIGH;
    }
  }

  /**
   * Get the score type for a particular element.
   */
  public static ScoreType getScoreType(final Element element) {
    if (element.hasAttribute("scoreType")) {
      final String str = element.getAttribute("scoreType");
      final String sortStr;
      if (null != str) {
        sortStr = str.toUpperCase();
      } else {
        sortStr = "INTEGER";
      }
      final ScoreType sort = Enum.valueOf(ScoreType.class, sortStr);
      if (null == sort) {
        return ScoreType.INTEGER;
      } else {
        return sort;
      }
    } else {
      return ScoreType.INTEGER;
    }
  }

}
