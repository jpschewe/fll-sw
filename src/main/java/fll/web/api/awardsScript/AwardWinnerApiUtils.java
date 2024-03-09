/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api.awardsScript;

import java.io.IOException;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Some utilities for dealing with data for the award winners API.
 */
final class AwardWinnerApiUtils {

  private AwardWinnerApiUtils() {
  }

  /**
   * @param logger the logger to send logging output to
   * @param method the name of the calling method to use for logging
   * @param request the request
   * @param response the response to possibly send back an error
   * @return the parsed information, empty on an error
   * @throws IOException if there is an error sending an error response to the
   *           {code}response{code}
   */
  static Optional<PutPathInfo> parsePutPathInfo(final org.apache.logging.log4j.Logger logger,
                                                final String method,
                                                final HttpServletRequest request,
                                                final HttpServletResponse response)
      throws IOException {
    final @Nullable String pathInfo = request.getPathInfo();
    if (null == pathInfo) {
      logger.error("{}: got null path info", method);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Got null path info");
      return Optional.empty();
    }

    logger.debug("{}: pathInfo: {}", method, pathInfo);
    final String[] pathPieces = pathInfo.split("/");
    if (pathPieces.length != 3) {
      final String msg = String.format("wrong number of pieces in path info '%s'. Expecting 3, but found %d", pathInfo,
                                       pathPieces.length);
      logger.error("{}: {}", method, msg);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
      return Optional.empty();
    }

    // pathPieces[0] is always empty because the path starts with a slash
    final String categoryName = pathPieces[1];
    try {
      final int teamNumber = Integer.parseInt(pathPieces[2]);
      return Optional.of(new PutPathInfo(categoryName, teamNumber));
    } catch (final NumberFormatException e) {
      final String msg = String.format("team number is not parsable: '%s'", pathPieces[2]);
      logger.error("{}: {}", method, msg, e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
      return Optional.empty();
    }
  }

  static final class PutPathInfo {
    PutPathInfo(final String categoryName,
                final int teamNumber) {
      this.categoryName = categoryName;
      this.teamNumber = teamNumber;
    }

    private final String categoryName;

    public String getCategoryName() {
      return categoryName;
    }

    private final int teamNumber;

    public int getTeamNumber() {
      return teamNumber;
    }
  }

  // CHECKSTYLE:OFF data class
  @JsonIgnoreProperties(ignoreUnknown = true)
  static final class PutData {
    public int place = -1;

    /**
     * If {code}false{code} do not change the description of an existing entry in
     * the database.
     */
    public boolean descriptionSpecified = false;

    public @Nullable String description = null;

    public @Nullable String awardGroup = null;
  }
  // CHECKSTYLE:ON
}
