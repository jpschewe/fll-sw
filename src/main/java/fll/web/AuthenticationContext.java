/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Authentication information for a web session.
 */
public final class AuthenticationContext implements Serializable {

  /**
   * @return not logged in state
   */
  public static AuthenticationContext notLoggedIn() {
    return new AuthenticationContext(false, false, null, Collections.singleton(UserRole.PUBLIC));
  }

  /**
   * @return used during setup
   */
  public static AuthenticationContext inSetup() {
    return new AuthenticationContext(true, false, null, Collections.singleton(UserRole.PUBLIC));
  }

  /**
   * @param username the user
   * @param roles the roles the user is in
   * @return a logged in state
   */
  public static AuthenticationContext loggedIn(final String username,
                                               final Set<UserRole> roles) {
    return new AuthenticationContext(false, true, username, Collections.unmodifiableSet(roles));
  }

  private AuthenticationContext(final boolean inSetup,
                                final boolean loggedIn,
                                final @Nullable String username,
                                final Set<UserRole> roles) {
    this.inSetup = inSetup;
    this.loggedIn = loggedIn;
    this.username = username;
    this.roles = roles;
    this.created = LocalDateTime.now();
  }

  private final boolean inSetup;

  /**
   * When the session is in setup it is granted some special permissions.
   * 
   * @return true if during setup
   */
  public boolean isInSetup() {
    return inSetup;
  }

  private final boolean loggedIn;

  /**
   * @return true if a user is logged in
   */
  public boolean getLoggedIn() {
    return loggedIn;
  }

  private final @Nullable String username;

  /**
   * @return the logged in user, null if not {@link #getLoggedIn()}
   */
  public @Nullable String getUsername() {
    return username;
  }

  private final Set<UserRole> roles;

  /**
   * @return the roles that the session has
   */
  public Set<UserRole> getRoles() {
    return roles;
  }

  /**
   * @return is this user an admin
   */
  public boolean isAdmin() {
    return roles.contains(UserRole.ADMIN);
  }

  /**
   * @return is this user a judge or admin
   */
  public boolean isJudge() {
    return roles.contains(UserRole.ADMIN)
        || roles.contains(UserRole.JUDGE);
  }

  /**
   * @return is this user a ref or admin
   */
  public boolean isRef() {
    return roles.contains(UserRole.ADMIN)
        || roles.contains(UserRole.REF);
  }

  private final LocalDateTime created;

  /**
   * @return when this object was created
   */
  public LocalDateTime getCreated() {
    return created;
  }

  @Override
  public String toString() {
    return String.format("%s [loggedIn: %b inSetup: %b username: %s roles: %s", getClass().getSimpleName(),
                         this.loggedIn, this.inSetup, this.username, this.roles);
  }

  /**
   * Defaults {@code inSetup} to false.
   * 
   * @param request
   *          {@link #requireRoles(ServletRequest, ServletResponse, HttpSession, Set, boolean)}
   * @param response
   *          {@link #requireRoles(ServletRequest, ServletResponse, HttpSession, Set, boolean)}
   * @param session
   *          {@link #requireRoles(ServletRequest, ServletResponse, HttpSession, Set, boolean)}
   * @param requiredRoles
   *          {@link #requireRoles(ServletRequest, ServletResponse, HttpSession, Set, boolean)}
   * @return {@link #requireRoles(ServletRequest, ServletResponse, HttpSession, Set, boolean)}
   * @throws ServletException
   *           {@link #requireRoles(ServletRequest, ServletResponse, HttpSession, Set, boolean)}
   * @throws IOException
   *           {@link #requireRoles(ServletRequest, ServletResponse, HttpSession, Set, boolean)}
   */
  public boolean requireRoles(final ServletRequest request,
                              final ServletResponse response,
                              final HttpSession session,
                              final Set<UserRole> requiredRoles)
      throws ServletException, IOException {
    return requireRoles(request, response, session, requiredRoles, false);
  }

  /**
   * Check that the current user contains one of the {@code requiredRoles} or is
   * an admin or setup is allowed and the software is currently in setup.
   * 
   * @param request http request
   * @param response http response
   * @param session used to populate the message
   * @param requiredRoles the roles that are required
   * @param allowSetup true if being in setup allows bypass of roles
   * @return true if the roles are met, false if the roles are not met and the
   *         page is forwarded to login
   * @throws ServletException on an error forwarding to login
   * @throws IOException on an error forwarding to login
   */
  public boolean requireRoles(final ServletRequest request,
                              final ServletResponse response,
                              final HttpSession session,
                              final Set<UserRole> requiredRoles,
                              final boolean allowSetup)
      throws ServletException, IOException {

    if (allowSetup
        && isInSetup()) {
      return true;
    } else if (isAdmin()) {
      return true;
    } else {
      if (!roles.containsAll(requiredRoles)) {
        SessionAttributes.appendToMessage(session,
                                          "<p>You need to be logged in as a user with the following roles to view this page: "
                                              + requiredRoles.stream().map(Object::toString)
                                                             .collect(Collectors.joining(","))
                                              + "</p>");
        request.getRequestDispatcher("/login.jsp").forward(request, response);
        return false;
      } else {
        return true;
      }
    }

  }
}
