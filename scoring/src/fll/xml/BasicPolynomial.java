/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Element;

/**
 * 
 */
public class BasicPolynomial implements Evaluatable, Serializable {

  public BasicPolynomial(final Element ele, final GoalScope goalScope) {

    final List<Term> terms = new LinkedList<Term>();
    for(final Element termEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("term"))) {
      final Term term = new Term(termEle, goalScope);
      terms.add(term);
    }
    mTerms = Collections.unmodifiableList(terms);
    
    double constant = 0;
    for (final Element constantEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName("constant"))) {
      final double value = Double.valueOf(constantEle.getAttribute("value"));
      constant += value;
    }
    mConstant = constant;
  }

  private final List<Term> mTerms;

  public List<Term> getTerms() {
    return mTerms;
  }

  private final double mConstant;

  public double getConstant() {
    return mConstant;
  }

}
