/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import fll.Queries;
import fll.Team;
import fll.Utilities;

import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Generate some XML documents.
 * 
 * @version $Revision$
 */
public final class XMLUtils {

  private static final Logger LOG = Logger.getLogger(XMLUtils.class);

  private XMLUtils() {
  }

  /**
   * Create an XML document that represents the data in teams
   */
  public static Document createTeamsDocument(final Connection connection, final Collection teams) throws SQLException {
    final Document document = DOCUMENT_BUILDER.newDocument();
    final Element top = document.createElement("teams");
    document.appendChild(top);
    final Iterator iter = teams.iterator();
    while (iter.hasNext()) {
      final Team team = (Team) iter.next();
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
   * @param challengeDocument
   *          describes the tournament
   * @param teams
   *          the teams for this tournament
   * @param connection
   *          the database connection used to retrieve the judge information
   * @param tournament
   *          the tournament to generate the document for, used for deciding
   *          which set of judges to use
   * @return the document
   */
  public static Document createSubjectiveScoresDocument(final Document challengeDocument, final Collection<Team> teams, final Connection connection,
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

      final NodeList subjectiveCategories = challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");
      for (int cat = 0; cat < subjectiveCategories.getLength(); cat++) {
        final Element categoryDescription = (Element) subjectiveCategories.item(cat);
        final String categoryName = categoryDescription.getAttribute("name");
        final Element categoryElement = document.createElement(categoryName);
        top.appendChild(categoryElement);

        prep.setString(1, categoryName);
        rs = prep.executeQuery();
        while (rs.next()) {
          final String judge = rs.getString(1);
          final String division = rs.getString(2);

          final Iterator teamIter = teams.iterator();
          while (teamIter.hasNext()) {
            final Team team = (Team) teamIter.next();
            final String teamDiv = Queries.getEventDivision(connection, team.getTeamNumber());
            if ("All".equals(division) || division.equals(teamDiv)) {
              final Element scoreElement = document.createElement("score");
              categoryElement.appendChild(scoreElement);

              scoreElement.setAttribute("teamName", team.getTeamName());
              scoreElement.setAttribute("teamNumber", String.valueOf(team.getTeamNumber()));
              scoreElement.setAttribute("division", teamDiv);
              scoreElement.setAttribute("organization", team.getOrganization());
              scoreElement.setAttribute("judge", judge);
              scoreElement.setAttribute("NoShow", "false");

              prep2 = connection.prepareStatement("SELECT * FROM " + categoryName + " WHERE TeamNumber = ? AND Tournament = ? AND Judge = ?");
              prep2.setInt(1, team.getTeamNumber());
              prep2.setString(2, currentTournament);
              prep2.setString(3, judge);
              rs2 = prep2.executeQuery();
              if (rs2.next()) {
                final NodeList goals = categoryDescription.getElementsByTagName("goal");
                for (int i = 0; i < goals.getLength(); i++) {
                  final Element goalDescription = (Element) goals.item(i);
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
      Utilities.closeResultSet(rs);
      Utilities.closeResultSet(rs2);
      Utilities.closePreparedStatement(prep);
      Utilities.closePreparedStatement(prep2);
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
   * @param challengeDocument
   *          the document to look in
   * @param name
   *          the name to look for
   * @return the element or null if one is not found
   */
  public static Element getSubjectiveCategoryByName(final Document challengeDocument, final String name) {
    final NodeList subjectiveCategories = challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");
    for (int categoryIndex = 0; categoryIndex < subjectiveCategories.getLength(); categoryIndex++) {
      final Element categoryElement = (Element) subjectiveCategories.item(categoryIndex);
      final String categoryName = categoryElement.getAttribute("name");
      if (categoryName.equals(name)) {
        return categoryElement;
      }
    }
    return null;
  }

}
