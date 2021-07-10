/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

/**
 * Roles that a user can be in.
 */
public enum UserRole {
  PUBLIC("This role has no special permissions"), REF("This role can enter performance scores"), JUDGE(
      "This role can enter subjective scores"), ADMIN(
          "This role can do anything"), HEAD_JUDGE("This role is a judge that can run reports");

  UserRole(final String description) {
    this.description = description;
  }

  private final String description;

  /**
   * @return description of the role
   */
  public String getDescription() {
    return description;
  }

}
