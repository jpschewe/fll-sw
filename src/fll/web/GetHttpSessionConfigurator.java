/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

/**
 * Put the http session in user properties as {@link #HTTP_SESSION_KEY}.
 */
public class GetHttpSessionConfigurator extends Configurator {

  public static final String HTTP_SESSION_KEY = "httpSession";

  @Override
  public void modifyHandshake(final ServerEndpointConfig config,
                              final HandshakeRequest request,
                              final HandshakeResponse response) {

    final HttpSession httpSession = (HttpSession) request.getHttpSession();

    config.getUserProperties().put(HTTP_SESSION_KEY, httpSession);
  }

}
