/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;

import javax.servlet.jsp.JspWriter;

import java.text.NumberFormat;
import java.text.ParseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import fll.Utilities;

import fll.web.debug.DebugJspWriter;


/**
 * Java code behind uploading subjective scores
 *
 * @version $Revision$
 */
final public class UploadSubjectiveData {

  /**
   * Just for debugging
   *
   * @param args [0] - database host
   */
  public static void main(final String[] args) {
    try {
      final JspWriter out = new DebugJspWriter(new OutputStreamWriter(System.out));
      final File file = new File("/home/jpschewe/download/subjective-bad.zip");
      final String currentTournament = "NewHope";
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge.xml"));

      final Connection connection = Utilities.createDBConnection(args[0]);
      saveSubjectiveData(out, file, currentTournament, challengeDocument, connection);
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  private UploadSubjectiveData() {
     
  }

  /**
   * Save the data stored in file to the database.
   *
   * @param out where to write any output, mostly for debugging
   * @param file the file to read the data from
   * @param challengeDocument the already parsed challenge document.  Used to
   * get information about the subjective categories.
   * @param connection the database connection to write to
   */
  public static void saveSubjectiveData(final JspWriter out,
                                        final File file,
                                        final String currentTournament,
                                        final Document challengeDocument,
                                        final Connection connection)
    throws SQLException, IOException, ParseException {
    final ZipFile zipfile = new ZipFile(file);
    //read in score data
    final ZipEntry scoreZipEntry = zipfile.getEntry("score.xml");
    if(null == scoreZipEntry) {
      throw new RuntimeException("Zipfile does not contain score.xml as expected");
    }
    final InputStream scoreStream = zipfile.getInputStream(scoreZipEntry);
    final Document scoreDocument = XMLUtils.parseXMLDocument(scoreStream);
    scoreStream.close();
    zipfile.close();

    final Element scoresElement = scoreDocument.getDocumentElement();
    Element scoreCategoryElement = (Element)scoresElement.getFirstChild();
    while(null != scoreCategoryElement) {
      final String categoryName = scoreCategoryElement.getNodeName();
      final Element categoryElement = XMLUtils.getSubjectiveCategoryByName(challengeDocument, categoryName);
      final NodeList goalDescriptions = categoryElement.getElementsByTagName("goal");
      
      if(null == categoryElement) {
        throw new RuntimeException("Cannot find subjective category description for category in score document: " + categoryElement.getNodeName());
      } else {

        PreparedStatement insertPrep = null;
        try {
          //prepare statements for delete and insert
          final StringBuffer insertSQLColumns = new StringBuffer();
          insertSQLColumns.append("REPLACE " + categoryName + " (TeamNumber, Tournament, Judge");
          final StringBuffer insertSQLValues = new StringBuffer();
          insertSQLValues.append(") VALUES ( ?, ?, ?");
          for(int goalIndex=0; goalIndex<goalDescriptions.getLength(); goalIndex++) {
            final Element goalDescription = (Element)goalDescriptions.item(goalIndex);
            insertSQLColumns.append(", " + goalDescription.getAttribute("name"));
            insertSQLValues.append(", ?");
          }
          insertPrep = connection.prepareStatement(insertSQLColumns.toString()
                                                   + insertSQLValues.toString() + ")");
          //initialze the tournament
          insertPrep.setString(2, currentTournament);
            
          final NodeList scores = scoreCategoryElement.getElementsByTagName("score");
          for(int scoreIndex=0; scoreIndex<scores.getLength(); scoreIndex++) {
            final Element scoreElement = (Element)scores.item(scoreIndex);
            
            if(scoreElement.hasAttribute("modified")
               && "true".equalsIgnoreCase(scoreElement.getAttribute("modified"))) {
              final int teamNumber = NumberFormat.getInstance().parse(scoreElement.getAttribute("teamNumber")).intValue();
              final String judge = scoreElement.getAttribute("judge");

              insertPrep.setInt(1, teamNumber);
              insertPrep.setString(3, judge);
              
              for(int goalIndex=0; goalIndex<goalDescriptions.getLength(); goalIndex++) {
                final Element goalDescription = (Element)goalDescriptions.item(goalIndex);
                final String goalName = goalDescription.getAttribute("name");
                final String value = scoreElement.getAttribute(goalName);
                if(null != value && !"".equals(value.trim())) {
                  insertPrep.setString(goalIndex+4, value.trim());
                } else {
                  insertPrep.setNull(goalIndex+4, Types.INTEGER);
                }
              }
            
              //execute sql here
              insertPrep.executeUpdate();
            }
          }

        } finally {
          Utilities.closePreparedStatement(insertPrep);
        }
      }
      scoreCategoryElement = (Element)scoreCategoryElement.getNextSibling();
    }
  }

  
}
