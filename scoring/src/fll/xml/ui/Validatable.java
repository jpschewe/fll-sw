/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

/**
 * Common interface for objects that can be validated.
 */
/*package*/ interface Validatable {

  /**
   * Check if the object is valid.
   * 
   * @return if the object is valid
   */
  boolean checkValidity();

}
