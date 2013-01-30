/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import fll.db.Queries;
import fll.web.playoff.BracketData;
import fll.web.playoff.BracketData.TeamBracketCell;
import fll.web.playoff.DatabaseTeamScore;
import fll.web.playoff.Playoff;
import fll.web.playoff.TeamScore;
import fll.xml.PerformanceScoreCategory;

/**
 * Methods used to generate or interpret JSON
 * 
 * @author jjkoletar
 */
public final class JsonUtilities {
  private JsonUtilities() {
  }

  public static class BracketLeafResultSet {
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "Read in the javascript")
    public final TeamBracketCell leaf;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "Read in the javascript")
    public final double score;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "Read in the javascript")
    public final String originator;

    public BracketLeafResultSet(final TeamBracketCell tbc,
                                final double scr,
                                final String origin) {
      leaf = tbc;
      score = scr;
      originator = origin;
    }

    public BracketLeafResultSet() { // Null constructor to have some sort of
                                    // item in null lists
      score = -1.0;
      leaf = null;
      originator = null;
    }
  }

  public static String generateJsonBracketInfo(final Map<Integer, Integer> ids,
                                               final Connection connection,
                                               final PerformanceScoreCategory perf,
                                               final BracketData bracketData,
                                               final boolean showOnlyVerifiedScores,
                                               final boolean showFinalsScores) {
    List<BracketLeafResultSet> datalist = new LinkedList<BracketLeafResultSet>();
    try {
      final int currentTournament = Queries.getCurrentTournament(connection);
      for (Map.Entry<Integer, Integer> entry : ids.entrySet()) {
        final int playoffRound = entry.getValue();
        final TeamBracketCell tbc = (TeamBracketCell) bracketData.getData(playoffRound, entry.getKey());
        if (tbc == null) {
          return "{\"refresh\":\"true\"}";
        }
        final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection);
        final int teamNumber = tbc.getTeam().getTeamNumber();
        final int runNumber = Playoff.getRunNumber(connection, teamNumber, playoffRound);
        final TeamScore teamScore = new DatabaseTeamScore("Performance", currentTournament, teamNumber, runNumber,
                                                          connection);
        final double computedTeamScore = perf.evaluate(teamScore);
        final boolean realScore = !Double.isNaN(computedTeamScore);
        final boolean noShow = Queries.isNoShow(connection, currentTournament, tbc.getTeam().getTeamNumber(), runNumber);
        // Sane request checks
        if (noShow) {
          datalist.add(new BracketLeafResultSet(tbc, -2.0, entry.getKey()
              + "-" + playoffRound));
        } else if (!realScore
            || !showOnlyVerifiedScores
            || Queries.isVerified(connection, currentTournament, teamNumber,
                                  Queries.getNumSeedingRounds(connection, currentTournament)
                                      + entry.getValue())) {
          if ((entry.getValue() == numPlayoffRounds && !showFinalsScores)
              || !realScore) {
            datalist.add(new BracketLeafResultSet(tbc, -1.0, entry.getKey()
                + "-" + entry.getValue()));
          } else {
            datalist.add(new BracketLeafResultSet(tbc, computedTeamScore, entry.getKey()
                + "-" + entry.getValue()));
          }
        }
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
    if (datalist.size() == 0) {
      // Add some data, so gson's happy.
      datalist.add(new BracketLeafResultSet());
    }
    Gson gson = new Gson();
    return gson.toJson(datalist);
  }

  public static class DisplayResponse {
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "Read in the javascript")
    public final String displayURL;

    public DisplayResponse(final String displayURL) {
      this.displayURL = displayURL;
    }
  }

  public static String generateDisplayResponse(final String displayURL) {
    return new Gson().toJson(new DisplayResponse(displayURL));
  }
}
