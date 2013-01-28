/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import fll.web.playoff.TeamScore;

/**
 * Common parent class of {@link ConditionStatement} and
 * {@link EnumConditionStatement}.
 */
public abstract class AbstractConditionStatement implements Evaluatable, Serializable {

  /**
   * Does this conditional statement evaluate to true?
   */
  // FIXME public abstract boolean isTrue(TeamScore teamScore);

}
