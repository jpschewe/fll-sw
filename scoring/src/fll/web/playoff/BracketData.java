package fll.web.playoff;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Team;
import fll.db.Queries;

/**
 * Class to provide convenient access to the contents of the PlayoffData table.
 * A bracket data object contains all the playoff meta data for the current
 * tournament, in a specified division, for a given range of playoff rounds.
 * This data can be thought of a sparse matrix containing entries only where a
 * team name would appear if the matrix was overlaid on an elimination bracket.
 * Then, by iterating over rounds and rows, the data access method provides
 * cell-by-cell output in either row-major or column-major order, as you desire.
 * By specifying options to the constructor or calling additional functions
 * after initial creation of a BracketData object, you can insert bracket label
 * cells, table assignment labels, table assignment form elements, etc.
 * 
 * @author Dan Churchill
 */
public class BracketData {

  private static final Logger LOG = Logger.getLogger(BracketData.class);

  /**
   * Data type for brackets.
   */
  public abstract static class BracketDataType {
  }

  /**
   * Team bracket cells.
   */
  public static class TeamBracketCell extends BracketDataType {
    private Team _team;

    public Team getTeam() {
      return _team;
    }

    public void setTeam(final Team t) {
      _team = t;
    }

    private String _table;

    public String getTable() {
      return _table;
    }

    public void setTable(final String t) {
      _table = t;
    }

    private int _dbLine;

    public int getDBLine() {
      return _dbLine;
    }

    public void setDBLine(final int v) {
      _dbLine = v;
    }

    private boolean _printed;

    public boolean getPrinted() {
      return _printed;
    }

    public void setPrinted(final boolean b) {
      _printed = b;
    }
  }

  /**
   * Cell for bracket labels.
   */
  public static class BracketLabelCell extends BracketDataType {
    public BracketLabelCell(final int num) {
      _label = "Bracket "
          + num;
    }

    public BracketLabelCell(final String lbl) {
      _label = lbl;
    }

    private final String _label;

    public String getLabel() {
      return _label;
    }
  }

  /**
   * Cell for table labels on the big screen display. For now, this type of
   * bracket data cell contains only the string. In the future, we may also wish
   * to store a color associated with the given table.
   */
  public static class BigScreenTableAssignmentCell extends BracketDataType {
    public BigScreenTableAssignmentCell(final String table) {
      _table = table;
    }

    private final String _table;

    public String getTable() {
      return _table;
    }
  }

  /**
   * Cell that doesn't exist because it is part of a spanned cell
   */
  public static class SpannedOverBracketCell extends BracketDataType {
    public SpannedOverBracketCell() {
      _comment = null;
    }

    public SpannedOverBracketCell(final String comment) {
      _comment = comment;
    }

    private final String _comment;

    /**
     * Gets the comment string, if any.
     * 
     * @return The comment string to use, or null if no string was set.
     */
    public String getComment() {
      return _comment;
    }
  }

  /**
   * Cell that has an inner table with label, table assignment, and printed
   * checkbox and info.
   */
  public static class ScoreSheetFormBracketCell extends BracketDataType {
    public ScoreSheetFormBracketCell(final List<String> allTables,
                                     final String label,
                                     final int matchNum,
                                     final boolean printed,
                                     final String tableA,
                                     final String tableB,
                                     final Team teamA,
                                     final Team teamB,
                                     final int rowsSpanned) {
      super();
      _allTables = allTables;
      _label = label;
      _matchNum = matchNum;
      _printed = printed;
      _tableA = tableA;
      _tableB = tableB;
      _teamA = teamA;
      _teamB = teamB;
      _rowsSpanned = rowsSpanned;
    }

    private int _rowsSpanned;

    private String _label;

    private String _tableA;

    private String _tableB;

    private boolean _printed;

    private List<String> _allTables;

    private int _matchNum;

    private Team _teamA;

    private Team _teamB;

    public List<String> getAllTables() {
      return _allTables;
    }

    public void setAllTables(final List<String> allTables) {
      _allTables = allTables;
    }

    public String getLabel() {
      return _label;
    }

    public void setLabel(final String label) {
      _label = label;
    }

    public int getMatchNum() {
      return _matchNum;
    }

    public void setMatchNum(final int matchNum) {
      _matchNum = matchNum;
    }

    public boolean getPrinted() {
      return _printed;
    }

    public void setPrinted(final boolean printed) {
      _printed = printed;
    }

    public Team getTeamA() {
      return _teamA;
    }

    public void setTeamA(final Team teamA) {
      _teamA = teamA;
    }

    public Team getTeamB() {
      return _teamB;
    }

    public void setTeamB(final Team teamB) {
      _teamB = teamB;
    }

    public String getTableA() {
      return _tableA;
    }

    public void setTableA(final String tableA) {
      _tableA = tableA;
    }

    public String getTableB() {
      return _tableB;
    }

    public void setTableB(final String tableB) {
      _tableB = tableB;
    }

    public int getRowsSpanned() {
      return _rowsSpanned;
    }

    public void setRowsSpanned(final int rowsSpanned) {
      _rowsSpanned = rowsSpanned;
    }
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
    MEET_TOP_OF_CELL(0), MEET_BOTTOM_OF_CELL(1);

    private int _moduloMinimum;

    /* package */TopRightCornerStyle(final int moduloMin) {
      _moduloMinimum = moduloMin;
    }

    public int getModuloMinimum() {
      return _moduloMinimum;
    }
  }

  // Map of round number to map of line number (of the conceptual table - not
  // the column of the PlayoffData table) to playoff meta data for that
  // round number and line number.
  private final Map<Integer, SortedMap<Integer, BracketDataType>> _bracketData;

  private int _firstRound;

  private final int _lastRound;

  private final int _firstRoundSize;

  private final int _rowsPerTeam;

  private final int _numSeedingRounds;

  private final int _finalsRound;

  private boolean _showFinalScores;

  private boolean _showOnlyVerifiedScores;

  /**
   * Constructs a bracket data object with playoff data from the database.
   * 
   * @param pConnection Database connection to use.
   * @param pDivision Division from which to look up playoff data.
   * @param pFirstRound The first playoff round of interest (1st playoff round
   *          is 1, not the number of seeding rounds + 1)
   * @param pLastRound The last playoff round of interest.
   * @param pRowsPerTeam A positive, even number defining how many rows will be
   *          allocated for each team in the first round. This determines
   *          overall spacing for the entire table. Recommended value: 4.
   * @throws SQLException
   */
  public BracketData(final Connection pConnection, final String pDivision, final int pFirstRound, final int pLastRound, final int pRowsPerTeam)
      throws SQLException {
    super();
    if (pRowsPerTeam % 2 != 0
        || pRowsPerTeam < 2) {
      throw new RuntimeException("Error building BracketData structure:"
          + " Illegal rows-per-team value specified." + " Value must be a multiple of 2 greater than 0.");
    }

    final int tournament = Queries.getCurrentTournament(pConnection);


    _rowsPerTeam = pRowsPerTeam;
    _firstRoundSize = Queries.getFirstPlayoffRoundSize(pConnection, pDivision);
    _numSeedingRounds = Queries.getNumSeedingRounds(pConnection, tournament);

    _showFinalScores = true;
    _showOnlyVerifiedScores = true;

    if (pFirstRound < 1) {
      _firstRound = 1;
    } else {
      _firstRound = pFirstRound;
    }

    _lastRound = pLastRound;

    _finalsRound = Queries.getNumPlayoffRounds(pConnection, pDivision);

    _bracketData = new TreeMap<Integer, SortedMap<Integer, BracketDataType>>();
    for (int i = _firstRound; i <= _lastRound; i++) {
      _bracketData.put(i, new TreeMap<Integer, BracketDataType>());
    }

    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = pConnection.prepareStatement("SELECT PlayoffRound,LineNumber,Team,AssignedTable,Printed" //
          + " FROM PlayoffData" //
          + " WHERE Tournament= ?" //
          + " AND event_division= ?" //
          + " AND PlayoffRound>= ?" //
          + " AND PlayoffRound<= ?");
      stmt.setInt(1, tournament);
      stmt.setString(2, pDivision);
      stmt.setInt(3, _firstRound);
      stmt.setInt(4, _lastRound);
      rs = stmt.executeQuery();
      while (rs.next()) {
        final int round = rs.getInt(1);
        final int line = rs.getInt(2);
        final int team = rs.getInt(3);
        final String table = rs.getString(4);
        final boolean printed = rs.getBoolean(5);

        final SortedMap<Integer, BracketDataType> roundData = _bracketData.get(round);

        final TeamBracketCell d = new TeamBracketCell();
        d.setTable(table);
        d.setTeam(Team.getTeamFromDatabase(pConnection, team));
        d.setDBLine(line);
        d.setPrinted(printed);
        // Very brief explanation of the math:
        // Let y = target output row in the table (this is the key value of the
        // inner Map elements of _bracketData)
        // Let x = the number of rows per team (variable '_rowsPerTeam') in the
        // left-most column of the output table
        // (Given: x is a positive, even value. The math would work for other
        // values, but it's meaningless when we
        // can't have fractional table rows...)
        // Let r = the round index (variable 'adjustedRound') starting at 0 in
        // the left-most column
        // Let n = the row number (variable 'line') as obtained from the
        // database: 1,2,3,...
        // Then y = n * x * 2^r - (x * 2^(r-1) + 0.5 * x - 1)
        // The rounding operation is purely to negate any possible floating
        // point inaccuracies introduced by Math.pow, etc.
        final int adjustedRound = round
            - _firstRound;
        final int row;
        if (_firstRound < _finalsRound
            && round == _finalsRound && line == 3) {
          row = topRowOfConsolationBracket();
        } else if (_firstRound < _finalsRound
            && round == _finalsRound && line == 4) {
          row = topRowOfConsolationBracket()
              + _rowsPerTeam;
        } else if (_firstRound < _finalsRound
            && round == _finalsRound + 1 && line == 2) {
          row = topRowOfConsolationBracket()
              + _rowsPerTeam / 2;
        } else {
          row = (int) Math.round(line
              * _rowsPerTeam * (Math.pow(2, adjustedRound)) - (_rowsPerTeam
                  * Math.pow(2, adjustedRound - 1) + 0.5 * _rowsPerTeam - 1));
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Putting team "
              + d.getTeam() + " with dbLine " + d.getDBLine() + " to row " + row + " of output table\n");
        }
        if (roundData.put(row, d) != null) {
          throw new RuntimeException("Error - Map keys were not unique - PlayoffData "
              + "might be inconsistent (you should verify that there are not multiple teams" + " occupying the same round and row for tournament:'"
              + tournament + "' and" + " division:'" + pDivision + "')");
        }
      }

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Constructor including explicit show scores flag.
   * 
   * @param pShowFinals True if the final scores should be displayed (e.g. for
   *          the administrative brackets) or false if they should not (e.g. for
   *          the big screen display).
   * @throws SQLException
   */
  public BracketData(final Connection connection,
                     final String division,
                     final int pFirstRound,
                     final int pLastRound,
                     final int pRowsPerTeam,
                     final boolean pShowFinals) throws SQLException {
    this(connection, division, pFirstRound, pLastRound, pRowsPerTeam);
    _showFinalScores = pShowFinals;
  }

  /**
   * Constructor including explicit show scores flag.
   * 
   * @param pShowFinals True if the final scores should be displayed (e.g. for
   *          the administrative brackets) or false if they should not (e.g. for
   *          the big screen display).
   * @param pShowOnlyVerifiedScores True if only verified scores should be
   *          displayed, false if all scores should be displayed.
   * @throws SQLException
   */
  public BracketData(final Connection connection,
                     final String division,
                     final int pFirstRound,
                     final int pLastRound,
                     final int pRowsPerTeam,
                     final boolean pShowFinals,
                     final boolean pShowOnlyVerifiedScores) throws SQLException {
    this(connection, division, pFirstRound, pLastRound, pRowsPerTeam);
    _showFinalScores = pShowFinals;
    _showOnlyVerifiedScores = pShowOnlyVerifiedScores;
  }

  /**
   * Returns the playoff meta data for a specified location in the brackets.
   * 
   * @param round The column of the playoff data table, otherwise known as the
   *          round number.
   * @param row The row number to retrieve.
   * @return The BracketDataType for the given cell, or null if there is no data
   *         at that cell.
   */
  public BracketDataType getData(final int round, final int row) {
    return _bracketData.get(round).get(row);
  }

  /**
   * Returns the number of rows in the specified round number.
   * 
   * @return Html table row number of the last row for the rounds stored in the
   *         instance of BracketData, or 0 if there are no rows in it.
   */
  public int getNumRows() {
    try {
      if (_firstRound < _finalsRound
          && _lastRound >= _finalsRound) {
        final int sfr = _bracketData.get(_finalsRound).lastKey().intValue();
        final int fr = _bracketData.get(_firstRound).lastKey().intValue();
        return sfr > fr ? sfr : fr;
      }
      return _bracketData.get(_firstRound).lastKey().intValue();
    } catch (final NoSuchElementException e) {
      return 0;
    }
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
   * cells are generated with a specified width of 400 pixels. If the cell
   * contains text, the \<td\>element will have attribute class='Leaf'. Font
   * tags for team number, team name, and score have classes of 'TeamNumber',
   * 'TeamName', and 'TeamScore', respectively.
   * 
   * @param connection Database connection for looking up team scores, etc.
   * @param tournament The current tournament.
   * @param row Row number of the bracket data we are displaying.
   * @param round Round number (column) of data we are displaying.
   * @return Properly formed \<td\>element.
   * @throws SQLException If database access fails.
   */
  public String getHtmlCell(final Connection connection, final int tournament, final int row, final int round) throws SQLException {
    final SortedMap<Integer, BracketDataType> roundData = _bracketData.get(round);
    if (roundData == null) {
      return "<td>ERROR: No data for round "
          + round + ".</td>";
    }
    final StringBuffer sb = new StringBuffer();
    final BracketDataType d = roundData.get(row);
    if (d == null) {
      sb.append("<td width='400'>&nbsp;</td>");
    } else if (d instanceof SpannedOverBracketCell) {
      final String comment = ((SpannedOverBracketCell) d).getComment();
      if (comment != null) {
        sb.append("<!-- "
            + comment + "-->");
      }
    } else if (d instanceof TeamBracketCell) {
      sb.append("<td width='400' class='Leaf'>");
      if (round == _finalsRound) {
        sb.append(getDisplayString(connection, tournament, round
            + _numSeedingRounds, ((TeamBracketCell) d).getTeam(), _showFinalScores, _showOnlyVerifiedScores));
      } else if (_showFinalScores
          || round != _finalsRound + 1) {
        sb.append(getDisplayString(connection, tournament, round
            + _numSeedingRounds, ((TeamBracketCell) d).getTeam(), true, _showOnlyVerifiedScores));
      }
      sb.append("</td>");

    } else if (d instanceof BracketLabelCell) {
      sb.append("<td width='400'><font size='4'>");
      sb.append(((BracketLabelCell) d).getLabel()
          + "</font>");
      sb.append("</td>");
    } else if (d instanceof ScoreSheetFormBracketCell) {
      final ScoreSheetFormBracketCell myD = (ScoreSheetFormBracketCell) d;
      sb.append("<td width='400' valign='middle'");
      if (myD.getTeamA().getTeamNumber() > 0
          && myD.getTeamB().getTeamNumber() > 0) {
        sb.append(" rowspan='"
            + myD.getRowsSpanned() + "'>");
        sb.append("<table>\n  <tr><td colspan='3' align='center'><font size='4'>");
        sb.append(myD.getLabel());
        sb.append("</font></td></tr>");
        sb.append("<tr>");
        sb.append("<td rowspan='2' align='center' valign='middle'>");
        sb.append("<input type='checkbox' name='print"
            + myD.getMatchNum() + "'");
        if (!myD.getPrinted()) {
          sb.append(" checked");
        }
        sb.append("/>");
        sb.append("<input type='hidden' name='teamA"
            + myD.getMatchNum() + "' value='" + myD.getTeamA().getTeamNumber() + "'/>");
        sb.append("<input type='hidden' name='teamB"
            + myD.getMatchNum() + "' value='" + myD.getTeamB().getTeamNumber() + "'/>");
        sb.append("<input type='hidden' name='round"
            + myD.getMatchNum() + "' value='" + round + "'/>");
        sb.append("</td>");
        sb.append("<td align='right'>Table A: </td>");
        sb.append("<td align='left'><select name='tableA"
            + myD.getMatchNum() + "' size='1'>");
        Iterator<String> myit = myD.getAllTables().iterator();
        while (myit.hasNext()) {
          final String optStr = myit.next();
          sb.append("<option");
          if (optStr.equals(myD.getTableA())) {
            sb.append(" selected");
          }
          sb.append(">"
              + optStr + "</option>");
        }
        sb.append("</select></td></tr>");
        sb.append("<tr><td align='right'>Table B: </td>");
        sb.append("<td align='left'><select name='tableB"
            + myD.getMatchNum() + "' size='1'>");
        myit = myD.getAllTables().iterator();
        while (myit.hasNext()) {
          final String optStr = myit.next();
          sb.append("<option");
          if (optStr.equals(myD.getTableB())) {
            sb.append(" selected");
          }
          sb.append(">"
              + optStr + "</option>");
        }
        sb.append("</select></td></tr></table></td>");
      } else {
        // this block is not typically invoked because we use a BracketLabelCell
        // in addBracketLabelsAndScoreGenFormElements when one of the teams is
        // not present, but in theory this could be used instead to center just
        // the label in a rowspanned cell.
        sb.append(" rowspan='"
            + myD.getRowsSpanned() + "' align='center'>");
        sb.append("<font size='4'>");
        sb.append(myD.getLabel()
            + "</font>");
        sb.append("</td>");
      }
    } else if (d instanceof BigScreenTableAssignmentCell) {
      // TODO: Add a background-color:rgb(r,g,b) to the td style attribute based
      // on a color from the database table information
      sb.append("<td align='right' style='padding-right:30px'><span class='table_assignment'>");
      sb.append(((BigScreenTableAssignmentCell) d).getTable());
      sb.append("</span></td>");
    }

    return sb.toString();
  }

  /**
   * Returns a string including a table row element with table header cells
   * providing the playoff round number.
   */
  public String getHtmlHeaderRow() {
    final StringBuffer sb = new StringBuffer("<tr>\n");
    for (int i = _firstRound; i <= _lastRound
        && i <= _finalsRound; i++) {
      sb.append("  <th colspan='2'>Playoff Round "
          + i + "</th>\n");
    }
    sb.append("</tr>\n");
    return sb.toString();
  }

  /**
   * Used to get a bridge cell that goes just to the right of the specified
   * round's column.
   * 
   * @param row The table row for which to look up a bridge cell.
   * @param round Playoff round for which to look up bridge cell info. This
   *          should be the column just to the left of where the bridge cell
   *          will be located.
   * @param cs The corner style that determines how the top right corner cells
   *          meet.
   * @see TopRightCornerStyle
   * @return Properly formatted HTML \<td\>element for a bridge cell.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "ICAST_IDIV_CAST_TO_DOUBLE" }, justification = "Double cast is OK as we are ok with the rounding")
  public String getHtmlBridgeCell(final int row, final int round, final TopRightCornerStyle cs) {
    final StringBuffer sb = new StringBuffer();
    final int ar = round
        - _firstRound;
    if (_firstRound < _finalsRound
        && rowIsInConsolationBracket(row)) {
      if (round != _finalsRound) {
        // This is a bridge cell before (or after!) the 3rd/4th place brackets -
        // it's just a blank cell
        sb.append("<td width='10'>&nbsp;</td>");
      } else {
        if (row == topRowOfConsolationBracket()) {
          // top of the 3rd/4th place bracket
          if (cs.equals(TopRightCornerStyle.MEET_BOTTOM_OF_CELL)) {
            sb.append("<td width='10' class='BridgeTop'>&nbsp;</td>");
          } else if (cs.equals(TopRightCornerStyle.MEET_TOP_OF_CELL)) {
            sb.append("<td width='10' class='Bridge' rowspan='"
                + (_rowsPerTeam + 1) + "'>&nbsp;</td>");
          } else {
            throw new RuntimeException("Unknown value for TopRightCornerStyle");
          }
        } else if (row > topRowOfConsolationBracket()
            && row <= topRowOfConsolationBracket()
                + _rowsPerTeam) {
          if (cs.equals(TopRightCornerStyle.MEET_TOP_OF_CELL)) {
            sb.append("<!-- skip column for bridge -->");
          } else if (row < topRowOfConsolationBracket()
              + _rowsPerTeam) {
            sb.append("<td width='10' class='BridgeMiddle'>&nbsp;</td>");
          } else if (row == topRowOfConsolationBracket()
              + _rowsPerTeam) {
            sb.append("<td width='10' class='BridgeBottom'>&nbsp;</td>");
          }
        } else {
          sb.append("<td width='10'>&nbsp;</td>");
        }
      }
    } else {
      // Very brief explanation of the math:
      // Let y = row (this is the key value of the inner Map elements of
      // _bracketData)
      // Let x = the number of rows per team (variable '_rowsPerTeam') in the
      // left-most column of the output table
      // (Given: x is a positive, even value. The math would work for other
      // values, but it's meaningless when we
      // can't have fractional table rows...)
      // Let r = the round index (variable 'ar') starting at 0 in the left-most
      // column
      // Then a normalized modulo function that assigns the index of 0 to the
      // top row of each bracket
      // is (y + x * 2^(r+1) - (x * 2^(r-1) - x/2 + 1)) % (x * 2^(r+1))
      // If the resulting value is 0, then row y is the top of a bracket in that
      // round.
      // If the resulting value is x * 2^r, then row y is the bottom of a
      // bracket in that round.
      // And all rows in between will have the 'Bridge' style applied to the
      // cells. All other rows
      // will simply be empty. The rounding operation is purely to negate any
      // possible floating
      // point inaccuracies introduced by Math.pow, etc.
      // modVal is an index that indicates the table row number within the
      // current bracket of the current line.
      // modVal 0 is the row in which the top team name of a bracket occurs,
      // modVal 1 is first row below the top
      // bracket line, etc. The number of the lines in the bracket varies based
      // on the round (later rounds have
      // more rows between bracket lines) and on the _rowsPerTeam value.
      final int modVal = (int) (//
      Math.round((row
          + _rowsPerTeam //
          * Math.pow(2, ar + 1) //
          - _rowsPerTeam //
          * Math.pow(2, ar - 1) //
          + _rowsPerTeam / 2 - 1)) //
      % Math.round(_rowsPerTeam
          * Math.pow(2, ar + 1)));

      if (modVal <= Math.round(_rowsPerTeam
          * Math.pow(2, ar))
          && round <= _finalsRound) {
        if (cs.equals(TopRightCornerStyle.MEET_BOTTOM_OF_CELL)) {
          if (modVal >= 1
              && modVal < (_rowsPerTeam * (int) Math.round(Math.pow(2, ar)))) {
            // If we are in the middle a bridge use the BridgeMiddle class
            sb.append("<td width='10' class='BridgeMiddle" /*
                                                            * rowspan='" +
                                                            * (_rowsPerTeam
                                                            * *(int)
                                                            * Math.round(Math
                                                            * .pow(2, ar)))
                                                            */
                + "'>&nbsp;</td>");
          } else if (modVal == 0) {
            // If we are on the first line of the bridge, use the BridgeTop
            // class
            sb.append("<td width='10' class='BridgeTop'>&nbsp;</td>");
          } else if (modVal == (_rowsPerTeam * (int) Math.round(Math.pow(2, ar)))) {
            // If we are on the last line of the bridge, use the BridgeBottom
            // class
            sb.append("<td width='10' class='BridgeBottom'>&nbsp;</td>");
          }
        } else if (cs.equals(TopRightCornerStyle.MEET_TOP_OF_CELL)
            && modVal == 0) {
          sb.append("<td width='10' class='Bridge' rowspan='"
              + (_rowsPerTeam
                  * (int) Math.round(Math.pow(2, ar)) + 1) + "'>&nbsp;</td>");
        } else {
          sb.append("<!-- skip column for bridge -->");
        }
      } else {
        // Outside of a bridge
        sb.append("<td width='10'>&nbsp;</td>");
      }
    }
    return sb.toString();
  }

  /**
   * @param row
   * @return true if row is below (numerically greater than) the bottom-most
   *         teamname of the first displayed round.
   */
  private boolean rowIsInConsolationBracket(final int row) {
    final int firstDisplayedRoundSize = (getFirstRoundSize() / ((int) Math.round(Math.pow(2, _firstRound - 1))));
    return row > (1 + (firstDisplayedRoundSize - 1)
        * getRowsPerTeam());
  }

  private int topRowOfConsolationBracket() {
    final int firstDisplayedRoundSize = (getFirstRoundSize() / ((int) Math.round(Math.pow(2, _firstRound - 1))));
    return 3
        + (firstDisplayedRoundSize - 1) * getRowsPerTeam();
  }

  /**
   * Adds labels for the bracket numbers to the specified playoff round number.
   * If this function is used, addBracketLabelsAndScoreGenFormElements must not
   * be used.
   * 
   * @param roundNumber
   */
  public void addBracketLabels(final int roundNumber) {
    final SortedMap<Integer, BracketDataType> roundData = _bracketData.get(roundNumber);
    if (roundData != null) {
      final List<Integer> rows = new LinkedList<Integer>();
      Iterator<Integer> it = roundData.keySet().iterator();
      while (it.hasNext()) {
        final int firstTeamRow = it.next().intValue();
        if (!it.hasNext()) {
          return;
        }
        final int secondTeamRow = it.next().intValue();
        rows.add(firstTeamRow
            + (secondTeamRow - firstTeamRow) / 2);
      }

      if (roundNumber == _finalsRound) {
        it = rows.iterator();
        roundData.put(it.next(), new BracketLabelCell("1st/2nd Place"));
        if (it.hasNext()) {
          roundData.put(it.next(), new BracketLabelCell("3rd/4th Place"));
        }
      } else {
        int bracketNumber = 1;
        it = rows.iterator();
        while (it.hasNext()) {
          roundData.put(it.next(), new BracketLabelCell(bracketNumber++));
        }
      }
    }
  }

  /**
   * Adds bracket data cells containing hard table assignments (i.e. those saved
   * in the database) to the cells just below the top team of a bracket and just
   * above the bottom team. If no table assignment is present in the database,
   * no bracket data cell is created. If the BracketData class has been
   * specified to have fewer than 4 lines per team (i.e. 2 lines per team only)
   * then this function will have no effect.
   * 
   * @param connection
   * @param tournament
   * @param eventDivision
   * @throws SQLException
   */
  public void addStaticTableLabels(final Connection connection, final int tournament, final String eventDivision) throws SQLException {
    if (_rowsPerTeam < 4) {
      LOG.warn("Table labels cannot be added to bracket data because there are too few lines per team for them to fit.");
      return; // if there aren't enough rows-per-team to include table labels,
      // just return
    }
    for (final Integer round : _bracketData.keySet()) {
      // We can't modify the map while we iterate over it - we'll add these
      // after identifying all new cells
      final SortedMap<Integer, BracketDataType> newCells = new TreeMap<Integer, BracketDataType>();
      int dblinenum = 0;
      int tablelinemod = -1;
      final SortedMap<Integer, BracketDataType> roundData = _bracketData.get(round);
      for (final Map.Entry<Integer, BracketDataType> entry : roundData.entrySet()) {
        final Integer lineNumber = entry.getKey();
        final BracketDataType cell = entry.getValue();
        if (cell != null
            && cell instanceof TeamBracketCell) {
          dblinenum++;
          tablelinemod += 2;
          if (tablelinemod > 1) {
            tablelinemod = -1;
          }
          // Get the table assignment from cell info
          final String table = Queries.getAssignedTable(connection, tournament, eventDivision, round.intValue(), dblinenum);
          if (table != null) {
            newCells.put(lineNumber.intValue()
                + tablelinemod, new BigScreenTableAssignmentCell(table));
          }
        }
      }
      // Merge the new cells into the roundData
      roundData.putAll(newCells);
    }
  }

  /**
   * Populates all rounds of the bracket with labels and HTML form elements for
   * table assignment and scoresheet generation. If this function is used,
   * addBracketLabels must not be used.
   * 
   * @return The number of matches for which form elements were generated.
   * @throws SQLException if database connection is broken.
   * @throws RuntimeException if playoffData table has mismatched teams in the
   *           brackets.
   */
  public int addBracketLabelsAndScoreGenFormElements(final Connection pConnection, final int tournament, final String division) throws SQLException {
    // Get the list of tournament tables
    final List<String[]> tournamentTables = Queries.getTournamentTables(pConnection);
    final List<String> tables = new LinkedList<String>();
    final Iterator<String[]> ttIt = tournamentTables.iterator();
    while (ttIt.hasNext()) {
      final String[] tt = ttIt.next();
      tables.add(tt[0]);
      tables.add(tt[1]);
    }
    // Prevent divide by 0 errors if no tables were set in the database.
    if (tables.isEmpty()) {
      tables.add("Table 1");
      tables.add("Table 2");
    }

    Iterator<String> tAssignIt = tables.iterator();
    int assignCount = Queries.getTableAssignmentCount(pConnection, tournament, division)
        % tables.size();
    while (assignCount-- > 0) {
      tAssignIt.next();
    }

    // Build the cells...
    int matchNum = 1;
    for (int i = _firstRound; i <= _lastRound
        && i <= _finalsRound; i++) {
      final SortedMap<Integer, BracketDataType> roundData = _bracketData.get(i);
      if (roundData != null) {
        final List<Integer[]> rows = new LinkedList<Integer[]>();
        final Iterator<Integer> it = roundData.keySet().iterator();
        while (it.hasNext()) {
          final Integer firstTeamRow = it.next();
          if (!it.hasNext()) {
            throw new RuntimeException("Mismatched team in playoff brackets. Check database for corruption.");
          }
          final Integer secondTeamRow = it.next();
          rows.add(new Integer[] { firstTeamRow, secondTeamRow });
        }

        int bracketNumber = 1;
        final Iterator<Integer[]> rit = rows.iterator();
        Integer[] curArray;
        String bracketLabel;
        while (rit.hasNext()) {
          if (i == _finalsRound
              && bracketNumber == 1) {
            bracketLabel = "1st/2nd Place";
          } else if (i == _finalsRound
              && bracketNumber == 2) {
            bracketLabel = "3rd/4th Place";
          } else {
            bracketLabel = "Bracket "
                + bracketNumber;
          }
          curArray = rit.next();
          // Build the cell - if both teams are not present, just do a normal
          // bracket label cell
          if (((TeamBracketCell) roundData.get(curArray[0])).getTeam().getTeamNumber() > 0
              && ((TeamBracketCell) roundData.get(curArray[1])).getTeam().getTeamNumber() > 0) {

            String tableA = ((TeamBracketCell) roundData.get(curArray[0])).getTable();
            if (null == tableA
                || tableA.length() == 0) {
              if (!tAssignIt.hasNext()) {
                tAssignIt = tables.iterator();
              }
              tableA = tAssignIt.next();
            }
            String tableB = ((TeamBracketCell) roundData.get(curArray[1])).getTable();
            if (null == tableB
                || tableB.length() == 0) {
              if (!tAssignIt.hasNext()) {
                tAssignIt = tables.iterator();
              }
              tableB = tAssignIt.next();
            }

            final TeamBracketCell topCell = (TeamBracketCell) roundData.get(curArray[0]);
            final TeamBracketCell bottomCell = (TeamBracketCell) roundData.get(curArray[1]);
            roundData.put(curArray[0] + 1, new ScoreSheetFormBracketCell(tables, bracketLabel, matchNum++, topCell.getPrinted()
                && bottomCell.getPrinted(), tableA, tableB, topCell.getTeam(), bottomCell.getTeam(), curArray[1].intValue()
                - curArray[0].intValue() - 1));
            // Put placeholders for the rows that are to be spanned over
            for (int j = curArray[0].intValue() + 2; j < curArray[1].intValue(); j++) {
              roundData.put(j, new SpannedOverBracketCell("spanned row"
                  + j));
            }
          } else {
            final int firstRow = curArray[0].intValue();
            final int lastRow = curArray[1].intValue();
            final int line = firstRow
                + (lastRow - firstRow) / 2;

            roundData.put(line, new BracketLabelCell(bracketLabel));
          }
          bracketNumber++;
        }
      }
    }
    return matchNum - 1;
  }

  /**
   * Obtains string to display for a team's info, given a team number. This
   * handles a TIE, null and BYE. This function respects the set value for
   * whether to display or hide verified scores. If unverified scores are
   * displayed, they will be shown as red text.
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
                                        final int currentTournament,
                                        final int runNumber,
                                        final Team team,
                                        final boolean showScore,
                                        final boolean showOnlyVerifiedScores) throws IllegalArgumentException, SQLException {
    if (Team.BYE.equals(team)) {
      return "<font class='TeamName'>BYE</font>";
    } else if (Team.TIE.equals(team)) {
      return "<font class='TIE'>TIE</font>";
    } else if (null == team
        || Team.NULL.equals(team)) {
      return "&nbsp;";
    } else {
      final StringBuffer sb = new StringBuffer();
      sb.append("<font class='TeamNumber'>#");
      sb.append(team.getTeamNumber());
      sb.append("</font>&nbsp;<font class='TeamName'>");
      sb.append(team.getTeamName());
      sb.append("</font>");
      if (showScore
          && Queries.performanceScoreExists(connection, team, runNumber)
          && (!showOnlyVerifiedScores || Queries.isVerified(connection, currentTournament, team, runNumber))
          && !Playoff.isBye(connection, currentTournament, team, runNumber)) {
        final boolean scoreVerified = Queries.isVerified(connection, currentTournament, team, runNumber);
        if (!scoreVerified) {
          sb.append("<span style='color:red'>");
        }
        sb.append("<font class='TeamScore'>&nbsp;Score: ");
        if (Playoff.isNoShow(connection, currentTournament, team, runNumber)) {
          sb.append("No Show");
        } else {
          // only display score if it's not a bye
          sb.append(Playoff.getPerformanceScore(connection, currentTournament, team, runNumber));
        }
        sb.append("</font>");
        if (!scoreVerified) {
          sb.append("</span>");
        }
      }
      return sb.toString();
    }
  }

  public void setShowOnlyVerifiedScores(final boolean v) {
    _showOnlyVerifiedScores = v;
  }

  public boolean getShowOnlyVerifiedScores() {
    return _showOnlyVerifiedScores;
  }
}
