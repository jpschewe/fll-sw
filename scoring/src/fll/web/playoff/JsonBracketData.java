package fll.web.playoff;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import org.w3c.dom.Element;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fll.db.Queries;
import fll.Team;
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
   
    private BracketData getBracketData() {
        return _bracketdata;
    }
    
    public static class ForceJsRefresh {
        public Boolean refresh = true;
    }
    
    public static class BracketLeafResultSet {
        public BracketDataType leaf;
        
        public Double score;
        
        public BracketLeafResultSet(final BracketDataType bdt, final Double scr) {
            leaf = bdt;
            score = scr;
        }
    }
    
    
    /**
     * Constructor taking in all we need, a BracketData instance to work with.
     */
    public JsonBracketData (final BracketData bd) {
        _bracketdata = bd;
        _gson = new Gson();
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
   * @param ids A list of strings consisting of row, a dash (-) symbol, then round.
   */
    public String getMultipleBracketLocationsJson(final List<String> ids, DataSource datasource, final Element performanceElement) throws SQLException, ParseException {
        List<BracketLeafResultSet> datalist = new ArrayList<BracketLeafResultSet>();
        try {
            final Connection connection = datasource.getConnection();
            final int currentTournament = Queries.getCurrentTournament(connection);
            for (Iterator<String> i = ids.iterator(); i.hasNext(); ) {
                String item = i.next();
                String[] params = item.split("-");
                final BracketDataType bdt = this.getBracketData().getData(Integer.parseInt(params[1]), Integer.parseInt(params[0]));
                final TeamScore teamScore = new DatabaseTeamScore(performanceElement, currentTournament, ((TeamBracketCell) bdt).getTeam().getTeamNumber(), Queries.getNumSeedingRounds(connection, currentTournament)+Integer.parseInt(params[1]), connection);
                if (this.getBracketData().getData(Integer.parseInt(params[1]), Integer.parseInt(params[0])) != null) {
                    if (Double.isNaN(ScoreUtils.computeTotalScore(teamScore))) {
                        datalist.add(new BracketLeafResultSet(bdt, -1.0));
                    } else {
                        datalist.add(new BracketLeafResultSet(bdt, ScoreUtils.computeTotalScore(teamScore)));
                    }
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
        return _gson.toJson(datalist);
    }
    
}
   