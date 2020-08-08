/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.util.FLLInternalException;
import fll.util.FP;
import fll.web.playoff.TeamScore;

/**
 * Base type for goals in the challenge description.
 */
public abstract class AbstractGoal implements Serializable {

  /**
   * XML attribute for category (aka. goal group).
   */
  public static final String CATEGORY_ATTRIBUTE = "category";

  /**
   * XML attribute for goal title.
   */
  public static final String TITLE_ATTRIBUTE = "title";

  /**
   * XML attribute for goal name.
   */
  public static final String NAME_ATTRIBUTE = "name";

  /**
   * XML tag for goal description.
   */
  public static final String DESCRIPTION_TAG_NAME = "description";

  private final PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);

  /**
   * Default constructor for creating a new goal.
   */
  public AbstractGoal() {
  }

  /**
   * Constructor for reading from an XML document.
   *
   * @param ele the XML element to parse
   */
  public AbstractGoal(@Nonnull final Element ele) {
    mName = ele.getAttribute(NAME_ATTRIBUTE);
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);

    final List<Element> descEles = ChallengeParser.getChildElementsByTagName(ele, DESCRIPTION_TAG_NAME);
    if (descEles.size() > 0) {
      final Element descEle = descEles.get(0);
      mDescription = descEle.getTextContent();
    } else {
      mDescription = null;
    }

    if (ele.hasAttribute(CATEGORY_ATTRIBUTE)) {
      mCategory = ele.getAttribute(CATEGORY_ATTRIBUTE);
    } else {
      mCategory = null;
    }
  }

  /**
   * @param name see {@link #getName()}
   */
  public AbstractGoal(final String name) {
    this();

    mName = name;
    mTitle = null;
    mDescription = null;
    mCategory = null;
  }

  private @Nullable String mCategory;

  /**
   * @return the category of the goal, may be null
   */
  public @Nullable String getCategory() {
    return mCategory;
  }

  /**
   * @param v see {@link #getCategory()}
   *          Fires property change event.
   */
  public void setCategory(final @Nullable String v) {
    final String old = mCategory;
    mCategory = v;
    this.propChangeSupport.firePropertyChange("category", old, v);
  }

  private String mName = "no_name";

  /**
   * @return the name of the goal
   */
  @Nonnull
  public String getName() {
    return mName;
  }

  /**
   * @param v see {@link #getName()}
   *          Fires property change event.
   */
  public void setName(@Nonnull final String v) {
    final String old = mName;
    mName = v;
    this.propChangeSupport.firePropertyChange("name", old, v);
  }

  private @Nullable String mTitle;

  /**
   * @return the title of the goal, may be null.
   */
  public @Nullable String getTitle() {
    return mTitle;
  }

  /**
   * @param v see {@link #getTitle()}
   *          Fires property change event.
   */
  public void setTitle(final @Nullable String v) {
    final String old = mTitle;
    mTitle = v;
    this.propChangeSupport.firePropertyChange("title", old, v);
  }

  private @Nullable String mDescription;

  /**
   * @return the description
   */
  public String getDescription() {
    return null == mDescription ? null : mDescription.trim().replaceAll("\\s+", " ");
  }

  /**
   * @param v see {@link #getDescription()}
   *          Fires property change event.
   */
  public void setDescription(final @Nullable String v) {
    final String old = mDescription;
    mDescription = v;
    this.propChangeSupport.firePropertyChange("description", old, v);
  }

  /**
   * Get the raw score.
   *
   * @param teamScore the score to evaluate
   * @return the score or NaN if there is currently no score for this goal
   */
  public abstract double getRawScore(TeamScore teamScore);

  /**
   * Get the computed score.
   *
   * @param teamScore the score to evaluate
   * @return the score or NaN if there is currently no score for this goal
   */
  public abstract double getComputedScore(TeamScore teamScore);

  /**
   * @return true if this is a computed goal
   */
  public abstract boolean isComputed();

  /***
   * @return true if this is an enumerated goal
   */
  public abstract boolean isEnumerated();

  /**
   * @return Read-only collection of the values.
   */
  public abstract Collection<EnumeratedValue> getValues();

  /**
   * Get the enumerated values from the goal and sort them for display.
   * This ensures that all usages have the elements in the same order.
   *
   * @return the enumerated values for this goal
   */
  public List<EnumeratedValue> getSortedValues() {
    final List<EnumeratedValue> values = new LinkedList<>(getValues());
    Collections.sort(values, EnumeratedValueLowestFirst.INSTANCE);
    return values;
  }

  /**
   * @return the score type
   */
  public abstract ScoreType getScoreType();

  /**
   * @return minimum raw value for the goal
   */
  public abstract double getMin();

  /**
   * @return maximum raw value for the goal
   */
  public abstract double getMax();

  /**
   * @return if this goal is a yes/no.
   */
  public boolean isYesNo() {
    if (isComputed()
        || isEnumerated()) {
      return false;
    } else {
      return FP.equals(0, getMin(), ChallengeParser.INITIAL_VALUE_TOLERANCE)
          && FP.equals(1, getMax(), ChallengeParser.INITIAL_VALUE_TOLERANCE);
    }
  }

  /**
   * @param score the raw score
   * @return the value after the score type has been applied
   */
  protected final double applyScoreType(final double score) {
    switch (getScoreType()) {
    case FLOAT:
      return score;
    case INTEGER:
      return ((long) score);
    default:
      throw new FLLInternalException("Unknown score type: "
          + getScoreType());
    }
  }

  /**
   * @param document the document used to create elements
   * @param ele the goal element to be populated with this object's state
   */
  protected void populateXml(final Document document,
                             final Element ele) {
    ele.setAttribute(NAME_ATTRIBUTE, getName());
    ele.setAttribute(TITLE_ATTRIBUTE, getTitle());

    if (!StringUtils.isEmpty(getDescription())) {
      final Element descriptionEle = document.createElement(DESCRIPTION_TAG_NAME);
      descriptionEle.appendChild(document.createTextNode(getDescription()));
      ele.appendChild(descriptionEle);
    }

    if (!StringUtils.isEmpty(getCategory())) {
      ele.setAttribute(CATEGORY_ATTRIBUTE, getCategory());
    }
  }

  private static final class EnumeratedValueLowestFirst implements Comparator<EnumeratedValue>, Serializable {
    public static final EnumeratedValueLowestFirst INSTANCE = new EnumeratedValueLowestFirst();

    private EnumeratedValueLowestFirst() {
    }

    @Override
    public int compare(final EnumeratedValue one,
                       final EnumeratedValue two) {
      return Double.compare(one.getScore(), two.getScore());
    }
  }

  /**
   * @param doc the document to create elements with
   * @return XML element that represents the current state
   */
  public abstract Element toXml(Document doc);

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
