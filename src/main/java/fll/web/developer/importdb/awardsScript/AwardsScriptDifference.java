/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

/**
 * Base class for differences in the awards script between two databases.
 */
public abstract class AwardsScriptDifference {

  /**
   * @return human readable description of the difference.
   */
  public abstract String getDescription();
}
