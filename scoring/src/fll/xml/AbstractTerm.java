/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

/**
 * Common elements of {@link Term} and {@link VariableRef}.
 */
public abstract class AbstractTerm implements Evaluatable, Serializable {

  public AbstractTerm(final Element ele) {
    double coefficient = 1;
    for (final Element constantEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("constant"))) {
      final double value = Double.valueOf(constantEle.getAttribute("value"));
      coefficient = coefficient
          * value;
    }

    mCoefficient = coefficient;
  }

  private final double mCoefficient;

  public double getCoefficient() {
    return mCoefficient;
  }

}
