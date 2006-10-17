/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.meterware.httpunit.Button;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HTMLElement;
import com.meterware.httpunit.HTMLElementPredicate;
import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import fll.TestUtils;
import fll.Utilities;
import fll.xml.ChallengeParser;

/**
 * Do full tournament tests.
 * 
 * @version $Revision$
 */
public class FullTournamentTest extends TestCase {

  private static final Logger LOG = Logger.getLogger(FullTournamentTest.class);

  /**
   * Test a full tournament.
   */
  public void test0() throws MalformedURLException, IOException, SAXException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParseException,
      SQLException {

    final int numSeedingRounds = 3;
    
    // create connection to database with test data
    Class.forName("org.hsqldb.jdbcDriver").newInstance();
    Connection testDataConn = null;
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      testDataConn = DriverManager.getConnection("jdbc:hsqldb:res:/fll/web/data/flldb-ft");
      final String testTournament = "Field";
      Assert.assertNotNull("Error connecting to test data database", testDataConn);

      // --- initialize database ---
      final WebConversation conversation = new WebConversation();
      WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT + "setup/");
      WebResponse response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());

      WebForm form = response.getFormWithID("setup");
      Assert.assertNotNull(form);
      form.setCheckbox("force_rebuild", true); // rebuild the whole
      // database
      final InputStream challengeDocIS = FullTournamentTest.class.getResourceAsStream("data/challenge-ft.xml");
      Assert.assertNotNull(challengeDocIS);
      final UploadFileSpec challengeUpload = new UploadFileSpec("challenge-test.xml", challengeDocIS, "text/xml");
      form.setParameter("xmldocument", new UploadFileSpec[] { challengeUpload });
      request = form.getRequest("reinitializeDatabase");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull("Error initializing database: " + response.getText(), response.getElementWithID("success"));
      challengeDocIS.close();

      // --- load teams ---
      // find upload form on admin page
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithID("uploadTeams");

      // set the parameters
      final InputStream teamsIS = FullTournamentTest.class.getResourceAsStream("data/teams-ft.csv");
      Assert.assertNotNull(teamsIS);
      final UploadFileSpec teamsUpload = new UploadFileSpec("teams.csv", teamsIS, "text/plain");
      form.setParameter("file1", new UploadFileSpec[] { teamsUpload });
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      // Assert.assertNotNull("Error uploading data file: " +
      // response.getText(), response.getElementWithID("success"));
      teamsIS.close();

      // skip past the filter page
      form = response.getFormWithName("filterTeams");
      request = form.getRequest("next");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());

      // team column selection
      form = response.getFormWithName("verifyTeams");
      Assert.assertNotNull(form);
      form.setParameter("TeamNumber", "tea_number");
      form.setParameter("TeamName", "tea_name");
      form.setParameter("Organization", "org_name");
      form.setParameter("Region", "eve_name");
      form.setParameter("Division", "div_name");
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull("Error loading teams: " + response.getText(), response.getElementWithID("success"));

      // create tournaments for all regions
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/index.jsp");
      request.setParameter("addTournamentsForRegions", "1");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully added tournaments for regions"));

      // initialize tournaments by region
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/tournamentInitialization.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("form");
      Assert.assertNotNull(form);
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      form = response.getFormWithName("verify");
      Assert.assertNotNull(form);
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully initialized tournament for teams based on region."));

      // set the tournamet 
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/index.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithID("currentTournament");
      Assert.assertNotNull(form);
      form.setParameter("currentTournament", testTournament);
      request = form.getRequest();
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      Assert.assertNotNull("Error loading teams: " + response.getText(), response.getElementWithID("success"));
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Tournament changed to " + testTournament));
      
      // assign judges
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/judges.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("judges");
      Assert.assertNotNull(form);
      
      //TODO: need to add rows to form if test database has more judges than categories
      
      // assign judges from database
      int judgeIndex = 0;
      stmt = testDataConn.createStatement();
      rs = stmt.executeQuery("SELECT id, category, Division FROM Judges");
      while(rs.next()) {
        final String id = rs.getString(1);
        final String category = rs.getString(2);
        final String division = rs.getString(3);
        form.setParameter("id" + judgeIndex, id);
        form.setParameter("cat" + judgeIndex, category);
        form.setParameter("div" + judgeIndex, division);
        ++judgeIndex;
      }
      Utilities.closeResultSet(rs);
      
      // submit those values
      request = form.getRequest("submit", "Finished");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNull("Got error from judges assignment", response.getElementWithID("error"));

      // commit judges information
      form = response.getFormWithName("judges");
      Assert.assertNotNull(form);
      request = form.getRequest("submit", "Commit");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully assigned judges."));      

      
      // assign table labels
      request = new GetMethodWebRequest(TestUtils.URL_ROOT + "admin/tables.jsp");
      response = conversation.getResponse(request);
      Assert.assertTrue("Received non-HTML response from web server", response.isHTML());
      form = response.getFormWithName("tables");
      Assert.assertNotNull(form);
      form.setParameter("SideA0", "red");
      form.setParameter("SideB0", "blue");
      request = form.getRequest("submit", "Finished");
      response = conversation.getResponse(request);
      Assert.assertTrue(response.isHTML());
      Assert.assertNull("Got error from judges assignment", response.getElementWithID("error"));
      Assert.assertNotNull(response.getFirstMatchingTextBlock(MATCH_TEXT, "Successfully assigned tables."));      
      
      
      /*
       * --- Enter 3 runs for each team --- 
       * Use data from test data base, converted from Field 2005.
       * Enter 4th run and rest of playoffs
       */
      prep = testDataConn.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?");
      prep.setString(1, testTournament);
      rs = prep.executeQuery();
      Assert.assertTrue("No performance scores in test data", rs.next());
      final int maxRuns = rs.getInt(1);
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(prep);

      final Document challengeDocument = ChallengeParser.parse(FullTournamentTest.class.getResourceAsStream("data/challenge-ft.xml"));
      Assert.assertNotNull(challengeDocument);
      final Element rootElement = challengeDocument.getDocumentElement();
      final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
      
      prep = testDataConn.prepareStatement("SELECT * FROM Performance WHERE Tournament = ? AND RunNumber = ?");
      boolean initializedPlayoff = false;
      prep.setString(1, testTournament);
      // TODO should try this in parallel with threads
      for(int runNumber=1; runNumber<=maxRuns; ++runNumber) {
        if(!initializedPlayoff && runNumber > numSeedingRounds) {
          // TODO make sure to check the result of checking the seeding rounds
          
          // initialize the playoff brackets with playoff/index.jsp form
          request = new GetMethodWebRequest(TestUtils.URL_ROOT + "playoff");
          response = conversation.getResponse(request);
          Assert.assertTrue(response.isHTML());
          form = response.getFormWithName("initialize");
          Assert.assertNotNull(form);
          final String[] divisions = form.getOptionValues("division");
          for(int divIdx = 0; divIdx<divisions.length; ++divIdx) {
            if(LOG.isDebugEnabled()) {
              LOG.debug("Initializing playoff brackets for division " + divisions[divIdx]);
            }
            form.setParameter("division", divisions[divIdx]);
            request = form.getRequest();
            response = conversation.getResponse(request);
            Assert.assertTrue(response.isHTML());
          }
          initializedPlayoff = true;
        }
        
        prep.setInt(2, runNumber);
        rs = prep.executeQuery();
        
        while(rs.next()) {
          final int teamNumber = rs.getInt("TeamNumber");
          if(LOG.isDebugEnabled()) {
            LOG.debug("Setting score for " + teamNumber + " run: " + runNumber);
          }
          // need to get the score entry form
          request = new GetMethodWebRequest(TestUtils.URL_ROOT + "scoreEntry/select_team.jsp");
          response = conversation.getResponse(request);
          Assert.assertTrue(response.isHTML());
          form = response.getFormWithName("selectTeam");
          Assert.assertNotNull(form);
          form.setParameter("TeamNumber", String.valueOf(teamNumber));
          request = form.getRequest();
          response = conversation.getResponse(request);
          Assert.assertTrue(response.isHTML());
          
          form = response.getFormWithName("scoreEntry");
          
          if(rs.getBoolean("NoShow")) {
            form.setParameter("NoShow", "1");
          } else {
            // walk over challenge descriptor to get all element names and then use the values from rs
            final NodeList goals = performanceElement.getElementsByTagName("goal");
            for(int i=0; i<goals.getLength(); i++) {
              final Element element = (Element)goals.item(i);
              final String name = element.getAttribute("name");
              final int min = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("min")).intValue();
              final int max = Utilities.NUMBER_FORMAT_INSTANCE.parse(element.getAttribute("max")).intValue();
              if(LOG.isDebugEnabled()) {
                LOG.debug("Setting form parameter: " + name + " min: " + min + " max: " + max + " readonly: " + form.isReadOnlyParameter(name));
              }
              if(form.isReadOnlyParameter(name)) {
                final int value = rs.getInt(name);
                final int defaultValue = Utilities.NUMBER_FORMAT_INSTANCE.parse(form.getParameterValue(name)).intValue();
                final int difference = value - defaultValue;
                if(LOG.isDebugEnabled()) {
                  LOG.debug("Need to increment/decrement " + name + " " + difference);
                }
                final Button button;
                if(difference < 0) {
                  button = form.getButtonWithID("inc_" + name + "_1");
                } else {
                  button = form.getButtonWithID("dec_" + name + "_-1");
                }
                Assert.assertNotNull("Cannot find button for increment/decrement of " + name, button);
                for(int val=0; val<Math.abs(difference); ++val) {
                  button.click();
                }
              } else {
                final String value = rs.getString(name);
                form.setParameter(name, value);
              }
            }
          }
          
          // submit score
          request = form.getRequest("submit");
          response = conversation.getResponse(request);
          Assert.assertTrue(response.isHTML());
          Assert.assertEquals("Errors: " + response.getText(), 0, response.getElementsWithName("error").length);
        }
      }
      
      
      // final Connection connection = TestUtils.createDBConnection("fll", "fll");
      // Assert.assertNotNull("Could not create test database connection",
      // connection);
      // download subjective zip
      // insert scores into zip
      // upload scores
      // compute final scores
      // generate reports
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(prep);
      Utilities.closeConnection(testDataConn);
    }
  }

  private static final HTMLElementPredicate MATCH_TEXT = new HTMLElementPredicate() {
    public boolean matchesCriteria(final Object htmlElement, final Object criteria) {
      return ((HTMLElement) htmlElement).getText().equals(criteria);
    }
  };
}
