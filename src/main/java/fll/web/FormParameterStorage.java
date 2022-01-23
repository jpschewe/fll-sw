/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Objects;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Keep track of form parameters through a handle a redirect.
 * The form parameters are stored using
 * {@link #storeParameters(HttpServletRequest, HttpSession)} and applied using
 * {@link #applyParameters(HttpServletRequest, HttpSession)}.
 */
public final class FormParameterStorage implements Serializable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String SESSION_KEY = FormParameterStorage.class.getName();

  private final @Nullable String originalUri;

  private final Map<String, String[]> parameters;

  private FormParameterStorage(final @Nullable String originalUri,
                               final Map<String, String[]> parameters) {
    this.originalUri = originalUri;
    this.parameters = new HashMap<>(parameters);
  }

  /**
   * Store any form parameters found in {@code request} for later application with
   * {@link #applyParameters(HttpServletRequest, HttpSession)}.
   * 
   * @param request the request
   * @param session where the session variables are stored
   */
  public static void storeParameters(final HttpServletRequest request,
                                     final HttpSession session) {

    final Object forwardUriObj = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    final String origUri;
    if (null != forwardUriObj) {
      origUri = forwardUriObj.toString();
    } else {
      origUri = request.getRequestURI();
    }

    final Map<String, String[]> params = request.getParameterMap();
    if (null != params
        && !params.isEmpty()
        && null != origUri) {
      // only need to store form parameters when forwarded and parameters exist

      final FormParameterStorage storage = new FormParameterStorage(origUri, params);
      session.setAttribute(SESSION_KEY, storage);
      LOGGER.debug("Stored parameters: {}", params.keySet());
    }

    session.setAttribute(SessionAttributes.REDIRECT_URL, origUri);

    LOGGER.debug("forward.request_uri: {}", request.getAttribute("jakarta.servlet.forward.request_uri"));
    LOGGER.debug("URI: {}", request.getRequestURI());
    LOGGER.debug("Stored URI: {}", origUri);
    LOGGER.debug("forward.context_path: {}", request.getAttribute("jakarta.servlet.forward.context_path"));
    LOGGER.debug("forward.servlet_path: {}", request.getAttribute("jakarta.servlet.forward.servlet_path"));
    LOGGER.debug("forward.path_info: {}", request.getAttribute("jakarta.servlet.forward.path_info"));
    LOGGER.debug("forward.query_string: {}", request.getAttribute("jakarta.servlet.forward.query_string"));
  }

  /**
   * If the request is for the URI that the parameters are stored, return a new
   * request object with the form parameters added.
   * 
   * @param request original request
   * @param session where session variables are stored
   * @return request object to use going forward
   */
  public static HttpServletRequest applyParameters(final HttpServletRequest request,
                                                   final HttpSession session) {

    final FormParameterStorage storage = SessionAttributes.getAttribute(session, SESSION_KEY,
                                                                        FormParameterStorage.class);

    final String path = request.getRequestURI();

    if (null != storage
        && Objects.equal(storage.originalUri, path)) {

      // make sure we don't get the parameters a second time
      session.removeAttribute(SESSION_KEY);

      LOGGER.debug("Found stored parameters: {}", storage.parameters.keySet());
      return new AdditionalParameterServletRequest(request, storage.parameters);
    } else {
      return request;
    }

  }

}
