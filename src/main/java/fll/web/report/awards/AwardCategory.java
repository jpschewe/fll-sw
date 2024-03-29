/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

import java.io.Serializable;

/**
 * Common elements for categories that are given awards.
 */
public interface AwardCategory extends Serializable {

  /**
   * @return display string for the category
   */
  String getTitle();

  /**
   * @return if the winners are per award group, otherwise per tournament
   */
  boolean getPerAwardGroup();

}
