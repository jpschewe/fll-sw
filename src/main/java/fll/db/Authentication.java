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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.FLLRuntimeException;
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
          try {
            final UserRole role = UserRole.valueOf(roleStr);
            roles.add(role);
          } catch (final IllegalArgumentException e) {
            LOGGER.warn("Found unknown role ({}) in the database, ignoring", roleStr, e);
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
        users.add(user);
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
          users.add(user);
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

    try (PreparedStatement removeKeys = connection.prepareStatement("DELETE FROM valid_login where fll_user = ?")) {
      removeKeys.setString(1, user);
      removeKeys.executeUpdate();
    }

    try (
        PreparedStatement removeUser = connection.prepareStatement("DELETE FROM fll_authentication where fll_user = ?")) {
      removeUser.setString(1, user);
      removeUser.executeUpdate();
    }
  }

  /**
   * Remove a valid login by user.
   * 
   * @param connection database connection
   * @param user the user to remove valid login information for
   * @throws SQLException on a database error
   */
  public static void removeValidLoginByUser(final Connection connection,
                                            final String user)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("DELETE FROM valid_login WHERE fll_user = ?")) {
      prep.setString(1, user);
      prep.executeUpdate();
    }
  }

  /**
   * @param connection database connection
   * @param user the user to change the password for
   * @param passwordHash the new hashed password
   * @throws SQLException on a database error
   */
  public static void changePassword(final Connection connection,
                                    final String user,
                                    final String passwordHash)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE fll_authentication SET fll_pass = ? WHERE fll_user = ?")) {
      prep.setString(1, passwordHash);
      prep.setString(2, user);
      prep.executeUpdate();
    }
  }

  /**
   * Remove a valid login by magic key.
   * 
   * @param connection database connection
   * @param magicKey the valid login key
   * @throws SQLException on a database error
   */
  public static void removeValidLoginByKey(final Connection connection,
                                           final String magicKey)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("DELETE FROM valid_login WHERE magic_key = ?")) {
      prep.setString(1, magicKey);
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
    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("valid_login")) {
      GenerateDB.createValidLogin(connection);
    }

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
   * Get the authentication information.
   *
   * @param connection database connection
   * @return key is user, value is hashed pass
   * @throws SQLException on a database error
   */
  public static Map<String, String> getAuthInfo(final Connection connection) throws SQLException {
    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("valid_login")) {
      GenerateDB.createValidLogin(connection);
    }

    final Map<String, String> retval = new HashMap<>();
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT fll_user, fll_pass FROM fll_authentication")) {
      while (rs.next()) {
        final String user = rs.getString(1);
        final String pass = rs.getString(2);
        retval.put(user, pass);
      }
    }
    return retval;
  }

  /**
   * Add a valid login to the database.
   *
   * @param connection database connection
   * @param magicKey used in cookies to store the valid login
   * @param user the user that has logged in
   * @throws SQLException on a database error
   */
  public static void addValidLogin(final Connection connection,
                                   final String user,
                                   final String magicKey)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO valid_login (fll_user, magic_key) VALUES(?, ?)")) {
      prep.setString(1, user);
      prep.setString(2, magicKey);
      prep.executeUpdate();
    }
  }

  /**
   * Check if any of the specified login keys matches one that was stored.
   *
   * @param connection database connection
   * @param keys the keys to check
   * @return the username that the key matches, null otherwise
   * @throws SQLException on a database error
   */
  public static @Nullable String checkValidLogin(final Connection connection,
                                                 final Collection<String> keys)
      throws SQLException {
    // not doing the comparison with SQL to avoid SQL injection attack
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT fll_user, magic_key FROM valid_login")) {
      while (rs.next()) {
        final String user = rs.getString(1);
        final String compare = rs.getString(2);
        for (final String magicKey : keys) {
          if (Objects.equals(magicKey, compare)) {
            return user;
          }
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

    final String hashedPass = DigestUtils.md5Hex(pass);
    try (
        PreparedStatement addUser = connection.prepareStatement("INSERT INTO fll_authentication (fll_user, fll_pass) VALUES(?, ?)")) {
      addUser.setString(1, user);
      addUser.setString(2, hashedPass);
      addUser.executeUpdate();
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
}
