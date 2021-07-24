/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.tags;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Tag support to specifying which roles are required to access a page.
 */
public class RequireRoles extends TagSupport {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Simple constructor.
   */
  public RequireRoles() {
    super();
  }

  @Override
  public int doEndTag() throws JspException {
    LOGGER.debug("Top of RequireRoles.doEndTag");

    final HttpSession session = pageContext.getSession();
    final ServletRequest request = pageContext.getRequest();
    final ServletResponse response = pageContext.getResponse();
    final AuthenticationContext authentication = SessionAttributes.getAuthentication(session);

    final Set<UserRole> requiredRoles = parseRoles();

    validateState(requiredRoles);

    try {
      final boolean valid = authentication.requireRoles(request, response, session, requiredRoles, allowSetup);
      if (!valid) {
        return SKIP_PAGE;
      } else {
        return EVAL_PAGE;
      }
    } catch (ServletException | IOException e) {
      throw new JspException(e);
    }
  }

  private void validateState(final Set<UserRole> requiredRoles) throws JspException {
    if (requiredRoles.isEmpty()
        && !allowSetup) {
      throw new JspException("No roles specified and allowSetup is false");
    }
  }

  private Set<UserRole> parseRoles() throws JspException {
    if (null == roles) {
      return Collections.emptySet();
    } else {
      final Set<UserRole> requiredRoles = new HashSet<>();
      final String[] tokens = roles.split(",");
      for (final String token : tokens) {
        try {
          final UserRole role = UserRole.valueOf(token);
          requiredRoles.add(role);
        } catch (IllegalArgumentException | NullPointerException e) {
          throw new JspException("Error parsing '"
              + token
              + "'", e);
        }
      }
      return requiredRoles;
    }
  }

  private @Nullable String roles = null;

  /**
   * @param roles the string of roles that are required
   */
  public void setRoles(final String roles) {
    this.roles = roles;
  }

  private boolean allowSetup = false;

  /**
   * @param v if true allow visiting the page in setup without checking the roles
   */
  public void setAllowSetup(final boolean v) {
    this.allowSetup = v;
  }
}
