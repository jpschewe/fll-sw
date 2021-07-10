/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.FLLRuntimeException;

/**
 * Information about a display.
 */
public final class DisplayInfo implements Serializable, Comparable<DisplayInfo> {

  /**
   * Name used for the default display when a name is needed.
   * Needs to match remoteControl.js
   */
  public static final String DEFAULT_DISPLAY_NAME = "Default";

  /**
   * Follow the default display.
   */
  public static final String DEFAULT_REMOTE_PAGE = "default";

  /**
   * Constant for the welcome page.
   */
  public static final String WELCOME_REMOTE_PAGE = "welcome";

  /**
   * Constant for displaying the scoreboard.
   */
  public static final String SCOREBOARD_REMOTE_PAGE = "scoreboard";

  /**
   * Constant for displaying the slideshow.
   */
  public static final String SLIDESHOW_REMOTE_PAGE = "slideshow";

  /**
   * Constant for displaying the playoffs.
   */
  public static final String HEAD_TO_HEAD_REMOTE_PAGE = "playoffs";

  /**
   * Constant for displaying the finalist teams.
   */
  public static final String FINALIST_TEAMS_REMOTE_PAGE = "finalistTeams";

  /**
   * Constant for displaying a page outside of the normal pages.
   */
  public static final String SPECIAL_REMOTE_PAGE = "special";

  private static final Object LOCK = new Object();

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Create a string that's a valid HTML name.
   */
  private static @Nullable String sanitizeDisplayName(final @Nullable String str) {
    if (StringUtils.isBlank(str)) {
      return null;
    } else if (DEFAULT_DISPLAY_NAME.equalsIgnoreCase(str)) {
      return "_default"; // make sure this display doesn't conflict with the
                         // internal default
    } else {
      String ret = str;
      final Matcher illegalCharMatcher = ILLEGAL_CHAR_PATTERN.matcher(ret);
      ret = illegalCharMatcher.replaceAll("_");

      return ret;
    }
  }

  private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9_]");

  /**
   * Add a display name to the list of known displays.
   * This sets the current time as the last seen time. So this method can also
   * be used to update the last seen time.
   *
   * @param application used to track the list of all display names
   * @param session used to store the display name for the page to see. Variable
   *          is "displayName".
   *          This name will be a legal HTML identifier.
   * @param name the name to set for the display, may be different from what is
   *          stored
   */
  public static void appendDisplayName(final ServletContext application,
                                       final HttpSession session,
                                       final @Nullable String name) {
    session.removeAttribute(SessionAttributes.DISPLAY_NAME);

    final String sanitized = sanitizeDisplayName(name);

    if (null == sanitized) {
      // nothing to store
      return;
    }

    session.setAttribute(SessionAttributes.DISPLAY_NAME, sanitized);

    synchronized (LOCK) {
      final Collection<DisplayInfo> displayInformation = internalGetDisplayInformation(application);
      final DisplayInfo check = new DisplayInfo(sanitized);
      if (!displayInformation.contains(check)) {
        displayInformation.add(check);
        application.setAttribute(ApplicationAttributes.DISPLAY_INFORMATION, displayInformation);
      }
    }
  }

  /**
   * Delete a display. It will reappear if the display is still active.
   * Also cleans up the related application attributes.
   * If <code>displayInfo</code> is the default display this method does nothing
   * as the default display cannot be deleted.
   *
   * @param application where the displays are stored
   * @param displayInfo the display to be deleted
   */
  public static void deleteDisplay(final ServletContext application,
                                   final DisplayInfo displayInfo) {
    if (!displayInfo.isDefaultDisplay()) {
      synchronized (LOCK) {
        final SortedSet<DisplayInfo> displayInformation = internalGetDisplayInformation(application);

        displayInformation.remove(displayInfo);
        application.setAttribute(ApplicationAttributes.DISPLAY_INFORMATION, displayInformation);
      }
    }
  }

  /**
   * @param application used to get all of the displays
   * @param session used to get the display name
   * @return a non-null {@link DisplayInfo} object
   * @see #getInfoForDisplay(ServletContext, String)
   */

  public static DisplayInfo getInfoForDisplay(final ServletContext application,
                                              final HttpSession session) {
    final String displayName = SessionAttributes.getDisplayName(session);
    return getInfoForDisplay(application, displayName);
  }

  /**
   * Get the appropriate {@link DisplayInfo} object for the name.
   * If the named display is following the default display or doesn't have a
   * name, then the default display is returned.
   *
   * @param application used to get all of the displays
   * @param displayName the name of the display, may be null
   * @return a non-null {@link DisplayInfo} object
   */
  public static DisplayInfo getInfoForDisplay(final ServletContext application,
                                              final @Nullable String displayName) {
    final DisplayInfo display = getNamedDisplay(application, displayName);
    if (null == display
        || display.isFollowDefault()) {
      return findOrCreateDefaultDisplay(application);
    } else {
      return display;
    }
  }

  /**
   * Create a {@link DisplayInfo} object with {@link #DEFAULT_DISPLAY_NAME}.
   */
  private static DisplayInfo createDefault() {
    final DisplayInfo def = new DisplayInfo(DEFAULT_DISPLAY_NAME);
    def.setRemotePage(WELCOME_REMOTE_PAGE);
    return def;
  }

  /**
   * Get the display information from the application context. If the default
   * display isn't there, then it's added.
   *
   * @return an unmodifiable collection sorted by display name with the default
   *         display first
   * @param application the application context to find information in
   */

  public static Collection<DisplayInfo> getDisplayInformation(final ServletContext application) {
    return Collections.unmodifiableCollection(internalGetDisplayInformation(application));
  }

  private static SortedSet<DisplayInfo> internalGetDisplayInformation(final ServletContext application) {
    @SuppressWarnings("unchecked")
    SortedSet<DisplayInfo> displayInformation = ApplicationAttributes.getAttribute(application,
                                                                                   ApplicationAttributes.DISPLAY_INFORMATION,
                                                                                   SortedSet.class);
    if (null == displayInformation) {
      displayInformation = new TreeSet<>();
      // don't need set as it will happen in the next if statement body
    }
    if (displayInformation.isEmpty()
        || !displayInformation.first().isDefaultDisplay()) {
      displayInformation.add(createDefault());
      application.setAttribute(ApplicationAttributes.DISPLAY_INFORMATION, displayInformation);
    }

    return displayInformation;
  }

  /**
   * Find the default display information. If it doesn't exist, create it and
   * add it to the application.
   *
   * @param application where to find display information
   * @return a non-null {@link DisplayInfo} object
   */

  public static DisplayInfo findOrCreateDefaultDisplay(final ServletContext application) {
    final Collection<DisplayInfo> displayInformation = getDisplayInformation(application);
    for (final DisplayInfo info : displayInformation) {
      if (info.getName().equals(DEFAULT_DISPLAY_NAME)) {
        return info;
      }
    }

    final DisplayInfo info = createDefault();
    displayInformation.add(info);
    application.setAttribute(ApplicationAttributes.DISPLAY_INFORMATION, displayInformation);

    return info;
  }

  /**
   * Find the {@link DisplayInfo} with the specified name.
   *
   * @param application where to find the information
   * @param name the name of the display to find
   * @return the display or null if not known
   */
  public static @Nullable DisplayInfo getNamedDisplay(final ServletContext application,
                                                      final @Nullable String name) {
    final Collection<DisplayInfo> displayInformation = getDisplayInformation(application);
    for (final DisplayInfo info : displayInformation) {
      if (info.getName().equals(name)) {
        return info;
      }
    }
    return null;
  }

  /**
   * Create an object to store the information about a display.
   *
   * @param name the name of the display.
   */
  private DisplayInfo(final String name) {
    mName = name;
    mLastSeen = LocalTime.now();
    mRemotePage = DEFAULT_REMOTE_PAGE;

    // make sure there is 1 bracket object all of the time
    mBrackets.add(new H2HBracketDisplay(this, 0, "", 1));
  }

  private final String mName;

  /**
   * @return display name
   */
  public String getName() {
    return mName;
  }

  private LocalTime mLastSeen;

  /**
   * @return when the display was last seen
   */

  public LocalTime getLastSeen() {
    return mLastSeen;
  }

  /**
   * Update the last seen time to be now.
   *
   * @param application used to store the updated {@link DisplayInfo} object.
   */
  public void updateLastSeen(final ServletContext application) {
    mLastSeen = LocalTime.now();

    LOGGER.trace("updateLastSeen: "
        + getName()
        + " -> "
        + mLastSeen
        + " default: "
        + isDefaultDisplay());

    if (!isDefaultDisplay()) {
      synchronized (LOCK) {
        final SortedSet<DisplayInfo> displayInformation = internalGetDisplayInformation(application);

        LOGGER.trace("Before remove of display copy: "
            + displayInformation);

        final boolean removed = displayInformation.remove(this); // remove
                                                                 // possible
                                                                 // outdated
                                                                 // copy
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("updateLastSeen: removed old data for display "
              + getName()
              + "? "
              + removed);
        }

        displayInformation.add(this); // insert updated version

        LOGGER.trace("After add of updated display information: "
            + displayInformation);

        application.setAttribute(ApplicationAttributes.DISPLAY_INFORMATION, displayInformation);
      }
    }
  }

  /**
   * @return Is this the default display?
   */
  public boolean isDefaultDisplay() {
    return DEFAULT_DISPLAY_NAME.equals(mName);
  }

  private String mRemotePage;

  /**
   * @return The the page that should be shown on this display
   */
  public String getRemotePage() {
    return mRemotePage;
  }

  /**
   * @param v {@link #getRemotePage()}
   */
  public void setRemotePage(final String v) {
    mRemotePage = v;
  }

  /**
   * Needs to match remoteControl.js.
   *
   * @return Get the prefix for form parameters for this display
   */
  public String getFormParamPrefix() {
    if (isDefaultDisplay()) {
      return "";
    } else {
      return getName()
          + "_";
    }
  }

  /**
   * @return the parameter name for the remote page
   */
  public String getRemotePageFormParamName() {
    return getFormParamPrefix()
        + "remotePage";
  }

  /**
   * @return the parameter name for the remote URL
   */
  public String getRemoteUrlFormParamName() {
    return getFormParamPrefix()
        + "remoteURL";
  }

  /**
   * @return the parameter for deleting a display
   */
  public String getDeleteFormParamName() {
    return getFormParamPrefix()
        + "delete";
  }

  /**
   * @return the form parameter for the special URL to display
   */
  public String getSpecialUrlFormParamName() {
    return getFormParamPrefix()
        + "remoteURL";
  }

  /**
   * @return the parameter name for the award group to display the finalist
   *         schedule for
   */
  public String getFinalistScheduleAwardGroupFormParamName() {
    return getFormParamPrefix()
        + "finalistDivision";
  }

  /**
   * @return the parameter name for the number of head to head brackets to display
   */
  public String getHead2HeadNumBracketsFormParamName() {
    return getFormParamPrefix()
        + "numBrackets";
  }

  /**
   * @param bracketIdx the index for the bracket
   * @return the parameter for the bracket to display in multiple brackets
   */
  public String getHead2HeadBracketFormParamName(final int bracketIdx) {
    return getFormParamPrefix()
        + "playoffDivision_"
        + bracketIdx;
  }

  /**
   * @param bracketIdx the index for the bracket
   * @return the parameter for the first round to display in multiple brackets
   */
  public String getHead2HeadFirstRoundFormParamName(final int bracketIdx) {
    return getFormParamPrefix()
        + "playoffRoundNumber_"
        + bracketIdx;
  }

  @Override
  @EnsuresNonNullIf(expression = "#1", result = true)
  public boolean equals(final @Nullable Object o) {
    if (o instanceof DisplayInfo) {
      final DisplayInfo other = (DisplayInfo) o;
      return other.getName().equals(getName());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return mName.hashCode();
  }

  @Override
  public String toString() {
    return "DisplayInfo for: "
        + getName();
  }

  @Override
  public int compareTo(final DisplayInfo o) {
    if (o.getName().equals(getName())) {
      return 0;
    } else if (isDefaultDisplay()) {
      return -1;
    } else if (o.isDefaultDisplay()) {
      return 1;
    } else {
      return getName().compareTo(o.getName());
    }
  }

  /**
   * @return is this display following the default display
   */
  public boolean isFollowDefault() {
    return DEFAULT_REMOTE_PAGE.equals(mRemotePage);
  }

  /**
   * State that this display follows the default display.
   * Throws an exception if this is the default display.
   */
  public void setFollowDefault() {
    if (isDefaultDisplay()) {
      throw new FLLRuntimeException("The default display cannot follow default");
    } else {
      setRemotePage(DEFAULT_REMOTE_PAGE);
    }
  }

  /**
   * @return is the welcome page being displayed
   */
  public boolean isWelcome() {
    return WELCOME_REMOTE_PAGE.equals(mRemotePage);
  }

  /**
   * @return is the scoreboard being displayed
   */
  public boolean isScoreboard() {
    return SCOREBOARD_REMOTE_PAGE.equals(mRemotePage);
  }

  /**
   * @return is the head to head page being displayed
   */
  public boolean isHeadToHead() {
    return HEAD_TO_HEAD_REMOTE_PAGE.equals(mRemotePage);
  }

  /**
   * @return is the finalist teams page being displayed
   */
  public boolean isFinalistTeams() {
    return FINALIST_TEAMS_REMOTE_PAGE.equals(mRemotePage);
  }

  /**
   * @return is the slideshow being displayed
   */
  public boolean isSlideshow() {
    return SLIDESHOW_REMOTE_PAGE.equals(mRemotePage);
  }

  /**
   * @return is the special page being displayed
   */
  public boolean isSpecial() {
    return SPECIAL_REMOTE_PAGE.equals(mRemotePage);
  }

  private @Nullable String mSpecialUrl = null;

  /**
   * @return the special URL to display
   */
  public @Nullable String getSpecialUrl() {
    return mSpecialUrl;
  }

  /**
   * @param v see {@link #getSpecialUrl()}
   */
  public void setSpecialUrl(final @Nullable String v) {
    mSpecialUrl = v;
  }

  private @Nullable String mFinalistScheduleAwardGroup;

  /**
   * @return which award group to show the finalist schedule for
   */
  public @Nullable String getFinalistScheduleAwardGroup() {
    return mFinalistScheduleAwardGroup;
  }

  /**
   * @param v see {@link #getFinalistScheduleAwardGroup()}
   */
  public void setFinalistScheduleAwardGroup(final @Nullable String v) {
    mFinalistScheduleAwardGroup = v;
  }

  private final List<H2HBracketDisplay> mBrackets = new LinkedList<>();

  /**
   * @return Head to head brackets to display on this display.
   */
  public List<H2HBracketDisplay> getBrackets() {
    return Collections.unmodifiableList(mBrackets);
  }

  /**
   * @param v see {@link #getBrackets()}
   */
  public void setBrackets(final List<H2HBracketDisplay> v) {
    mBrackets.clear();
    mBrackets.addAll(v);
  }

  /**
   * Information about a head to head bracket to be displayed.
   */
  public static final class H2HBracketDisplay implements Serializable {
    /**
     * @param parent the display info to delegate to for values
     * @param index {@link #getIndex()}
     * @param bracket {@link #getBracket()}
     * @param firstRound {@link #getFirstRound()}
     */
    public H2HBracketDisplay(final @UnknownInitialization DisplayInfo parent,
                             final int index,
                             final String bracket,
                             final int firstRound) {
      mParent = parent;
      mIndex = index;
      mBracket = bracket;
      mFirstRound = firstRound;
    }

    private final @NotOnlyInitialized DisplayInfo mParent;

    private final int mIndex;

    /**
     * @return The index of this object inside it's list
     * @see DisplayInfo#getBrackets()
     */
    public int getIndex() {
      return mIndex;
    }

    private final String mBracket;

    /**
     * @return The bracket to display.
     */
    public String getBracket() {
      return mBracket;
    }

    private final int mFirstRound;

    /**
     * @return The first round to display.
     */
    public int getFirstRound() {
      return mFirstRound;
    }

    /**
     * @return the form parameter name for the bracket name for this bracket
     */
    public String getHead2HeadBracketFormParamName() {
      return mParent.getHead2HeadBracketFormParamName(mIndex);
    }

    /**
     * @return the form parameter for the first round for this bracket
     */
    public String getHead2HeadFirstRoundFormParamName() {
      return mParent.getHead2HeadFirstRoundFormParamName(mIndex);
    }

  } // class H2HBracketDisplay

  private final List<String> scoreboardJudgingGroups = new LinkedList<>();

  /**
   * @return the judging groups to display on the score board, may be empty
   *         meaning
   *         display all
   */

  public List<String> getScoreboardJudgingGroups() {
    return Collections.unmodifiableList(scoreboardJudgingGroups);
  }

  /**
   * @param v see {@link #getScoreboardJudgingGroups()}
   */
  public void setScoreboardJudgingGroups(final List<String> v) {
    scoreboardJudgingGroups.clear();
    scoreboardJudgingGroups.addAll(v);
  }

  /**
   * Helper function for {@link #getScoreboardJudgingGroups()} that handles the
   * configured value being empty or not matching any of the judging groups for
   * the
   * tournament.
   *
   * @param allJudgingGroups filter from this list
   * @return the configured judging groups or all if none are configured
   */
  public List<String> determineScoreboardJudgingGroups(final List<String> allJudgingGroups) {
    final List<String> configuredGroups = getScoreboardJudgingGroups();
    if (configuredGroups.isEmpty()) {
      return allJudgingGroups;
    } else {
      // filter to those selected
      final List<String> filtered = allJudgingGroups.stream().filter(v -> configuredGroups.contains(v))
                                                    .collect(Collectors.toList());
      if (filtered.isEmpty()) {
        return allJudgingGroups;
      } else {
        return filtered;
      }
    }
  }

  /**
   * @return the parameter name for the remote page
   */
  public String getJudgingGroupsFormParamName() {
    return getFormParamPrefix()
        + "judgingGroups";
  }

}
