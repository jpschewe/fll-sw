package fll.web.playoff;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import fll.Team;
import fll.util.LogUtils;
import fll.web.playoff.BracketData;

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
    
    /**
     * Constructor taking in all we need, a BracketData instance to work with.
     */
    public JsonBracketData (final BracketData bd) {
        _bracketdata = bd;
        _gson = new Gson();
    }
    
    public String getBracketLocationJson(final int round, final int row) {
        return _gson.toJson(this.getBracketData().getData(round, row));
    }
    
}
   