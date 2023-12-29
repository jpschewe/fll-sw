/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import fll.util.FLLRuntimeException;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;

/**
 * Put the http session in user properties as {@link #HTTP_SESSION_KEY}.
 */
public class GetHttpSessionConfigurator extends Configurator {

  /**
   * Name of the key storing the http session object.
   */
  public static final String HTTP_SESSION_KEY = "httpSession";

  @Override
  public void modifyHandshake(final ServerEndpointConfig config,
                              final HandshakeRequest request,
                              final HandshakeResponse response) {

    final HttpSession httpSession = (HttpSession) request.getHttpSession();

    config.getUserProperties().put(HTTP_SESSION_KEY, httpSession);
  }

  /**
   * @param session a session configured with {@link GetHttpSessionConfigurator}
   * @return the http session
   * @throws FLLRuntimeException if the session isn't available
   */
  public static HttpSession getHttpSession(final Session session) {
    final HttpSession httpSession = (HttpSession) session.getUserProperties()
                                                         .get(HTTP_SESSION_KEY);
  
    if (null == httpSession) {
      throw new FLLRuntimeException("Unable to find httpSession in the userProperties");
    }
  
    return httpSession;
  }

}
