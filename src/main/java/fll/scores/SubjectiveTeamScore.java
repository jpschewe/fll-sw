/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.scores;

import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

import fll.xml.NonNumericCategory;

/**
 * Represents a subjective team score.
 */
public interface SubjectiveTeamScore extends TeamScore {

  /**
   * @return ID of the judge that created the score
   */
  @SideEffectFree
  String getJudge();

  /**
   * @return the "Great Job..." comment
   */
  @SideEffectFree
  @Nullable
  String getCommentGreatJob();

  /**
   * @return the "Think About..." comment
   */
  @SideEffectFree
  @Nullable
  String getCommentThinkAbout();

  /**
   * @param goalName the goal to get the comment for
   * @return the comment stored for the goal
   */
  @SideEffectFree
  @Nullable
  String getGoalComment(String goalName);

  /**
   * @return unmodifiable set of {@link NonNumericCategory#getTitle()}
   */
  @SideEffectFree
  Set<String> getNonNumericNominations();

  /**
   * @return the judge note for the score.
   */
  @SideEffectFree
  @Nullable
  String getNote();

}
