/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.xml.ChallengeDescription;

/**
 * Keys for all attributes in the application. These are initialized from
 * 'jspf/init.jspf', unless otherwise noted. Each key has an associated accessor
 * function as well that helps with type safety.
 *
 * @author jpschewe
 * @version $Revision$
 */
public final class ApplicationAttributes {

  /**
   * {@link javax.sql.DataSource} that is connected to the tournament database.
   * Initialized in 'jspf/init.jspf'.
   */
  public static final String DATASOURCE = "datasource";

  /**
   * Application attribute to hold names of all displays. Type is
   * SortedSet&lt;{@link DisplayInfo}&gt;. The default display is sorted first.
   */
  public static final String DISPLAY_INFORMATION = "displayInformation";

  private ApplicationAttributes() {
    // no instances
  }

  /**
   * {@link ChallengeDescription} that describes the current tournament.
   * See {@link #getChallengeDescription(ServletContext)}
   */
  public static final String CHALLENGE_DESCRIPTION = "challengeDescription";

  /**
   * @param application application variable store
   * @return the stored challenge description
   */
  public static ChallengeDescription getChallengeDescription(final ServletContext application) {
    return getNonNullAttribute(application, CHALLENGE_DESCRIPTION, ChallengeDescription.class);
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
   * @param <T> the expected type
   * @return the value
   */
  public static <T> @Nullable T getAttribute(final ServletContext application,
                                             final String attribute,
                                             final Class<T> clazz) {
    final Object o = application.getAttribute(attribute);
    if (o == null
        || clazz.isInstance(o)) {
      return clazz.cast(o);
    } else {
      throw new RuntimeException(String.format("Expecting application attribute '%s' to be of type '%s', but was of type '%s'",
                                               attribute, clazz, o.getClass()));
    }
  }

  /**
   * Get an application attribute and throw a {@link NullPointerException} if it's
   * null.
   *
   * @param application where to get the attribute from
   * @param attribute the name of the attribute to retrieve
   * @param <T> the type of value stored in the attribute
   * @param clazz the type of value stored in the attribute
   * @return the attribute value
   * @see #getAttribute(ServletContext, String, Class)
   */
  public static <T> T getNonNullAttribute(final ServletContext application,
                                          final String attribute,
                                          final Class<T> clazz) {
    final T retval = getAttribute(application, attribute, clazz);
    if (null == retval) {
      throw new NullPointerException(String.format("Session attribute %s is null when it's not expected to be",
                                                   attribute));
    }
    return retval;
  }

  /**
   * @param application application variable store
   * @return the database connection
   */
  public static DataSource getDataSource(final ServletContext application) {
    return getNonNullAttribute(application, ApplicationAttributes.DATASOURCE, DataSource.class);
  }

  /**
   * Key for authentications that need refreshing.
   */
  public static final String AUTH_REFRESH = "authRefresh";

  /**
   * @param application application variable store
   * @return authentications that need refreshing if created before the specified
   *         time
   */
  public static Map<String, LocalDateTime> getAuthRefresh(final ServletContext application) {
    @SuppressWarnings("unchecked") // can't get generics out of the application
    final Map<String, LocalDateTime> authRefresh = (Map<String, LocalDateTime>) getAttribute(application, AUTH_REFRESH,
                                                                                             Map.class);
    if (null == authRefresh) {
      final Map<String, LocalDateTime> newValue = new HashMap<>();
      application.setAttribute(AUTH_REFRESH, newValue);
      return newValue;
    } else {
      return authRefresh;
    }
  }

  /**
   * Key for authentications that need logging out.
   */
  public static final String AUTH_LOGGED_OUT = "authLoggedOut";

  /**
   * @param application application variable store
   * @return authentications that need to be logged out if created before the
   *         specified time
   */
  public static Map<String, LocalDateTime> getAuthLoggedOut(final ServletContext application) {
    @SuppressWarnings("unchecked") // can't get generics out of the application
    final Map<String, LocalDateTime> authLoggedOut = (Map<String, LocalDateTime>) getAttribute(application,
                                                                                               AUTH_LOGGED_OUT,
                                                                                               Map.class);
    if (null == authLoggedOut) {
      final Map<String, LocalDateTime> newValue = new HashMap<>();
      application.setAttribute(AUTH_LOGGED_OUT, newValue);
      return newValue;
    } else {
      return authLoggedOut;
    }
  }

}
