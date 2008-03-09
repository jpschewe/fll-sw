/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

/**
 * Keys for session variables.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public final class SessionAttributes {

  private SessionAttributes() {
    // no instances
  }

  /**
   * A {@link String} that is a message to display. This is set in many pages
   * and servlets to pass a message onto the next page to display to the user.
   */
  public static final String MESSAGE = "message";

  /**
   * {@link javax.sql.DataSource} that is connected to the tournament database.
   * Initialized in 'jspf/init.jspf'.
   */
  public static final String DATASOURCE = "datasource";

  /**
   * List of {@link fll.Team}s. Used in playoff/brackets.jsp to keep track of
   * the current list of teams in bracket order.
   */
  public static final String CURRENT_ROUND = "currentRound";

  /**
   * {@link String} that keeps track of what page is being shown on the big
   * screen. Used in conjunction with {@link ApplicationAttributes#DISPLAY_PAGE}
   * to keep from refreshing the display too often.
   */
  public static final String SESSION_DISPLAY_PAGE = "sessionDisplayPage";

}
