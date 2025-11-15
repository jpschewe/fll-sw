/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.playoff;

import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Represents a performance score.
 */
public interface PerformanceTeamScore extends TeamScore {

  /**
   * Is this score a bye?
   *
   * @return true if this score is a no show
   */
  @SideEffectFree
  boolean isBye();

  /**
   * Is the score verified.
   * 
   * @return true if this score has been verified
   */
  @SideEffectFree
  boolean isVerified();

  /**
   * When the score is entered from a tablet where the table is known, this has
   * the table name, otherwise the value is "ALL" meaning that the person entering
   * the score has "all tables" selected.
   * 
   * @return the table that the score was entered from.
   */
  @SideEffectFree
  String getTable();

  /**
   * What run do these scores apply to?
   * 
   * @return the run for the scores
   */
  @SideEffectFree
  int getRunNumber();

}
