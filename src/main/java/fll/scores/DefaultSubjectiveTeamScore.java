/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.scores;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Subjective score populated from values.
 */
public final class DefaultSubjectiveTeamScore extends DefaultTeamScore implements SubjectiveTeamScore {

  /**
   * @param teamNumber see {@link #getTeamNumber()}
   * @param simpleGoals simple goal values
   * @param enumGoals enum goal values
   * @param noShow see {@link #isNoShow()}
   * @param judge see {@link #getJudge()}
   * @param commentGreatJob see {@link #getCommentGreatJob()}
   * @param commentThinkAbout see {@link #getCommentThinkAbout()}
   * @param goalComments see {@link #getGoalComment(String)}
   * @param note {@link #getNote()}
   * @param nonNumericNominations see {@link #getNonNumericNominations()}
   */
  /* package */ DefaultSubjectiveTeamScore(final int teamNumber,
                                           final Map<String, Double> simpleGoals,
                                           final Map<String, String> enumGoals,
                                           final boolean noShow,
                                           final String judge,
                                           final @Nullable String commentGreatJob,
                                           final @Nullable String commentThinkAbout,
                                           final Map<String, @Nullable String> goalComments,
                                           final @Nullable String note,
                                           final Set<String> nonNumericNominations) {
    super(teamNumber, simpleGoals, enumGoals, noShow);
    this.judge = judge;
    this.commentGreatJob = commentGreatJob;
    this.commentThinkAbout = commentThinkAbout;
    this.goalComments = new HashMap<>(goalComments);
    this.note = note;
    this.nonNumericNominations = Collections.unmodifiableSet(new HashSet<>(nonNumericNominations));
  }

  private final String judge;

  @Override
  public String getJudge() {
    return judge;
  }

  private final @Nullable String commentGreatJob;

  @Override
  public @Nullable String getCommentGreatJob() {
    return commentGreatJob;
  }

  private final @Nullable String commentThinkAbout;

  @Override
  public @Nullable String getCommentThinkAbout() {
    return commentThinkAbout;
  }

  private final Map<String, @Nullable String> goalComments;

  @Override
  public @Nullable String getGoalComment(final String goalName) {
    return goalComments.get(goalName);
  }

  private final Set<String> nonNumericNominations;

  @Override
  public Set<String> getNonNumericNominations() {
    return nonNumericNominations;
  }

  private final @Nullable String note;

  @Override
  public @Nullable String getNote() {
    return note;
  }

}
