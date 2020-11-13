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
  /**
   * This role has no special permissions.
   */
  PUBLIC,
  /** This role can enter performance scores. */
  REF,
  /** This role can enter subjective scores. */
  JUDGE,
  /** This role can do anything. */
  ADMIN;

}
