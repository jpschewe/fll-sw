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
import java.sql.Statement;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
public class XMLUtils {

  /**
   * Just for debugging.
   *
   * @param args ignored
   */
  public static void main(String args[]) {
    try {
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge.xml"));

      if(null == challengeDocument) {
        throw new RuntimeException("Error parsing challenge.xml");
      }
      final Connection connection = Utilities.createDBConnection("disk");
      final Map teams = Queries.getTournamentTeams(connection);
      //final Document document = createTeamsDocument(teams.values());
      final Document document = createSubjectiveScoresDocument(challengeDocument, teams.values(), connection, "STATE");
      final XMLWriter xmlwriter = new XMLWriter();
      xmlwriter.setOutput(System.out, null);
      xmlwriter.write(document);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  private XMLUtils() {
  }

  /**
   * Create an XML document that represents the data in teams
   */
  public static Document createTeamsDocument(final Collection teams) {
    final Document document = _documentBuilder.newDocument();
    final Element top = document.createElement("teams");
    document.appendChild(top);
    final Iterator iter = teams.iterator();
    while(iter.hasNext()) {
      final Team team = (Team)iter.next();
      final Element teamElement = document.createElement("team");
      teamElement.setAttribute("teamName", team.getTeamName());
      teamElement.setAttribute("teamNumber", String.valueOf(team.getTeamNumber()));
      teamElement.setAttribute("division", String.valueOf(team.getDivision()));
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
   * @param connection the database connection used to retrieve the judge information
   * @param tournament the tournament to generate the document for, used for
   * deciding which set of judges to use
   * @return the document
   */
  public static Document createSubjectiveScoresDocument(final Document challengeDocument,
                                                        final Collection teams,
                                                        final Connection connection,
                                                        final String tournament)
    throws SQLException {
    ResultSet rs = null;
    ResultSet rs2 = null;
    PreparedStatement prep = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      prep = connection.prepareStatement("SELECT id, Division FROM Judges WHERE category = ? AND Tournament = '" + tournament + "'");

      final String currentTournament = Queries.getCurrentTournament(connection);
      
      final Document document = _documentBuilder.newDocument();
      final Element top = document.createElement("scores");
      document.appendChild(top);
    
      final NodeList subjectiveCategories = challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
        final Element categoryDescription = (Element)subjectiveCategories.item(cat);
        final String categoryName = categoryDescription.getAttribute("name");
        final Element categoryElement = document.createElement(categoryName);
        top.appendChild(categoryElement);

        prep.setString(1, categoryName);
        rs = prep.executeQuery();
        while(rs.next()) {
          final String judge = rs.getString(1);
          final String division = rs.getString(2);
          
          final Iterator teamIter = teams.iterator();
          while(teamIter.hasNext()) {
            final Team team = (Team)teamIter.next();
            //HACK when we make divisions be strings this will go away
            final String teamDiv = String.valueOf(team.getDivision());
            if("All".equals(division) || division.equals(teamDiv)) {
              final Element scoreElement = document.createElement("score");
              categoryElement.appendChild(scoreElement);
            
              scoreElement.setAttribute("teamName", team.getTeamName());
              scoreElement.setAttribute("teamNumber", String.valueOf(team.getTeamNumber()));
              scoreElement.setAttribute("division", String.valueOf(team.getDivision()));
              scoreElement.setAttribute("organization", team.getOrganization());
              scoreElement.setAttribute("judge", judge);
            
              rs2 = stmt.executeQuery("SELECT * FROM " + categoryName + " WHERE TeamNumber = " + team.getTeamNumber() + " AND Tournament = \"" + currentTournament + "\" AND Judge = \"" + judge + "\"");
              if(rs2.next()) {
                final NodeList goals = categoryDescription.getElementsByTagName("goal");
                for(int i=0; i<goals.getLength(); i++) {
                  final Element goalDescription = (Element)goals.item(i);
                  final String goalName = goalDescription.getAttribute("name");
                  final String value = rs2.getString(goalName);
                  if(!rs2.wasNull()) {
                    scoreElement.setAttribute(goalName, value);
                  } 
                }
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
    }
  }
  
    
  private static final DocumentBuilder _documentBuilder;

  //create basic document builder
  static  {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      _documentBuilder = factory.newDocumentBuilder();
      _documentBuilder.setErrorHandler(new ErrorHandler() {
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
    } catch(final ParserConfigurationException pce) {
      throw new RuntimeException(pce.getMessage());
    }
  }

  /**
   * Parse xmlDoc an XML document.  Just does basic parsing, no validity
   * checks.
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
    } catch(final ParserConfigurationException pce) {
      throw new RuntimeException(pce.getMessage());
    } catch(final SAXException se) {
      throw new RuntimeException(se.getMessage());
    } catch(final IOException ioe) {
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
  public static Element getSubjectiveCategoryByName(final Document challengeDocument,
                                                    final String name) {
    final NodeList subjectiveCategories = challengeDocument.getDocumentElement().getElementsByTagName("subjectiveCategory");
    for(int categoryIndex=0; categoryIndex<subjectiveCategories.getLength(); categoryIndex++) {
      final Element categoryElement = (Element)subjectiveCategories.item(categoryIndex);
      final String categoryName = categoryElement.getAttribute("name");
      if(categoryName.equals(name)) {
        return categoryElement;
      }
    }
    return null;
  }
  
}
