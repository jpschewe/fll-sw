/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Used to lookup variables.
 */
public interface VariableScope {

  public Variable getVariable(final String name) throws ScopeException;

}
