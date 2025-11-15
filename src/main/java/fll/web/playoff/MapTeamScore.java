/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A team score in a Map containing strings from form data.
 */
public final class MapTeamScore extends TeamScore {

  /**
   * @param teamNumber {@link #getTeamNumber()}
   * @param runNumber {@link #getRunNumber()}
   * @param map used to read the goal scores
   */
  public MapTeamScore(final int teamNumber,
                      final int runNumber,
                      final Map<String, String> map) {
    super(teamNumber, runNumber);
    this.map = map;
  }

  @Override
  protected @Nullable String internalGetEnumRawScore(final String goalName) {
    return map.get(goalName);
  }

  @Override
  protected double internalGetRawScore(final String goalName) {
    final String value = map.get(goalName);
    if (null == value) {
      return Double.NaN;
    } else {
      return Double.parseDouble(value);
    }
  }

  /**
   * @see fll.web.playoff.TeamScore#isNoShow()
   */
  @Override
  public boolean isNoShow() {
    if (!scoreExists()) {
      return false;
    } else {
      final String noShow = map.get("NoShow");
      if (null == noShow) {
        throw new RuntimeException("Missing parameter: NoShow");
      }
      return noShow.equalsIgnoreCase("true")
          || noShow.equalsIgnoreCase("t")
          || noShow.equals("1");
    }
  }

  @Override
  public boolean isBye() {
    // one cannot enter a BYE through the web interface
    return false;
  }

  @Override
  public boolean isVerified() {
    if (NON_PERFORMANCE_RUN_NUMBER == getRunNumber()) {
      return false;
    } else if (!scoreExists()) {
      return false;
    } else {
      final String verified = map.get("Verified");
      if (null == verified) {
        throw new RuntimeException("Missing parameter: Verified");
      }
      return verified.equalsIgnoreCase("true")
          || verified.equalsIgnoreCase("t")
          || verified.equals("1");
    }
  }

  @Override
  public boolean scoreExists() {
    return true;
  }

  private final Map<String, String> map;

  @Override
  public String getTable() {
    final String table = map.get("tablename");
    if (null == table) {
      return "UNKNOWN";
    } else {
      return table;
    }
  }
}
