/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fll.web.playoff.TeamScore;

public abstract class AbstractGoal implements Serializable {

  public AbstractGoal(final Element ele) {
    mName = ele.getAttribute("name");
    mTitle = ele.getAttribute("title");

    final NodeList descEles = ele.getElementsByTagName("description");
    if (descEles.getLength() > 0) {
      final Node descEle = descEles.item(0);
      mDescription = descEle.getTextContent();
    } else {
      mDescription = null;
    }
  }

  private final String mName;

  public String getName() {
    return mName;
  }

  private final String mTitle;

  public String getTitle() {
    return mTitle;
  }

  private final String mDescription;

  public String getDescription() {
    return mDescription;
  }

  /**
   * Get the raw score.
   * 
   * @return the score or NaN if there is currently no score for this goal
   */
  public abstract double getRawScore(final TeamScore teamScore);

  /**
   * Get the computed score.
   * 
   * @return the score or NaN if there is currently no score for this goal
   */
  public abstract double getComputedScore(final TeamScore teamScore);

}
