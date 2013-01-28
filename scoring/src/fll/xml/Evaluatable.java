/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import fll.web.playoff.TeamScore;

/**
 * Something that can have a score computed for.
 */
public interface Evaluatable {

  /**
   * Compute the numeric score given the individual score elements for a team.
   * 
   * @param teamScore the individual score elements for a team
   * @return the score
   */
  //FIXME public double evaluate(final TeamScore teamScore);

}
