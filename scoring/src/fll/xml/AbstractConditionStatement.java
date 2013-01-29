/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;

/**
 * Common parent class of {@link ConditionStatement} and
 * {@link EnumConditionStatement}.
 */
public abstract class AbstractConditionStatement implements Serializable {

  public AbstractConditionStatement(final Element ele) {
    if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("less-than")).hasNext()) {
      mComparison = InequalityComparison.LESS_THAN;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("less-than-or-equal")).hasNext()) {
      mComparison = InequalityComparison.LESS_THAN_OR_EQUAL;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("greater-than")).hasNext()) {
      mComparison = InequalityComparison.GREATER_THAN;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("greater-than-or-equal")).hasNext()) {
      mComparison = InequalityComparison.GREATER_THAN_OR_EQUAL;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("equal-to")).hasNext()) {
      mComparison = InequalityComparison.EQUAL_TO;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName("not-equal-to")).hasNext()) {
      mComparison = InequalityComparison.NOT_EQUAL_TO;
    } else {
      throw new FLLInternalException("Unknown comparison");
    }
  }

  private final InequalityComparison mComparison;

  public InequalityComparison getComparison() {
    return mComparison;
  }

  /**
   * Does this conditional statement evaluate to true?
   */
  public abstract boolean isTrue(TeamScore teamScore);

}
