/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.web.playoff.TeamScore;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Common parent class of {@link ConditionStatement} and
 * {@link EnumConditionStatement}.
 */
public abstract class AbstractConditionStatement implements Serializable {

  /**
   * Construct from an XML element.
   *
   * @param ele the element to parse
   */
  public AbstractConditionStatement(final Element ele) {
    if (new NodelistElementCollectionAdapter(ele.getElementsByTagName(InequalityComparison.LESS_THAN.getTagName())).hasNext()) {
      mComparison = InequalityComparison.LESS_THAN;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName(InequalityComparison.LESS_THAN_OR_EQUAL.getTagName())).hasNext()) {
      mComparison = InequalityComparison.LESS_THAN_OR_EQUAL;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName(InequalityComparison.GREATER_THAN.getTagName())).hasNext()) {
      mComparison = InequalityComparison.GREATER_THAN;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName(InequalityComparison.GREATER_THAN_OR_EQUAL.getTagName())).hasNext()) {
      mComparison = InequalityComparison.GREATER_THAN_OR_EQUAL;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName(InequalityComparison.EQUAL_TO.getTagName())).hasNext()) {
      mComparison = InequalityComparison.EQUAL_TO;
    } else if (new NodelistElementCollectionAdapter(ele.getElementsByTagName(InequalityComparison.NOT_EQUAL_TO.getTagName())).hasNext()) {
      mComparison = InequalityComparison.NOT_EQUAL_TO;
    } else {
      throw new FLLInternalException("Unknown comparison");
    }
  }

  /**
   * Default constructor uses {@link InequalityComparison#EQUAL_TO} as the value
   * for {@link #getComparison()}.
   */
  public AbstractConditionStatement() {
    mComparison = InequalityComparison.EQUAL_TO;
  }

  private InequalityComparison mComparison;

  /**
   * @return the comparator to use.
   */

  public InequalityComparison getComparison() {
    return mComparison;
  }

  /**
   * See {@link #getComparison()}.
   *
   * @param v the new value for the comparator
   */
  public void setComparison(final InequalityComparison v) {
    mComparison = v;
  }

  /**
   * Does this conditional statement evaluate to true?
   *
   * @param teamScore the score to evaluate
   * @return if this condition is true for the specified score
   */
  public abstract boolean isTrue(TeamScore teamScore);

  /**
   * @param doc used to create elements
   * @return an XML element representing {@link #getComparison()}
   */
  protected final Element getComparisonElement(final Document doc) {
    return doc.createElement(mComparison.getTagName());
  }

  /**
   * Convert this object to XML.
   * 
   * @param doc used to create elements
   * @return an XML element representing this object
   */
  public abstract Element toXml(Document doc);

}
