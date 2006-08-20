package fll.web.playoff;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import fll.Team;
import fll.Queries;
import fll.Utilities;

/**
 * Class to provide convenient access to the contents of the PlayoffData table.
 * A bracket data object contains all the playoff meta data for the current
 * tournament, in a specified division, for a given range of playoff
 * rounds.  This data can be thought of a sparse matrix containing entries
 * only where a team name would appear if the matrix was overlayed on an
 * elimination bracket. Then, by iterating over rounds and rows, the data
 * access method provides cell-by-cell output in either row-major or
 * column-major order, as you desire. By setting various options, you can
 * automatically insert things like bracket labels, table assignment
 * labels, table assignment selectors, etc. Of course, it is also possible
 * to simply request the data for a specific cell and handle it directly,
 * if desired.
 *
 * @author Dan Churchill
 * 
 */
public class BracketData {

  /**
   * Data type for brackets.
   */
  public static class BracketDataType { }

  /**
   * Team backet cells.
   */
  public static class TeamBracketCell extends BracketDataType {
    private Team _team;
    public Team getTeam() { return _team; }
    public void setTeam(final Team t) { _team = t; }
    private String _table;
    public String getTable() { return _table; }
    public void setTable(final String t) { _table = t; }
    private int _dbLine;
    public int getDBLine() { return _dbLine; }
    public void setDBLine(final int v) { _dbLine = v; }
  }

  /**
   * Cell for bracket labels.
   */
  public static class BracketLabelCell extends BracketDataType {
    public BracketLabelCell(final int num) {
      _label = "Bracket " + num;
    }
    public BracketLabelCell(final String lbl) {
      _label = lbl;
    }
    private final String _label;
    public String getLabel() { return _label; }
  }

  /**
   * This enumeration is used to determine how the top right corner cells of the
   * brackets meet. E.g. if brackets are drawn using colored backgrounds as they
   * are on the scrolling display, then the bridge style should be applied to
   * the cell just to the right of the top of the bracket. If it is drawn using
   * cell borders as in the administrative brackets, that same cell should
   * simply be an empty cell.
   */
  public static enum TopRightCornerStyle {
    MEET_TOP_OF_CELL(0),
    MEET_BOTTOM_OF_CELL(1);

    private int _moduloMinimum;
    /*package*/ TopRightCornerStyle(final int moduloMin) {
      _moduloMinimum = moduloMin;
    }
    public int getModuloMinimum() {
      return _moduloMinimum;
    }
  }

  // Map of round number to map of line number to playoff meta data for that
  // round number and line number.
  private Map<Integer, SortedMap<Integer, BracketDataType>> _bracketData;
  private int _firstRound;
  private int _lastRound;
  private int _firstRoundSize;
  private int _rowsPerTeam;
  private int _numSeedingRounds;
  private int _semiFinalsRound;
  private int _finalsRound;
  private boolean _showFinalScores;

  // No public default constructor available
  private BracketData() {}

  /**
   * Constructs a bracket data object with playoff data from the database.
   * 
   * @param connection
   *          Database connection to use.
   * @param division
   *          Divsion from which to look up playoff data.
   * @param firstRound
   *          The first playoff round of interest (1st playoff round is 1, not
   *          the number of seeding rounds + 1)
   * @param _lastRound
   *          The last playoff round of interest.
   * @param rowPerTeam
   *          A positive, even number defining how many rows will be allocated
   *          for each team in the first round. This determines overall spacing
   *          for the entire table. Recommended value: 4.
   * @throws SQLException
   */
  public BracketData(final Connection connection,
                     final String division,
                     final int pFirstRound,
                     final int pLastRound,
                     final int pRowsPerTeam)
  throws SQLException {
    super();
    if(pRowsPerTeam % 2 != 0 || pRowsPerTeam < 2) {
      throw new RuntimeException("Error building BracketData structure:" +
          " Illegal rows-per-team value specified." +
          " Value must be a multiple of 2 greater than 0.");
    }

    _rowsPerTeam = pRowsPerTeam;
    _firstRoundSize = Queries.getFirstPlayoffRoundSize(connection, division);
    _numSeedingRounds = Queries.getNumSeedingRounds(connection);
    
    _showFinalScores = true;
    
    if(pFirstRound < 1) {
      _firstRound = 1;
    } else {
      _firstRound = pFirstRound;
    }

    _lastRound = pLastRound;
    
    _finalsRound = Queries.getNumPlayoffRounds(connection, division);
    _semiFinalsRound = _finalsRound - 1;
    
    _bracketData = new TreeMap<Integer, SortedMap<Integer, BracketDataType>>();
    for(int i=_firstRound; i<=_lastRound; i++) {
      _bracketData.put(new Integer(i), new TreeMap<Integer, BracketDataType>());
    }

    final String tournament = Queries.getCurrentTournament(connection);

    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery(
          "SELECT PlayoffRound,LineNumber,Team,AssignedTable"
          + " FROM PlayoffData"
          + " WHERE Tournament='" + tournament + "'"
          + " AND Division='" + division + "'"
          + " AND PlayoffRound>=" + _firstRound
          + " AND PlayoffRound<=" + _lastRound);
      while(rs.next()) {
        final Integer round = new Integer(rs.getInt(1));
        final Integer line = new Integer(rs.getInt(2));
        final int team = rs.getInt(3);
        final String table = rs.getString(4);
        
        SortedMap<Integer, BracketDataType> roundData = _bracketData.get(round);
        
        final TeamBracketCell d = new TeamBracketCell();
        d.setTable(table);
        d.setTeam(Team.getTeamFromDatabase(connection, team));
        d.setDBLine(line);
        // Very brief explaination of the math:
        // Let y = target output row in the table (this is the key value of the inner Map elements of _bracketData)
        // Let x = the number of rows per team (variable '_rowsPerTeam') in the left-most column of the output table
        //  (Given: x is a positive, even value. The math would work for other values, but it's meaningless when we
        //   can't have fractional table rows...)
        // Let r = the round index (variable 'adjustedRound') starting at 0 in the left-most column
        // Let n = the row number (variable 'line') as obtained from the database: 1,2,3,...
        // Then y = n * x * 2^r - (x * 2^(r-1) + 0.5 * x - 1)
        // The rounding operation is purely to negate any possible floating point inaccuracies introduced by Math.pow, etc.
        int adjustedRound = round-_firstRound;
        final int row = (int)Math.round(line*_rowsPerTeam*(Math.pow(2, adjustedRound))
            - (_rowsPerTeam*Math.pow(2, adjustedRound-1) + 0.5*_rowsPerTeam - 1));
        System.out.print("Putting team " + d.getTeam() + " with dbLine " + d.getDBLine() + " to row " + row + " of output table\n");
        if(roundData.put(row, d) != null) {
          throw new RuntimeException("Error - Map keys were not unique - PlayoffData " +
              "might be inconsistent (you should verify that there are not multiple teams" +
              " occupying the same round and row for tournament:'" + tournament + "' and" +
              " division:'" + division + "')");
        }
      }
      
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }
  /**
   * Constructor including explicit show scores flag.
   * 
   * @param pShowFinals
   *          True if the final scores should be displayed (e.g. for the
   *          administrative brackets) or false if they should not (e.g. for the
   *          big screen display).
   * @throws SQLException
   */
  public BracketData(final Connection connection,
      final String division,
      final int pFirstRound,
      final int pLastRound,
      final int pRowsPerTeam,
      final boolean pShowFinals)
  throws SQLException {
    this(connection, division, pFirstRound, pLastRound, pRowsPerTeam);
    _showFinalScores = pShowFinals;
  }

  /**
   * Returns the playoff meta data for a specified location in the brackets.
   * 
   * @param col
   *          The column of the playoff data table, otherwise known as the round
   *          number.
   * @param row
   *          The row number to retrieve.
   * @return The BracketDataType for the given cell, or null if there is no data
   *         at that cell.
   */
  public BracketDataType getData(final int round, final int row) {
    return _bracketData.get(new Integer(round)).get(new Integer(row));
  }

  /**
   * Returns the number of rows in the specified round number.
   * 
   * @param round
   *          Playoff round number, starting at 1.
   * @return Row number of the last row in that round, or 0 if there are no rows
   *         in it.
   */
  public int getNumRows() {
    if(_firstRound == _semiFinalsRound && _lastRound >= _finalsRound) {
      final int sfr = _bracketData.get(new Integer(_semiFinalsRound)).lastKey().intValue();
      final int fr = _bracketData.get(new Integer(_finalsRound)).lastKey().intValue();
      return sfr > fr ? sfr : fr;
    }
    return _bracketData.get(new Integer(_firstRound)).lastKey().intValue();
  }
  
  public int getFirstRound() {
    return _firstRound;
  }
  
  public int getLastRound() {
    return _lastRound;
  }
  
  public int getFirstRoundSize() {
    return _firstRoundSize;
  }

  public int getRowsPerTeam() {
    return _rowsPerTeam;
  }

  /**
   * Formats the HTML code to insert for a single table cell of one of the
   * playoff bracket display pages (both administrative and scrolling). All
   * cells are generated with a specified width of 200 pixels. If the cell
   * contains text, the \<td\> element will have attribute class='Leaf'. Font
   * tags for team number, team name, and score have classes of 'TeamNumber',
   * 'TeamName', and 'TeamScore', respectively.
   * 
   * @param connection
   *          Database connection for looking up team scores, etc.
   * @param tournament
   *          The current tournament.
   * @param row
   *          Row number of the bracket data we are displaying.
   * @param round
   *          Round number (column) of data we are displaying.
   * @return Properly formed \<td\> element.
   * @throws SQLException
   *           If database access fails.
   */
  public String getHtmlCell(final Connection connection,
                            final String tournament,
                            final int row,
                            final int round)
  throws SQLException {
    final SortedMap<Integer, BracketDataType> roundData = _bracketData.get(new Integer(round));
    if(roundData == null) {
      return "<td>ERROR: No data for round " + round + ".</td>";
    }
    final StringBuffer sb = new StringBuffer();
    sb.append("<td width='200'");
    final BracketDataType d = roundData.get(new Integer(row));
    if(d == null) {
      sb.append(">&nbsp;");
    } else if(d instanceof TeamBracketCell) {
      sb.append(" class='Leaf'>");
      if(round == _finalsRound) {
        sb.append(getDisplayString(connection, tournament,
                                   round+_numSeedingRounds, ((TeamBracketCell)d).getTeam(), _showFinalScores));
      } else if(round != _finalsRound+1) {
        sb.append(getDisplayString(connection, tournament,
                                   round+_numSeedingRounds, ((TeamBracketCell)d).getTeam()));
      }
      
    } else if(d instanceof BracketLabelCell) {
      sb.append("><font size='4'>");
      sb.append(((BracketLabelCell)d).getLabel() + "</font>");
    }
    sb.append("</td>");
    
    return sb.toString();
  }

  /**
   * Used to get a bridge cell that goes just to the right of the specified
   * round's column.
   * 
   * @param row
   *          The table row for which to look up a bridge cell.
   * @param round
   *          Playoff round for which to look up bridge cell info. This should
   *          be the column just to the left of where the bridge cell will be
   *          located.
   * @param cs
   *          The corner style that determines how the top right corner cells
   *          meet.
   * @see #TopRightCornerStyle
   * @return Properly formatted HTML \<td\> element for a bridge cell.
   */
  public String getHtmlBridgeCell(final int row,
                                  final int round,
                                  final TopRightCornerStyle cs) {
    final StringBuffer sb = new StringBuffer();
    final int ar = round-_firstRound;
    // Very brief explaination of the math:
    // Let y = row (this is the key value of the inner Map elements of _bracketData)
    // Let x = the number of rows per team (variable '_rowsPerTeam') in the left-most column of the output table
    //  (Given: x is a positive, even value. The math would work for other values, but it's meaningless when we
    //   can't have fractional table rows...)
    // Let r = the round index (variable 'ar') starting at 0 in the left-most column
    // Then a normalized modulo function that assigns the index of 0 to the top row of each bracket
    // is (y + x * 2^(r+1) - (x * 2^(r-1) - x/2 + 1)) % (x * 2^(r+1))
    // If the resulting value is 0, then row y is the top of a bracket in that round.
    // If the resulting value is x * 2^r, then row y is the bottom of a bracket in that round.
    // And all rows in between will have the 'Bridge' style applied to the cells. All other rows
    // will simply be empty. The rounding operation is purely to negate any possible floating
    // point inaccuracies introduced by Math.pow, etc.
    final int modVal = (int)(
      Math.round((row + _rowsPerTeam*Math.pow(2, ar+1) - _rowsPerTeam*Math.pow(2, ar-1) + _rowsPerTeam/2 - 1))
                                             %
                           Math.round((_rowsPerTeam*Math.pow(2, ar+1))));

    if(modVal >= cs.getModuloMinimum() &&
        modVal <= Math.round(_rowsPerTeam*Math.pow(2, ar)) &&
        round <= _finalsRound) {
      // In a bridge
      if(round == _semiFinalsRound &&
        ( (Math.round((row + _rowsPerTeam*Math.pow(2, ar+1) - _rowsPerTeam*Math.pow(2, ar-1) + _rowsPerTeam/2 - 1)))
                                                /
                        (Math.round((_rowsPerTeam*Math.pow(2, ar+1))))
        ) > 2 ) {
        // This is the bridge cell before the 3rd/4th place brackets - it's just a blank cell
        sb.append("<td width='10'>&nbsp;</td>");
      } else if(cs.equals(TopRightCornerStyle.MEET_BOTTOM_OF_CELL) && modVal == 1) {
        // If we are on the first line of the bridge, emit a rowspan'd td cell
        sb.append("<td width='10' class='Bridge' rowspan='" + _rowsPerTeam + "'>&nbsp;</td>");
      } else if(cs.equals(TopRightCornerStyle.MEET_TOP_OF_CELL) && modVal == 0) {
        sb.append("<td width='10' class='Bridge' rowspan='"
            + (_rowsPerTeam*(int)Math.round(Math.pow(2, ar))+1) + "'>&nbsp;</td>");
      } else {
        sb.append("<!-- skip column for bridge -->");
      }
    } else {
      // Outside of a bridge
      sb.append("<td width='10'>&nbsp;</td>");
    }
    return sb.toString();
  }

  /**
   * Adds labels for the bracket numbers to the specified playoff round number.
   * 
   * @param roundNumber
   */
  public void addBracketLabels(final int roundNumber) {
    SortedMap<Integer, BracketDataType> roundData =
      _bracketData.get(new Integer(roundNumber));
    if(roundData != null) {
      Vector<Integer> rows = new Vector<Integer>();
      Iterator<Integer> it = roundData.keySet().iterator();
      while(it.hasNext()) {
        final int firstTeamRow = it.next().intValue();
        if(!it.hasNext()) {
          return;
        }
        final int secondTeamRow = it.next().intValue();
        rows.add(firstTeamRow + (secondTeamRow-firstTeamRow)/2);
      }

      if(roundNumber == _finalsRound) {
        it = rows.iterator();
        roundData.put(it.next(), new BracketLabelCell("1st/2nd Place"));
        if(it.hasNext()) {
          roundData.put(it.next(), new BracketLabelCell("3rd/4th Place"));
        }
      } else {
        int bracketNumber = 1;
        it = rows.iterator();
        while(it.hasNext()) {
          roundData.put(it.next(), new BracketLabelCell(bracketNumber++));
        }
      }
    }
  }

  /**
   * Defaults showScore to true.
   * 
   * @see #getDisplayString(Connection, String, int, Team, boolean)
   */
  public static String getDisplayString(final Connection connection,
                                        final String currentTournament,
                                        final int runNumber,
                                        final Team team)
    throws IllegalArgumentException, SQLException {
    return getDisplayString(connection, currentTournament, runNumber, team, true);
  }
  
  /**
   * What to display given a team number, handles TIE, null and BYE
   *
   * @param connection connection to the database
   * @param currentTournament the current tournament
   * @param runNumber the current run, used to get the score
   * @param team team to get display string for
   * @param showScore if the score should be shown
   * @throws IllegalArgumentException if teamNumber is invalid
   * @throws SQLException on a database error
   */
  public static String getDisplayString(final Connection connection,
                                        final String currentTournament,
                                        final int runNumber,
                                        final Team team,
                                        final boolean showScore)
  throws IllegalArgumentException, SQLException {
    if(Team.BYE.equals(team)) {
      return "<font class='TeamName'>BYE</font>";
    } else if(Team.TIE.equals(team)) {
      return "<font class='TIE'>TIE</font>";
    } else if(null == team || Team.NULL.equals(team)) {
      return "&nbsp;";
    } else {
      final StringBuffer sb = new StringBuffer();
      sb.append("<font class='TeamNumber'>#");
      sb.append(team.getTeamNumber());
      sb.append("</font>&nbsp;<font class='TeamName'>");
      sb.append(team.getTeamName());
      sb.append("</font>");
      if(showScore && Playoff.performanceScoreExists(connection, team, runNumber)
          && !Playoff.isBye(connection, currentTournament, team, runNumber)) {
        sb.append("<font class='TeamScore'>&nbsp;Score: ");
        if(Playoff.isNoShow(connection, currentTournament, team, runNumber)) {
          sb.append("No Show");
        } else {
          //only display score if it's not a bye
          sb.append(Playoff.getPerformanceScore(connection, currentTournament, team, runNumber));
        }
        sb.append("</font>");
      }
      return sb.toString();
    }
  }
}
