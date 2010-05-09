/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.GlobalParameters;

/**
 * Push an XML document into the database without recreating the database. This
 * can be somewhat dangerous and is mostly here for debugging.
 */
public final class ImportDocument {

  private static final Logger LOGGER = Logger.getLogger(ImportDocument.class);

  /**
   * Insert the specified document into the specified database.
   * <ul>
   * <li>arg[0] - the location of the database (eg. ./flldb)
   * <li>arg[1] - the location of the document to insert in the database
   * </ul>
   */
  public static void main(final String[] args) throws IOException {
    if (args.length < 2) {
      LOGGER.fatal("Usage ImportDocument <xml file> <db>");
      System.exit(1);
    }
    final File challengeFile = new File(args[0]);
    if (!challengeFile.exists()) {
      LOGGER.fatal(challengeFile.getAbsolutePath()
          + " doesn't exist");
      System.exit(1);
    }
    if (!challengeFile.canRead()) {
      LOGGER.fatal(challengeFile.getAbsolutePath()
          + " is not readable");
      System.exit(1);
    }
    if (!challengeFile.isFile()) {
      LOGGER.fatal(challengeFile.getAbsolutePath()
          + " is not a file");
      System.exit(1);
    }
    final Document document = ChallengeParser.parse(new FileReader(challengeFile));
    if (null == document) {
      LOGGER.fatal("Error parsing challenge descriptor");
      System.exit(1);
    }

    if (!Utilities.testHSQLDB(args[1])) {
      LOGGER.fatal("There is a problem with the database, see previous log messages");
      System.exit(1);
    }
    PreparedStatement prep = null;
    Connection connection = null;
    int exitCode = 0;
    try {
      connection = Utilities.createDataSource(args[1]).getConnection();
      prep = connection.prepareStatement("UPDATE global_parameters SET Value = ? WHERE Param = ?");
      prep.setString(2, GlobalParameters.CHALLENGE_DOCUMENT);

      // dump the document into a byte array so we can push it into the database
      final XMLWriter xmlwriter = new XMLWriter();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xmlwriter.setOutput(baos, "UTF8");
      xmlwriter.write(document);
      final byte[] bytes = baos.toByteArray();
      final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      prep.setAsciiStream(1, bais, bytes.length);
      prep.executeUpdate();
    } catch (final UnsupportedEncodingException uee) {
      LOGGER.fatal("UTF8 not a supported encoding???", uee);
      exitCode = 1;
    } catch (final SQLException sqle) {
      LOGGER.fatal("Error talking to database", sqle);
      exitCode = 1;
    } finally {
      SQLFunctions.closePreparedStatement(prep);
      SQLFunctions.closeConnection(connection);
    }
    LOGGER.info("Inserted document "
        + args[0] + " into database " + args[1]);
    System.exit(exitCode);
  }

  private ImportDocument() {
    // no instances
  }

  /**
   * If the new document
   * differs from the current document in a way that the database structure will be
   * modified.
   * 
   * @param curDoc the current document
   * @param newDoc the document to check against
   * @return null if everything checks out OK, otherwise the error message
   */
  public static String compareStructure(final Document curDoc,
                                       final Document newDoc)  {
    final Element curDocRoot = curDoc.getDocumentElement();
    final Element newDocRoot = newDoc.getDocumentElement();
    final Element curPerfElement = (Element) curDocRoot.getElementsByTagName("Performance").item(0);
    final Element newPerfElement = (Element) newDocRoot.getElementsByTagName("Performance").item(0);

    final Map<String, String> curPerGoals = gatherColumnDefinitions(curPerfElement);
    final Map<String, String> newPerGoals = gatherColumnDefinitions(newPerfElement);
    if (curPerGoals.size() != newPerGoals.size()) {
      return "New document has "
          + newPerGoals.size() + " performance goals, current document has " + curPerGoals.size() + " performance goals";
    }
    
//    final List<Element> curSubCats = XMLUtils.filterToElements(curRootElement.getElementsByTagName("subjectiveCategory"));
//    final List<Element> newSubCats = XMLUtils.filterToElements(newRootElement.getElementsByTagName("subjectiveCategory"));
//    if(curSubCats.size() != newSubCats.size()) {
//      return "New document has "
//                  + newSubCats.size() + " subjective categories, current document has " + curSubCats.size() + " subjective categories";
//    }
    
    return null;
  }

  /**
   * Get the column definitions for all goals in the specified element
   */
  private static Map<String, String> gatherColumnDefinitions(final Element element) {
    final Map<String, String> goalDefs = new HashMap<String, String>();
    
    for (final Element goal : XMLUtils.filterToElements(element.getElementsByTagName("goal"))) {
      final String columnDefinition = GenerateDB.generateGoalColumnDefinition(goal);
      final String goalName = goal.getAttribute("name");
      goalDefs.put(goalName, columnDefinition);
    }

    return goalDefs;
  }

}
