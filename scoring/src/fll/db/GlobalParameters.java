/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

/**
 * Constants for the global parameters in the database
 */
public final class GlobalParameters {
  public static final String CURRENT_TOURNAMENT = "CurrentTournament";
  public static final String STANDARDIZED_MEAN = "StandardizedMean";
  public static final double STANDARDIZED_MEAN_DEFAULT = 100;
  public static final String STANDARDIZED_SIGMA = "StandardizedSigma";
  public static final double STANDARDIZED_SIGMA_DEFAULT = 20;
  public static final String CHALLENGE_DOCUMENT = "ChallengeDocument";
  private GlobalParameters() {
    // no instances
  }
  public static final String DATABASE_VERSION = "DatabaseVersion";
  
}
