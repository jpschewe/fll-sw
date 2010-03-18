/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;

import org.junit.Test;

import com.thoughtworks.selenium.SeleneseTestCase;

import fll.db.GenerateDB;
import fll.web.InitializeDatabaseTest;

/**
 * Test editing the tournaments list 
 */
public class EditTournamentsTest extends SeleneseTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp("http://localhost:9080/setup");
  }
  
  @Test
  public void testAddTournament() throws IOException {
    selenium.open("http://localhost:9080/fll-sw/setup/");
    
    final InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml");
    InitializeDatabaseTest.initializeDatabase(selenium, challengeStream, true);
    
    selenium.click("Admin Index");
    selenium.waitForPageToLoad("60000");

    selenium.click("Edit Tournaments");
    selenium.waitForPageToLoad("60000");
    
    Assert.assertFalse("Should not have internal tournament listed", selenium.isTextPresent(GenerateDB.INTERNAL_TOURNAMENT_NAME));
    
    selenium.click("Add Row");
    selenium.waitForPageToLoad("60000");

//    selenium.

  }

}
