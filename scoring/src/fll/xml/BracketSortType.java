package fll.xml;

/**
 * How to sort teams as they are seeded into the brackets.
 * 
 * @author jpschewe
 */
public enum BracketSortType {

  SEEDING("Use the best score from the seeding rounds"), ALPHA_TEAM("Sort alphabetically on team name"), RANDOM(
      "Use random assignment");

  private BracketSortType(final String description) {
    mDescription = description;
  }

  private final String mDescription;

  public String getDescription() {
    return mDescription;
  }
}
