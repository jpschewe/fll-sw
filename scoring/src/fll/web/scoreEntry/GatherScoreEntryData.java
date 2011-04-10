/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Gather the data required for scoreEntry.jsp.
 * 
 * @web.servlet name="GatherScoreEntryData"
 * @web.servlet-mapping url-pattern="/scoreEntry/GatherScoreEntryData"
 */
public class GatherScoreEntryData extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    try {
      final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

      session.setAttribute("EditFlag", request.getParameter("EditFlag"));
      
      // support the unverified runs select box
      final String lTeamNum = request.getParameter("TeamNumber");
      if (null == lTeamNum) {
        session.setAttribute(SessionAttributes.MESSAGE,
                             "<p name='error' class='error'>Attempted to load score entry page without providing a team number.</p>");
        response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
        return;
      }
      final int dashIndex = lTeamNum.indexOf('-');
      final int teamNumber;
      final String runNumberStr;
      if (dashIndex > 0) {
        // teamNumber - runNumber
        final String teamStr = lTeamNum.substring(0, dashIndex);
        teamNumber = Integer.parseInt(teamStr);
        runNumberStr = lTeamNum.substring(dashIndex + 1);
      } else {
        runNumberStr = request.getParameter("RunNumber");
        teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(lTeamNum).intValue();
      }
      final DataSource datasource = SessionAttributes.getDataSource(session);
      final Connection connection = datasource.getConnection();
      final int tournament = Queries.getCurrentTournament(connection);
      final int numSeedingRounds = Queries.getNumSeedingRounds(connection, tournament);
      session.setAttribute("numSeedingRounds", numSeedingRounds);
      final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
      if (!tournamentTeams.containsKey(new Integer(teamNumber))) {
        throw new RuntimeException("Selected team number is not valid: "
            + teamNumber);
      }
      final Team team = tournamentTeams.get(new Integer(teamNumber));
      session.setAttribute("team", team);

      // the next run the team will be competing in
      final int nextRunNumber = Queries.getNextRunNumber(connection, team.getTeamNumber());

      // what run number we're going to edit/enter
      int lRunNumber;
      if ("1".equals(request.getParameter("EditFlag"))) {
        if (null == runNumberStr) {
          session.setAttribute(SessionAttributes.MESSAGE,
                               "<p name='error' class='error'>Please choose a run number when editing scores</p>");
          response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
          return;
        }
        final int runNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(runNumberStr).intValue();
        if (runNumber == 0) {
          lRunNumber = nextRunNumber - 1;
          if (lRunNumber < 1) {
            session.setAttribute(SessionAttributes.MESSAGE,
                                 "<p name='error' class='error'>Selected team has no performance score for this tournament.</p>");
            response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
            return;
          }
        } else {
          if (!Queries.performanceScoreExists(connection, teamNumber, runNumber)) {
            session.setAttribute(SessionAttributes.MESSAGE, "<p name='error' class='error'>Team has not yet competed in run "
                + runNumber + ".  Please choose a valid run number.</p>");
            response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
            return;
          }
          lRunNumber = runNumber;
        }
      } else {
        if (nextRunNumber > numSeedingRounds) {
          if (!Queries.isPlayoffDataInitialized(connection, Queries.getEventDivision(connection, teamNumber))) {
            session.setAttribute(SessionAttributes.MESSAGE,
                                 "Selected team has completed its seeding runs. The playoff brackets"
                                     + " must be initialized from the playoff page"
                                     + " before any more scores may be entered for this team (#"
                                     + teamNumber
                                     + ")."
                                     + " If you were intending to double check a score, you probably just forgot to check"
                                     + " the box for doing so.</p>");
            response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
            return;
          } else if (!Queries.didTeamReachPlayoffRound(connection, nextRunNumber, teamNumber,
                                                       Queries.getEventDivision(connection, teamNumber))) {
            session.setAttribute(SessionAttributes.MESSAGE,
                                 "<p name='error' class='error'>Selected team has not advanced to the next playoff round.</p>");
            response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
            return;
          }
        }
        lRunNumber = nextRunNumber;
      }
      session.setAttribute("lRunNumber", lRunNumber);

      final String roundText;
      if (lRunNumber > numSeedingRounds) {
        roundText = "Playoff&nbsp;Round&nbsp;"
            + (lRunNumber - numSeedingRounds);
      } else {
        roundText = "Run&nbsp;Number&nbsp;"
            + lRunNumber;
      }
      session.setAttribute("roundText", roundText);

      final String minimumAllowedScoreStr = ((Element) challengeDocument.getDocumentElement()
                                                                        .getElementsByTagName("Performance").item(0)).getAttribute("minimumScore");
      final int minimumAllowedScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(minimumAllowedScoreStr).intValue();
      session.setAttribute("minimumAllowedScore", minimumAllowedScore);

      // check if this is the last run a team has completed
      final int maxRunCompleted = Queries.getMaxRunNumber(connection, teamNumber);
      session.setAttribute("isLastRun", Boolean.valueOf(lRunNumber == maxRunCompleted));

      // check if the score being edited is a bye
      session.setAttribute("isBye", Boolean.valueOf(Queries.isBye(connection, tournament, teamNumber, lRunNumber)));
      session.setAttribute("isNoShow",
                           Boolean.valueOf(Queries.isNoShow(connection, tournament, teamNumber, lRunNumber)));

      // check if previous run is verified
      final boolean previousVerified;
      if (lRunNumber > 1) {
        previousVerified = Queries.isVerified(connection, tournament, teamNumber, lRunNumber - 1);
      } else {
        previousVerified = true;
      }
      session.setAttribute("previousVerified", previousVerified);

      if (lRunNumber <= numSeedingRounds) {
        if ("1".equals(request.getParameter("EditFlag"))) {
          session.setAttribute("top_info_color", "yellow");
        } else {
          session.setAttribute("top_info_color", "#e0e0e0");
        }
      } else {
        session.setAttribute("top_info_color", "#00ff00");
      }

      if ("1".equals(request.getParameter("EditFlag"))) {
        session.setAttribute("body_background", "yellow");
      } else {
        session.setAttribute("body_background", "transparent");
      }

      response.sendRedirect(response.encodeRedirectURL("scoreEntry.jsp"));
    } catch (final ParseException pe) {
      throw new RuntimeException(pe);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
