/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Common elements of {@link Term} and {@link VariableRef}.
 */
public abstract class AbstractTerm implements Evaluatable, Serializable {

  public static final String CONSTANT_TAG_NAME = "constant";

  public static final String CONSTANT_VALUE_ATTRIBUTE = "value";

  /**
   * Collapses all constant elements into a single coefficient.
   * 
   * @param ele the element to parse
   */
  public AbstractTerm(final Element ele) {
    double coefficient = 1;
    for (final Element constantEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(CONSTANT_TAG_NAME))) {
      final double value = Double.valueOf(constantEle.getAttribute(CONSTANT_VALUE_ATTRIBUTE));
      coefficient = coefficient
          * value;
    }

    mCoefficient = coefficient;
  }

  private double mCoefficient;

  public double getCoefficient() {
    return mCoefficient;
  }

  public void setCoefficient(final double v) {
    mCoefficient = v;
  }
  
  protected Element createConstantElement(final Document doc) {
    final Element ele = doc.createElement(CONSTANT_TAG_NAME);
    ele.setAttribute(CONSTANT_VALUE_ATTRIBUTE, Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(mCoefficient));
    return ele;
  }

}
