/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import fll.Tournament;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 
 */
abstract class BaseScheduleServlet extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Calls
   * {@link #outputSchedule(Connection, TournamentData, ChallengeDescription, TournamentSchedule, OutputStream)}
   * to write the schedule.
   */
  @Override
  protected final void processRequest(final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final ServletContext application,
                                      final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC, UserRole.SCORING_COORDINATOR), false)) {
      return;
    }

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    final DataSource datasource = tournamentData.getDataSource();
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = tournamentData.getCurrentTournament();

      final int currentTournamentID = tournament.getTournamentID();

      if (!TournamentSchedule.scheduleExistsInDatabase(connection, currentTournamentID)) {
        SessionAttributes.appendToMessage(session, "<p class='error'>There is no schedule for this tournament.</p>");
        WebUtils.sendRedirect(application, response, "/admin/index.jsp");
        return;
      }

      final TournamentSchedule schedule = new TournamentSchedule(connection, currentTournamentID);

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", getFilename(tournament)));
      outputSchedule(connection, tournamentData, description, schedule, response.getOutputStream());

    } catch (final SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new FLLRuntimeException(sqle);
    }
  }

  abstract void outputSchedule(Connection connection,
                               TournamentData tournamentData,
                               ChallengeDescription description,
                               TournamentSchedule schedule,
                               OutputStream output)
      throws SQLException, IOException;

  /**
   * @param tournament the tournament
   * @return the filename that is sent with the download back to the browser
   */
  abstract String getFilename(Tournament tournament);
}
