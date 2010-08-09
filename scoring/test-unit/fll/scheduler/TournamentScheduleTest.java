/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import junit.framework.Assert;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.junit.Test;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.xml.ChallengeParser;

/**
 * Tests for {@link TournamentSchedule}.
 */
public class TournamentScheduleTest {

  @Test
  public void testForNoSchedule() throws SQLException, UnsupportedEncodingException {
    Utilities.loadDBDriver();
    
    final String url = "jdbc:hsqldb:mem:ut_ts_test_no1";
    Connection memConnection = null;
    try {
      final InputStream stream = TournamentScheduleTest.class.getResourceAsStream("/fll/db/data/challenge-test.xml");
      Assert.assertNotNull(stream);
      final Document document = ChallengeParser.parse(new InputStreamReader(stream));

      memConnection = DriverManager.getConnection(url);
      
      GenerateDB.generateDB(document, memConnection, true);
      final boolean exists = Queries.scheduleExistsInDatabase(memConnection, 1);
      Assert.assertFalse(exists);
      
    } finally {
      SQLFunctions.close(memConnection);
      memConnection = null;
    }
  }

}
