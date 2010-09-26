/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.assign;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import net.mtu.eggplant.util.BasicFileFilter;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * 
 */
public class AssignTournaments {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @param args
   * @throws ParseException
   * @throws IOException
   */
  public static void main(final String[] args) throws IOException, ParseException {
    LogUtils.initializeLogging();

    // define the tournaments
    final Map<String, Map<String, TournamentInfo>> tournaments = new HashMap<String, Map<String, TournamentInfo>>();

    // div 1
    final Map<String, TournamentInfo> div1Tournaments = new HashMap<String, TournamentInfo>();
    TournamentInfo tournament = new TournamentInfo("11-21 Plymouth Middle", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-12 Benjamin E. Mays International Magnet", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-12 Rogers Middle", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-12 Sanford Middle", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-13 Benjamin E. Mays International Magnet", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-5 Crosswinds East Metro Arts & Science", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-5 Eagleview Community", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-5 North St. Paul High", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    // div1 only
    tournament = new TournamentInfo("12-5 St. Louis Park Junior High", "1", 10, 32);
    div1Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-6 North St. Paul High", "1", 10, 16);
    div1Tournaments.put(tournament.getName(), tournament);
    tournaments.put("1", div1Tournaments);

    // div 2
    final Map<String, TournamentInfo> div2Tournaments = new HashMap<String, TournamentInfo>();
    tournament = new TournamentInfo("11-21 Plymouth Middle", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-12 Benjamin E. Mays International Magnet", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-12 Rogers Middle", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-12 Sanford Middle", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-13 Benjamin E. Mays International Magnet", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-5 Crosswinds East Metro Arts & Science", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-5 Eagleview Community", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-5 North St. Paul High", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    // tournament = new TournamentInfo("12-5 St. Louis Park Junior High", "2",
    // 10, 16);
    // div2Tournaments.put(tournament.getName(), tournament);
    tournament = new TournamentInfo("12-6 North St. Paul High", "2", 10, 16);
    div2Tournaments.put(tournament.getName(), tournament);
    tournaments.put("2", div2Tournaments);

    final File file = chooseFile();
    if (null != file) {
      final AssignTournaments assign = new AssignTournaments(file, tournaments);
      final List<TeamInfo> allTeams = assign.loadTeamInformation();
      assign.schedule(allTeams);
      
      final File outputFile = new File("assignments.csv");
      assign.writeSchedule(outputFile);
      LOGGER.info("Wrote assignments to " + outputFile.getAbsolutePath());
    }

  }

  private static final Preferences PREFS = Preferences.userNodeForPackage(AssignTournaments.class);

  private static final String STARTING_DIRECTORY_PREF = "startingDirectory";

  private static File chooseFile() {
    final String startingDirectory = PREFS.get(STARTING_DIRECTORY_PREF, null);

    final JFileChooser fileChooser = new JFileChooser();
    final FileFilter filter = new BasicFileFilter("csv or directory", "csv");
    fileChooser.setFileFilter(filter);
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    if (null != startingDirectory) {
      fileChooser.setCurrentDirectory(new File(startingDirectory));
    }

    final int returnVal = fileChooser.showOpenDialog(null);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      final File currentDirectory = fileChooser.getCurrentDirectory();
      PREFS.put(STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());
      return fileChooser.getSelectedFile();
    } else {
      return null;
    }
  }

  private static String signupDateHeader = "Signup date";

  private static String teamNumHeader = "Team Number";

  private static String divisionHeader = "Division";

  private static String nameHeader = "Team Name";

  private static String pref1Header = "Tournament Preference 1";

  private static String pref2Header = "Tournament Preference 2";

  private static String pref3Header = "Tournament Preference 3";

  private final Map<String, Integer> columnAssignments = new HashMap<String, Integer>();

  private List<TeamInfo> loadTeamInformation() throws IOException, ParseException {
    LOGGER.info(new Formatter().format("Reading file %s", file.getAbsoluteFile()));

    if (!file.canRead()
        || !file.isFile()) {
      LOGGER.fatal("File is not readable or not a file: "
          + file.getAbsolutePath());
      return Collections.emptyList();
    }

    final CSVReader csvreader = new CSVReader(new FileReader(file));
    findColumns(csvreader);

    return parseData(csvreader);
  }

  private List<TeamInfo> parseData(final CSVReader csvReader) throws IOException, ParseException {
    final List<TeamInfo> teams = new LinkedList<TeamInfo>();
    String[] line = csvReader.readNext();
    while (null != line) {
      final TeamInfo team = parseLine(line);
      if (null != team) {
        teams.add(team);
      }
      line = csvReader.readNext();
    }

    return teams;
  }

  private TeamInfo parseLine(final String[] line) throws ParseException {
    final Date signupDate = parseSignupDate(line[columnAssignments.get(signupDateHeader)]);
    final int teamNumber = Integer.valueOf(line[columnAssignments.get(teamNumHeader)]);
    final String division = line[columnAssignments.get(divisionHeader)];
    final String name = line[columnAssignments.get(nameHeader)];
    final String pref1 = line[columnAssignments.get(pref1Header)];
    final String pref2 = line[columnAssignments.get(pref2Header)];
    final String pref3 = line[columnAssignments.get(pref3Header)];

    return new TeamInfo(signupDate, teamNumber, name, division, pref1, pref2, pref3);
  }

  private static Date parseSignupDate(final String str) throws ParseException {
    return SIGNUP_DATE_FORMAT.get().parse(str);
  }

  private static String formatSignupDate(final Date d) throws ParseException {
    return SIGNUP_DATE_FORMAT.get().format(d);
  }

  private static final ThreadLocal<DateFormat> SIGNUP_DATE_FORMAT = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("MM/dd/yy hh:mm a");
    }
  };

  private void initializeColumnAssignments() {
    columnAssignments.clear();
    columnAssignments.put(signupDateHeader, -1);
    columnAssignments.put(teamNumHeader, -1);
    columnAssignments.put(divisionHeader, -1);
    columnAssignments.put(nameHeader, -1);
    columnAssignments.put(pref1Header, -1);
    columnAssignments.put(pref2Header, -1);
    columnAssignments.put(pref3Header, -1);
  }

  /**
   * Find the index of the columns. If a column can't be found, output an error
   * and exit.
   * 
   * @throws IOException
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="DM_EXIT", justification="Method is documented to cause an exit")
  private void findColumns(final CSVReader csvreader) throws IOException {
    initializeColumnAssignments();

    while (columnAssignments.get(signupDateHeader).equals(-1)) {
      final String[] line = csvreader.readNext();
      if (null == line) {
        LOGGER.fatal("Cannot find header line and reached EOF");
        System.exit(1);
      }

      initializeColumnAssignments();

      for (int i = 0; i < line.length; ++i) {
        for (final Map.Entry<String, Integer> entry : columnAssignments.entrySet()) {
          if (line[i].equals(entry.getKey())) {
            entry.setValue(i);
          }
        }
      }
    }

    for (final Map.Entry<String, Integer> entry : columnAssignments.entrySet()) {
      if (entry.getValue().equals(-1)) {
        LOGGER.fatal("Could not find column for '"
            + entry.getKey() + "'");
        System.exit(1);
      }
    }
  }

  private final File file;

  private final Map<String, Map<String, TournamentInfo>> tournaments;

  public AssignTournaments(final File file, final Map<String, Map<String, TournamentInfo>> tournaments) {
    this.file = file;
    this.tournaments = tournaments;
  }

  public void writeSchedule(final File file) throws IOException, ParseException {
    final CSVWriter writer = new CSVWriter(new FileWriter(file));
    final String[] header = { "Tournament", "Team Name", "Team Number", "Division", "Registration Date", "Pref 1", "Pref 2", "Pref 3" };

    writer.writeNext(header);
    for (final Map<String, TournamentInfo> tournamentMap : tournaments.values()) {
      for (final TournamentInfo tournament : tournamentMap.values()) {
        final Set<TeamInfo> teams = tournament.getTeams();
        LOGGER.info(tournament.getName()
            + " div: " + tournament.getDivision() + " min: " + tournament.getMin() + " max: " + tournament.getMax() + " teams: " + teams.size());
        for(final TeamInfo team : teams) {
          String[] line = 
          {
           tournament.getName(),
           team.getName(),
           String.valueOf(team.getNumber()),
           team.getDivision(),
           formatSignupDate(team.getRegistrationDate()),
           team.getPref1(),
           team.getPref2(),
           team.getPref3()
          };
          writer.writeNext(line);
        }
      }
    }
    writer.close();

  }

  /**
   * Results are stored in the tournament objects.
   * 
   * @param allTeams all teams, sorted by registration date
   */
  public void schedule(final List<TeamInfo> allTeams) {
    final List<TeamInfo> unassigned = new LinkedList<TeamInfo>();
    for (final TeamInfo team : allTeams) {
      final Map<String, TournamentInfo> divTournaments = tournaments.get(team.getDivision());
      if (null == divTournaments) {
        throw new RuntimeException(String.format("No tournaments for division: '%s' team: %d", team.getDivision(), team.getNumber()));
      } else {
        if (!tryToAssignToTournament(team, divTournaments, team.getPref1())) {
          if (!tryToAssignToTournament(team, divTournaments, team.getPref2())) {
            if (!tryToAssignToTournament(team, divTournaments, team.getPref3())) {
              unassigned.add(team);
            }
          }
        }
      }
    }

    // assign teams that don't have a tournament
    for (final TeamInfo team : unassigned) {
      final Map<String, TournamentInfo> divTournaments = tournaments.get(team.getDivision());
      if (null == divTournaments) {
        throw new RuntimeException(String.format("No tournaments for division: %s team: %d", team.getDivision(), team.getNumber()));
      } else {
        boolean assigned = false;
        for (final TournamentInfo tournament : divTournaments.values()) {
          if (!tournament.isFull()) {
            tournament.addTeam(team);
            assigned = true;
          }
        }
        if (!assigned) {
          throw new RuntimeException(String.format("Unable to find any teams for team: %d - this is a programming error", team.getNumber()));
        }
      }
    }

  }

  private boolean tryToAssignToTournament(final TeamInfo team, final Map<String, TournamentInfo> divTournaments, final String pref) {
    final TournamentInfo preferredTournament = divTournaments.get(pref);
    if (null == preferredTournament) {
      LOGGER.warn(String.format("Unknown tournament '%s' team: %s div: %s", pref, team.getNumber(), team.getDivision()));
      return false;
    } else {
      if (!preferredTournament.isFull()) {
        preferredTournament.addTeam(team);
        return true;
      } else {
        return false;
      }
    }
  }
}
