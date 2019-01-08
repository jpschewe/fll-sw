/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools;

/**
 * Interface for {@link BaseMessage} messsages that have sequence number.
 */
public interface SequenceNumberCommand {

  /**
   * 
   * @return the sequence number
   */
  int getSeq();
  
  /**
   * 
   * @param v the new sequence number
   */
  void setSeq(int v);
  
}
