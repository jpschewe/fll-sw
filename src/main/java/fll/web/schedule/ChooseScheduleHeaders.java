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
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.db.CategoryColumnMapping;
import fll.db.TournamentParameters;
import fll.scheduler.SchedParams;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
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
   * @param application application attributes
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    pageContext.setAttribute("default_duration", SchedParams.DEFAULT_SUBJECTIVE_MINUTES);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID());
      pageContext.setAttribute("numSeedingRounds", numSeedingRounds);

      final int numPracticeRounds = TournamentParameters.getNumPracticeRounds(connection, tournament.getTournamentID());
      pageContext.setAttribute("numPracticeRounds", numPracticeRounds);

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID());
      final int numPracticeRounds = TournamentParameters.getNumPracticeRounds(connection, tournament.getTournamentID());

      final ChallengeDescription challenge = ApplicationAttributes.getChallengeDescription(application);

      final @Nullable String[] headerLine = getHeaderLine(uploadScheduleData);

      final String teamNumber = WebUtils.getNonNullRequestParameter(request, "teamNumber");
      final String teamName = WebUtils.getNonNullRequestParameter(request, "teamName");
      final String organization = WebUtils.getNonNullRequestParameter(request, "organization");
      final String awardGroup = WebUtils.getNonNullRequestParameter(request, "awardGroup");
      final String judgingGroup = WebUtils.getNonNullRequestParameter(request, "judgingGroup");

      final String[] practiceColumn = new String[numPracticeRounds];
      final String[] practiceTableColumn = new String[numPracticeRounds];
      for (int i = 0; i < numPracticeRounds; ++i) {
        final int round = i
            + 1;
        practiceColumn[i] = WebUtils.getNonNullRequestParameter(request, String.format("practice%d", round));
        practiceTableColumn[i] = WebUtils.getNonNullRequestParameter(request, String.format("practiceTable%d", round));
      }

      final String[] perfColumn = new String[numSeedingRounds];
      final String[] perfTableColumn = new String[numSeedingRounds];
      for (int i = 0; i < numSeedingRounds; ++i) {
        final int round = i
            + 1;
        perfColumn[i] = WebUtils.getNonNullRequestParameter(request, String.format("perf%d", round));
        perfTableColumn[i] = WebUtils.getNonNullRequestParameter(request, String.format("perfTable%d", round));
      }

      final Collection<CategoryColumnMapping> subjectiveColumnMappings = challenge.getSubjectiveCategories().stream() //
                                                                                  .map(cat -> {
                                                                                    final String column = WebUtils.getNonNullRequestParameter(request,
                                                                                                                                              String.format("%s:header",
                                                                                                                                                            cat.getName()));
                                                                                    return new CategoryColumnMapping(cat.getName(),
                                                                                                                     column);
                                                                                  }) //
                                                                                  .collect(Collectors.toList());

      final ColumnInformation columnInfo = new ColumnInformation(uploadScheduleData.getHeaderRowIndex(), headerLine,
                                                                 teamNumber,
                                                                 StringUtils.isBlank(organization) ? null
                                                                     : organization,
                                                                 StringUtils.isBlank(teamName) ? null : teamName,
                                                                 StringUtils.isBlank(awardGroup) ? null : awardGroup,
                                                                 StringUtils.isBlank(judgingGroup) ? null
                                                                     : judgingGroup,
                                                                 subjectiveColumnMappings, perfColumn, perfTableColumn,
                                                                 practiceColumn, practiceTableColumn);

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
