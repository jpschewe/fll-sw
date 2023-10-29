/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Create an object to store the information about a display.
   *
   * @param name {@link #getName()}
   * @param uuid {@link #getUuid()}
   * @param useRemoteControl {@link #isUseRemoteControl()}
   */
  /* package */ DisplayInfo(final String uuid,
                            final String name,
                            final boolean useRemoteControl) {
    this.uuid = uuid;
    mName = name;
    mLastSeen = LocalTime.now();
    mRemotePage = DEFAULT_REMOTE_PAGE;
    this.useRemoteControl = useRemoteControl;

    // make sure there is 1 bracket object all of the time
    mBrackets.add(new H2HBracketDisplay(this, 0, "", 1));
  }

  private final boolean useRemoteControl;

  /**
   * @return if this display is controlled by the remote control page
   */
  public boolean isUseRemoteControl() {
    return useRemoteControl;
  }

  private final String uuid;

  /**
   * @return uuid for this display
   */
  public String getUuid() {
    return uuid;
  }

  private String mName;

  /**
   * @return display name
   */
  public String getName() {
    return mName;
  }

  /**
   * @param v {@link #getName()}
   */
  public void setName(final String v) {
    mName = v;
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
   */
  public void updateLastSeen() {
    mLastSeen = LocalTime.now();

    LOGGER.trace("updateLastSeen: {} -> {} default: {}", getName(), mLastSeen, isDefaultDisplay());
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
    return getUuid()
        + "_";
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
      return other.getUuid().equals(getUuid());
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
    if (o.getUuid().equals(getUuid())) {
      return 0;
    } else if (isDefaultDisplay()) {
      return -1;
    } else if (o.isDefaultDisplay()) {
      return 1;
    } else {
      return getUuid().compareTo(o.getUuid());
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

  private final List<String> scoreboardAwardGroups = new LinkedList<>();

  /**
   * @return the award groups to display on the score board, may be empty
   *         meaning
   *         display all
   */

  public List<String> getScoreboardAwardGroups() {
    return Collections.unmodifiableList(scoreboardAwardGroups);
  }

  /**
   * @param v see {@link #getScoreboardAwardGroups()}
   */
  public void setScoreboardAwardGroups(final List<String> v) {
    scoreboardAwardGroups.clear();
    scoreboardAwardGroups.addAll(v);
  }

  /**
   * Helper function for {@link #getScoreboardAwardGroups()} that handles the
   * configured value being empty or not matching any of the award groups for
   * the
   * tournament.
   *
   * @param allAwardGroups filter from this list
   * @return the configured award groups or all if none are configured
   */
  public List<String> determineScoreboardAwardGroups(final List<String> allAwardGroups) {
    final List<String> configuredGroups = getScoreboardAwardGroups();
    if (configuredGroups.isEmpty()) {
      return allAwardGroups;
    } else {
      // filter to those selected
      final List<String> filtered = allAwardGroups.stream().filter(v -> configuredGroups.contains(v))
                                                  .collect(Collectors.toList());
      if (filtered.isEmpty()) {
        return allAwardGroups;
      } else {
        return filtered;
      }
    }
  }

  /**
   * @param awardGroup the award group to check
   * @return true if the award group is shown on this display
   */
  public boolean isShowAwardGroup(final String awardGroup) {
    final List<String> configuredGroups = getScoreboardAwardGroups();
    if (configuredGroups.isEmpty()) {
      // display all award groups
      return true;
    } else {
      return configuredGroups.contains(awardGroup);
    }
  }

  /**
   * @return the parameter name for the remote page
   */
  public String getAwardGroupsFormParamName() {
    return getFormParamPrefix()
        + "awardGroups";
  }

  /**
   * This returns a URL starting with a slash. It is expected that the consumer of
   * this information will prepend the context path.
   * 
   * @return the URL
   */
  public String getUrl() {
    if (isWelcome()) {
      return "/welcome.jsp";
    } else if (isScoreboard()) {
      return "/scoreboard/dynamic.jsp";
    } else if (isSlideshow()) {
      return "/slideshow.jsp";
    } else if (isHeadToHead()) {
      return "/playoff/remoteMain.jsp";
    } else if (isFinalistTeams()) {
      return "/report/finalist/FinalistTeams.jsp?finalistTeamsScroll=true";
    } else if (isSpecial()) {
      return "/custom/"
          + getSpecialUrl();
    } else {
      return "/welcome.jsp";
    }
  }

}
