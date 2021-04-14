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
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.db.Queries;
import fll.db.TableInformation;
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import net.mtu.eggplant.util.StringUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

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
public class BracketData extends BracketInfo {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Data type for brackets.
   */
  public abstract static class BracketDataType {
  }

  /**
   * Data type that has a dbLine.
   */
  public abstract static class DbBracketDataType extends BracketDataType {
    /**
     * @param dbLine {@link #getDBLine()}
     */
    public DbBracketDataType(@JsonProperty("dbLine") final int dbLine) {
      this.dbLine = dbLine;
    }

    private final int dbLine;

    /**
     * @return line in the database
     */
    public int getDBLine() {
      return dbLine;
    }

  }

  /**
   * Team bracket cells.
   */
  public static class TeamBracketCell extends DbBracketDataType {

    /**
     * @param team {@link #getTeam()}
     * @param table {@link #getTable()}
     * @param dbLine {@link #getDBLine()}
     * @param printed {@link #getPrinted()}
     */
    public TeamBracketCell(@JsonProperty("team") final Team team,
                           @JsonProperty("table") final String table,
                           @JsonProperty("dbline") final int dbLine,
                           @JsonProperty("printed") final boolean printed) {
      super(dbLine);
      this.team = team;
      this.table = table;
      this.printed = printed;
    }

    private final Team team;

    /**
     * @return the team
     */
    public Team getTeam() {
      return team;
    }

    private final String table;

    /**
     * @return the table that the performance is on
     */
    public @Nullable String getTable() {
      return table;
    }

    private final boolean printed;

    /**
     * @return has the score sheet been printed
     */
    public boolean getPrinted() {
      return printed;
    }
  }

  /**
   * Cell for bracket labels.
   */
  public static class BracketLabelCell extends BracketDataType {
    /**
     * @param lbl {@link #getLabel()}
     */
    public BracketLabelCell(final String lbl) {
      this.label = lbl;
    }

    private final String label;

    /**
     * @return bracket label
     */
    public String getLabel() {
      return label;
    }
  }

  /**
   * Cell for table labels on the big screen display. For now, this type of
   * bracket data cell contains only the string. In the future, we may also wish
   * to store a color associated with the given table.
   */
  public static class BigScreenTableAssignmentCell extends DbBracketDataType {
    /**
     * @param round {@link #getRound()}
     * @param row {@link #getRow()}
     * @param table {@link #getTable()}
     * @param dbLine {@link #getDBLine()}
     */
    public BigScreenTableAssignmentCell(final int round,
                                        final int row,
                                        final String table,
                                        final int dbLine) {
      super(dbLine);
      this.round = round;
      this.row = row;
      this.table = table;
    }

    private final int round;

    /**
     * @return the playoff round
     */
    public int getRound() {
      return round;
    }

    private final int row;

    /**
     * @return the display row
     */
    public int getRow() {
      return row;
    }

    private final String table;

    /**
     * @return the table for the cell
     */
    public String getTable() {
      return table;
    }
  }

  /**
   * Cell that doesn't exist because it is part of a spanned cell.
   */
  public static class SpannedOverBracketCell extends BracketDataType {
    /**
     * Comment is set to null.
     */
    public SpannedOverBracketCell() {
      this.comment = null;
    }

    /**
     * @param comment {@link #getComment()}
     */
    public SpannedOverBracketCell(final String comment) {
      this.comment = comment;
    }

    private final @Nullable String comment;

    /**
     * Gets the comment string, if any.
     * 
     * @return The comment string to use, or null if no string was set.
     */
    public @Nullable String getComment() {
      return comment;
    }
  }

  /**
   * Cell that has an inner table with label, table assignment, and printed
   * checkbox and info.
   */
  public static class ScoreSheetFormBracketCell extends BracketDataType {

    /**
     * @param allTables {@link #getAllTables()}
     * @param label {@link #getLabel()}
     * @param matchNum {@link #getMatchNum()}
     * @param printed {@link #getPrinted()}
     * @param tableA {@link #getTableA()}
     * @param tableB {@link #getTableB()}
     * @param teamA {@link #getTeamA()}
     * @param teamB {@link #getTeamB()}
     * @param rowsSpanned {@link #getRowsSpanned()}
     */
    public ScoreSheetFormBracketCell(final List<TableInformation> allTables,
                                     final String label,
                                     final int matchNum,
                                     final boolean printed,
                                     final String tableA,
                                     final String tableB,
                                     final Team teamA,
                                     final Team teamB,
                                     final int rowsSpanned) {
      super();
      this.allTables.addAll(allTables);
      this.label = label;
      this.matchNum = matchNum;
      this.printed = printed;
      this.tableA = tableA;
      this.tableB = tableB;
      this.teamA = teamA;
      this.teamB = teamB;
      this.rowsSpanned = rowsSpanned;
    }

    private final int rowsSpanned;

    private final String label;

    private final String tableA;

    private final String tableB;

    private final boolean printed;

    private final List<TableInformation> allTables = new LinkedList<>();

    private final int matchNum;

    private final Team teamA;

    private final Team teamB;

    /**
     * @return all possible tables
     */
    public List<TableInformation> getAllTables() {
      return allTables;
    }

    /**
     * @return bracket label
     */
    public String getLabel() {
      return label;
    }

    /**
     * @return match number
     */
    public int getMatchNum() {
      return matchNum;
    }

    /**
     * @return has this score sheet been printed
     */
    public boolean getPrinted() {
      return printed;
    }

    /**
     * @return the first team
     */
    public Team getTeamA() {
      return teamA;
    }

    /**
     * @return the second team
     */
    public Team getTeamB() {
      return teamB;
    }

    /**
     * @return the table for the first team
     */
    public String getTableA() {
      return tableA;
    }

    /**
     * @return the table for the second team
     */
    public String getTableB() {
      return tableB;
    }

    /**
     * @return how many rows are spanned by this cell
     */
    public int getRowsSpanned() {
      return rowsSpanned;
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
  public enum TopRightCornerStyle {
    /**
     * Draw to the top of the cell.
     */
    MEET_TOP_OF_CELL,
    /**
     * Draw to the bottom of the cell.
     */
    MEET_BOTTOM_OF_CELL;
  }

  // Map of round number to map of row number (of the conceptual HTML table -
  // not
  // the column of the PlayoffData table) to playoff meta data for that
  // round number and line number.
  private final Map<Integer, SortedMap<Integer, BracketDataType>> bracketData;

  private final int firstRoundSize;

  /**
   * The last performance run number before the first playoff round.
   * Not equal to num seeding rounds because of the possibility of a team being
   * in multiple playoff brackets.
   */
  private final int baseRunNumber;

  private final int rowsPerTeam;

  private final int finalsRound;

  /**
   * @return the round number that is the finals
   */
  public int getFinalsRound() {
    return finalsRound;
  }

  private final boolean showFinalScores;

  private final boolean showOnlyVerifiedScores;

  private final int currentTournament;

  private final int bracketIndex;

  private String bracketOutput = "";

  /**
   * @return the string that was generated with
   *         {@link #generateBracketOutput(Connection, TopRightCornerStyle)} or
   *         the empty string if the method has not been called.
   */
  @JsonIgnore
  public String getBracketOutput() {
    return bracketOutput;
  }

  /**
   * @return If this object is in a list, the index in the list.
   */
  public int getBracketIndex() {
    return bracketIndex;
  }

  /**
   * Constructor that assumes the object is not in a list.
   * 
   * @param pConnection database connection
   * @param pDivision playoff bracket name
   * @param pFirstRound the first playoff round to display
   * @param pLastRound the last playoff round to display
   * @param pRowsPerTeam how many rows to use to draw each team
   * @param pShowFinals should the final scores be displayed?
   * @param pShowOnlyVerifiedScores true if only verified scores should be
   *          displayed
   * @throws SQLException on a database error
   */
  public BracketData(final Connection pConnection,
                     final String pDivision,
                     final int pFirstRound,
                     final int pLastRound,
                     final int pRowsPerTeam,
                     final boolean pShowFinals,
                     final boolean pShowOnlyVerifiedScores)
      throws SQLException {
    this(pConnection, pDivision, pFirstRound, pLastRound, pRowsPerTeam, pShowFinals, pShowOnlyVerifiedScores, 0);
  }

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
   * @param bracketIndex the index of this bracket in a list, if not in a list,
   *          this should be 0
   * @param pShowFinals should the final scores be displayed?
   * @param pShowOnlyVerifiedScores true if only verified scores should be
   *          displayed
   * @throws SQLException on a database error
   */
  public BracketData(final Connection pConnection,
                     final String pDivision,
                     final int pFirstRound,
                     final int pLastRound,
                     final int pRowsPerTeam,
                     final boolean pShowFinals,
                     final boolean pShowOnlyVerifiedScores,
                     final int bracketIndex)
      throws SQLException {
    super(pDivision, pFirstRound < 1 ? 1 : pFirstRound, pLastRound);
    this.showFinalScores = pShowFinals;
    this.showOnlyVerifiedScores = pShowOnlyVerifiedScores;
    this.bracketIndex = bracketIndex;

    if (pRowsPerTeam
        % 2 != 0
        || pRowsPerTeam < 2) {
      throw new RuntimeException("Error building BracketData structure:"
          + " Illegal rows-per-team value specified."
          + " Value must be a multiple of 2 greater than 0.");
    }

    this.currentTournament = Queries.getCurrentTournament(pConnection);

    this.rowsPerTeam = pRowsPerTeam;
    this.firstRoundSize = Queries.getFirstPlayoffRoundSize(pConnection, currentTournament, getBracketName());

    this.finalsRound = Queries.getNumPlayoffRounds(pConnection, currentTournament, getBracketName());

    this.bracketData = new TreeMap<Integer, SortedMap<Integer, BracketDataType>>();
    for (int i = getFirstRound(); i <= getLastRound(); i++) {
      bracketData.put(i, new TreeMap<Integer, BracketDataType>());
    }

    PreparedStatement stmt = null;
    ResultSet rs = null;
    PreparedStatement minRunNumberPrep = null;
    ResultSet minRunNumber = null;
    try {
      minRunNumberPrep = pConnection.prepareStatement("select MIN(run_number) from PlayoffData WHERE event_division = ? AND Tournament = ?");
      minRunNumberPrep.setString(1, getBracketName());
      minRunNumberPrep.setInt(2, currentTournament);
      minRunNumber = minRunNumberPrep.executeQuery();
      if (minRunNumber.next()) {
        baseRunNumber = minRunNumber.getInt(1)
            - 1;
      } else {
        baseRunNumber = TournamentParameters.getNumSeedingRounds(pConnection, currentTournament);
      }

      stmt = pConnection.prepareStatement("SELECT PlayoffRound,LineNumber,Team,AssignedTable,Printed" //
          + " FROM PlayoffData" //
          + " WHERE Tournament= ?" //
          + " AND event_division= ?" //
          + " AND PlayoffRound>= ?" //
          + " AND PlayoffRound<= ?");
      stmt.setInt(1, currentTournament);
      stmt.setString(2, getBracketName());
      stmt.setInt(3, getFirstRound());
      stmt.setInt(4, getLastRound());
      rs = stmt.executeQuery();
      while (rs.next()) {
        final int round = rs.getInt(1);
        final int line = rs.getInt(2);
        final int teamNumber = rs.getInt(3);
        final String table = rs.getString(4);
        final boolean printed = rs.getBoolean(5);

        final SortedMap<Integer, BracketDataType> roundData = bracketData.get(round);

        final Team team = Team.getTeamFromDatabase(pConnection, teamNumber);
        final TeamBracketCell d = new TeamBracketCell(team, table, line, printed);

        final int row = getRowNumberForLine(round, line);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Putting team "
              + d.getTeam()
              + " with dbLine "
              + d.getDBLine()
              + " to row "
              + row
              + " of output table\n");
        }
        if (roundData.put(row, d) != null) {
          throw new RuntimeException("Error - Map keys were not unique - PlayoffData "
              + "might be inconsistent (you should verify that there are not multiple teams"
              + " occupying the same round and row for tournament:'"
              + currentTournament
              + "' and"
              + " division:'"
              + getBracketName()
              + "')");
        }
      }

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(minRunNumber);
      SQLFunctions.close(minRunNumberPrep);
    }
  }

  /**
   * Very brief explanation of the math:
   * 
   * <pre>
   * Let y = target output row in the table (this is the key value of the
   * inner Map elements of _bracketData)
   * Let x = the number of rows per team (variable '_rowsPerTeam') in the
   * left-most column of the output table
   * (Given: x is a positive, even value. The math would work for other
   * values, but it's meaningless when we
   * can't have fractional table rows...)
   * Let r = the round index (variable 'adjustedRound') starting at 0 in
   * the left-most column
   * Let n = the row number (variable 'line') as obtained from the
   * database: 1,2,3,...
   * Then y = n * x * 2^r - (x * 2^(r-1) + 0.5 * x - 1)
   * The rounding operation is purely to negate any possible floating
   * point inaccuracies introduced by Math.pow, etc.
   * </pre>
   * 
   * @param round the playoff round
   * @param line the database line
   * @return the row number
   */
  public int getRowNumberForLine(final int round,
                                 final int line) {
    final int adjustedRound = round
        - getFirstRound();
    final int row;
    if (getFirstRound() < finalsRound
        && round == finalsRound
        && line == 3) {
      row = topRowOfConsolationBracket();
    } else if (getFirstRound() < finalsRound
        && round == finalsRound
        && line == 4) {
      row = topRowOfConsolationBracket()
          + rowsPerTeam;
    } else if (getFirstRound() < finalsRound
        && round == finalsRound
            + 1
        && line == 2) {
      row = topRowOfConsolationBracket()
          + rowsPerTeam
              / 2;
    } else {
      row = (int) Math.round(line
          * rowsPerTeam
          * (Math.pow(2, adjustedRound))
          - (rowsPerTeam
              * Math.pow(2, adjustedRound
                  - 1)
              + 0.5
                  * rowsPerTeam
              - 1));
    }
    return row;
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
  public BracketDataType getData(final int round,
                                 final int row) {
    final SortedMap<Integer, BracketDataType> theRound = bracketData.get(round);
    if (theRound != null) {
      return theRound.get(row);
    } else {
      return null;
    }
  }

  /**
   * Returns the maximum number of rows for this BracketData object.
   * 
   * @return Html table row number of the last row for the rounds stored in the
   *         instance of BracketData, or 0 if there are no rows in it.
   */
  public int getNumRows() {
    try {
      if (getFirstRound() < finalsRound
          && getLastRound() >= finalsRound) {
        final int sfr = bracketData.get(finalsRound).lastKey();
        final int fr = bracketData.get(getFirstRound()).lastKey();
        return sfr > fr ? sfr : fr;
      } else {
        return bracketData.get(getFirstRound()).lastKey().intValue();
      }
    } catch (final NoSuchElementException e) {
      return 0;
    }
  }

  private int getFirstRoundSize() {
    return firstRoundSize;
  }

  private int getRowsPerTeam() {
    return rowsPerTeam;
  }

  /**
   * The inverse of {@link #constructLeafId(int, int)}.
   * 
   * @param lid the leaf id to parse
   * @return (bracket index, dbLine, playoffRound) or null if it's not parsable
   */
  public static ImmutableTriple<Integer, Integer, Integer> parseLeafId(final String lid) {
    final String[] pieces = lid.split("\\-");
    if (pieces.length >= 3) {
      final String bracketIdxStr = pieces[0];
      final String dbLineStr = pieces[1];
      final String roundStr = pieces[2];

      return new ImmutableTriple<>(Integer.parseInt(bracketIdxStr), Integer.parseInt(dbLineStr),
                                   Integer.parseInt(roundStr));
    } else {
      return null;
    }
  }

  /**
   * @see #constructLeafId(int, int, int)
   */
  private String constructLeafId(final int dbLine,
                                 final int round) {
    return constructLeafId(bracketIndex, dbLine, round);
  }

  /**
   * Inverse operation of {@link #parseLeafId(String)}.
   * Needs to match code in playoff/h2hutils.js that constructs a leaf ID as
   * well.
   * 
   * @param bracketIndex the bracket index on the page
   * @param dbLine the database line
   * @param round the playoff round
   * @return the identifier
   */
  public static String constructLeafId(final int bracketIndex,
                                       final int dbLine,
                                       final int round) {
    return bracketIndex
        + "-"
        + dbLine
        + "-"
        + round;
  }

  /**
   * Formats the HTML code to insert for a single table cell of one of the
   * playoff bracket display pages (both administrative and scrolling). All
   * cells are generated with a specified width of 400 pixels. If the cell
   * contains text, the \<td\>element will have attribute class='Leaf'. Font
   * tags for team number, team name, and score have classes of 'TeamNumber',
   * 'TeamName', and 'TeamScore', respectively.
   * 
   * @param connection database connection
   * @param row Row number of the bracket data we are displaying.
   * @param round Round number (column) of data we are displaying.
   * @throws SQLException If database access fails.
   */
  private void appendHtmlCell(Connection connection,
                              final StringBuilder sb,
                              final int row,
                              final int round)
      throws SQLException {
    final SortedMap<Integer, BracketDataType> roundData = bracketData.get(round);
    if (roundData == null) {
      sb.append("<td>ERROR: No data for round "
          + round
          + ".</td>\n");
      return;
    }

    final BracketDataType d = roundData.get(row);
    if (d == null) {
      sb.append("<td width='400'>&nbsp;</td>\n");
    } else if (d instanceof SpannedOverBracketCell) {
      final String comment = ((SpannedOverBracketCell) d).getComment();
      if (comment != null) {
        sb.append("<!-- "
            + comment
            + "-->");
      }
    } else if (d instanceof TeamBracketCell) {
      final TeamBracketCell tbc = (TeamBracketCell) d;
      final int dbLine = tbc.getDBLine();
      final String leafId = constructLeafId(dbLine, round);

      sb.append("<td width='400' class='Leaf js-leaf' id='"
          + leafId
          + "'>");
      if (round == finalsRound) {
        sb.append(getDisplayString(connection, currentTournament, round
            + baseRunNumber, tbc.getTeam(), showFinalScores, showOnlyVerifiedScores));
      } else if (showFinalScores
          || round != finalsRound
              + 1) {
        sb.append(getDisplayString(connection, currentTournament, round
            + baseRunNumber, tbc.getTeam(), true, showOnlyVerifiedScores));
      }
      sb.append("</td>\n");

    } else if (d instanceof BracketLabelCell) {
      sb.append("<td width='400'><font size='4'>");
      sb.append(((BracketLabelCell) d).getLabel()
          + "</font>");
      sb.append("</td>\n");
    } else if (d instanceof ScoreSheetFormBracketCell) {
      final ScoreSheetFormBracketCell myD = (ScoreSheetFormBracketCell) d;
      sb.append("<td width='400' valign='middle'");
      if (myD.getTeamA().getTeamNumber() > 0
          && myD.getTeamB().getTeamNumber() > 0) {
        sb.append(" rowspan='"
            + myD.getRowsSpanned()
            + "'>");
        sb.append("<table>\n  <tr><td colspan='3' align='center'><font size='4'>");
        sb.append(myD.getLabel());
        sb.append("</font></td>\n</tr>\n");
        sb.append("<tr>");
        sb.append("<td rowspan='2' align='center' valign='middle'>");
        sb.append("<input type='checkbox' name='print"
            + myD.getMatchNum()
            + "'");
        if (!myD.getPrinted()) {
          sb.append(" checked");
        }
        sb.append("/>");
        sb.append("<input type='hidden' name='teamA"
            + myD.getMatchNum()
            + "' value='"
            + myD.getTeamA().getTeamNumber()
            + "'/>");
        sb.append("<input type='hidden' name='teamB"
            + myD.getMatchNum()
            + "' value='"
            + myD.getTeamB().getTeamNumber()
            + "'/>");
        sb.append("<input type='hidden' name='round"
            + myD.getMatchNum()
            + "' value='"
            + round
            + "'/>");
        sb.append("</td>\n");
        sb.append("<td align='right'>Table A: </td>\n");
        sb.append("<td align='left'>");

        final List<TableInformation> tableInfo = myD.getAllTables();

        final String tableASelect = "tableA"
            + myD.getMatchNum();
        final String tableAAssigned = myD.getTableA();

        outputTableSelect(sb, tableASelect, tableAAssigned, tableInfo);

        sb.append("</td>\n</tr>\n");

        sb.append("<tr><td align='right'>Table B: </td>\n");
        sb.append("<td align='left'>");

        final String tableBSelect = "tableB"
            + myD.getMatchNum();
        final String tableBAssigned = myD.getTableB();

        outputTableSelect(sb, tableBSelect, tableBAssigned, tableInfo);
        sb.append("</td>\n</tr>\n");

        sb.append("</table>\n</td>\n");
      } else {
        // this block is not typically invoked because we use a BracketLabelCell
        // in addBracketLabelsAndScoreGenFormElements when one of the teams is
        // not present, but in theory this could be used instead to center just
        // the label in a rowspanned cell.
        sb.append(" rowspan='"
            + myD.getRowsSpanned()
            + "' align='center'>");
        sb.append("<font size='4'>");
        sb.append(myD.getLabel()
            + "</font>");
        sb.append("</td>\n");
      }
    } else if (d instanceof BigScreenTableAssignmentCell) {
      final BigScreenTableAssignmentCell tableCell = (BigScreenTableAssignmentCell) d;
      final String table = tableCell.getTable();

      // reference the row and round for the leaf that the table is for
      final String leafId = constructLeafId(tableCell.getDBLine(), tableCell.getRound());

      // always setup the html for a table assignment, just don't put the data
      // in it until it's available
      sb.append("<td align='right' style='padding-right:30px'><span class='table_assignment' id='"
          + leafId
          + "-table'>");
      if (null != table) {
        sb.append(table);
      }
      sb.append("</span></td>\n");
    }

  }

  /**
   * Output the calls that will keep all of the table selection boxes in sync.
   * 
   * @return javascript to live inside a function
   */
  public String getTableSyncFunctionsOutput() {
    final StringBuilder sb = new StringBuilder();

    for (int round = getFirstRound(); round <= getLastRound(); round++) {
      final SortedMap<Integer, BracketDataType> roundData = bracketData.get(round);

      if (roundData != null) {
        for (int row = 1; row <= getNumRows(); row++) {
          final BracketDataType d = roundData.get(row);
          if (d instanceof ScoreSheetFormBracketCell) {
            final ScoreSheetFormBracketCell myD = (ScoreSheetFormBracketCell) d;

            if (myD.getTeamA().getTeamNumber() > 0
                && myD.getTeamB().getTeamNumber() > 0) {

              final int match = myD.getMatchNum();

              sb.append(String.format("$(\"#tableA%d\").change(function() { matchTables($(\"#tableA%d\"), $(\"#tableB%d\")); });%n",
                                      match, match, match));
              sb.append(String.format("$(\"#tableB%d\").change(function() { matchTables($(\"#tableB%d\"), $(\"#tableA%d\")); });%n",
                                      match, match, match));
            } // 2 valid teams
          } // ScoreSheet bracket cell
        } // foreach row
      } // valid round data
    } // foreach round

    return sb.toString();
  }

  /**
   * Generate the bracket output. This method is to be called once the object is
   * configured. The result of this method can be retrieved later with
   * {@link #getBracketOutput()}.
   * 
   * @param connection database connection
   * @param topRightCornerStyle how to connect the top right corner
   * @throws SQLException on a database error
   */
  public void generateBracketOutput(final Connection connection,
                                    final TopRightCornerStyle topRightCornerStyle)
      throws SQLException {
    final StringBuilder sb = new StringBuilder();

    sb.append("<table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>\n");
    appendHtmlHeaderRow(sb);

    for (int rowIndex = 1; rowIndex <= getNumRows(); rowIndex++) {

      sb.append("<tr>\n");

      // Get each cell. Insert bridge cells between columns.
      for (int i = getFirstRound(); i < getLastRound(); i++) {
        appendHtmlCell(connection, sb, rowIndex, i);
        appendHtmlBridgeCell(sb, rowIndex, i, topRightCornerStyle);
      }

      appendHtmlCell(connection, sb, rowIndex, getLastRound());

      sb.append("</tr>\n");
    }

    sb.append("</table>\n");

    bracketOutput = sb.toString();
  }

  private void outputTableSelect(final StringBuilder sb,
                                 final String select,
                                 final String assigned,
                                 final List<TableInformation> tableInfo) {
    sb.append("<select name='"
        + select
        + "' id='"
        + select
        + "' size='1'>");

    for (final TableInformation info : tableInfo) {
      sb.append("<option");
      if (info.getSideA().equals(assigned)) {
        sb.append(" selected");
      }
      sb.append(">"
          + info.getSideA()
          + "</option>");

      sb.append("<option");
      if (info.getSideB().equals(assigned)) {
        sb.append(" selected");
      }
      sb.append(">"
          + info.getSideB()
          + "</option>");
    }
    sb.append("</select>");
  }

  /**
   * Returns a string including a table row element with table header cells
   * providing the playoff round number.
   */
  private void appendHtmlHeaderRow(final StringBuilder sb) {
    sb.append("<tr>\n");
    for (int i = getFirstRound(); i <= getLastRound()
        && i <= finalsRound; i++) {
      sb.append("  <th colspan='2'>Head to Head Round "
          + i
          + "</th>\n");
    }
    sb.append("</tr>\n");
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
   */
  @SuppressFBWarnings(value = { "ICAST_IDIV_CAST_TO_DOUBLE" }, justification = "Double cast is OK as we are ok with the rounding")
  private void appendHtmlBridgeCell(final StringBuilder sb,
                                    final int row,
                                    final int round,
                                    final TopRightCornerStyle cs) {
    final int ar = round
        - getFirstRound();
    if (getFirstRound() < finalsRound
        && rowIsInConsolationBracket(row)) {
      if (round != finalsRound) {
        // This is a bridge cell before (or after!) the 3rd/4th place brackets -
        // it's just a blank cell
        sb.append("<td width='10'>&nbsp;</td>\n");
      } else {
        if (row == topRowOfConsolationBracket()) {
          // top of the 3rd/4th place bracket
          if (cs.equals(TopRightCornerStyle.MEET_BOTTOM_OF_CELL)) {
            sb.append("<td width='10' class='BridgeTop'>&nbsp;</td>");
          } else if (cs.equals(TopRightCornerStyle.MEET_TOP_OF_CELL)) {
            sb.append("<td width='10' class='Bridge' rowspan='"
                + (rowsPerTeam
                    + 1)
                + "'>&nbsp;</td>\n");
          } else {
            throw new RuntimeException("Unknown value for TopRightCornerStyle");
          }
        } else if (row > topRowOfConsolationBracket()
            && row <= topRowOfConsolationBracket()
                + rowsPerTeam) {
          if (cs.equals(TopRightCornerStyle.MEET_TOP_OF_CELL)) {
            sb.append("<!-- skip column for bridge -->");
          } else if (row < topRowOfConsolationBracket()
              + rowsPerTeam) {
            sb.append("<td width='10' class='BridgeMiddle'>&nbsp;</td>\n");
          } else if (row == topRowOfConsolationBracket()
              + rowsPerTeam) {
            sb.append("<td width='10' class='BridgeBottom'>&nbsp;</td>\n");
          }
        } else {
          sb.append("<td width='10'>&nbsp;</td>\n");
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
          + rowsPerTeam //
              * Math.pow(2, ar
                  + 1) //
          - rowsPerTeam //
              * Math.pow(2, ar
                  - 1) //
          + rowsPerTeam
              / 2
          - 1)) //
          % Math.round(rowsPerTeam
              * Math.pow(2, ar
                  + 1)));

      if (modVal <= Math.round(rowsPerTeam
          * Math.pow(2, ar))
          && round <= finalsRound) {
        if (cs.equals(TopRightCornerStyle.MEET_BOTTOM_OF_CELL)) {
          if (modVal >= 1
              && modVal < (rowsPerTeam
                  * (int) Math.round(Math.pow(2, ar)))) {
            // If we are in the middle a bridge use the BridgeMiddle class
            sb.append("<td width='10' class='BridgeMiddle" /*
                                                            * rowspan='" +
                                                            * (_rowsPerTeam
                                                            * *(int)
                                                            * Math.round(Math
                                                            * .pow(2, ar)))
                                                            */
                + "'>&nbsp;</td>\n");
          } else if (modVal == 0) {
            // If we are on the first line of the bridge, use the BridgeTop
            // class
            sb.append("<td width='10' class='BridgeTop'>&nbsp;</td>\n");
          } else if (modVal == (rowsPerTeam
              * (int) Math.round(Math.pow(2, ar)))) {
            // If we are on the last line of the bridge, use the BridgeBottom
            // class
            sb.append("<td width='10' class='BridgeBottom'>&nbsp;</td>\n");
          }
        } else if (cs.equals(TopRightCornerStyle.MEET_TOP_OF_CELL)
            && modVal == 0) {
          sb.append("<td width='10' class='Bridge' rowspan='"
              + (rowsPerTeam
                  * (int) Math.round(Math.pow(2, ar))
                  + 1)
              + "'>&nbsp;</td>\n");
        } else {
          sb.append("<!-- skip column for bridge -->");
        }
      } else {
        // Outside of a bridge
        sb.append("<td width='10'>&nbsp;</td>\n");
      }
    }
  }

  /**
   * @param row
   * @return true if row is below (numerically greater than) the bottom-most
   *         teamname of the first displayed round.
   */
  private boolean rowIsInConsolationBracket(final int row) {
    final int firstDisplayedRoundSize = (getFirstRoundSize()
        / ((int) Math.round(Math.pow(2, getFirstRound()
            - 1))));
    return row > (1
        + (firstDisplayedRoundSize
            - 1)
            * getRowsPerTeam());
  }

  private int topRowOfConsolationBracket() {
    final int firstDisplayedRoundSize = (getFirstRoundSize()
        / ((int) Math.round(Math.pow(2, getFirstRound()
            - 1))));
    return 3
        + (firstDisplayedRoundSize
            - 1)
            * getRowsPerTeam();
  }

  /**
   * Adds labels for the bracket numbers to the specified playoff round number.
   * If this function is used, addBracketLabelsAndScoreGenFormElements must not
   * be used.
   * 
   * @param roundNumber the round number to set the labels for
   */
  public void addBracketLabels(final int roundNumber) {
    final SortedMap<Integer, BracketDataType> roundData = bracketData.get(roundNumber);
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
            + (secondTeamRow
                - firstTeamRow)
                / 2);
      }

      if (roundNumber == finalsRound) {
        it = rows.iterator();
        roundData.put(it.next(), new BracketLabelCell(formatBracketLabel(getBracketName(), 1, roundNumber)));
        if (it.hasNext()) {
          // 3rd and 4th place is always the second match
          roundData.put(it.next(), new BracketLabelCell(formatBracketLabel(getBracketName(), 2, roundNumber)));
        }
      } else {
        int bracketNumber = 1;
        it = rows.iterator();
        while (it.hasNext()) {
          final String bracketLabel = formatBracketLabel(getBracketName(), bracketNumber, roundNumber);

          roundData.put(it.next(), new BracketLabelCell(bracketLabel));

          ++bracketNumber;
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
   * @param connection database connection
   * @throws SQLException on a database exception
   */
  public void addStaticTableLabels(final Connection connection) throws SQLException {
    if (rowsPerTeam < 4) {
      LOG.warn("Table labels cannot be added to bracket data because there are too few lines per team for them to fit.");
      return; // if there aren't enough rows-per-team to include table labels,
      // just return
    }
    for (final Map.Entry<Integer, SortedMap<Integer, BracketDataType>> bracketEntry : bracketData.entrySet()) {
      final Integer round = bracketEntry.getKey();
      // We can't modify the map while we iterate over it - we'll add these
      // after identifying all new cells
      final SortedMap<Integer, BracketDataType> newCells = new TreeMap<Integer, BracketDataType>();
      int dblinenum = 0;
      int tablelinemod = -1;
      final SortedMap<Integer, BracketDataType> roundData = bracketEntry.getValue();
      for (final Map.Entry<Integer, BracketDataType> entry : roundData.entrySet()) {
        final Integer rowNumber = entry.getKey();
        final BracketDataType cell = entry.getValue();
        if (cell != null
            && cell instanceof TeamBracketCell) {
          dblinenum++;
          tablelinemod += 2;
          if (tablelinemod > 1) {
            tablelinemod = -1;
          }
          // Get the table assignment from cell info
          final String table = Queries.getAssignedTable(connection, currentTournament, getBracketName(),
                                                        round.intValue(), dblinenum);
          newCells.put(rowNumber.intValue()
              + tablelinemod, new BigScreenTableAssignmentCell(round.intValue(), rowNumber, table, dblinenum));
        }
      }
      // Merge the new cells into the roundData
      roundData.putAll(newCells);
    }
  }

  /**
   * Create the label that shows between the brackets.
   * 
   * @param division the head to head bracket name
   * @param bracketNumber the match number
   * @param roundNumber the round number
   * @return the label to display
   */
  private String formatBracketLabel(final String division,
                                    final int bracketNumber,
                                    final int roundNumber) {
    final String bracketLabel;
    if (roundNumber == finalsRound
        && bracketNumber == 1) {
      bracketLabel = "1st/2nd Place";
    } else if (roundNumber == finalsRound
        && bracketNumber == 2) {
      bracketLabel = "3rd/4th Place";
    } else {
      bracketLabel = String.format("%s Round %d Match %d", division, roundNumber, bracketNumber);
    }

    return bracketLabel;
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
   * @param pConnection database connection
   * @param tournament the tournament to work with
   * @param division the bracket name
   */
  public int addBracketLabelsAndScoreGenFormElements(final Connection pConnection,
                                                     final int tournament,
                                                     final String division)
      throws SQLException {
    // Get the list of tournament tables
    final List<TableInformation> tournamentTables = TableInformation.getTournamentTableInformation(pConnection,
                                                                                                   tournament,
                                                                                                   division);

    final List<TableInformation> tablesToUse = tournamentTables.stream().filter(t -> t.getUse())
                                                               .collect(Collectors.toList());
    if (tablesToUse.isEmpty()
        && !tournamentTables.isEmpty()) {
      LOG.warn("Tables are defined, but none are set to be used by bracket "
          + division
          + ". This is unexpected, using all tables");
      tablesToUse.addAll(tournamentTables);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Division: "
          + division
          + " all Tables: "
          + tournamentTables
          + " use tables: "
          + tablesToUse);
    }

    // Prevent divide by 0 errors if no tables were set in the database.
    if (tablesToUse.isEmpty()) {
      tablesToUse.add(new TableInformation(0, "Table 1", "Table 2", true));
    }

    Iterator<TableInformation> tAssignIt = tablesToUse.iterator();

    // Build the cells...
    int matchNum = 1;
    for (int roundNumber = getFirstRound(); roundNumber <= getLastRound()
        && roundNumber <= finalsRound; roundNumber++) {
      final SortedMap<Integer, BracketDataType> roundData = bracketData.get(roundNumber);
      if (roundData != null) {
        final List<Integer[]> rows = new LinkedList<Integer[]>();
        final Iterator<Integer> it = roundData.keySet().iterator();
        while (it.hasNext()) {
          final Integer firstTeamRow = it.next();
          if (!it.hasNext()) {
            throw new RuntimeException("Mismatched team in head to head brackets. Check database for corruption.");
          }
          final Integer secondTeamRow = it.next();
          rows.add(new Integer[] { firstTeamRow, secondTeamRow });
        }

        int bracketNumber = 1;
        final Iterator<Integer[]> rit = rows.iterator();
        Integer[] curArray;
        while (rit.hasNext()) {
          final String bracketLabel = formatBracketLabel(division, bracketNumber, roundNumber);
          curArray = rit.next();
          // Build the cell - if both teams are not present, just do a normal
          // bracket label cell
          if (((TeamBracketCell) roundData.get(curArray[0])).getTeam().getTeamNumber() > 0
              && ((TeamBracketCell) roundData.get(curArray[1])).getTeam().getTeamNumber() > 0) {

            if (!tAssignIt.hasNext()) {
              tAssignIt = tablesToUse.iterator();
            }

            final String storedTableA = ((TeamBracketCell) roundData.get(curArray[0])).getTable();
            final String storedTableB = ((TeamBracketCell) roundData.get(curArray[1])).getTable();

            final String tableA;
            final String tableB;
            if (null == storedTableA
                && null != storedTableB) {
              tableB = storedTableB;

              final TableInformation info = TableInformation.getTableInformationForTableSide(tournamentTables,
                                                                                             storedTableB);
              if (null == info) {
                throw new FLLInternalException("Cannot find information for table "
                    + storedTableB);
              }

              if (info.getSideA().equals(storedTableB)) {
                tableA = info.getSideB();
              } else {
                tableA = info.getSideA();
              }
            } else if (null != storedTableA
                && null == storedTableB) {
              tableA = storedTableA;

              final TableInformation info = TableInformation.getTableInformationForTableSide(tournamentTables,
                                                                                             storedTableA);
              if (null == info) {
                throw new FLLInternalException("Cannot find information for table "
                    + storedTableA);
              }

              if (info.getSideA().equals(storedTableA)) {
                tableB = info.getSideB();
              } else {
                tableB = info.getSideA();
              }
            } else if (null != storedTableA
                && null != storedTableB) {
              tableA = storedTableA;
              tableB = storedTableB;
            } else {
              // assign both
              if (!tAssignIt.hasNext()) {
                tAssignIt = tablesToUse.iterator();
              }
              final TableInformation info = tAssignIt.next();

              tableA = info.getSideA();
              tableB = info.getSideB();

              if (LOG.isTraceEnabled()) {
                LOG.trace("Assigning tables "
                    + tableA
                    + ", "
                    + tableB
                    + " to bracket "
                    + bracketLabel);
              }
            }

            final TeamBracketCell topCell = (TeamBracketCell) roundData.get(curArray[0]);
            final TeamBracketCell bottomCell = (TeamBracketCell) roundData.get(curArray[1]);
            roundData.put(curArray[0]
                + 1, new ScoreSheetFormBracketCell(tournamentTables, bracketLabel, matchNum++, topCell.getPrinted()
                    && bottomCell.getPrinted(), tableA, tableB, topCell.getTeam(), bottomCell.getTeam(),
                                                   curArray[1].intValue()
                                                       - curArray[0].intValue()
                                                       - 1));
            // Put place holders for the rows that are to be spanned over
            for (int j = curArray[0].intValue()
                + 2; j < curArray[1].intValue(); j++) {
              roundData.put(j, new SpannedOverBracketCell("spanned row"
                  + j));
            }
          } else {
            final int firstRow = curArray[0].intValue();
            final int lastRow = curArray[1].intValue();
            final int line = firstRow
                + (lastRow
                    - firstRow)
                    / 2;

            roundData.put(line, new BracketLabelCell(bracketLabel));
          }
          bracketNumber++;
        } // foreach row
      } // roundData not null
    } // foreach round

    return matchNum
        - 1;
  }

  /**
   * Obtains string to display for a team's info, given a team number. This
   * handles a TIE, null and BYE. This function respects the set value for
   * whether to display or hide verified scores. If unverified scores are
   * displayed, they will be shown as red text.
   * 
   * @param connection database connection
   * @param currentTournament the current tournament
   * @param runNumber the current performance run, used to get the score
   * @param team team to get display string for
   * @param showScore if the score should be shown
   * @throws IllegalArgumentException if teamNumber is invalid
   * @throws SQLException on a database error
   */
  private String getDisplayString(Connection connection,
                                  final int currentTournament,
                                  final int runNumber,
                                  final @Nullable Team team,
                                  final boolean showScore,
                                  final boolean showOnlyVerifiedScores)
      throws IllegalArgumentException, SQLException {
    if (Team.BYE.equals(team)) {
      return "<span class='TeamName'>BYE</span>";
    } else if (Team.TIE.equals(team)) {
      return "<span class='TIE'>TIE</span>";
    } else if (null == team
        || Team.NULL.equals(team)) {
      return "&nbsp;";
    } else {

      final StringBuffer sb = new StringBuffer();
      sb.append("<span class='TeamNumber'>#");
      sb.append(team.getTeamNumber());
      sb.append("</span>&nbsp;<span class='TeamName'>");
      sb.append(StringUtils.trimString(team.getTeamName(), Team.MAX_TEAM_NAME_LEN));
      sb.append("</span>");

      final boolean performanceScoreExists = Queries.performanceScoreExists(connection, currentTournament, team,
                                                                            runNumber);
      sb.append("<!-- performance score exists: "
          + performanceScoreExists
          + " -->\n");

      final boolean scoreVerified = Queries.isVerified(connection, currentTournament, team, runNumber);
      sb.append("<!-- verified: "
          + scoreVerified
          + " -->\n");

      if (showScore
          && performanceScoreExists
          && (!showOnlyVerifiedScores
              || scoreVerified)
          && !Playoff.isBye(connection, currentTournament, team, runNumber)) {
        if (!scoreVerified) {
          sb.append("<span style='color:red'>");
        }
        sb.append("<span class='TeamScore'>&nbsp;Score: ");
        if (Playoff.isNoShow(connection, currentTournament, team, runNumber)) {
          sb.append("No Show");
        } else {
          // only display score if it's not a bye
          sb.append(Playoff.getPerformanceScore(connection, currentTournament, team, runNumber));
        }
        sb.append("</span>");
        if (!scoreVerified) {
          sb.append("</span>");
        }
      }
      return sb.toString();
    }
  }

}
