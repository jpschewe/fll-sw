/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;

import fll.Utilities;
import fll.db.GlobalParameters;
import fll.util.FLLInternalException;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.DescriptionInfo;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Display a challenge description.
 * The "url" parameter specifies the challenge description to display,
 * if empty the active challenge description is displayed.
 * If the "download" parameter is true, then the browser is told to download the
 * file and the stylesheet instruction is not added.
 */
@WebServlet("/challenge.xml")
public class DisplayChallengeDescriptor extends BaseFLLServlet {

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

    final boolean download = WebUtils.getBooleanRequestParameter(request, "download", false);

    final @Nullable String url = request.getParameter("url");
    Document challengeDocument;
    final String filename;

    if (null == url) {
      filename = "challenge.xml";

      // get the active challenge description
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {

        challengeDocument = GlobalParameters.getChallengeDescription(connection).toXml();
      } catch (final SQLException sqle) {
        throw new RuntimeException("Error talking to the database", sqle);
      }
    } else {
      final URL challengeUrl = DescriptionInfo.getKnownChallengeUrl(url);
      final String pathStr = challengeUrl.getPath();
      final Path path = Path.of(pathStr);
      final int pathNameCount = path.getNameCount();
      if (pathNameCount < 1) {
        LOGGER.warn("Odd challenge URL that doesn't have a path with names: {}", challengeUrl);
        filename = "challenge.xml";
      } else {
        filename = path.getName(pathNameCount
            - 1).toString();
      }
      challengeDocument = getChallengeDocument(challengeUrl);
    }

    if (!download) {
      // add the stylesheet instruction
      final ProcessingInstruction stylesheet = challengeDocument.createProcessingInstruction("xml-stylesheet",
                                                                                             "type='text/css' href='fll.css'");
      challengeDocument.insertBefore(stylesheet, challengeDocument.getDocumentElement());
    }

    response.reset();
    response.setContentType("text/xml");
    response.setHeader("Content-Disposition", String.format("%sfilename=%s", download ? "attachment;" : "", filename));

    XMLUtils.writeXML(challengeDocument, response.getWriter(), Utilities.DEFAULT_CHARSET.name());
  }

  private static Document getChallengeDocument(final URL foundUrl) {
    try (InputStream stream = foundUrl.openStream()) {
      try (Reader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET)) {
        final ChallengeDescription description = ChallengeParser.parse(reader);
        return description.toXml();
      }
    } catch (final IOException e) {
      throw new FLLInternalException(String.format("I/O Error reading description: %s", foundUrl), e);
    } catch (final RuntimeException e) {
      throw new FLLInternalException(String.format("Error reading description: %s", foundUrl.toString()), e);
    }

  }

}
