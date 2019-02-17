/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collection;

import javax.annotation.Nonnull;

/**
 * Used to lookup goals.
 */
public interface GoalScope {

  /**
   * Get the specified goal.
   * 
   * @param name the name of the goal to find
   * @return the found goal
   * @throws ScopeException if the goal cannot be found
   */
  @Nonnull
  public AbstractGoal getGoal(final String name) throws ScopeException;

  /**
   * 
   * @return all goals currently known to the scope
   */
  @Nonnull
  public Collection<AbstractGoal> getAllGoals();
  
  
}
