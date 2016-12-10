/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A variable is a polynomial with a value.
 */
public class Variable extends BasicPolynomial {

  public static final String TAG_NAME = "variable";

  public static final String NAME_ATTRIBUTE = "name";

  public Variable(final Element ele,
                  final GoalScope goalScope) {
    super(ele, goalScope);

    mName = ele.getAttribute("name");
  }

  private String mName;

  public String getName() {
    return mName;
  }

  public void setName(final String v) {
    mName = v;
  }

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    populateXml(doc, ele);
    ele.setAttribute(NAME_ATTRIBUTE, mName);
    return ele;
  }

}
