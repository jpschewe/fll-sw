/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import fll.Utilities;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import jakarta.servlet.jsp.PageContext;

/**
 * Base class for image management.
 */
/* package */ abstract class ImageManagement extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String DELETE_PREFIX = "delete_";

  private final String basePath;

  /* package */ ImageManagement(final String basePath) {
    this.basePath = basePath;
  }

  /* package */ static void populateContext(final String basePath,
                                            final ServletContext application,
                                            final PageContext page) {
    final String imagePath = application.getRealPath("/"
        + basePath);
    final List<String> files = Utilities.getGraphicFiles(new File(imagePath));
    page.setAttribute("files", files);
    page.setAttribute("DELETE_PREFIX", DELETE_PREFIX);
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), true)) {
      return;
    }

    final Path basePathAbsolute = Paths.get(application.getRealPath("/"
        + basePath));

    // delete first in case the uploaded files have the same names as something
    // being deleted
    for (final String name : Collections.list(request.getParameterNames())) {
      if (name.startsWith(DELETE_PREFIX)) {
        final String filename = name.substring(DELETE_PREFIX.length());
        if (!filename.startsWith(basePath)) {
          LOGGER.warn("Attempting to delete file outside {}, ignoring: {}", basePath, filename);
        } else {
          // add 1 to remove the path separator
          final String relativeFilename = filename.substring(basePath.length()
              + 1);
          final Path file = basePathAbsolute.resolve(relativeFilename);
          if (!Files.deleteIfExists(file)) {
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='warning'>Unable to delete '%s'</div>", file));
          } else {
            SessionAttributes.appendToMessage(session,
                                              String.format("<div class='success'>Deleted image: %s</div>", file));
          }
        }
      }
    }

    for (final Part part : request.getParts()) {
      final String submittedFileName = part.getSubmittedFileName();
      if (!part.getName().startsWith(DELETE_PREFIX)
          && !StringUtils.isBlank(submittedFileName)) {
        final Path filename = Paths.get(submittedFileName).getFileName();
        if (null != filename) {
          final Path uploadLocation = basePathAbsolute.resolve(filename);
          part.write(uploadLocation.toString());
          SessionAttributes.appendToMessage(session, String.format("<div class='success'>Uploaded: '%s'</div>",
                                                                   submittedFileName));
        } else {
          LOGGER.warn("Unable to determine filename for '{}'", submittedFileName);
        }
      }
    }

    final String referrer = request.getHeader("Referer");
    response.sendRedirect(response.encodeRedirectURL(StringUtils.isEmpty(referrer) ? "index.jsp" : referrer));
  }

}
