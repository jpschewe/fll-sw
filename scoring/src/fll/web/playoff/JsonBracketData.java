package fll.web.playoff;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import org.w3c.dom.Element;

import com.google.gson.Gson;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.playoff.BracketData;
import fll.web.playoff.BracketData.BracketDataType;
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

  private static final Logger LOG = LogUtils.getLogger();

  private BracketData _bracketdata;

  private Gson _gson;

  private Boolean _showFinalsScores = false;

  private Boolean _showOnlyVerifiedScores = true;

  private BracketData getBracketData() {
    return _bracketdata;
  }

  public static class ForceJsRefresh {
    public Boolean refresh = true;
  }

  public static class BracketLeafResultSet {
    public BracketDataType leaf;

    public Double score;

    public String originator;

    public BracketLeafResultSet (final BracketDataType bdt, final Double scr, final String origin) {
      leaf = bdt;
      score = scr;
      originator = origin;
    }
    public BracketLeafResultSet () { //Null constructor to have some sort of item in null lists
      score = -1.0;
    }
  }


  /**
   * Constructor taking in all we need, a BracketData instance to work with.
   */
  public JsonBracketData (final BracketData bd) {
    _bracketdata = bd;
    _gson = new Gson();
  }

  /**
   * Constructor taking two extra booleans that govern output.
   */
  public JsonBracketData(final BracketData bd, final Boolean showFinals, final Boolean showOnlyVerified) {
    this(bd);
    _showFinalsScores = showFinals;
    _showOnlyVerifiedScores = showOnlyVerified;
  }


  public String getBracketLocationJson(final int round, final int row) {
    if (round < this.getBracketData().getFirstRound())
    {
      //Our instance of bracket data doesn't have that round, we better tell the javascript to get its act together.
      return _gson.toJson(new ForceJsRefresh());
    }
    return _gson.toJson(this.getBracketData().getData(round, row));
  }
  /**
   * Constructs a bracket data object with playoff data from the database.
   * 
   * @param ids A map of key round and value row
   */
  public String getMultipleBracketLocationsJson(final Map<Integer, Integer> ids, DataSource datasource, final Element performanceElement) throws SQLException, ParseException {
    List<BracketLeafResultSet> datalist = new LinkedList<BracketLeafResultSet>();
    try {
      final Connection connection = datasource.getConnection();
      final int currentTournament = Queries.getCurrentTournament(connection);
      for (Map.Entry<Integer, Integer> entry : ids.entrySet()) {
        final TeamBracketCell tbc = (TeamBracketCell) this.getBracketData().getData(entry.getValue(), entry.getKey());
        if (tbc == null) {
          throw new RuntimeException("Unable to retrieve data for identifier "+entry.getKey()+"-"+entry.getValue());
        }
        final int numSeedingRounds = Queries.getNumSeedingRounds(connection, currentTournament);
        final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection/*, tbc.getTeam().getDivision()*/); //Should pass division, but code was getting angry
        final int teamNumber = tbc.getTeam().getTeamNumber();
        final TeamScore teamScore = new DatabaseTeamScore(performanceElement, currentTournament, teamNumber, numSeedingRounds+entry.getValue(), connection);
        final double computedTeamScore = ScoreUtils.computeTotalScore(teamScore);
        final boolean realScore = !Double.isNaN(computedTeamScore);
        final boolean noShow = Queries.isNoShow(connection, currentTournament, tbc.getTeam().getTeamNumber(), numSeedingRounds+entry.getValue());
        //Sane request checks
        if (tbc != null) {
          if (noShow) {
            datalist.add(new BracketLeafResultSet(tbc, -2.0, entry.getKey()+"-"+entry.getValue()));
          } else if (!realScore
              || !_showOnlyVerifiedScores
              || Queries.isVerified(connection, currentTournament, teamNumber, Queries.getNumSeedingRounds(connection, currentTournament)+entry.getValue())) {
            if (entry.getValue() == numPlayoffRounds || !realScore) {
              datalist.add(new BracketLeafResultSet(tbc, -1.0, entry.getKey()+"-"+entry.getValue()));
            } else {
              datalist.add(new BracketLeafResultSet(tbc, computedTeamScore, entry.getKey()+"-"+entry.getValue()));
            }
          }
        }
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } catch (final ParseException e) {
      throw new RuntimeException(e);
    }
    if (datalist.size() == 0) {
      //Add some data, so gson's happy.
      datalist.add(new BracketLeafResultSet());
    }
    return _gson.toJson(datalist);
  }

}
