/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Thrown when the goals in a goal group are not contiguous.
 */
public class GoalGroupSplitException extends ChallengeXMLException {

  /**
   * @param goalGroupName name of goal group that is split
   */
  public GoalGroupSplitException(final String goalGroupName) {
    super(String.format("Goal group '%s' is broken up into multiple sections. All goals in the same goal group need to be next to each other in the challenge description",
                        goalGroupName));
  }

}
