/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.awt.Color;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.ScoreStandardization;
import fll.Tournament;
import fll.Utilities;
import fll.db.AwardDeterminationOrder;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.util.FP;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.web.playoff.DatabaseTeamScore;
import fll.web.report.awards.AwardCategory;
import fll.web.report.awards.AwardsScriptReport;
import fll.xml.ChallengeDescription;
import fll.xml.Goal;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;
import fll.xml.ScoreType;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;
import fll.xml.WinnerType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Final computed scores report.
 */
@WebServlet("/report/FinalComputedScores")
public final class FinalComputedScores extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private record ScoreData(AwardCategory category,
                           int rank,
                           double rankFraction,
                           String scoreText,
                           boolean missing) {
  }

  private record TeamScoreData(int teamNumber,
                               String teamName,
                               String organization,
                               String judgingGroup,
                               List<ScoreData> subjectiveRawScoreData,
                               List<ScoreData> subjectiveScaledScoreData,
                               ScoreData performanceRawScoreData,
                               ScoreData performanceScaledScoreData,
                               double overallScore,
                               double weightedRank) {
  }

  private static final Comparator<TeamScoreData> OVERALL_COMPARATOR = Comparator.comparingDouble(TeamScoreData::overallScore)
                                                                                .reversed();

  private static final Comparator<TeamScoreData> RANK_COMPARATOR = Comparator.comparingDouble(TeamScoreData::weightedRank);

  /**
   * If 2 scores are within this amount of each other they are
   * considered a tie.
   */
  public static final double TIE_TOLERANCE = 1E-6;

  private static final double HEADER_MARGIN_INCHES = 1;

  private static final double FOOTER_MARGIN_INCHES = 1;

  private static final DateTimeFormatter REPORT_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder().appendValue(ChronoField.MONTH_OF_YEAR,
                                                                                                                 2)
                                                                                                    .appendLiteral('/')
                                                                                                    .appendValue(ChronoField.DAY_OF_MONTH,
                                                                                                                 2)
                                                                                                    .appendLiteral('/')
                                                                                                    .appendValue(ChronoField.YEAR,
                                                                                                                 4)
                                                                                                    .appendLiteral(' ')
                                                                                                    .appendValue(ChronoField.HOUR_OF_DAY,
                                                                                                                 2)
                                                                                                    .appendLiteral(':')
                                                                                                    .appendValue(ChronoField.MINUTE_OF_HOUR,
                                                                                                                 2)
                                                                                                    .toFormatter();

  /**
   * Group name for all values of the group.
   */
  public static final String ALL_GROUP_NAME = "_all_";

  /**
   * Used to specify the order of the report.
   */
  public enum SortOrder {
    /** Sort the report by weighted rank. */
    WEIGHTED_RANK,
    /** Sort the report by overall score. */
    OVERALL_SCORE,
  }

  /**
   * Used to specify how to select teams for the report.
   */
  public enum ReportSelector {
    /** Select teams by award group. */
    AWARD_GROUP("Award Group", "event_division"),

    /** Select teams by judging station. */
    JUDGING_STATION("Judging Group", "judging_station");

    private final String title;

    /**
     * @return title to put in the report
     */
    public String getTitle() {
      return title;
    }

    private final String column;

    /**
     * @return database column to filter teams by
     */
    public String getColumn() {
      return column;
    }

    /**
     * @param title {@see #getTitle()}
     * @param column {@see #getColumn()}
     */
    ReportSelector(final String title,
                   final String column) {
      this.title = title;
      this.column = column;
    }
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE, UserRole.REPORT_GENERATOR), false)) {
      return;
    }

    final ReportSelector selector = ReportSelector.valueOf(WebUtils.getNonNullRequestParameter(request, "selector"));
    final SortOrder sortOrder = SortOrder.valueOf(WebUtils.getNonNullRequestParameter(request, "sortOrder"));
    final String groupName = WebUtils.getNonNullRequestParameter(request, "groupName");

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final DataSource datasource = tournamentData.getDataSource();

    try (Connection connection = datasource.getConnection()) {
      ScoreStandardization.computeSummarizedScoresIfNeeded(connection, challengeDescription,
                                                          tournamentData.getCurrentTournament());

      final Tournament tournament = tournamentData.getCurrentTournament();

      final int percentageHurdle = TournamentParameters.getPerformanceAdvancementPercentage(connection,
                                                                                            tournament.getTournamentID());
      final double performanceHurdle;
      if (percentageHurdle > 0
          && percentageHurdle < 100) {
        // set to a realistic value
        performanceHurdle = percentageHurdle
            / 100.0;
      } else {
        performanceHurdle = 0;
      }

      final Set<Integer> bestTeams = determineTeamsMeetingPerformanceHurdle(performanceHurdle, connection,
                                                                            tournament.getTournamentID(),
                                                                            challengeDescription.getWinner());

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=finalComputedScores.pdf");

      final String challengeTitle = challengeDescription.getTitle();

      try {
        final Document document = generateReport(connection, challengeDescription, challengeTitle, tournament,
                                                 bestTeams, percentageHurdle, groupName, selector, sortOrder);
        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();
        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the final scores PDF", e);
      }

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Determine which teams meet the performance hurdle.
   * This is computed per division and stored in mTeamsMeetingPerformanceHurdle.
   *
   * @param performanceHurdle the percentage hurdle as a floating point number
   *          between 0 and 1. Outside this range causes the return value to be
   *          empty.
   * @return the set of teams that have a good enough performance score
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Winner criteria determines the sort")
  private Set<Integer> determineTeamsMeetingPerformanceHurdle(final double performanceHurdle,
                                                              final Connection connection,
                                                              final int tournament,
                                                              final WinnerType winnerCriteria)
      throws SQLException {

    final Set<Integer> bestTeams = new HashSet<>();
    if (performanceHurdle <= 0
        || performanceHurdle >= 1) {
      return bestTeams;
    }

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT TeamNumber FROM performance_seeding_max, TournamentTeams" //
          + " WHERE performance_seeding_max.TeamNumber = TournamentTeams.TeamNumber" //
          + "  AND TournamentTeams.Tournament = ?" //
          + "  AND TournamentTeams.Tournament = performance_seeding_max.tournament" //
          + "  AND TournamentTeams.event_division = ?" //
          + " ORDER by performance_seeding_max.score "
          + winnerCriteria.getSortString());
      prep.setInt(1, tournament);

      for (final String division : Queries.getAwardGroups(connection)) {
        final Set<Integer> teamNumbers = Queries.getTeamNumbersInEventDivision(connection, tournament, division);
        final int numTeams = teamNumbers.size();
        final int hurdle = (int) Math.floor(numTeams
            * performanceHurdle);

        prep.setString(2, division);

        int count = 0;
        rs = prep.executeQuery();
        while (count < hurdle
            && rs.next()) {
          final int teamNumber = rs.getInt(1);
          bestTeams.add(teamNumber);
          ++count;
        }
        SQLFunctions.close(rs);
        rs = null;
      } // foreach division

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    return bestTeams;
  }

  private static final Color TOP_SCORE_BACKGROUND = Color.LIGHT_GRAY;

  /**
   * Generate the actual report.
   */
  private Document generateReport(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final String challengeTitle,
                                  final Tournament tournament,
                                  final Set<Integer> bestTeams,
                                  final int percentageHurdle,
                                  final String groupName,
                                  final ReportSelector selector,
                                  final SortOrder sortOrder)
      throws SQLException, IOException {
    if (tournament.getTournamentID() != Queries.getCurrentTournament(connection)) {
      throw new FLLRuntimeException("Cannot generate final score report for a tournament other than the current tournament");
    }

    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                                               FOPUtils.STANDARD_MARGINS, HEADER_MARGIN_INCHES,
                                                               FOOTER_MARGIN_INCHES);
    layoutMasterSet.appendChild(pageMaster);

    final Element legend = createLegend(document, percentageHurdle);

    final List<AwardCategory> awardOrder = Collections.unmodifiableList(AwardDeterminationOrder.get(connection,
                                                                                                    challengeDescription));

    if (ALL_GROUP_NAME.equals(groupName)) {
      final Collection<String> groups;
      if (ReportSelector.AWARD_GROUP.equals(selector)) {
        groups = AwardsScriptReport.getAwardGroupOrder(connection, tournament);
      } else if (ReportSelector.JUDGING_STATION.equals(selector)) {
        groups = Queries.getJudgingStations(connection, tournament.getTournamentID());
      } else {
        throw new FLLInternalException(String.format("Unknown report selector: %s", selector));
      }

      for (final String group : groups) {
        final @Nullable Element pageSequence = createAwardGroupPageSequence(connection, document, awardOrder,
                                                                            challengeDescription, challengeTitle,
                                                                            winnerCriteria, tournament, pageMasterName,
                                                                            legend, bestTeams, group, selector,
                                                                            sortOrder);
        if (null != pageSequence) {
          rootElement.appendChild(pageSequence);
        }
      }
    } else {
      final @Nullable Element pageSequence = createAwardGroupPageSequence(connection, document, awardOrder,
                                                                          challengeDescription, challengeTitle,
                                                                          winnerCriteria, tournament, pageMasterName,
                                                                          legend, bestTeams, groupName, selector,
                                                                          sortOrder);
      if (null != pageSequence) {
        rootElement.appendChild(pageSequence);
      }
    }

    return document;
  }

  private @Nullable Element createAwardGroupPageSequence(final Connection connection,
                                                         final Document document,
                                                         final List<AwardCategory> awardOrder,
                                                         final ChallengeDescription challengeDescription,
                                                         final String challengeTitle,
                                                         final WinnerType winnerCriteria,
                                                         final Tournament tournament,
                                                         final String pageMasterName,
                                                         final Element legend,
                                                         final Set<Integer> bestTeams,
                                                         final String groupName,
                                                         final ReportSelector selector,
                                                         final SortOrder sortOrder)
      throws SQLException {
    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);

    final Element headerContent = createAwardGroupHeader(document, challengeTitle, tournament, groupName, selector);

    final Element header = FOPUtils.createXslFoElement(document, FOPUtils.STATIC_CONTENT_TAG);
    pageSequence.appendChild(header);
    header.setAttribute("flow-name", "xsl-region-before");
    header.appendChild(headerContent);

    pageSequence.appendChild(legend.cloneNode(true));

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    // Figure out how many subjective categories have weights > 0.
    final int nonZeroWeights = (int) challengeDescription.getSubjectiveCategories().stream()
                                                         .filter(c -> c.getWeight() > 0).count()
        + (int) challengeDescription.getVirtualSubjectiveCategories().stream().filter(c -> c.getWeight() > 0).count();

    final Element divTable = FOPUtils.createBasicTable(document);
    documentBody.appendChild(divTable);
    divTable.setAttribute("font-size", "8pt");

    divTable.appendChild(FOPUtils.createTableColumn(document, 30)); // org / team
    divTable.appendChild(FOPUtils.createTableColumn(document, 10)); // judging group
    divTable.appendChild(FOPUtils.createTableColumn(document, 10)); // weight

    for (int i = 0; i < nonZeroWeights; i++) {
      divTable.appendChild(FOPUtils.createTableColumn(document, 15));
    }

    divTable.appendChild(FOPUtils.createTableColumn(document, 15)); // performance
    divTable.appendChild(FOPUtils.createTableColumn(document, 10)); // weighted rank
    divTable.appendChild(FOPUtils.createTableColumn(document, 10)); // overall

    final Element tableHeader = createTableHeader(document, challengeDescription, awardOrder);
    divTable.appendChild(tableHeader);

    final Element tableBody = writeScores(connection, document, awardOrder, challengeDescription, groupName, selector,
                                          sortOrder, winnerCriteria, tournament, bestTeams);
    divTable.appendChild(tableBody);

    if (!tableBody.hasChildNodes()) {
      return null;
    } else {
      return pageSequence;
    }
  }

  private Element createLegend(Document document,
                               final int percentageHurdle) {
    final Element staticContent = FOPUtils.createXslFoElement(document, FOPUtils.STATIC_CONTENT_TAG);
    staticContent.setAttribute("flow-name", "xsl-region-after");

    staticContent.setAttribute("text-align", FOPUtils.TEXT_ALIGN_RIGHT);
    staticContent.setAttribute("font-size", "10pt");

    if (percentageHurdle > 0
        && percentageHurdle < 100) {
      final Element hurdleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      final String hurdleText = String.format("* - teams in the top %d%% of performance scores", percentageHurdle);
      hurdleBlock.appendChild(document.createTextNode(hurdleText));
      staticContent.appendChild(hurdleBlock);
    }

    final Element block1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    staticContent.appendChild(block1);
    block1.appendChild(document.createTextNode("bold score - top team in a category & judging group (rank)"));

    final Element block3 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    staticContent.appendChild(block3);
    block3.appendChild(document.createTextNode("@ - zero score on required goal"));

    return staticContent;
  }

  /**
   * @param connection database connection
   * @param winnerCriteria from {@link ChallengeDescription#getWinner()}
   * @param tournament the tournament to get scores for
   * @param awardGroup the award group to get scores for
   * @return team number, {rank, score}
   * @throws SQLException on a database error
   */
  public static Map<Integer, ImmutablePair<Integer, Double>> gatherRankedPerformanceTeams(final Connection connection,
                                                                                          final WinnerType winnerCriteria,
                                                                                          final Tournament tournament,
                                                                                          final String awardGroup)
      throws SQLException {
    return gatherRankedPerformanceTeams(connection, winnerCriteria, tournament, awardGroup, ReportSelector.AWARD_GROUP);
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Winner criteria determines the sort, group column determines the column")
  public static Map<Integer, ImmutablePair<Integer, Double>> gatherRankedPerformanceTeams(final Connection connection,
                                                                                          final WinnerType winnerCriteria,
                                                                                          final Tournament tournament,
                                                                                          final String groupName,
                                                                                          final ReportSelector selector)
      throws SQLException {
    final Map<Integer, ImmutablePair<Integer, Double>> rankedTeams = new HashMap<>();

    // 1 - tournament
    // 2 - award group
    // 3 - category
    // 4 - goal group
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT final_scores.team_number, final_scores.final_score"
            + " FROM final_scores, TournamentTeams" //
            + " WHERE final_scores.Tournament = ?" //
            + " AND TournamentTeams.Tournament = final_scores.tournament" //
            + String.format(" AND TournamentTeams.%s = ?", selector.getColumn()) //
            + " AND TournamentTeams.TeamNumber = final_scores.team_number"//
            + " AND final_scores.category = ?" //
            + String.format(" ORDER BY final_scores.final_score %s", winnerCriteria.getSortString()))) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, groupName);
      prep.setString(3, PerformanceScoreCategory.CATEGORY_NAME);

      int numTied = 1;
      int rank = 0;
      double prevScore = Double.NaN;
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          double score = rs.getDouble(2);
          if (rs.wasNull()) {
            score = Double.NaN;
          }

          if (!FP.equals(score, prevScore, TIE_TOLERANCE)) {
            rank += numTied;
            numTied = 1;
          } else {
            ++numTied;
          }

          rankedTeams.put(teamNumber, ImmutablePair.of(rank, score));

          prevScore = score;
        } // foreach result
      } // result set
    } // prepared statement

    return rankedTeams;
  }

  /**
   * Used with
   * {@link FinalComputedScores#iterateOverSubjectiveScores(Connection, String, WinnerType, Tournament, String, SubjectiveScoreVisitor)}.
   */
  @FunctionalInterface
  public interface SubjectiveScoreVisitor {
    /**
     * @param teamNumber the number of the team
     * @param score the score of the team
     * @param rank the rank of the team in the judging group
     */
    void visit(int teamNumber,
               double score,
               int rank);
  }

  /**
   * Iterate over the standardized scores for a subjective category.
   *
   * @param connection database connection
   * @param categoryName the name of the category to select scores for
   * @param winnerCriteria who is the winner
   * @param tournament which tournament to get scores for
   * @param judgingStation which judging station
   * @param visitor called with the data
   * @throws SQLException if a database error occurs
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "winner criteria determines the sort")
  public static void iterateOverSubjectiveScores(final Connection connection,
                                                 final String categoryName,
                                                 final WinnerType winnerCriteria,
                                                 final Tournament tournament,
                                                 final String judgingStation,
                                                 final SubjectiveScoreVisitor visitor)
      throws SQLException {

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT final_scores.team_number, final_scores.final_score"//
            + " FROM final_scores, TournamentTeams" //
            + " WHERE final_scores.tournament = ?" //
            + " AND TournamentTeams.Tournament = final_scores.tournament" //
            + " AND TournamentTeams.TeamNumber = final_scores.team_number"//
            + " AND TournamentTeams.judging_station = ?" //
            + " AND final_scores.category = ?" //
            + String.format(" ORDER BY final_scores.final_score %s", winnerCriteria.getSortString()))) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, judgingStation);
      prep.setString(3, categoryName);

      int numTied = 1;
      int rank = 0;
      double prevScore = Double.NaN;
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          double score = rs.getDouble(2);
          if (rs.wasNull()) {
            score = Double.NaN;
          }

          if (!FP.equals(score, prevScore, TIE_TOLERANCE)) {
            rank += numTied;
            numTied = 1;
          } else {
            ++numTied;
          }

          visitor.visit(teamNumber, score, rank);

          prevScore = score;
        }

      } // try ResultSet
    } // try PreparedStatment
  }

  /**
   * @param connection the database connection
   * @param description the challenge description
   * @param tournament the tournament to get scores for
   * @return category, Judging Group, team number, {rank, scaled score} - includes
   *         {@link SubjectiveScoreCategory} and
   *         {@link VirtualSubjectiveScoreCategory}
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name and winner criteria determines the sort")
  public static Map<AwardCategory, Map<String, Map<Integer, ImmutablePair<Integer, Double>>>> gatherRankedSubjectiveTeams(final Connection connection,
                                                                                                                          final ChallengeDescription description,
                                                                                                                          final Tournament tournament)
      throws SQLException {
    final WinnerType winnerCriteria = description.getWinner();
    final Map<AwardCategory, Map<String, Map<Integer, ImmutablePair<Integer, Double>>>> retval = new HashMap<>();
    final List<String> judgingStations = Queries.getJudgingStations(connection, tournament.getTournamentID());

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> categoryRanks = new HashMap<>();

      for (final String judgingStation : judgingStations) {
        final Map<Integer, ImmutablePair<Integer, Double>> rankedTeams = new HashMap<>();

        iterateOverSubjectiveScores(connection, category.getName(), winnerCriteria, tournament, judgingStation,
                                    (teamNumber,
                                     score,
                                     rank) -> {
                                      rankedTeams.put(teamNumber, ImmutablePair.of(rank, score));
                                    });

        categoryRanks.put(judgingStation, rankedTeams);
      } // foreach judging station

      retval.put(category, categoryRanks);

    } // foreach category

    for (final VirtualSubjectiveScoreCategory category : description.getVirtualSubjectiveCategories()) {
      final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> categoryRanks = new HashMap<>();

      for (final String judgingStation : judgingStations) {
        final Map<Integer, ImmutablePair<Integer, Double>> rankedTeams = new HashMap<>();

        iterateOverSubjectiveScores(connection, category.getName(), winnerCriteria, tournament, judgingStation,
                                    (teamNumber,
                                     score,
                                     rank) -> {
                                      rankedTeams.put(teamNumber, ImmutablePair.of(rank, score));
                                    });

        categoryRanks.put(judgingStation, rankedTeams);
      } // foreach judging station

      retval.put(category, categoryRanks);

    } // foreach category

    return retval;
  }

  private Element writeScores(final Connection connection,
                              final Document document,
                              final List<AwardCategory> awardOrder,
                              final ChallengeDescription description,
                              final String groupName,
                              final ReportSelector selector,
                              final SortOrder sortOrder,
                              final WinnerType winnerCriteria,
                              final Tournament tournament,
                              final Set<Integer> bestTeams)
      throws SQLException {

    final PerformanceScoreCategory performanceCategory = description.getPerformance();
    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);

    final List<TeamScoreData> reportData = gatherReportData(connection, awardOrder, description, groupName, selector,
                                                            winnerCriteria, tournament, performanceCategory);

    switch (sortOrder) {
    case WEIGHTED_RANK:
      Collections.sort(reportData, RANK_COMPARATOR);
      break;
    case OVERALL_SCORE:
      Collections.sort(reportData, OVERALL_COMPARATOR);
      break;
    default:
      LOGGER.warn("Got unknown sort for final computed scores report, falling back to overall score: {}", sortOrder);
      Collections.sort(reportData, OVERALL_COMPARATOR);
      break;
    }

    // write the report
    for (final TeamScoreData teamScoreData : reportData) {
      if (teamScoreData.subjectiveRawScoreData().size() != teamScoreData.subjectiveScaledScoreData().size()) {
        throw new FLLInternalException(String.format("raw and scaled score data lists have different lengths: %d != %d",
                                                     teamScoreData.subjectiveRawScoreData().size(),
                                                     teamScoreData.subjectiveScaledScoreData().size()));
      }

      final int teamNumber = teamScoreData.teamNumber();
      final String organization = teamScoreData.organization();
      final String teamName = teamScoreData.teamName();
      final String judgingGroup = teamScoreData.judgingGroup();

      final double overallScore = teamScoreData.overallScore();

      final Element row1 = FOPUtils.createTableRow(document);
      tableBody.appendChild(row1);
      row1.setAttribute("keep-with-next", "always");

      // The first row of the team table...
      // First column is organization name
      row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, organization));

      row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, judgingGroup));

      row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Raw:"));

      final List<ScoreData> rawSubjScoreData = teamScoreData.subjectiveRawScoreData();
      for (final ScoreData sd : rawSubjScoreData) {
        final Element subjCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, sd.scoreText());
        if (sd.missing()) {
          subjCell.setAttribute("color", "red");
        }
        row1.appendChild(subjCell);
      }

      final ScoreData perfRawScoreData = teamScoreData.performanceRawScoreData();
      final Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, perfRawScoreData.scoreText());
      row1.appendChild(cell);
      if (perfRawScoreData.missing()) {
        cell.setAttribute("color", "red");
      }

      // The "Overall score" column is not filled in for raw scores
      row1.appendChild(FOPUtils.createTableCell(document, null, ""));

      // The second row of the team table...
      final Element row2 = FOPUtils.createTableRow(document);
      tableBody.appendChild(row2);

      // First column contains the team # and name
      final String row2BorderColor = "gray";
      final double row2BorderWidth = 0.5;

      final Element teamNameCol = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                           String.format("%d %s", teamNumber, teamName));
      row2.appendChild(teamNameCol);
      FOPUtils.addBottomBorder(teamNameCol, row2BorderWidth, row2BorderColor);

      // judging group is empty in the second row
      final Element blankCell = FOPUtils.createTableCell(document, null, "");
      row2.appendChild(blankCell);
      FOPUtils.addBottomBorder(blankCell, row2BorderWidth, row2BorderColor);

      // "Scaled:"
      final Element scaledCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Scaled:");
      row2.appendChild(scaledCell);
      FOPUtils.addBottomBorder(scaledCell, row2BorderWidth, row2BorderColor);

      // scaled subjective scores
      for (final ScoreData subjScaledData : teamScoreData.subjectiveScaledScoreData()) {
        final AwardCategory awardCategory = subjScaledData.category();
        switch (awardCategory) {
        case SubjectiveScoreCategory category -> {
          final double catWeight = category.getWeight();
          if (catWeight > 0.0) {

            final Element subjCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              subjScaledData.scoreText());
            row2.appendChild(subjCell);
            if (1 == subjScaledData.rank()) {
              subjCell.setAttribute("font-weight", "bold");
              subjCell.setAttribute("background-color", FOPUtils.renderColor(TOP_SCORE_BACKGROUND));
            }
            FOPUtils.addBottomBorder(subjCell, row2BorderWidth, row2BorderColor);
            if (subjScaledData.missing()) {
              subjCell.setAttribute("color", "red");
            }
          } // non-zero category weight
        }
        case VirtualSubjectiveScoreCategory category -> {
          final double catWeight = category.getWeight();
          if (catWeight > 0.0) {
            final Element subjCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              subjScaledData.scoreText());
            row2.appendChild(subjCell);
            if (1 == subjScaledData.rank()) {
              subjCell.setAttribute("font-weight", "bold");
              subjCell.setAttribute("background-color", FOPUtils.renderColor(TOP_SCORE_BACKGROUND));
            }
            FOPUtils.addBottomBorder(subjCell, row2BorderWidth, row2BorderColor);
            if (subjScaledData.missing()) {
              subjCell.setAttribute("color", "red");
            }

          } // non-zero category weight
        }
        default -> {
          LOGGER.debug("Skipping category of type {}", awardCategory.getClass());
        }
        }
      }

      // 2nd to last column has the scaled performance score
      final ScoreData perfData = teamScoreData.performanceScaledScoreData();
      final int perfRank = perfData.rank();

      final Element perfCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, perfData.scoreText());
      row2.appendChild(perfCell);
      if (1 == perfRank) {
        perfCell.setAttribute("font-weight", "bold");
        perfCell.setAttribute("background-color", FOPUtils.renderColor(TOP_SCORE_BACKGROUND));
      }
      FOPUtils.addBottomBorder(perfCell, row2BorderWidth, row2BorderColor);
      if (perfData.missing()) {
        perfCell.setAttribute("color", "red");
      }

      // insert weighted rank
      final double weightedRank = teamScoreData.weightedRank();
      final Element weightedRankCell;
      if (Double.isNaN(weightedRank)) {
        weightedRankCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Missing Score");
        weightedRankCell.setAttribute("color", "red");
      } else {
        weightedRankCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                    Utilities.getFloatingPointNumberFormat().format(weightedRank));
      }
      row2.appendChild(weightedRankCell);
      FOPUtils.addBottomBorder(weightedRankCell, row2BorderWidth, row2BorderColor);

      // Last column contains the overall scaled score
      final Element overallCell;
      if (Double.isNaN(overallScore)) {
        overallCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "No Score");
        overallCell.setAttribute("color", "red");
      } else {
        final String overallScoreSuffix;
        if (bestTeams.contains(teamNumber)) {
          overallScoreSuffix = String.format("%1$s*", Utilities.NON_BREAKING_SPACE);
        } else {
          overallScoreSuffix = String.format("%1$s%1$s", Utilities.NON_BREAKING_SPACE);
        }

        overallCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                               Utilities.getFloatingPointNumberFormat().format(overallScore)
                                                   + overallScoreSuffix);
      }
      row2.appendChild(overallCell);
      FOPUtils.addBottomBorder(overallCell, row2BorderWidth, row2BorderColor);

    }

    return tableBody;
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table name")
  private List<TeamScoreData> gatherReportData(final Connection connection,
                                               final List<AwardCategory> awardOrder,
                                               final ChallengeDescription description,
                                               final String groupName,
                                               final ReportSelector selector,
                                               final WinnerType winnerCriteria,
                                               final Tournament tournament,
                                               final PerformanceScoreCategory performanceCategory)
      throws SQLException {
    final Map<AwardCategory, Map<String, Map<Integer, ImmutablePair<Integer, Double>>>> teamSubjectiveRanks = gatherRankedSubjectiveTeams(connection,
                                                                                                                                          description,
                                                                                                                                          tournament);

    final Map<Integer, ImmutablePair<Integer, Double>> teamPerformanceRanks = gatherRankedPerformanceTeams(connection,
                                                                                                           winnerCriteria,
                                                                                                           tournament,
                                                                                                           groupName,
                                                                                                           selector);

    final List<TeamScoreData> reportData = new LinkedList<>();

    try (
        PreparedStatement overallPrep = connection.prepareStatement("SELECT Teams.Organization, Teams.TeamName, Teams.TeamNumber, overall_score, TournamentTeams.judging_station" //
            + " FROM overall_scores, Teams, TournamentTeams" //
            + " WHERE overall_scores.tournament = ?"//
            + " AND TournamentTeams.tournament = overall_scores.tournament" //
            + String.format(" AND TournamentTeams.%s = ?", selector.getColumn()) //
            + " AND TournamentTeams.TeamNumber = Teams.TeamNumber" //
            + " AND TournamentTeams.TeamNumber = overall_scores.team_number" //
            + " ORDER BY overall_scores.overall_score "
            + winnerCriteria.getSortString() //
            + ", Teams.TeamNumber" //
        )) {
      overallPrep.setInt(1, tournament.getTournamentID());
      overallPrep.setString(2, groupName);

      // compute data
      try (ResultSet overallResult = overallPrep.executeQuery()) {
        while (overallResult.next()) {
          final int teamNumber = overallResult.getInt(3);
          final String organization = overallResult.getString(1);
          final String teamName = castNonNull(overallResult.getString(2));
          final String judgingGroup = castNonNull(overallResult.getString(5));

          final double overallScore;
          final double ts = overallResult.getDouble(4);
          if (overallResult.wasNull()) {
            LOGGER.warn("Team {} has no overall score", teamNumber);
            overallScore = Double.NaN;
          } else {
            overallScore = ts;
          }

          final List<ScoreData> rawSubjScoreData = computeRawSubjectiveScoreColumns(connection, awardOrder, tournament,
                                                                                    winnerCriteria.getSortString(),
                                                                                    teamNumber);

          final ScoreData perfRawScoreData = computeRawPerformanceScore(connection, tournament, performanceCategory,
                                                                        performanceCategory.getScoreType(), teamNumber);

          double weightedRankSum = 0;
          int elementsInWeightedRank = 0;

          // scaled subjective scores
          final List<ScoreData> subjectiveScaledScoreData = new LinkedList<>();
          for (final AwardCategory awardCategory : awardOrder) {
            switch (awardCategory) {
            case SubjectiveScoreCategory category -> {
              final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> catRanks = teamSubjectiveRanks.get(category);
              if (null == catRanks) {
                throw new FLLInternalException("team subjective rank data is not consistent with the subjective categories (catRanks). This suggests a bug in gatherRankedSubjectiveTeams.");
              }

              final Map<Integer, ImmutablePair<Integer, Double>> judgingRanks = catRanks.get(judgingGroup);
              if (null == judgingRanks) {
                throw new FLLInternalException("team subjective rank data is not consistent with the subjective categories (judgingRanks). This suggests a bug in gatherRankedSubjectiveTeams.");
              }

              final double catWeight = category.getWeight();
              if (catWeight > 0.0) {

                final ScoreData scoreData = computeCategoryScaledScore(awardCategory, teamNumber, judgingRanks);
                final int catRank = scoreData.rank();
                subjectiveScaledScoreData.add(scoreData);

                if (catRank > 0) {
                  weightedRankSum += catWeight
                      * scoreData.rank();
                } else {
                  weightedRankSum = Double.NaN;
                }
                ++elementsInWeightedRank;

              } // non-zero category weight
            }
            case VirtualSubjectiveScoreCategory category -> {
              final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> catRanks = teamSubjectiveRanks.get(category);
              if (null == catRanks) {
                throw new FLLInternalException("team subjective rank data is not consistent with the subjective categories (catRanks). This suggests a bug in gatherRankedSubjectiveTeams.");
              }

              final Map<Integer, ImmutablePair<Integer, Double>> judgingRanks = catRanks.get(judgingGroup);
              if (null == judgingRanks) {
                throw new FLLInternalException("team subjective rank data is not consistent with the subjective categories (judgingRanks). This suggests a bug in gatherRankedSubjectiveTeams.");
              }

              final double catWeight = category.getWeight();
              if (catWeight > 0.0) {
                final ScoreData scoreData = computeCategoryScaledScore(awardCategory, teamNumber, judgingRanks);
                final int catRank = scoreData.rank();
                subjectiveScaledScoreData.add(scoreData);

                if (catRank > 0) {
                  weightedRankSum += catWeight
                      * scoreData.rank();
                } else {
                  weightedRankSum = Double.NaN;
                }
                ++elementsInWeightedRank;

              } // non-zero category weight
            }
            default -> {
              LOGGER.debug("Skipping category of type {}", awardCategory.getClass());
            }
            }
          }

          final ScoreData perfScaledData = computeCategoryScaledScore(performanceCategory, teamNumber,
                                                                      teamPerformanceRanks);
          final int perfRank = perfScaledData.rank();

          if (perfRank > 0) {

            weightedRankSum += performanceCategory.getWeight()
                * perfScaledData.rank();
          } else {
            weightedRankSum = Double.NaN;
          }
          ++elementsInWeightedRank;

          // insert weighted rank
          final double weightedRank;
          if (Double.isNaN(weightedRankSum)) {
            weightedRank = Double.NaN;
          } else {
            weightedRank = weightedRankSum
                / elementsInWeightedRank;
          }

          final TeamScoreData teamScoreData = new TeamScoreData(teamNumber, teamName,
                                                                null == organization ? "" : organization, judgingGroup,
                                                                rawSubjScoreData, subjectiveScaledScoreData,
                                                                perfRawScoreData, perfScaledData, overallScore,
                                                                weightedRank);
          reportData.add(teamScoreData);
        } // foreach score result
      } // ResultSet
    } // PreparedStatement
    return reportData;
  }

  private ScoreData computeCategoryScaledScore(final AwardCategory category,
                                               final Integer teamNumber,
                                               final Map<Integer, ImmutablePair<Integer, Double>> rankInCategory) {
    final double scaledScore;
    final int rank;
    if (rankInCategory.containsKey(teamNumber)) {
      final ImmutablePair<Integer, Double> pair = rankInCategory.get(teamNumber);

      scaledScore = pair.getRight();
      rank = pair.getLeft();
    } else {
      scaledScore = Double.NaN;
      rank = -1;
    }

    final String rankText;
    if (-1 == rank) {
      rankText = String.format("%1$s%1$s%1$s%1$s%1$s", Utilities.NON_BREAKING_SPACE);
    } else {
      rankText = String.format("%1$s(%2$d/%3$d)", Utilities.NON_BREAKING_SPACE, rank, rankInCategory.size());
    }

    final String overallScoreText;
    if (Double.isNaN(scaledScore)) {
      overallScoreText = "No Score";
    } else {
      overallScoreText = Utilities.getFloatingPointNumberFormat().format(scaledScore);
    }

    final String scoreText = overallScoreText
        + rankText;

    final double rankFraction = (double) rank
        / (double) rankInCategory.size();
    return new ScoreData(category, rank, rankFraction, scoreText, Double.isNaN(scaledScore));
  }

  private ScoreData computeRawPerformanceScore(final Connection connection,
                                               final Tournament tournament,
                                               final PerformanceScoreCategory category,
                                               final ScoreType performanceScoreType,
                                               final int teamNumber)
      throws SQLException {
    try (PreparedStatement scorePrep = connection.prepareStatement("SELECT score" //
        + " FROM performance_seeding_max"
        + " WHERE TeamNumber = ?"//
        + " AND performance_seeding_max.tournament = ?")) {

      // Column for the highest performance score of the seeding rounds
      scorePrep.setInt(1, teamNumber);
      scorePrep.setInt(2, tournament.getTournamentID());

      try (ResultSet rawScoreRS = scorePrep.executeQuery()) {
        final double rawScore;
        if (rawScoreRS.next()) {
          final double v = rawScoreRS.getDouble(1);
          if (rawScoreRS.wasNull()) {
            rawScore = Double.NaN;
          } else {
            rawScore = v;
          }
        } else {
          rawScore = Double.NaN;
        }

        final String text;
        if (Double.isNaN(rawScore)) {
          text = "No Score";
        } else {
          text = Utilities.getFormatForScoreType(performanceScoreType).format(rawScore);
        }

        return new ScoreData(category, -1, Double.NaN, text, Double.isNaN(rawScore));

      } // ResultSet
    } // PreparedStatement
  }

  private Element createTableHeader(final Document document,
                                    final ChallengeDescription challengeDescription,
                                    final List<AwardCategory> awardOrder)
      throws SQLException {

    final Element tableHeader = FOPUtils.createXslFoElement(document, "table-header");

    final Element row1 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row1);
    row1.setAttribute("font-weight", "bold");
    row1.setAttribute("keep-with-next", "always");

    row1.appendChild(FOPUtils.createTableCell(document, null, "Organization"));

    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Judging Group"));

    row1.appendChild(FOPUtils.createTableCell(document, null, "")); // weight/raw&scaled

    for (final AwardCategory awardCategory : awardOrder) {
      switch (awardCategory) {
      case SubjectiveScoreCategory category -> {
        final double weight = category.getWeight();
        if (weight > 0.0) {
          final String catTitle = category.getTitle();

          row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, catTitle));
        }
      }
      case VirtualSubjectiveScoreCategory category -> {
        final double weight = category.getWeight();
        if (weight > 0.0) {
          final String catTitle = category.getTitle();

          row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, catTitle));
        }
      }
      default -> {
        LOGGER.debug("Skipping category of type {}", awardCategory.getClass());
      }
      }
    }

    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                              PerformanceScoreCategory.CATEGORY_TITLE));

    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Weighted Rank"));
    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Overall Score"));

    // row 2 needs a bottom border, the border is added to each cell

    final Element row2 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row2);

    final Element teamNumCell = FOPUtils.createTableCell(document, null, "Team # / Team Name");
    row2.appendChild(teamNumCell);
    FOPUtils.addBottomBorder(teamNumCell, 1);
    teamNumCell.setAttribute("font-weight", "bold");

    final Element blankJudgingGroup = FOPUtils.createTableCell(document, null, "");
    row2.appendChild(blankJudgingGroup);
    FOPUtils.addBottomBorder(blankJudgingGroup, 1);

    final Element weightCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Weight:");
    FOPUtils.addBottomBorder(weightCell, 1);
    row2.appendChild(weightCell);

    for (final AwardCategory awardCategory : awardOrder) {
      switch (awardCategory) {
      case SubjectiveScoreCategory category -> {
        if (category.getWeight() > 0.0) {
          final Element catCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                           Double.toString(category.getWeight()));
          FOPUtils.addBottomBorder(catCell, 1);
          row2.appendChild(catCell);
        }
      }
      case VirtualSubjectiveScoreCategory category -> {
        if (category.getWeight() > 0.0) {
          final Element catCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                           Double.toString(category.getWeight()));
          FOPUtils.addBottomBorder(catCell, 1);
          row2.appendChild(catCell);
        }
      }
      default -> {
        LOGGER.debug("Skipping category of type {}", awardCategory.getClass());
      }
      }
    }

    final PerformanceScoreCategory performanceElement = challengeDescription.getPerformance();
    final Element perfCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                      Double.toString(performanceElement.getWeight()));
    FOPUtils.addBottomBorder(perfCell, 1);
    row2.appendChild(perfCell);

    final Element blankWeightedRank = FOPUtils.createTableCell(document, null, "");
    FOPUtils.addBottomBorder(blankWeightedRank, 1);
    row2.appendChild(blankWeightedRank);

    final Element blankOverallScore = FOPUtils.createTableCell(document, null, "");
    FOPUtils.addBottomBorder(blankOverallScore, 1);
    row2.appendChild(blankOverallScore);

    return tableHeader;
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Winner type is used to determine sort order")
  private List<ScoreData> computeRawSubjectiveScoreColumns(final Connection connection,
                                                           final List<AwardCategory> awardOrder,
                                                           final Tournament tournament,
                                                           final String ascDesc,
                                                           final int teamNumber)
      throws SQLException {

    final List<ScoreData> subjScoreData = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT computed_total"
        + " FROM subjective_computed_scores"
        + " WHERE team_number = ? AND tournament = ? AND category = ? ORDER BY computed_total "
        + ascDesc)) {

      for (final AwardCategory awardCategory : awardOrder) {
        switch (awardCategory) {
        case SubjectiveScoreCategory category -> {
          final double catWeight = category.getWeight();
          if (catWeight > 0.0) {
            final String catName = category.getName();
            prep.setInt(1, teamNumber);
            prep.setInt(2, tournament.getTournamentID());
            prep.setString(3, catName);
            try (ResultSet rs = prep.executeQuery()) {
              boolean scoreSeen = false;
              final StringBuilder rawScoreText = new StringBuilder();
              while (rs.next()) {
                final double v = rs.getDouble(1);
                if (!rs.wasNull()) {
                  if (scoreSeen) {
                    rawScoreText.append(", ");
                  } else {
                    scoreSeen = true;
                  }
                  rawScoreText.append(Utilities.getFormatForScoreType(category.getScoreType()).format(v));
                }
              }

              final String scoreText;
              if (!scoreSeen) {
                scoreText = "No Score";
              } else {
                final boolean zeroInRequiredGoal = checkZeroInRequiredGoal(connection, tournament, category,
                                                                           teamNumber);
                if (zeroInRequiredGoal) {
                  rawScoreText.append(" @");
                }
                scoreText = rawScoreText.toString();
              }

              subjScoreData.add(new ScoreData(awardCategory, -1, Double.NaN, scoreText, !scoreSeen));
            } // ResultSet
          } // category weight greater than 0
        }
        case VirtualSubjectiveScoreCategory category -> {
          final double catWeight = category.getWeight();
          if (catWeight > 0.0) {
            final String catName = category.getName();
            prep.setInt(1, teamNumber);
            prep.setInt(2, tournament.getTournamentID());
            prep.setString(3, catName);
            try (ResultSet rs = prep.executeQuery()) {
              boolean scoreSeen = false;
              final StringBuilder rawScoreText = new StringBuilder();
              while (rs.next()) {
                final double v = rs.getDouble(1);
                if (!rs.wasNull()) {
                  if (scoreSeen) {
                    rawScoreText.append(", ");
                  } else {
                    scoreSeen = true;
                  }
                  rawScoreText.append(Utilities.getFormatForScoreType(category.getScoreType()).format(v));
                }
              }

              final String scoreText;
              if (!scoreSeen) {
                scoreText = "No Score";
              } else {
                scoreText = rawScoreText.toString();
              }

              subjScoreData.add(new ScoreData(awardCategory, -1, Double.NaN, scoreText, !scoreSeen));
            } // ResultSet
          } // category weight greater than 0
        }
        default -> {
          LOGGER.debug("Skipping category of type {}", awardCategory.getClass());
        }
        } // switch
      }

    } // PreparedStatement

    return subjScoreData;
  }

  /**
   * Check if there is a zero score for a required goal in the specified category.
   *
   * @param connection the database connection
   * @param tournament which tournament is being worked with
   * @param category the category to check
   * @param teamNumber the team to check
   * @return true if there is a zero in a required goal
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  public static boolean checkZeroInRequiredGoal(final Connection connection,
                                                final Tournament tournament,
                                                final ScoreCategory category,
                                                final int teamNumber)
      throws SQLException {
    final Set<Goal> requiredGoals = category.getAllGoals().stream().filter(g -> g instanceof Goal).map(g -> (Goal) g)
                                            .filter(Goal::isRequired).collect(Collectors.toSet());

    if (!requiredGoals.isEmpty()) {
      boolean zeroInRequiredGoal = false;

      try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
          + category.getName()
          + " WHERE TeamNumber = ? AND Tournament = ?")) {
        prep.setInt(1, teamNumber);
        prep.setInt(2, tournament.getTournamentID());
        try (ResultSet rs = prep.executeQuery()) {
          while (!zeroInRequiredGoal
              && rs.next()) {
            final DatabaseTeamScore score = new DatabaseTeamScore(teamNumber, rs);

            final Iterator<Goal> iter = requiredGoals.iterator();
            while (!zeroInRequiredGoal
                && iter.hasNext()) {
              final Goal goal = iter.next();
              final double goalScore = score.getRawScore(goal.getName());
              if (FP.equals(0, goalScore, TIE_TOLERANCE)) {
                zeroInRequiredGoal = true;
              }
            }
          }

        } // result set
      } // prepared statement

      return zeroInRequiredGoal;
    } else {
      // no required goals
      return false;
    }
  }

  private Element createAwardGroupHeader(final Document document,
                                         final String challengeTitle,
                                         final Tournament tournament,
                                         final String groupName,
                                         final ReportSelector selector) {
    final String sidePadding = "3pt";
    final double borderWidth = 0.5;

    final Element header = FOPUtils.createBasicTable(document);
    header.appendChild(FOPUtils.createTableColumn(document, 1));
    header.appendChild(FOPUtils.createTableColumn(document, 1));
    FOPUtils.addBorders(header, borderWidth, borderWidth, borderWidth, borderWidth);

    header.setAttribute("font-family", "Times");
    header.setAttribute("font-size", "12pt");

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    header.appendChild(tableBody);

    final Element row1 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row1);
    row1.setAttribute("keep-with-next", "always");

    final Element challengeTitleCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, challengeTitle);
    row1.appendChild(challengeTitleCell);
    challengeTitleCell.setAttribute("padding-left", sidePadding);

    final Element titleCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
    row1.appendChild(titleCell);
    titleCell.setAttribute("padding-right", sidePadding);

    final Element titleCellContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    titleCell.appendChild(titleCellContainer);

    final Element titleCellBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    titleCellContainer.appendChild(titleCellBlock);
    titleCellBlock.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_RIGHT);

    final Element tournamentDescriptionEle = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    titleCellBlock.appendChild(tournamentDescriptionEle);
    final String titleText;
    final @Nullable String description = tournament.getDescription();
    if (null != description) {
      titleText = description;
    } else {
      titleText = tournament.getName();
    }
    tournamentDescriptionEle.appendChild(document.createTextNode(titleText));

    final Element timestampEle = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    titleCellBlock.appendChild(timestampEle);
    timestampEle.setAttribute("font-size", "8pt");
    timestampEle.appendChild(document.createTextNode(REPORT_TIMESTAMP_FORMATTER.format(LocalDateTime.now())));
    timestampEle.setAttribute("padding-left", sidePadding);

    final Element row2 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row2);

    final Element finalScoresCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                             "Final Computed Scores");
    row2.appendChild(finalScoresCell);
    finalScoresCell.setAttribute("padding-left", sidePadding);

    final Element awardGroupCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                            String.format("%s: %s", selector.getTitle(), groupName));
    row2.appendChild(awardGroupCell);
    awardGroupCell.setAttribute("padding-right", sidePadding);

    return header;
  }

} // class FinalComputedScores
