/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import javax.annotation.Nonnull;

import fll.xml.TiebreakerTest;

/**
 * Editor for {@link TiebreakerTest} objects.
 */
public class TiebreakerTestEditor extends PolynomialEditor {

  private final TiebreakerTest test;

  public TiebreakerTestEditor(@Nonnull final TiebreakerTest test) {
    super(test, false);
    this.test = test;
    
    //FIXME add winner type
    
  }

  /**
   * @return the tiebreaker test based on the current value of the editor
   */
  public TiebreakerTest getTest() {
    // FIXME implement
    throw new RuntimeException("Not implemented yet");
  }

}
