/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

/**
 * Interface for objects that can provide information about a task being
 * canceled.
 */
public interface CheckCanceled {

  /**
   * @return true if the task should be canceled
   */
  public boolean isCanceled();

}
