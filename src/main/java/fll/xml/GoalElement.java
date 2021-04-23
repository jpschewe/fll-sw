/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.List;

import javax.annotation.Nonnull;

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Common data between {@link AbstractGoal} and {@link GoalGroup}.
 */
public abstract class GoalElement implements Serializable, Evaluatable {

  /**
   * XML attribute for goal title.
   */
  public static final String TITLE_ATTRIBUTE = "title";

  /**
   * XML tag for goal description.
   */
  public static final String DESCRIPTION_TAG_NAME = "description";

  private final @NotOnlyInitialized PropertyChangeSupport propChangeSupport;

  /**
   * Default constructor for a new object.
   */
  public GoalElement() {
    mTitle = "";
    mDescription = "";
    propChangeSupport = new PropertyChangeSupport(this);
  }

  /**
   * Constructor for reading from an XML document.
   *
   * @param ele the XML element to parse
   */
  public GoalElement(@Nonnull final Element ele) {
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);

    final List<Element> descEles = ChallengeParser.getChildElementsByTagName(ele, DESCRIPTION_TAG_NAME);
    if (descEles.size() > 0) {
      final Element descEle = descEles.get(0);
      mDescription = ChallengeDescription.removeExtraWhitespace(descEle.getTextContent());
    } else {
      mDescription = "";
    }
    propChangeSupport = new PropertyChangeSupport(this);
  }

  private String mTitle;

  /**
   * @return the title of the goal.
   */
  public String getTitle() {
    return mTitle;
  }

  /**
   * Fires property change event.
   * 
   * @param v see {@link #getTitle()}
   */
  public void setTitle(final String v) {
    final String old = mTitle;
    mTitle = v;
    firePropertyChange("title", old, v);
  }

  private String mDescription;

  /**
   * @return the description, empty string if unset
   */
  public String getDescription() {
    return mDescription;
  }

  /**
   * @param v see {@link #getDescription()}
   *          Fires property change event.
   */
  public void setDescription(final @Nullable String v) {
    final String old = mDescription;
    mDescription = ChallengeDescription.removeExtraWhitespace(v);
    firePropertyChange("description", old, v);
  }

  /**
   * @param property the property that changed
   * @param oldValue the old value
   * @param newValue the new value
   */
  protected final void firePropertyChange(final String property,
                                          final @Nullable Object oldValue,
                                          final @Nullable Object newValue) {
    propChangeSupport.firePropertyChange(property, oldValue, newValue);
  }

  /**
   * Add a listener for property change events.
   *
   * @param listener the listener to add
   */
  public final void addPropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    this.propChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove a property change listener.
   *
   * @param listener the listener to remove
   */
  public final void removePropertyChangeListener(@Nonnull final PropertyChangeListener listener) {
    this.propChangeSupport.removePropertyChangeListener(listener);
  }

  /**
   * Subclasses need to override this method to create the appropriate element and
   * then call {@link #populateXml(Document, Element)} to get data from this
   * class.
   * 
   * @param doc the document to create elements with
   * @return XML element that represents the current state
   */
  public abstract Element toXml(Document doc);

  /**
   * @param document the document used to create elements
   * @param ele the goal element to be populated with this object's state
   */
  protected void populateXml(final Document document,
                             final Element ele) {
    ele.setAttribute(TITLE_ATTRIBUTE, getTitle());

    final String description = getDescription();
    if (!StringUtils.isBlank(description)) {
      final Element descriptionEle = document.createElement(DESCRIPTION_TAG_NAME);
      descriptionEle.appendChild(document.createTextNode(description));
      ele.appendChild(descriptionEle);
    }
  }

  /**
   * @return true if this is a goal and can safely be cast to {@link AbstractGoal}
   */
  public abstract boolean isGoal();

  /**
   * @return true if this is a goal group and can safely be cast to
   *         {@link GoalGroup}
   */
  public abstract boolean isGoalGroup();

}
