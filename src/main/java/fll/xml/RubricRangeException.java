/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.xml;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Thrown when there is a validation error with a {@link RubricRange}.
 */
public class RubricRangeException extends ChallengeValidationException {

  private static String generateMessage(final Goal goal,
                                        final RubricRange range,
                                        final @Nullable String message) {

    return String.format("There is a problem with the range %s in the goal %s%s", range.getTitle(), goal.getTitle(),
                         null == message ? "" : String.format(": %s", message));
  }

  /**
   * Something is wrong about the list of {@link RubricRange} objects.
   * 
   * @param goal the goal with the problematic range
   */
  public RubricRangeException(final Goal goal) {
    super(String.format("There is a problem with the list of rubric ranges in goal %s", goal.getTitle()));
  }

  /**
   * Something is wrong about the list of {@link RubricRange} objects.
   * 
   * @param goal the goal with the problematic range
   * @param message description of the error
   */
  public RubricRangeException(final Goal goal,
                              final String message) {
    super(String.format("There is a problem with the list of rubric ranges in goal %s: %s", goal.getTitle(), message));
  }

  /**
   * @param goal the goal with the problematic range
   * @param range the problematic range
   */
  public RubricRangeException(final Goal goal,
                              final RubricRange range) {
    super(generateMessage(goal, range, null));
  }

  /**
   * @param goal the goal with the problematic range
   * @param range the problematic range
   * @param message description of the error
   */
  public RubricRangeException(final Goal goal,
                              final RubricRange range,
                              final String message) {
    super(generateMessage(goal, range, message));
  }

  /**
   * @param goal the goal with the problematic range
   * @param range the problematic range
   * @param message description of the error
   * @param cause {@link Throwable#getCause()}
   */
  public RubricRangeException(final Goal goal,
                              final RubricRange range,
                              final String message,
                              final Throwable cause) {
    super(generateMessage(goal, range, message), cause);
  }

}
