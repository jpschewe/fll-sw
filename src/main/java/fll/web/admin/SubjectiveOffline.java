/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.JudgeInformation;
import fll.SubjectiveScore;
import fll.Tournament;
import fll.Utilities;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.MissingRequiredParameterException;
import fll.web.SessionAttributes;
import fll.web.UploadProcessor;
import fll.web.UserRole;
import fll.web.api.JudgesServlet;
import fll.web.api.SubjectiveScoresServlet;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Process offline data file from the subjective application.
 */
@WebServlet("/admin/SubjectiveOffline")
public class SubjectiveOffline extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    try {
      // must be first to ensure the form parameters are set
      UploadProcessor.processUpload(request);

      if (null == request.getAttribute("subjectiveOfflineFile")) {
        throw new MissingRequiredParameterException("subjectiveOfflineFile");
      }
      final FileItem fileItem = (FileItem) request.getAttribute("subjectiveOfflineFile");
      if (null == fileItem) {
        throw new MissingRequiredParameterException("subjectiveOfflineFile");
      }

      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection(); InputStream fileStream = fileItem.getInputStream()) {
        final Tournament currentTournament = Tournament.getCurrentTournament(connection);

        final ObjectMapper jsonMapper = Utilities.createJsonMapper();

        final OfflineData offlineData = jsonMapper.readValue(fileStream, OfflineData.class);

        final int numNewJudges = JudgesServlet.processJudges(connection, currentTournament.getTournamentID(),
                                                             offlineData.getJudges());

        final int numModifiedScores = SubjectiveScoresServlet.processScores(connection, challengeDescription,
                                                                            currentTournament, offlineData.getScores());

        SessionAttributes.appendToMessage(session, String.format("<div>Added %d judges. Modified %d scores</div>",
                                                                 numNewJudges, numModifiedScores));
      } catch (final SQLException sqle) {
        LOGGER.error(sqle, sqle);
        throw new FLLRuntimeException("Error loading data into the database", sqle);
      }

    } catch (final FileUploadException fue) {
      LOGGER.error(fue);
      throw new FLLRuntimeException("Error handling the file upload", fue);
    }

    response.sendRedirect(response.encodeRedirectURL("/admin/performance-area.jsp"));
  }

  private static final class OfflineData implements Serializable {
    @SuppressWarnings("unused") // used by JSON parsing
    OfflineData(@JsonProperty("judges") Collection<JudgeInformation> judges,
                @JsonProperty("scores") Map<String, Map<String, Map<Integer, SubjectiveScore>>> scores) {
      this.judges = judges;
      this.scores = scores;
    }

    private final Collection<JudgeInformation> judges;

    public Collection<JudgeInformation> getJudges() {
      return judges;
    }

    private final Map<String, Map<String, Map<Integer, SubjectiveScore>>> scores;

    public Map<String, Map<String, Map<Integer, SubjectiveScore>>> getScores() {
      return scores;
    }
  }

}
