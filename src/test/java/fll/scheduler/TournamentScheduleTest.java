/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.TestUtils;
import fll.Tournament;
import fll.TournamentLevel;
import fll.Utilities;
import fll.db.CategoryColumnMapping;
import fll.db.GenerateDB;
import fll.db.Queries;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Tests for {@link TournamentSchedule}.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class TournamentScheduleTest {

  /**
   * @throws SQLException test error
   * @throws UnsupportedEncodingException test error
   */
  @Test
  public void testForNoSchedule() throws SQLException, UnsupportedEncodingException {
    Utilities.loadDBDriver();

    final String url = "jdbc:hsqldb:mem:ut_ts_test_no1";
    Connection memConnection = null;
    try {
      final InputStream stream = TournamentScheduleTest.class.getResourceAsStream("/fll/db/data/challenge-test.xml");
      assertNotNull(stream);
      final ChallengeDescription description = ChallengeParser.parse(new InputStreamReader(stream,
                                                                                           Utilities.DEFAULT_CHARSET));

      memConnection = DriverManager.getConnection(url);

      GenerateDB.generateDB(description, memConnection);
      final boolean exists = TournamentSchedule.scheduleExistsInDatabase(memConnection, 1);
      assertFalse(exists);

    } finally {
      SQLFunctions.close(memConnection);
      memConnection = null;
    }
  }

  /**
   * Column information for 16-16-test.xls.
   */
  @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
  private TournamentSchedule.ColumnInformation createColumnInformation1616Test(final int headerRowIndex,
                                                                               final @Nullable String[] headerRow) {
    final int numRounds = 3;
    final String[] perfColumn = new String[numRounds];
    final String[] perfTableColumn = new String[numRounds];
    for (int round = 0; round < numRounds; ++round) {
      perfColumn[round] = String.format(TournamentSchedule.PERF_HEADER_FORMAT, round
          + 1);
      perfTableColumn[round] = String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round
          + 1);
    }

    final Collection<CategoryColumnMapping> subjectiveColumnMappings = new LinkedList<>();
    subjectiveColumnMappings.add(new CategoryColumnMapping("teamwork", "Presentation"));
    subjectiveColumnMappings.add(new CategoryColumnMapping("robustdesign", "Technical"));
    subjectiveColumnMappings.add(new CategoryColumnMapping("programming", "Technical"));
    subjectiveColumnMappings.add(new CategoryColumnMapping("research", "Presentation"));

    return new TournamentSchedule.ColumnInformation(headerRowIndex, headerRow, TournamentSchedule.TEAM_NUMBER_HEADER,
                                                    TournamentSchedule.ORGANIZATION_HEADER,
                                                    TournamentSchedule.TEAM_NAME_HEADER, "Div",
                                                    TournamentSchedule.JUDGE_GROUP_HEADER, subjectiveColumnMappings,
                                                    perfColumn, perfTableColumn, new String[0], new String[0]);
  }

  /**
   * @throws SQLException test error
   * @throws IOException test error
   * @throws InvalidFormatException test error
   * @throws ParseException test error
   * @throws ScheduleParseException test error
   */
  @Test
  public void testStoreSchedule()
      throws SQLException, IOException, InvalidFormatException, ParseException, ScheduleParseException {
    Utilities.loadDBDriver();

    final String tournamentName = "ut_ts_test_ss1";
    final String url = "jdbc:hsqldb:mem:ut_ts_test_ss1";
    final InputStream stream = TournamentScheduleTest.class.getResourceAsStream("/fll/db/data/challenge-test.xml");
    assertNotNull(stream);
    final ChallengeDescription description = ChallengeParser.parse(new InputStreamReader(stream,
                                                                                         Utilities.DEFAULT_CHARSET));

    try (Connection memConnection = DriverManager.getConnection(url)) {

      GenerateDB.generateDB(description, memConnection);

      Tournament.createTournament(memConnection, tournamentName, null, null,
                                  TournamentLevel.getByName(memConnection,
                                                            TournamentLevel.DEFAULT_TOURNAMENT_LEVEL_NAME));
      final Tournament tournament = Tournament.findTournamentByName(memConnection, tournamentName);
      assertNotNull(tournament);

      Queries.setCurrentTournament(memConnection, tournament.getTournamentID());

      final boolean existsBefore = TournamentSchedule.scheduleExistsInDatabase(memConnection, 1);
      assertFalse(existsBefore);

      // load teams into the database
      for (int teamNumber = 1; teamNumber <= 32; ++teamNumber) {
        final String dup = Queries.addTeam(memConnection, teamNumber, teamNumber
            + " Name", teamNumber
                + " School");
        assertNull(dup);
        Queries.addTeamToTournament(memConnection, teamNumber, tournament.getTournamentID(),
                                    GenerateDB.DEFAULT_TEAM_DIVISION, GenerateDB.DEFAULT_TEAM_DIVISION);
      }

      // load schedule with matching team numbers
      final URL scheduleResource = TournamentScheduleTest.class.getResource("data/16-16-test.xls");
      assertNotNull(scheduleResource);
      InputStream scheduleStream = scheduleResource.openStream();
      final List<String> sheetNames = ExcelCellReader.getAllSheetNames(scheduleStream);
      scheduleStream.close();
      assertEquals(1, sheetNames.size(), "Expecting exactly 1 sheet in schedule spreadsheet");

      final String sheetName = sheetNames.get(0);

      scheduleStream = scheduleResource.openStream();
      final CellFileReader reader = new ExcelCellReader(scheduleStream, sheetName);
      final int headerRowIndex = 2;
      reader.skipRows(headerRowIndex);
      final @Nullable String @Nullable [] headerRow = reader.readNext();
      assertNotNull(headerRow);

      final ColumnInformation columnInfo = createColumnInformation1616Test(headerRowIndex, headerRow);

      scheduleStream = scheduleResource.openStream();
      final TournamentSchedule schedule = new TournamentSchedule("Test Tournament",
                                                                 new ExcelCellReader(scheduleStream, sheetName),
                                                                 columnInfo);
      scheduleStream.close();

      schedule.storeSchedule(memConnection, tournament.getTournamentID());

      final boolean existsAfter = TournamentSchedule.scheduleExistsInDatabase(memConnection,
                                                                              tournament.getTournamentID());
      assertTrue(existsAfter, "Schedule should exist now that it's been stored");
    }
  }

  /**
   * Column information for 12-hour-format.xls and 24-hour-format.xls.
   */
  @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
  private TournamentSchedule.ColumnInformation createColumnInformationTimeFormatTest(final int headerRowIndex,
                                                                                     final @Nullable String[] headerRow) {
    final int numRounds = 3;
    final String[] perfColumn = new String[numRounds];
    final String[] perfTableColumn = new String[numRounds];
    for (int round = 0; round < numRounds; ++round) {
      perfColumn[round] = String.format(TournamentSchedule.PERF_HEADER_FORMAT, round
          + 1);
      perfTableColumn[round] = String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round
          + 1);
    }

    final Collection<CategoryColumnMapping> subjectiveColumnMappings = new LinkedList<>();
    subjectiveColumnMappings.add(new CategoryColumnMapping("teamwork", "Core Values"));
    subjectiveColumnMappings.add(new CategoryColumnMapping("robustdesign", "Design"));
    subjectiveColumnMappings.add(new CategoryColumnMapping("programming", "Design"));
    subjectiveColumnMappings.add(new CategoryColumnMapping("research", "Project"));

    return new TournamentSchedule.ColumnInformation(headerRowIndex, headerRow, TournamentSchedule.TEAM_NUMBER_HEADER,
                                                    TournamentSchedule.ORGANIZATION_HEADER,
                                                    TournamentSchedule.TEAM_NAME_HEADER, "Div",
                                                    TournamentSchedule.JUDGE_GROUP_HEADER, subjectiveColumnMappings,
                                                    perfColumn, perfTableColumn, new String[0], new String[0]);
  }

  /**
   * Test that we can parse a 12 hour formatted schedule
   * and a 24 hour formatted schedule and that they come
   * out the same.
   *
   * @throws IOException test error
   * @throws InvalidFormatException test error
   * @throws ScheduleParseException test error
   * @throws ParseException test error
   */
  @Test
  public void testScheduleTimeFormat()
      throws InvalidFormatException, IOException, ParseException, ScheduleParseException {

    final int headerRowIndex = 2;
    final String sheetName = "Sheet1";

    final URL schedule12Resource = TournamentScheduleTest.class.getResource("data/12-hour-format.xls");
    assertNotNull(schedule12Resource);

    final URL schedule24Resource = TournamentScheduleTest.class.getResource("data/24-hour-format.xls");
    assertNotNull(schedule24Resource);

    try (InputStream schedule12Stream = schedule12Resource.openStream();
        CellFileReader reader12 = new ExcelCellReader(schedule12Stream, sheetName)) {

      reader12.skipRows(headerRowIndex);
      final @Nullable String @Nullable [] headerRow12 = reader12.readNext();
      assertNotNull(headerRow12);

      final ColumnInformation columnInfo12 = createColumnInformationTimeFormatTest(headerRowIndex, headerRow12);

      final TournamentSchedule schedule12 = loadSchedule(schedule12Resource, columnInfo12);

      try (InputStream schedule24Stream = schedule24Resource.openStream()) {
        final CellFileReader reader = new ExcelCellReader(schedule24Stream, sheetName);

        reader.skipRows(headerRowIndex);
        final @Nullable String @Nullable [] headerRow24 = reader.readNext();
        assertNotNull(headerRow24);

        final ColumnInformation columnInfo24 = createColumnInformationTimeFormatTest(headerRowIndex, headerRow24);

        final TournamentSchedule schedule24 = loadSchedule(schedule24Resource, columnInfo24);

        // write out the schedules to CSV and then compare
        final StringWriter output12 = new StringWriter();
        schedule12.writeToCSV(output12);
        final String str12 = output12.toString();

        final StringWriter output24 = new StringWriter();
        schedule24.writeToCSV(output24);
        final String str24 = output24.toString();

        assertEquals(str24, str12);
      }
    }
  }

  private TournamentSchedule loadSchedule(final URL path,
                                          final TournamentSchedule.ColumnInformation columnInfo)
      throws IOException, InvalidFormatException, ParseException, ScheduleParseException {
    InputStream scheduleStream = path.openStream();
    final List<String> sheetNames = ExcelCellReader.getAllSheetNames(scheduleStream);
    scheduleStream.close();
    assertEquals(1, sheetNames.size(), "Expecting exactly 1 sheet in schedule spreadsheet");

    final String sheetName = sheetNames.get(0);

    scheduleStream = path.openStream();
    final TournamentSchedule schedule = new TournamentSchedule("Test Tournament",
                                                               new ExcelCellReader(scheduleStream, sheetName),
                                                               columnInfo);
    scheduleStream.close();

    return schedule;

  }

}
