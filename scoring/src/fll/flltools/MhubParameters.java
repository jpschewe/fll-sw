/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import fll.db.GlobalParameters;

/**
 * Parameters for working with mhub at https://github.com/FirstLegoLeague/mhub.
 */
public class MhubParameters {

  /**
   * Key for {@link GlobalParameters} for the mhub hostname.
   */
  public static final String HOST_KEY = "mhub.hostname";

  /**
   * Default value for the host property. This is null, meaning don't talk to
   * mhub.
   */
  public static final String DEFAULT_HOST = null;

  /**
   * Key for {@link GlobalParameters} for the mhub port;
   */
  public static final String PORT_KEY = "mhub.port";

  /**
   * The default port that mhub listens on.
   */
  public static final int DEFAULT_PORT = 13900;

  /**
   * Key for {@link GlobalParameters} for the display node.
   */
  public static final String DISPLAY_NODE_KEY = "mhub.display.node";

  /**
   * Default node for the display.
   */
  public static final String DEFAULT_DISPLAY_NODE = "default";

  /**
   * @return the hostname to talk to mhub at, or null to not talk to mhub
   * @param connection the connection to the database
   */
  public static String getHostname(@Nonnull final Connection connection) throws SQLException {
    if (GlobalParameters.globalParameterExists(connection, HOST_KEY)) {
      final String value = GlobalParameters.getStringGlobalParameter(connection, HOST_KEY);
      if (StringUtils.isBlank(value)) {
        return null;
      } else {
        return value;
      }
    } else {
      return DEFAULT_HOST;
    }
  }

  /**
   * @param connection the database connection
   * @param hostname the new hostname, may be null
   * @throws SQLException if there is a problem talking to the database
   * @see #getHostname(Connection)
   */
  public static void setHostname(@Nonnull final Connection connection,
                                 final String hostname)
      throws SQLException {
    if (null == hostname) {
      // global_parameters table doesn't allow null values
      GlobalParameters.setStringGlobalParameter(connection, HOST_KEY, "");
    } else {
      GlobalParameters.setStringGlobalParameter(connection, HOST_KEY, hostname);
    }
  }

  /**
   * @param connection the connection to the database
   * @return the port number that mhub is listening on
   * @throws SQLException
   */
  public static int getPort(@Nonnull final Connection connection) throws SQLException {
    if (GlobalParameters.globalParameterExists(connection, PORT_KEY)) {
      return GlobalParameters.getIntGlobalParameter(connection, PORT_KEY);
    } else {
      return DEFAULT_PORT;
    }
  }

  /**
   * @param port the new port number
   * @throws SQLException if there is a database error
   * @param connection the database connection
   * @see #getPort(Connection)
   */
  public static void setPort(@Nonnull final Connection connection,
                             final int port)
      throws SQLException {
    GlobalParameters.setIntGlobalParameter(connection, PORT_KEY, port);
  }

  /**
   * @param connection the database connection
   * @return The node to send display messages to
   * @throws SQLException if there is a database error
   * @see fll.flltools.displaySystem.list.BaseListMessage
   */
  public static String getDisplayNode(@Nonnull final Connection connection) throws SQLException {
    if (GlobalParameters.globalParameterExists(connection, DISPLAY_NODE_KEY)) {
      return GlobalParameters.getStringGlobalParameter(connection, DISPLAY_NODE_KEY);
    } else {
      return DEFAULT_DISPLAY_NODE;
    }
  }

  /**
   * @param connection the database connection
   * @param node the new display noe
   * @throws SQLException if there is a database error
   * @see #getDisplayNode(Connection)
   */
  public static void setDisplayNode(@Nonnull final Connection connection,
                                    final String node)
      throws SQLException {
    if (StringUtils.isBlank(node)) {
      GlobalParameters.setStringGlobalParameter(connection, DISPLAY_NODE_KEY, DEFAULT_DISPLAY_NODE);
    } else {
      GlobalParameters.setStringGlobalParameter(connection, DISPLAY_NODE_KEY, node);
    }
  }

}