/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler.assign;

import java.util.Date;

class TeamInfo {
  
  private final String pref1;

  public String getPref1() {
    return pref1;
  }

  private final String pref2;

  public String getPref2() {
    return pref2;
  }

  private final String pref3;

  public String getPref3() {
    return pref3;
  }

  private final int number;

  public int getNumber() {
    return number;
  }

  private final String name;

  public String getName() {
    return name;
  }

  private final String division;

  public String getDivision() {
    return division;
  }

  private final Date registrationDate;

  public Date getRegistrationDate() {
    return registrationDate;
  }

  public TeamInfo(final Date registrationDate,
                  final int number,
                  final String name,
                  final String division,
                  final String pref1,
                  final String pref2,
                  final String pref3) {
    this.registrationDate = registrationDate;
    this.name = name;
    this.number = number;
    this.division = division;
    this.pref1 = pref1;
    this.pref2 = pref2;
    this.pref3 = pref3;
  }
}