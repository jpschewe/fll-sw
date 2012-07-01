/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;

/**
 * Log a user out of the application.
 */
@WebServlet("/DoLogout")
public class DoLogout extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final Collection<Cookie> loginCookies = CookieUtils.findLoginCookie(request);
    final DataSource datasource = ApplicationAttributes.getDataSource();
    final String hostHeader = request.getHeader("host");
    final int colonIndex = hostHeader.indexOf(":");
    final String domain;
    if (-1 != colonIndex) {
      domain = hostHeader.substring(0, colonIndex);
    } else {
      domain = hostHeader;
    }
    LOGGER.debug("domain: "
        + domain);

    for (final Cookie loginCookie : loginCookies) {
      Cookie delCookie = new Cookie(loginCookie.getName(), "");
      delCookie.setMaxAge(0);
      delCookie.setDomain(domain);
      response.addCookie(delCookie);

      Connection connection = null;
      try {
        connection = datasource.getConnection();
        Queries.removeValidLogin(connection, loginCookie.getValue());
        LOGGER.debug("Removed cookie from DB: "
            + loginCookie.getValue());
      } catch (final SQLException e) {
        throw new RuntimeException(e);
      } finally {
        SQLFunctions.close(connection);
      }
    }
    response.sendRedirect(response.encodeRedirectURL(request.getContextPath()));

  }

}
