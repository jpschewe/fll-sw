package fll.web.playoff;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;

import org.w3c.dom.Element;

import com.google.gson.Gson;

import fll.db.Queries;
import fll.web.playoff.BracketData;
import fll.web.playoff.BracketData.TeamBracketCell;
import fll.web.playoff.TeamScore;
import fll.web.playoff.DatabaseTeamScore;
import fll.util.ScoreUtils;

/**
 * Class to provide a more specific interface to the data needed to generate
 * JSON sent to the AJAX brackets, instead of mucking up BracketData too much.
 * 
 * @author jjkoletar
 */

public class JsonBracketData {

  private BracketData _bracketdata;

  private Gson _gson;

  private boolean _showFinalsScores = false;

  private boolean _showOnlyVerifiedScores = true;

  private BracketData getBracketData() {
    return _bracketdata;
  }

  public static class ForceJsRefresh {
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SS_SHOULD_BE_STATIC" }, justification = "Read in the javascript")
    public final boolean refresh = true;
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

  /**
   * Constructor taking in all we need, a BracketData instance to work with.
   */
  public JsonBracketData(final BracketData bd) {
    _bracketdata = bd;
    _gson = new Gson();
  }

  /**
   * Constructor taking two extra booleans that govern output.
   */
  public JsonBracketData(final BracketData bd,
                         final boolean showFinals,
                         final boolean showOnlyVerified) {
    this(bd);
    _showFinalsScores = showFinals;
    _showOnlyVerifiedScores = showOnlyVerified;
  }

  public String getBracketLocationJson(final int round,
                                       final int row) {
    if (round < this.getBracketData().getFirstRound()) {
      // Our instance of bracket data doesn't have that round, we better tell
      // the javascript to get its act together.
      return _gson.toJson(new ForceJsRefresh());
    }
    return _gson.toJson(this.getBracketData().getData(round, row));
  }

  /**
   * Constructs a bracket data object with playoff data from the database.
   * 
   * @param ids A map of key round and value row
   */
  public String getMultipleBracketLocationsJson(final Map<Integer, Integer> ids,
                                                final Connection connection,
                                                final Element performanceElement) throws SQLException, ParseException {
    List<BracketLeafResultSet> datalist = new LinkedList<BracketLeafResultSet>();
    try {
      final int currentTournament = Queries.getCurrentTournament(connection);
      for (Map.Entry<Integer, Integer> entry : ids.entrySet()) {
        final TeamBracketCell tbc = (TeamBracketCell) this.getBracketData().getData(entry.getValue(), entry.getKey());
        if (tbc == null) {
          return "{\"refresh\":\"true\"}";
        }
        final int numSeedingRounds = Queries.getNumSeedingRounds(connection, currentTournament);
        final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection); 
        final int teamNumber = tbc.getTeam().getTeamNumber();
        final TeamScore teamScore = new DatabaseTeamScore(performanceElement, currentTournament, teamNumber,
                                                          numSeedingRounds
                                                              + entry.getValue(), connection);
        final double computedTeamScore = ScoreUtils.computeTotalScore(teamScore);
        final boolean realScore = !Double.isNaN(computedTeamScore);
        final boolean noShow = Queries.isNoShow(connection, currentTournament, tbc.getTeam().getTeamNumber(),
                                                numSeedingRounds
                                                    + entry.getValue());
        // Sane request checks
        if (noShow) {
          datalist.add(new BracketLeafResultSet(tbc, -2.0, entry.getKey()
              + "-" + entry.getValue()));
        } else if (!realScore
            || !_showOnlyVerifiedScores
            || Queries.isVerified(connection, currentTournament, teamNumber,
                                  Queries.getNumSeedingRounds(connection, currentTournament)
                                      + entry.getValue())) {
          if ((entry.getValue() == numPlayoffRounds && !_showFinalsScores)
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
    } catch (final ParseException e) {
      throw new RuntimeException(e);
    }
    if (datalist.size() == 0) {
      // Add some data, so gson's happy.
      datalist.add(new BracketLeafResultSet());
    }
    return _gson.toJson(datalist);
  }

}
