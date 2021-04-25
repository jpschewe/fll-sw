/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.util.Collection;

/**
 * Used to lookup variables.
 */
public interface VariableScope {

  /**
   * Get the specified variable.
   * 
   * @param name the name of the variable to find
   * @return the found variable
   * @throws ScopeException if the variable cannot be found
   */
  Variable getVariable(String name) throws ScopeException;

  /**
   * @return all variables currently known to the scope
   */
  Collection<Variable> getAllVariables();

}
