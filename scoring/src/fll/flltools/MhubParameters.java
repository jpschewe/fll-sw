/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Nonnull;

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
   * @return the hostname to talk to mhub at, or null to not talk to mhub
   * @param connection the connection to the database
   */
  public static String getHostname(@Nonnull final Connection connection) throws SQLException {
    if (GlobalParameters.globalParameterExists(connection, HOST_KEY)) {
      final String value = GlobalParameters.getStringGlobalParameter(connection, HOST_KEY);
      if (null == value
          || value.isEmpty()) {
        return null;
      } else {
        return value;
      }
    } else {
      return DEFAULT_HOST;
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

}