package fll.xml;

/**
 * How to sort teams as they are seeded into the brackets.
 *
 * @author jpschewe
 */
public enum BracketSortType {

  SEEDING("Use the best score from regular match play"), //
  ALPHA_TEAM("Sort alphabetically on team name"), //
  RANDOM("Use random assignment");

  BracketSortType(final String description) {
    mDescription = description;
  }

  private final String mDescription;

  /**
   * @return the description of the sort type
   */
  public String getDescription() {
    return mDescription;
  }
}
