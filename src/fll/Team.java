/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

/**
 * Represents a team.
 *
 * @version $Revision$
 */
final public class Team {
  
  /**
   * Constant to represent the team number for a bye 
   */
  public static final int BYE_TEAM_NUMBER = -1;
  
  /**
   * Constant to represent the team number when there is a tie
   */
  public static final int TIE_TEAM_NUMBER = -2;
  
  /**
   * Team that represents a BYE
   */
  public final static Team BYE = new Team();

  /**
   * Team that represents a TIE.
   */
  public final static Team TIE = new Team();

  static {
    BYE.setTeamNumber(BYE_TEAM_NUMBER);
    BYE.setTeamName("BYE");
    TIE.setTeamNumber(TIE_TEAM_NUMBER);
    TIE.setTeamName("TIE");
  }
  
  public Team() {
     
  }

  private int _teamNumber;
  public int getTeamNumber() { return _teamNumber; }
  public void setTeamNumber(final int v) { _teamNumber = v; }

  private String _organization;
  public String getOrganization() { return _organization; }
  public void setOrganization(final String v) { _organization = v; }

  private String _teamName;
  public String getTeamName() { return _teamName; }
  public void setTeamName(final String v) { _teamName = v; }


  private String _region;
  public String getRegion() { return _region; }
  public void setRegion(final String v) { _region = v; }

  private int _division;
  public int getDivision() { return _division; }
  public void setDivision(final int v) { _division = v; }

  public boolean equals(final Object o) {
    if(o instanceof Team) {
      final Team other = (Team)o;
      return other.getTeamNumber() == getTeamNumber();
    } else {
      return false;
    }
  }

  public String toString() {
    return "[" + getTeamNumber() + " " + getTeamName() + "]";
  }
}
