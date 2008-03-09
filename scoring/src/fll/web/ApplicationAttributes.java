/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

/**
 * Keys for all attributes in the application. These are initialized from
 * 'jspf/init.jspf', unless otherwise noted.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public final class ApplicationAttributes {

  private ApplicationAttributes() {
    // no instances
  }
  
  /**
   * {@link java.sql.Connection} to the tournament database.
   * 
   * @deprecated Use SessionAttributes.DATASOURCE
   */
  public static final String CONNECTION = "connection";

  /**
   * {@link String} that holds the path to the database.
   */
  public static final String DATABASE = "database";
  
  /**
   * {@link org.w3c.dom.Document} that holds the current challenge descriptor.
   */
  public static final String CHALLENGE_DOCUMENT = "challengeDocument";
  
  /**
   * {@link String} that is displayed on the big screen display.
   */
  public static final String SCORE_PAGE_TEXT = "ScorePageText";

  /**
   * {@link String} that keeps track of the division of the brackets being displayed.
   */
  public static final String PLAYOFF_DIVISION = "playoffDivision";
  
  /**
   * {@link String} that keeps track of the run number of the brackets being displayed.
   */
  public static final String PLAYOFF_RUN_NUMBER = "playoffRunNumber";
  
  /**
   * {@link String} that keeps track of which display page is being shown.
   */
  public static final String DISPLAY_PAGE = "displayPage";
  
}
