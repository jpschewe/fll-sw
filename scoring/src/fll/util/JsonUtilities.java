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

import org.apache.log4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.GenerateDB;
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

  private static final Logger LOGGER = LogUtils.getLogger();

  private JsonUtilities() {
  }

  public static class BracketLeafResultSet {
    @SuppressFBWarnings(value = { "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "Read in the javascript")
    public TeamBracketCell leaf;

    public TeamBracketCell getLeaf() {
      return this.leaf;
    }

    public void setLeaf(final TeamBracketCell leaf) {
      this.leaf = leaf;
    }

    @SuppressFBWarnings(value = { "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "Read in the javascript")
    public double score;

    public double getScore() {
      return score;
    }

    public void setScore(final double score) {
      this.score = score;
    }

    @SuppressFBWarnings(value = { "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" }, justification = "Read in the javascript")
    public String originator;

    public String getOriginator() {
      return originator;
    }

    public void setOriginator(final String v) {
      this.originator = v;
    }

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

  /**
   * @return the list of bracket leaves or null if a refresh is required
   */
  public static List<BracketLeafResultSet> generateJsonBracketInfo(final String division,
                                                                   final Map<Integer, Integer> ids,
                                                                   final int bracketIdx,
                                                                   final Connection connection,
                                                                   final PerformanceScoreCategory perf,
                                                                   final BracketData bracketData,
                                                                   final boolean showOnlyVerifiedScores,
                                                                   final boolean showFinalsScores) {
    final List<BracketLeafResultSet> datalist = new LinkedList<BracketLeafResultSet>();
    try {
      final int currentTournament = Queries.getCurrentTournament(connection);
      for (Map.Entry<Integer, Integer> entry : ids.entrySet()) {
        final int dbLine = entry.getKey();
        final int playoffRound = entry.getValue();

        final int row = bracketData.getRowNumberForLine(playoffRound, dbLine);

        final TeamBracketCell tbc = (TeamBracketCell) bracketData.getData(playoffRound, row);
        if (tbc == null) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Didn't find data for round: %d row: %d, returning null to force refresh",
                                       playoffRound, row));
          }
          return null;
        }
        final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection);
        final int teamNumber = tbc.getTeam().getTeamNumber();
        final int runNumber = Playoff.getRunNumber(connection, division, teamNumber, playoffRound);
        final TeamScore teamScore = new DatabaseTeamScore(GenerateDB.PERFORMANCE_TABLE_NAME, currentTournament,
                                                          teamNumber, runNumber, connection);
        final double computedTeamScore = perf.evaluate(teamScore);
        final boolean realScore = !Double.isNaN(computedTeamScore);
        final boolean noShow = Queries.isNoShow(connection, currentTournament, tbc.getTeam().getTeamNumber(),
                                                runNumber);
        // Sane request checks
        final String leafId = BracketData.constructLeafId(bracketIdx, dbLine, playoffRound);
        if (noShow) {
          datalist.add(new BracketLeafResultSet(tbc, -2.0, leafId));
        } else if (!realScore
            || !showOnlyVerifiedScores || Queries.isVerified(connection, currentTournament, teamNumber, runNumber)) {
          if ((playoffRound == numPlayoffRounds
              && !showFinalsScores)
              || !realScore) {
            datalist.add(new BracketLeafResultSet(tbc, -1.0, leafId));
          } else {
            datalist.add(new BracketLeafResultSet(tbc, computedTeamScore, leafId));
          }
        }
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
    if (datalist.size() == 0) {
      // Add some data, JSON is happy
      datalist.add(new BracketLeafResultSet());
    }
    return datalist;
  }

}
