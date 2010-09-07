/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import net.sf.log4jdbc.ConnectionSpy;

/**
 * Based upon code from the bug report
 * http://code.google.com/p/log4jdbc/issues/detail?id=6
 */
public class DataSourceSpy implements DataSource, Serializable {
  private DataSource realDataSource;

  private boolean enabled = Boolean.parseBoolean(System.getProperty("log4jdbc.enabled", "true"));

  public DataSourceSpy() {
  }

  public DataSourceSpy(DataSource realDataSource) {
    this.realDataSource = realDataSource;
  }

  public void setRealDataSource(DataSource realDataSource) {
    this.realDataSource = realDataSource;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Connection getConnection() throws SQLException {
    if (enabled) {
      return new ConnectionSpy(this.realDataSource.getConnection());
    } else {
      return this.realDataSource.getConnection();
    }
  }

  public Connection getConnection(String username,
                                  String password) throws SQLException {
    if (enabled) {
      return new ConnectionSpy(realDataSource.getConnection(username, password));
    } else {
      return realDataSource.getConnection(username, password);
    }
  }

  public int getLoginTimeout() throws SQLException {
    return realDataSource.getLoginTimeout();
  }

  public PrintWriter getLogWriter() throws SQLException {
    return realDataSource.getLogWriter();
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return realDataSource.isWrapperFor(iface);
  }

  public void setLoginTimeout(int seconds) throws SQLException {
    realDataSource.setLoginTimeout(seconds);
  }

  public void setLogWriter(PrintWriter out) throws SQLException {
    realDataSource.setLogWriter(out);
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    return realDataSource.unwrap(iface);
  }

}