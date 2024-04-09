/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
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
import fll.UserImages;
import fll.Utilities;
import fll.db.Queries;
import fll.db.TournamentParameters;
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
import fll.web.Welcome;
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

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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
                                                                 new Margins(0.2, 0.2, 0.5, 0.5), 0, 0);
      layoutMasterSet.appendChild(pageMaster);

      final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
      rootElement.appendChild(pageSequence);
      pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

      final Element documentBody = FOPUtils.createBody(document);
      pageSequence.appendChild(documentBody);

      final @Nullable String challengeImageBase64 = getImageAsBase64(UserImages.CHALLENGE_LOGO_FILENAME);
      LOGGER.warn("Image logo found? {}", null != challengeImageBase64);

      final @Nullable String partnerImageBase64 = getImageAsBase64(Welcome.PARTNER_LOGO_FILENAME);
      final @Nullable String firstImageBase64 = getImageAsBase64(Welcome.FLL_LOGO_FILENAME);

      final String topText = TournamentParameters.getPitSignTopText(connection, tournament.getTournamentID());
      final String bottomText = TournamentParameters.getPitSignBottomText(connection, tournament.getTournamentID());

      // Allow the user to specify a team number, if the parameter isn't found,
      // then render all pit signs
      final @Nullable String teamNumberStr = request.getParameter("team_number");
      if (null == teamNumberStr) {
        response.setHeader("Content-Disposition", "filename=pit_signs.pdf");

        for (final TournamentTeam team : Queries.getTournamentTeams(connection, tournament.getTournamentID())
                                                .values()) {
          final Element page = renderTeam(document, schedule, challengeImageBase64, partnerImageBase64,
                                          firstImageBase64, team, topText, bottomText);
          documentBody.appendChild(page);
          page.setAttribute("page-break-after", "always");
        }
      } else {
        final int teamNumber = Integer.parseInt(teamNumberStr);
        response.setHeader("Content-Disposition", String.format("filename=pit_sign_%d.pdf", teamNumber));

        final TournamentTeam team = TournamentTeam.getTournamentTeamFromDatabase(connection, tournament, teamNumber);

        final Element page = renderTeam(document, schedule, challengeImageBase64, partnerImageBase64, firstImageBase64,
                                        team, topText, bottomText);
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
                             final @Nullable String challengeImageBase64,
                             final @Nullable String partnerImageBase64,
                             final @Nullable String firstImageBase64,
                             final TournamentTeam team,
                             final String topText,
                             final String bottomText) {

    final Element page = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    page.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_CENTER);
    page.setAttribute("font-size", "28pt");

    // challenge logo
    if (null != challengeImageBase64) {
      final Element imageBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      page.appendChild(imageBlock);

      final Element challengeImageGraphic = FOPUtils.createXslFoElement(document, "external-graphic");
      imageBlock.appendChild(challengeImageGraphic);
      challengeImageGraphic.setAttribute("content-width", "700px");
      challengeImageGraphic.setAttribute("content-height", "150px");
      challengeImageGraphic.setAttribute("scaling", "uniform");
      challengeImageGraphic.setAttribute("src", String.format("url('data:image/png;base64,%s')", challengeImageBase64));
    }

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

    // top text
    // read from tournament parameters and create a block for each carriage
    // return separated pice
    final Element topTextContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    page.appendChild(topTextContainer);
    topTextContainer.setAttribute("font-size", "14pt");
    topTextContainer.setAttribute("font-weight", "bold");
    FOPUtils.appendTextAsParagraphs(document, topText, topTextContainer, false);

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

    // bottom text
    // read from tournament parameters and create a block for each carriage
    // return separated pice
    final Element bottomTextContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    page.appendChild(bottomTextContainer);
    bottomTextContainer.setAttribute("font-size", "14pt");
    bottomTextContainer.setAttribute("font-weight", "bold");
    FOPUtils.appendTextAsParagraphs(document, bottomText, bottomTextContainer, false);

    // partner and FIRST logos
    if (null != partnerImageBase64
        || null != firstImageBase64) {
      final Element imageBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      page.appendChild(imageBlock);
      imageBlock.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_LEFT);
      imageBlock.setAttribute("text-align-last", "justify");

      if (null != partnerImageBase64) {
        final Element partnerImageGraphic = FOPUtils.createXslFoElement(document, "external-graphic");
        imageBlock.appendChild(partnerImageGraphic);
        partnerImageGraphic.setAttribute("content-width", "300px");
        partnerImageGraphic.setAttribute("content-height", "60px");
        partnerImageGraphic.setAttribute("scaling", "uniform");
        partnerImageGraphic.setAttribute("src", String.format("url('data:image/png;base64,%s')", partnerImageBase64));
      }

      final Element spacer = FOPUtils.createXslFoElement(document, FOPUtils.LEADER_TAG);
      imageBlock.appendChild(spacer);
      spacer.setAttribute("leader-pattern", "space");

      if (null != firstImageBase64) {
        final Element firstImageGraphic = FOPUtils.createXslFoElement(document, "external-graphic");
        imageBlock.appendChild(firstImageGraphic);
        firstImageGraphic.setAttribute("content-width", "300px");
        firstImageGraphic.setAttribute("content-height", "60px");
        firstImageGraphic.setAttribute("scaling", "uniform");
        firstImageGraphic.setAttribute("src", String.format("url('data:image/png;base64,%s')", firstImageBase64));
      }
    }

    return page;
  }

  /**
   * @param userImagesFilename the file to find relative to
   *          {@link UserImages#getImagesPath()}
   * @return the image as base64 or null if not found
   */
  private static @Nullable String getImageAsBase64(final String userImagesFilename) {

    final Base64.Encoder encoder = Base64.getEncoder();

    final Path imagePath = UserImages.getImagesPath().resolve(userImagesFilename);
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      if (!Files.exists(imagePath)) {
        LOGGER.debug("Cannot find image: {}", imagePath.toAbsolutePath().toString());
        return null;
      }

      Files.copy(imagePath, output);

      final String encoded = encoder.encodeToString(output.toByteArray());
      return encoded;
    } catch (final IOException e) {
      throw new FLLInternalException("Unable to read image", e);
    }

  }

}
