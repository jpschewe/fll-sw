package fll.xml;

/**
 * How to sort teams as they are seeded into the brackets.
 * 
 * @author jpschewe
 *
 */
public enum BracketSortType {

  /**
   * Use the best score from the seeding rounds.
   */
  SEEDING,
  /**
   * Sort alphabetically on team name.
   */
  ALPHA_TEAM,
  /**
   * Use a random assignment.
   */
  RANDOM;
}
