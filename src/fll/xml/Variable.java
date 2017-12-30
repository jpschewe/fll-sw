/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A variable is a polynomial with a value.
 */
public class Variable extends BasicPolynomial {

  public static final String TAG_NAME = "variable";

  public static final String NAME_ATTRIBUTE = "name";

  private final PropertyChangeSupport propChangeSupport;

  public Variable(final Element ele,
                  final GoalScope goalScope) {
    super(ele, goalScope);

    mName = ele.getAttribute("name");
    propChangeSupport = new PropertyChangeSupport(this);
  }

  private String mName;

  /**
   * @return the name of the variable
   */
  public String getName() {
    return mName;
  }

  /**
   * 
   * @param v the new name of the variable.
   * @see #getName()
   * Fires property change event.
   */
  public void setName(final String v) {
    final String old = mName;
    mName = v;
    this.propChangeSupport.firePropertyChange("name", old, v);
  }

  public Element toXml(final Document doc) {
    final Element ele = doc.createElement(TAG_NAME);
    populateXml(doc, ele);
    ele.setAttribute(NAME_ATTRIBUTE, mName);
    return ele;
  }

  /**
   * Add a listener for property change events.
   * 
   * @param listener the listener to add
   */
  public void addPropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    this.propChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove a property change listener.
   * 
   * @param listener the listener to remove
   */
  public void removePropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    this.propChangeSupport.removePropertyChangeListener(listener);
  }

}
