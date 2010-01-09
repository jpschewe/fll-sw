/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import javax.servlet.ServletContext;

import org.w3c.dom.Document;

/**
 * Keys for all attributes in the application. These are initialized from
 * 'jspf/init.jspf', unless otherwise noted. Each key has an associated accessor
 * function as well that helps with type safety.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public final class ApplicationAttributes {

  private ApplicationAttributes() {
    // no instances
  }

  /**
   * {@link String} that holds the path to the database.
   */
  public static final String DATABASE = "database";

  public static String getDatabase(final ServletContext application) {
    return getAttribute(application, DATABASE, String.class);
  }

  /**
   * {@link org.w3c.dom.Document} that holds the current challenge descriptor.
   */
  public static final String CHALLENGE_DOCUMENT = "challengeDocument";

  public static Document getChallengeDocument(final ServletContext application) {
    return getAttribute(application, CHALLENGE_DOCUMENT, Document.class);
  }

  /**
   * {@link String} that is displayed on the big screen display.
   */
  public static final String SCORE_PAGE_TEXT = "ScorePageText";

  /**
   * {@link String} that keeps track of the division of the brackets being
   * displayed.
   */
  public static final String PLAYOFF_DIVISION = "playoffDivision";

  /**
   * {@link String} that keeps track of the run number of the brackets being
   * displayed.
   */
  public static final String PLAYOFF_RUN_NUMBER = "playoffRunNumber";

  /**
   * {@link String} that keeps track of which display page is being shown.
   */
  public static final String DISPLAY_PAGE = "displayPage";

  /**
   * Get session attribute and send appropriate error if type is wrong. Note
   * that null is always valid.
   * 
   * @param application where to get the attribute
   * @param attribute the attribute to get
   * @param clazz the expected type
   */
  public static <T> T getAttribute(final ServletContext application, final String attribute, final Class<T> clazz) {
    final Object o = application.getAttribute(attribute);
    if (o == null
        || clazz.isInstance(o)) {
      return clazz.cast(o);
    } else {
      throw new RuntimeException(String.format("Expecting application attribute '%s' to be of type '%s', but was of type '%s'", attribute, clazz, o.getClass()));
    }
  }

}
