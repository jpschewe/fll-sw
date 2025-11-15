/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.FLLInternalException;

/**
 * A team score in a Map containing strings from form data from the web.
 */
public final class MapTeamScore extends BasePerformanceTeamScore {

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
  public @Nullable String getEnumRawScore(final String goalName) {
    return map.get(goalName);
  }

  @Override
  public double getRawScore(final String goalName) {
    final String value = map.get(goalName);
    if (null == value) {
      return Double.NaN;
    } else {
      return Double.parseDouble(value);
    }
  }

  @Override
  public boolean isNoShow() {
    return getBoolean("NoShow");
  }

  @Override
  public boolean isBye() {
    // can't get a bye from the web, however this might be used later for the
    // database implementation
    return getBoolean("Bye");
  }

  private boolean getBoolean(final String key) {
    final String value = map.get(key);
    if (null == value) {
      throw new FLLInternalException("Missing parameter: "
          + key);
    }
    return value.equalsIgnoreCase("true")
        || value.equalsIgnoreCase("t")
        || value.equals("1");

  }

  @Override
  public boolean isVerified() {
    return getBoolean("Verified");
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
