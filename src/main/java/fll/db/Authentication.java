/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.UserRole;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Utilities for working with authentication in the database.
 */
public final class Authentication {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private Authentication() {
  }

  /**
   * @param connection the database connection
   * @param username the user to check
   * @return the roles for the specified user
   * @throws SQLException on a database error
   */
  public static Set<UserRole> getRoles(final Connection connection,
                                       final String username)
      throws SQLException {
    final Set<UserRole> roles = new HashSet<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT fll_role FROM auth_roles WHERE fll_user = ?")) {
      prep.setString(1, username);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String roleStr = rs.getString("fll_role");
          if (null != roleStr) {
            try {
              final UserRole role = UserRole.valueOf(roleStr);
              roles.add(role);
            } catch (final IllegalArgumentException e) {
              LOGGER.warn("Found unknown role ({}) in the database, ignoring", roleStr, e);
            }
          }
        }
      }
    }
    return roles;
  }

  /**
   * @param connection database connection
   * @param username user to set roles for
   * @param roles {@link #getRoles(Connection, String)}
   * @throws SQLException on a database error
   */
  public static void setRoles(final Connection connection,
                              final String username,
                              final Set<UserRole> roles)
      throws SQLException {
    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM auth_roles WHERE fll_user = ?");
        PreparedStatement prep = connection.prepareStatement("INSERT INTO auth_roles (fll_user, fll_role) VALUES(?, ?)")) {
      delete.setString(1, username);
      delete.executeUpdate();

      prep.setString(1, username);
      for (final UserRole role : roles) {
        prep.setString(2, role.name());
        prep.executeUpdate();
      }
    }

  }

  /**
   * Get the list of current users known to the system.
   * 
   * @param connection the database connection
   * @return the usernames
   * @throws SQLException on a database error
   */
  public static Collection<String> getUsers(final Connection connection) throws SQLException {
    final Collection<String> users = new LinkedList<>();
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT fll_user from fll_authentication");) {
      while (rs.next()) {
        final String user = rs.getString(1);
        if (null != user) {
          users.add(user);
        }
      }
    }
    return users;
  }

  /**
   * Get the list of current users that have the {@link UserRole#ADMIN} role.
   * 
   * @param connection the database connection
   * @return the usernames
   * @throws SQLException on a database error
   */
  public static Collection<String> getAdminUsers(final Connection connection) throws SQLException {
    final Collection<String> users = new LinkedList<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT fll_user FROM fll_authentication, auth_roles" //
        + " WHERE fll_authentication.fll_user = auth_roles.fll_user" //
        + " AND auth_roles.fll_role = ?")) {
      prep.setString(1, UserRole.ADMIN.name());
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String user = rs.getString("fll_user");
          if (null != user) {
            users.add(user);
          }
        }
      }
    }
    return users;
  }

  /**
   * Remove a user.
   * 
   * @param connection the database connection
   * @param user the user to remove
   * @throws SQLException on a database error
   */
  public static void removeUser(final Connection connection,
                                final String user)
      throws SQLException {
    try (PreparedStatement removeRoles = connection.prepareStatement("DELETE FROM auth_roles where fll_user = ?")) {
      removeRoles.setString(1, user);
      removeRoles.executeUpdate();
    }

    try (
        PreparedStatement removeUser = connection.prepareStatement("DELETE FROM fll_authentication where fll_user = ?")) {
      removeUser.setString(1, user);
      removeUser.executeUpdate();
    }
  }

  /**
   * @param connection database connection
   * @param user the user to change the password for
   * @param password the new password
   * @throws SQLException on a database error
   */
  public static void changePassword(final Connection connection,
                                    final String user,
                                    final String password)
      throws SQLException {
    final String passwordHash = computePasswordHash(password);
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE fll_authentication SET fll_pass = ? WHERE fll_user = ?")) {
      prep.setString(1, passwordHash);
      prep.setString(2, user);
      prep.executeUpdate();
    }
  }

  /**
   * Check if the authentication table is empty or doesn't exist. This will
   * create the authentication table if it doesn't exist.
   *
   * @param connection database connection
   * @return true if the authentication table is missing or empty
   * @throws SQLException on a database error
   */
  public static boolean isAuthenticationEmpty(final Connection connection) throws SQLException {
    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("fll_authentication")) {
      GenerateDB.createAuthentication(connection);
      return true;
    }

    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * from fll_authentication");) {
      return !rs.next();
    }
  }

  /**
   * Get the hashed password for a user for checking.
   *
   * @param connection database connection
   * @param user the user to get the password for
   * @return the password or null if the user isn't in the database
   * @throws SQLException on a database error
   */
  public static @Nullable String getHashedPassword(final Connection connection,
                                                   final String user)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT fll_pass FROM fll_authentication WHERE fll_user = ?")) {
      prep.setString(1, user);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String pass = rs.getString(1);
          return pass;
        }
      }
    }
    return null;
  }

  /**
   * Add a user to the database.
   * 
   * @param connection database connection
   * @param user the user to add
   * @param pass the password for the user
   * @throws SQLException on a database error
   * @throws DuplicateUserException if the user already exists
   */
  public static void addUser(final Connection connection,
                             final String user,
                             final String pass)
      throws SQLException {
    final Collection<String> existingUsers = Authentication.getUsers(connection);
    if (existingUsers.contains(user)) {
      throw new DuplicateUserException(String.format("The user '%s' already exists", user));
    }

    final String hashedPass = computePasswordHash(pass);
    try (
        PreparedStatement addUser = connection.prepareStatement("INSERT INTO fll_authentication (fll_user, fll_pass) VALUES(?, ?)")) {
      addUser.setString(1, user);
      addUser.setString(2, hashedPass);
      addUser.executeUpdate();
    }
  }

  private static String computePasswordHash(final String password) {
    final String hashedPass = DigestUtils.md5Hex(password);
    return hashedPass;
  }

  /**
   * Check if the specified {@code password} is valid for {@code user}.
   * 
   * @param connection database connection
   * @param user the user to check
   * @param password the password to check
   * @return if the username and password is valid
   * @throws SQLException on a database error
   */
  public static boolean checkValidPassword(final Connection connection,
                                           final String user,
                                           final String password)
      throws SQLException {
    final String hashStored = getHashedPassword(connection, user);
    if (null == hashStored) {
      return false;
    } else {
      final String hash = computePasswordHash(password);
      return hashStored.equals(hash);
    }
  }

  /**
   * Thrown when a duplicate username is found.
   */
  public static final class DuplicateUserException extends FLLRuntimeException {
    /**
     * @param message {@link #getMessage()}
     */
    public DuplicateUserException(final String message) {
      super(message);
    }
  }

  /**
   * Specify that {@code username} needs to be refreshed from the database.
   * 
   * @param application application variable store
   * @param username the username to mark as needing a refresh
   */
  public static void markRefreshNeeded(final ServletContext application,
                                       final String username) {
    final Map<String, LocalDateTime> authRefresh = ApplicationAttributes.getAuthRefresh(application);
    authRefresh.put(username, LocalDateTime.now());
    application.setAttribute(ApplicationAttributes.AUTH_REFRESH, authRefresh);
  }

  /**
   * Specify that {@code username} needs to login again.
   * 
   * @param application application variable store
   * @param username the username to mark as needing a login
   */
  public static void markLoggedOut(final ServletContext application,
                                   final String username) {
    final Map<String, LocalDateTime> authLoggedOut = ApplicationAttributes.getAuthLoggedOut(application);
    authLoggedOut.put(username, LocalDateTime.now());
    application.setAttribute(ApplicationAttributes.AUTH_LOGGED_OUT, authLoggedOut);
  }

  /**
   * After {@link #ALLOWED_FAILURES} failures the amount of time to lock the
   * account per failure.
   */
  private static final Duration WAIT_PER_FAILURE = Duration.ofMinutes(1);

  /**
   * How many failures in a row before the account is locked.
   */
  private static final int ALLOWED_FAILURES = 5;

  /**
   * Check if {@code username} is currently locked.
   * 
   * @param connection database connection
   * @param username username to check
   * @return true if {@code username} is locked
   * @throws SQLException on a database error
   */
  public static boolean isAccountLocked(final Connection connection,
                                        final String username)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT num_failures, last_failure FROM fll_authentication WHERE fll_user = ?")) {
      prep.setString(1, username);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final Timestamp lastFailure = rs.getTimestamp("last_failure");
          if (null == lastFailure) {
            // no last failure timestamp, can't be locked out
            return false;
          } else {
            final int numFailures = rs.getInt("num_failures");
            if (numFailures >= ALLOWED_FAILURES) {
              final LocalDateTime unlockTime = lastFailure.toLocalDateTime()
                                                          .plus(WAIT_PER_FAILURE.multipliedBy(numFailures));
              return unlockTime.isAfter(LocalDateTime.now());
            } else {
              // not enough failures
              return false;
            }
          }
        } else {
          // non-existant users can't be locked out
          return false;
        }
      }
    }
  }

  /**
   * @param connection database connection
   * @param username user to record a successful login for
   * @throws SQLException on a database error
   */
  public static void recordSuccessfulLogin(final Connection connection,
                                           final String username)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("UPDATE fll_authentication " //
        + " SET last_failure = NULL " //
        + " ,num_failures = 0"
        + " WHERE fll_user = ?")) {
      prep.setString(1, username);
      prep.executeUpdate();
    }
  }

  /**
   * @param connection database connection
   * @param username user to record a failed login attempt for
   * @throws SQLException on a database error
   */
  public static void recordFailedLogin(final Connection connection,
                                       final String username)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("UPDATE fll_authentication " //
        + " SET last_failure = CURRENT_TIMESTAMP" //
        + " ,num_failures = num_failures + 1" //
        + " WHERE fll_user = ?")) {
      prep.setString(1, username);
      prep.executeUpdate();
    }
  }

  /**
   * @param connection database connection
   * @param username the account to unlock
   * @throws SQLException on a database error
   */
  public static void unlockAccount(final Connection connection,
                                   final String username)
      throws SQLException {
    // recording a successful login will unlock the account
    recordSuccessfulLogin(connection, username);
  }
}
