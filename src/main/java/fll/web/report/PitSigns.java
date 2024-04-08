/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.ScheduleWriter;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.util.FOPUtils.Margins;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Generate pit signs.
 */
@WebServlet("/report/PitSigns")
public class PitSigns extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    response.reset();
    response.setContentType("application/pdf");

    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final @Nullable TournamentSchedule schedule;
      if (TournamentSchedule.scheduleExistsInDatabase(connection, tournament.getTournamentID())) {
        schedule = new TournamentSchedule(connection, tournament.getTournamentID());
      } else {
        schedule = null;
      }

      final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

      final Element rootElement = FOPUtils.createRoot(document);
      document.appendChild(rootElement);

      final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
      rootElement.appendChild(layoutMasterSet);

      final String pageMasterName = "simple";
      final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName,
                                                                 FOPUtils.PAGE_LANDSCAPE_LETTER_SIZE,
                                                                 new Margins(1, 0.2, 0.5, 0.5), 0, 0);
      layoutMasterSet.appendChild(pageMaster);

      final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
      rootElement.appendChild(pageSequence);
      pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

      final Element documentBody = FOPUtils.createBody(document);
      pageSequence.appendChild(documentBody);

      // Allow the user to specify a team number, if the parameter isn't found,
      // then render all pit signs
      final @Nullable String teamNumberStr = request.getParameter("team_number");
      if (null == teamNumberStr) {
        response.setHeader("Content-Disposition", "filename=pit_signs.pdf");

        for (final TournamentTeam team : Queries.getTournamentTeams(connection, tournament.getTournamentID())
                                                .values()) {
          final Element page = renderTeam(document, schedule, team);
          documentBody.appendChild(page);
          page.setAttribute("page-break-after", "always");
        }
      } else {
        final int teamNumber = Integer.parseInt(teamNumberStr);
        response.setHeader("Content-Disposition", String.format("filename=pit_sign_%d.pdf", teamNumber));

        final TournamentTeam team = TournamentTeam.getTournamentTeamFromDatabase(connection, tournament, teamNumber);

        final Element page = renderTeam(document, schedule, team);
        documentBody.appendChild(page);
      }

      try {
        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the pit signs PDF", e);
      }

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  private Element renderTeam(final Document document,
                             final @Nullable TournamentSchedule schedule,
                             final TournamentTeam team) {

    final Element page = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    page.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_CENTER);
    page.setAttribute("font-size", "28pt");

    // logo
    // FIXME

    // organization
    final @Nullable String organization = team.getOrganization();
    if (null != organization) {
      final Element orgBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      page.appendChild(orgBlock);
      orgBlock.appendChild(document.createTextNode(organization));
    }

    // team name
    final Element nameBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    page.appendChild(nameBlock);
    nameBlock.appendChild(document.createTextNode(team.getTeamName()));
    nameBlock.setAttribute("font-weight", "bold");
    nameBlock.setAttribute("font-size", "36pt");

    // team number
    final Element numberBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    page.appendChild(numberBlock);
    numberBlock.appendChild(document.createTextNode(String.format("Team Number: %d", team.getTeamNumber())));

    // judging group
    final Element judgingGroupBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    page.appendChild(judgingGroupBlock);
    judgingGroupBlock.appendChild(document.createTextNode(String.format("Judging Group: %s", team.getJudgingGroup())));
    final @Nullable String wave = team.getWave();
    if (null != wave) {
      final Element waveText = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
      judgingGroupBlock.appendChild(waveText);
      waveText.setAttribute("font-size", "14pt");
      waveText.appendChild(document.createTextNode(String.format("%swave %s",
                                                                 String.valueOf(Utilities.NON_BREAKING_SPACE).repeat(4),
                                                                 wave)));
    }

    // header text 1
    // header text 2 - ? do two lines or allow carriage return in the text?
    // FIXME
    // 14pt

    // schedule
    if (null != schedule) {
      final Element scheduleContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
      page.appendChild(scheduleContainer);
      scheduleContainer.setAttribute("font-size", "18pt");
      final @Nullable TeamScheduleInfo si = schedule.getSchedInfoForTeam(team.getTeamNumber());
      if (null == si) {
        scheduleContainer.appendChild(document.createTextNode("No schedule"));
      } else {
        ScheduleWriter.appendTeamSchedule(document, schedule, si, scheduleContainer);
      }
    }

    // FIXME
    // 18pt

    // footer text
    // FIXME
    // 14pt

    // HTK logo - FIRST logo
    // FIXME

    return page;
  }

}
