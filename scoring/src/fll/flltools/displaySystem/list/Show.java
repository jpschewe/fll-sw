/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools.displaySystem.list;

/**
 * A message to tell the list module to display.
 */
public class Show extends BaseDisplayMessage {
  public Show() {
    super("default", "show");
  }

  /**
   * @return null as there is no data for this message
   */
  @Override
  public Object getData() {
    return null;
  }

}
