/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Utilities;
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

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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

      pageContext.setAttribute("runTypes", RunMetadata.RunType.values());

      computePerformanceRoundValues(pageContext, headerNames);

      final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session,
                                                                                          UploadScheduleData.KEY,
                                                                                          UploadScheduleData.class);

      final Collection<String> numberColumns = new LinkedList<>();
      final Collection<String> timeColumns = new LinkedList<>();
      final Collection<String> tableColumns = new LinkedList<>();
      checkHeaders(headerNames, uploadScheduleData.getHeaderRowIndex(), uploadScheduleData.getScheduleFile(),
                   uploadScheduleData.getSelectedSheet(), numberColumns, timeColumns, tableColumns);
      pageContext.setAttribute("numberColumns", numberColumns);
      pageContext.setAttribute("timeColumns", timeColumns);
      pageContext.setAttribute("tableColumns", tableColumns);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  /**
   * @param allHeaders can be an immutable collection
   * @param returnNumberColumns pass in an empty mutable collect, upon return will
   *          contain the number columns (possibly empty)
   * @param returnTimeColumns pass in an empty mutable collect, upon return will
   *          contain the time columns (possibly empty)
   * @param returnTableColumns pass in an empty mutable collect, upon return will
   *          contain the table columns (possibly empty)
   */
  private static void checkHeaders(final Collection<String> allHeaders,
                                   final int headerRowIndex,
                                   final File scheduleFile,
                                   final @Nullable String sheetName,
                                   final Collection<String> returnNumberColumns,
                                   final Collection<String> returnTimeColumns,
                                   final Collection<String> returnTableColumns) {

    try (CellFileReader reader = CellFileReader.createCellReader(scheduleFile, sheetName)) {
      reader.skipRows(headerRowIndex);
      final @Nullable String @Nullable [] headerRow = reader.readNext();
      if (null == headerRow) {
        LOGGER.error("No header row specified, returning all columns for all types");
        returnNumberColumns.addAll(allHeaders);
        returnTimeColumns.addAll(allHeaders);
        returnTableColumns.addAll(allHeaders);
        return;
      }

      // start all columns out as number and time columns
      final boolean[] isNumberColumn = new boolean[headerRow.length];
      Arrays.fill(isNumberColumn, true);
      final boolean[] isTimeColumn = new boolean[headerRow.length];
      Arrays.fill(isTimeColumn, true);
      final boolean[] isTableColumn = new boolean[headerRow.length];
      Arrays.fill(isTableColumn, true);

      boolean someNumberColumns = true;
      boolean someTimeColumns = true;
      boolean someTableColumns = true;

      try {
        @Nullable
        String @Nullable [] line;
        while (someNumberColumns
            && someTimeColumns
            && someTableColumns
            && null != (line = reader.readNext())) {

          for (int i = 0; i < line.length; ++i) {
            final @Nullable String value = line[i];
            if (null == value) {
              isNumberColumn[i] = false;
              isTimeColumn[i] = false;
            } else {
              if (someTimeColumns) {
                try {
                  final @Nullable LocalTime parsed = TournamentSchedule.parseTime(value);
                  if (null == parsed) {
                    // don't allow null times
                    isTimeColumn[i] = false;
                  }
                } catch (final DateTimeParseException e) {
                  isTimeColumn[i] = false;
                }
              }

              if (someNumberColumns) {
                if (StringUtils.isBlank(value)) {
                  isNumberColumn[i] = false;
                } else {
                  try {
                    Utilities.getIntegerNumberFormat().parse(value);
                  } catch (final ParseException e) {
                    isNumberColumn[i] = false;
                  }
                }
              }

              if (someTableColumns) {
                if (StringUtils.isBlank(value)) {
                  isTableColumn[i] = false;
                } else {
                  final @Nullable ImmutablePair<String, Number> tableInfo = TournamentSchedule.parseTable(value);
                  if (null == tableInfo) {
                    isTableColumn[i] = false;
                  }
                }
              }
            }

            // check if done
            if (someNumberColumns) {
              // if any values are true, there are potentially some number columns
              someNumberColumns = BooleanUtils.or(isNumberColumn);
            }
            if (someTimeColumns) {
              // if any values are true, there are potentially some time columns
              someTimeColumns = BooleanUtils.or(isTimeColumn);
            }
            if (someTableColumns) {
              // if any values are true, there are potentially some table columns
              someTableColumns = BooleanUtils.or(isTableColumn);
            }
          }
        }

      } catch (final IOException e) {
        LOGGER.warn("Error reading data for column types, returning the best guess at this point", e);
      } finally {
        // collect results
        if (someNumberColumns
            || someTimeColumns
            || someTableColumns) {

          for (int i = 0; i < headerRow.length; ++i) {
            final @Nullable String header = headerRow[i];
            if (null != header) {
              if (isNumberColumn[i]) {
                returnNumberColumns.add(header);
              }
              if (isTimeColumn[i]) {
                returnTimeColumns.add(header);
              }
              if (isTableColumn[i]) {
                returnTableColumns.add(header);
              }
            }
          }

        }
      }

    } catch (final InvalidFormatException | IOException e) {
      LOGGER.warn("Error reading header column, returning all columns for all types", e);
      returnNumberColumns.addAll(allHeaders);
      returnTimeColumns.addAll(allHeaders);
      returnTableColumns.addAll(allHeaders);
    }

  }

  private static void computePerformanceRoundValues(final PageContext page,
                                                    final Collection<String> headerNames) {
    final Map<Integer, String> performanceRoundValues = new HashMap<>();
    final Map<Integer, RunMetadata.RunType> performanceRoundRunType = new HashMap<>();
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
      performanceRoundRunType.put(roundNumber, RunMetadata.RunType.PRACTICE);
      performanceRoundScoreboard.put(roundNumber, true);
      ++roundNumber;
    }

    // performance rounds next
    for (final String header : performanceTimeHeaders) {
      performanceRoundValues.put(roundNumber, header);
      performanceRoundRunType.put(roundNumber, RunMetadata.RunType.REGULAR_MATCH_PLAY);
      performanceRoundScoreboard.put(roundNumber, true);
      ++roundNumber;
    }

    // fun run rounds next
    for (final String header : funRunTimeHeaders) {
      performanceRoundValues.put(roundNumber, header);
      performanceRoundRunType.put(roundNumber, RunMetadata.RunType.OTHER);
      performanceRoundScoreboard.put(roundNumber, true);
      ++roundNumber;
    }

    page.setAttribute("performanceRound_values", performanceRoundValues);
    page.setAttribute("performanceRound_runType", performanceRoundRunType);
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

        final boolean scoreboardDisplay = null != request.getParameter(String.format("perf%d_scoreboard", round));

        final String runTypeStr = WebUtils.getNonNullRequestParameter(request, String.format("perf%d_runType", round));
        final RunMetadata.RunType runType = RunMetadata.RunType.valueOf(runTypeStr);

        final RunMetadata runMetadata = new RunMetadata(round, displayName, scoreboardDisplay, runType);
        tournamentData.getRunMetadataFactory().storeRunMetadata(runMetadata);
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
