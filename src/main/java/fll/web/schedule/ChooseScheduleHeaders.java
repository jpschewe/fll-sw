/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.db.CategoryColumnMapping;
import fll.db.RunMetadata;
import fll.scheduler.SchedParams;
import fll.scheduler.TournamentSchedule;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.StoreColumnNames;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for chooseScheduleHeaders.jsp.
 */
@WebServlet("/schedule/ChooseScheduleHeaders")
public final class ChooseScheduleHeaders extends BaseFLLServlet {

  /**
   * Setup page variables used by the JSP.
   *
   * @param session session attributes
   * @param application application attributes
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    pageContext.setAttribute("default_duration", SchedParams.DEFAULT_SUBJECTIVE_MINUTES);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    @SuppressWarnings("unchecked") // cannot store generics in session
    final Collection<String> headerNames = SessionAttributes.getNonNullAttribute(session,
                                                                                 StoreColumnNames.HEADER_NAMES_KEY,
                                                                                 Collection.class);

    try (Connection connection = datasource.getConnection()) {
      for (final String header : headerNames) {
        if (TournamentSchedule.TEAM_NUMBER_HEADER.equals(header)) {
          pageContext.setAttribute("teamNumber_value", header);
        } else if (TournamentSchedule.TEAM_NAME_HEADER.equals(header)) {
          pageContext.setAttribute("teamName_value", header);
        } else if (TournamentSchedule.ORGANIZATION_HEADER.equals(header)) {
          pageContext.setAttribute("organization_value", header);
        } else if (TournamentSchedule.AWARD_GROUP_HEADER.equals(header)) {
          pageContext.setAttribute("awardGroup_value", header);
        } else if (TournamentSchedule.JUDGE_GROUP_HEADER.equals(header)) {
          pageContext.setAttribute("judgingGroup_value", header);
        } else if (TournamentSchedule.WAVE_HEADER.equals(header)) {
          pageContext.setAttribute("wave_value", header);
        }
      }

      // set judging group and award group to be the same if only 1 is specified
      final @Nullable Object awardGroupColumn = pageContext.getAttribute("awardGroup_value");
      final @Nullable Object judgingGroupColumn = pageContext.getAttribute("judgingGroup_value");
      if (null == awardGroupColumn
          && null != judgingGroupColumn) {
        pageContext.setAttribute("awardGroup_value", judgingGroupColumn);
      } else if (null != awardGroupColumn
          && null == judgingGroupColumn) {
        pageContext.setAttribute("judgingGroup_value", awardGroupColumn);
      }

      computePerformanceRoundValues(pageContext, headerNames);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  private static void computePerformanceRoundValues(final PageContext page,
                                                    final Collection<String> headerNames) {
    final Map<Integer, String> performanceRoundValues = new HashMap<>();
    final Map<Integer, Boolean> performanceRoundRegularMatch = new HashMap<>();
    final Map<Integer, Boolean> performanceRoundScoreboard = new HashMap<>();

    final List<String> practiceTimeHeaders = headerNames.stream() //
                                                        .filter(h -> h.startsWith(TournamentSchedule.BASE_PRACTICE_HEADER_SHORT)
                                                            && !h.endsWith("Table")) //
                                                        .sorted().toList();

    final List<String> performanceTimeHeaders = headerNames.stream() //
                                                           .filter(h -> h.startsWith(TournamentSchedule.BASE_PERF_HEADER)
                                                               && !h.endsWith("Table")) //
                                                           .sorted().toList();

    final List<String> funRunTimeHeaders = headerNames.stream() //
                                                      .filter(h -> h.startsWith("Fun Run")
                                                          && !h.endsWith("Table")) //
                                                      .sorted().toList();

    // check for practice rounds first
    int roundNumber = 1;
    for (final String header : practiceTimeHeaders) {
      performanceRoundValues.put(roundNumber, header);
      performanceRoundRegularMatch.put(roundNumber, false);
      performanceRoundScoreboard.put(roundNumber, false);
      ++roundNumber;
    }

    // performance rounds next
    for (final String header : performanceTimeHeaders) {
      performanceRoundValues.put(roundNumber, header);
      performanceRoundRegularMatch.put(roundNumber, true);
      performanceRoundScoreboard.put(roundNumber, true);
      ++roundNumber;
    }

    // fun run rounds next
    for (final String header : funRunTimeHeaders) {
      performanceRoundValues.put(roundNumber, header);
      performanceRoundRegularMatch.put(roundNumber, false);
      performanceRoundScoreboard.put(roundNumber, true);
      ++roundNumber;
    }

    page.setAttribute("performanceRound_values", performanceRoundValues);
    page.setAttribute("performanceRound_regularMatch", performanceRoundRegularMatch);
    page.setAttribute("performanceRound_scoreboard", performanceRoundScoreboard);
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final ChallengeDescription challenge = ApplicationAttributes.getChallengeDescription(application);

      final @Nullable String[] headerLine = getHeaderLine(uploadScheduleData);

      final String teamNumber = WebUtils.getNonNullRequestParameter(request, "teamNumber");
      final String teamName = WebUtils.getNonNullRequestParameter(request, "teamName");
      final String organization = WebUtils.getNonNullRequestParameter(request, "organization");
      final String awardGroup = WebUtils.getNonNullRequestParameter(request, "awardGroup");
      final String judgingGroup = WebUtils.getNonNullRequestParameter(request, "judgingGroup");
      final String wave = WebUtils.getNonNullRequestParameter(request, "wave");

      final String[] perfColumn = new String[uploadScheduleData.getNumPerformanceRuns()];
      final String[] perfTableColumn = new String[uploadScheduleData.getNumPerformanceRuns()];
      for (int i = 0; i < uploadScheduleData.getNumPerformanceRuns(); ++i) {
        final int round = i
            + 1;
        perfColumn[i] = WebUtils.getNonNullRequestParameter(request, String.format("perf%d_time", round));
        perfTableColumn[i] = WebUtils.getNonNullRequestParameter(request, String.format("perf%d_table", round));

        final String displayName = WebUtils.getNonNullRequestParameter(request, String.format("perf%d_name", round));
        final boolean regularMatchPlay = null != request.getParameter(String.format("perf%d_regularMatchPlay", round));
        final boolean scoreboardDisplay = null != request.getParameter(String.format("perf%d_scoreboard", round));
        // head to head runs are never scheduled
        final RunMetadata runMetadata = new RunMetadata(round, displayName, regularMatchPlay, scoreboardDisplay, false);
        tournamentData.storeRunMetadata(runMetadata);
      }

      final Collection<CategoryColumnMapping> subjectiveColumnMappings = new LinkedList<>();
      for (final SubjectiveScoreCategory category : challenge.getSubjectiveCategories()) {
        final String column = WebUtils.getNonNullRequestParameter(request,
                                                                  String.format("%s:header", category.getName()));
        final CategoryColumnMapping mapping = new CategoryColumnMapping(category.getName(), column);
        subjectiveColumnMappings.add(mapping);

        final String column2 = WebUtils.getNonNullRequestParameter(request,
                                                                   String.format("%s:header2", category.getName()));
        if (!"none".equals(column2)) {
          final CategoryColumnMapping mapping2 = new CategoryColumnMapping(category.getName(), column2);
          subjectiveColumnMappings.add(mapping2);
        }
      }

      final ColumnInformation columnInfo = new ColumnInformation(uploadScheduleData.getHeaderRowIndex(), headerLine,
                                                                 teamNumber,
                                                                 StringUtils.isBlank(organization) ? null
                                                                     : organization,
                                                                 StringUtils.isBlank(teamName) ? null : teamName,
                                                                 StringUtils.isBlank(awardGroup) ? null : awardGroup,
                                                                 StringUtils.isBlank(judgingGroup) ? null
                                                                     : judgingGroup,
                                                                 StringUtils.isBlank(wave) ? null : wave,
                                                                 subjectiveColumnMappings, perfColumn, perfTableColumn);

      uploadScheduleData.setColumnInformation(columnInfo);

      WebUtils.sendRedirect(application, response, "/schedule/specifySubjectiveStationDurations.jsp");
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }

  private @Nullable String[] getHeaderLine(final UploadScheduleData uploadScheduleData) {
    try (CellFileReader reader = CellFileReader.createCellReader(uploadScheduleData.getScheduleFile(),
                                                                 uploadScheduleData.getSelectedSheet())) {
      reader.skipRows(uploadScheduleData.getHeaderRowIndex());
      final @Nullable String @Nullable [] headerRow = reader.readNext();
      if (null == headerRow) {
        throw new FLLRuntimeException(String.format("Header row is null, cannot continue. Header row index is %d",
                                                    uploadScheduleData.getHeaderRowIndex()));
      } else {
        return headerRow;
      }
    } catch (IOException | InvalidFormatException e) {
      throw new FLLRuntimeException("Error reading schedule file", e);
    }
  }
}
