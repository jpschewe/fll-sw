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
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.db.CategoriesIgnored;
import fll.web.report.awards.AwardCategory;
import fll.web.report.awards.ChampionshipCategory;
import fll.web.report.awards.HeadToHeadCategory;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * The description of a tournament.
 */
public class ChallengeDescription implements Serializable {

  /**
   * XML attribute for {@link #getTitle()}.
   */
  public static final String TITLE_ATTRIBUTE = "title";

  /**
   * XML attribute for {@link #getRevision()}.
   */
  public static final String REVISION_ATTRIBUTE = "revision";

  /**
   * XML attribute for {@link #getRevisionComment()}.
   */
  public static final String REVISION_COMMENT_ATTRIBUTE = "revisionComment";

  /**
   * XML attribute for {@link #getCopyright()}.
   */
  public static final String COPYRIGHT_ATTRIBUTE = "copyright";

  /**
   * XML attribute for the schema version.
   */
  public static final String SCHEMA_VERSION_ATTRIBUTE = "schemaVersion";

  /**
   * It's assumed that this element is the root element from the Document from
   * {@link ChallengeParser#parse(java.io.Reader)}.
   *
   * @param ele the challenge description
   */
  public ChallengeDescription(final Element ele) {
    mTitle = ele.getAttribute(TITLE_ATTRIBUTE);
    mRevision = ele.getAttribute(REVISION_ATTRIBUTE);
    mRevisionComment = ele.getAttribute(REVISION_COMMENT_ATTRIBUTE);
    mWinner = ChallengeParser.getWinnerCriteria(ele);

    if (ele.hasAttribute(COPYRIGHT_ATTRIBUTE)) {
      mCopyright = ele.getAttribute(COPYRIGHT_ATTRIBUTE);
    } else {
      mCopyright = null;
    }

    final Element performanceElement = (Element) ele.getElementsByTagName(PerformanceScoreCategory.TAG_NAME).item(0);
    if (null == performanceElement) {
      throw new IllegalArgumentException("Cannot find performance score category element");
    }

    mPerformance = new PerformanceScoreCategory(performanceElement);

    for (final Element subjEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(SubjectiveScoreCategory.TAG_NAME))) {
      final SubjectiveScoreCategory subj = new SubjectiveScoreCategory(subjEle);
      mSubjectiveCategories.add(subj);
    }

    final List<SubjectiveScoreCategory> subjectiveCategoriesUnmodifiable = Collections.unmodifiableList(mSubjectiveCategories);
    for (final Element subjEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(VirtualSubjectiveScoreCategory.TAG_NAME))) {
      final VirtualSubjectiveScoreCategory subj = new VirtualSubjectiveScoreCategory(subjEle,
                                                                                     subjectiveCategoriesUnmodifiable);
      virtualSubjectiveCategories.add(subj);
    }

    for (final Element catEle : new NodelistElementCollectionAdapter(ele.getElementsByTagName(NonNumericCategory.TAG_NAME))) {
      final NonNumericCategory cat = new NonNumericCategory(catEle);
      nonNumericCategories.add(cat);
    }

  }

  /**
   * Default constructor uses the empty string for {@link #getRevision()},
   * {@link WinnerType#HIGH} for {@link #getWinner()}, null for
   * {@link #getCopyright()}, an empty {@link #getPerformance()} and no
   * {@link #getSubjectiveCategories()}.
   *
   * @param title the title of the challenge
   */
  public ChallengeDescription(final String title) {
    mTitle = title;
    mRevision = "";
    mRevisionComment = "";
    mWinner = WinnerType.HIGH;
    mCopyright = null;
    mPerformance = new PerformanceScoreCategory();
  }

  private @Nullable String mCopyright;

  /**
   * Copyright statement for the challenge.
   *
   * @return the statement or null if there is no copyright
   */
  public @Nullable String getCopyright() {
    return mCopyright;
  }

  /**
   * @param v see {@link #getCopyright()}
   */
  public void setCopyright(final @Nullable String v) {
    mCopyright = v;
  }

  private String mTitle;

  /**
   * The name of the challenge description, it cannot be null, but may be the
   * empty string, although this is not advised.
   *
   * @return the title of the tournament
   */
  public String getTitle() {
    return mTitle;
  }

  /**
   * @param v see {@link #getTitle()}
   */
  public void setTitle(final String v) {
    Objects.requireNonNull(v);
    mTitle = v;
  }

  private String mRevision;

  /**
   * @param v see {@link #setRevision(String)}
   */
  public void setRevision(final String v) {
    Objects.requireNonNull(v);
    mRevision = v;
  }

  /**
   * This is used to keep track of changes, it can be an empty string, but not
   * null.
   *
   * @return the revision of the description
   */
  public String getRevision() {
    return mRevision;
  }

  private String mRevisionComment;

  /**
   * @param v see {@link #setRevisionComment(String)}
   */
  public void setRevisionComment(final String v) {
    Objects.requireNonNull(v);
    mRevisionComment = v;
  }

  /**
   * Note for the {@link #getRevision()}, it can be an empty string, but not
   * null.
   *
   * @return the revision comment of the description
   */
  public String getRevisionComment() {
    return mRevisionComment;
  }

  private WinnerType mWinner;

  /**
   * @return how the winner is defined
   */
  public WinnerType getWinner() {
    return mWinner;
  }

  /**
   * @param v {@link #getWinner()}
   */
  public void setWinner(final WinnerType v) {
    mWinner = v;
  }

  private PerformanceScoreCategory mPerformance;

  /**
   * @return description of the performance category
   */
  public PerformanceScoreCategory getPerformance() {
    return mPerformance;
  }

  /**
   * Since {@link PerformanceScoreCategory} is mutable, this should not be
   * needed except to add or remove the performance element.
   *
   * @param v see {@link #getPerformance()}
   */
  public void setPerformance(final PerformanceScoreCategory v) {
    mPerformance = v;
  }

  private final List<SubjectiveScoreCategory> mSubjectiveCategories = new LinkedList<>();

  /**
   * @return unmodifiable list
   */
  public List<SubjectiveScoreCategory> getSubjectiveCategories() {
    return mSubjectiveCategories;
  }

  /**
   * Add a subjective category to the end of the list.
   * Since {@link ScoreCategory} is mutable, this does not check that the names
   * are unique. That needs to be done by a higher level class.
   *
   * @param v the category to add
   */
  public void addSubjectiveCategory(final SubjectiveScoreCategory v) {
    mSubjectiveCategories.add(v);
  }

  /**
   * Add a subjective category at the specified index.
   * Since {@link ScoreCategory} is mutable, this does not check that the names
   * are unique. That needs to be done by a higher level class.
   *
   * @param v the category to add
   * @param index the index to add the category at
   * @throws IndexOutOfBoundsException if {@code index} is outside the range of
   *           subjective categories
   */
  public void addSubjectiveCategory(final int index,
                                    final SubjectiveScoreCategory v)
      throws IndexOutOfBoundsException {
    mSubjectiveCategories.add(index, v);
  }

  /**
   * Remove a subjective category.
   *
   * @param v the category to remove
   * @return if the category was removed
   * @see List#remove(Object)
   */
  public boolean removeSubjectiveCategory(final SubjectiveScoreCategory v) {
    return mSubjectiveCategories.remove(v);
  }

  /**
   * Remove the subjective category at the specified index.
   *
   * @param index the index to remove the element from
   * @return the category that was removed
   * @throws IndexOutOfBoundsException if {@code index} is outside the range of
   *           subjective categories
   */
  public SubjectiveScoreCategory removeSubjectiveCategory(final int index) throws IndexOutOfBoundsException {
    return mSubjectiveCategories.remove(index);
  }

  /**
   * @param name category name
   * @return the category or null if not found
   */
  public @Nullable SubjectiveScoreCategory getSubjectiveCategoryByName(final String name) {
    return mSubjectiveCategories.stream().filter(c -> c.getName().equals(name)).findAny().orElse(null);
  }

  /**
   * @param title category title
   * @return the category or null if not found
   */
  public @Nullable SubjectiveScoreCategory getSubjectiveCategoryByTitle(final String title) {
    return mSubjectiveCategories.stream().filter(c -> c.getTitle().equals(title)).findAny().orElse(null);
  }

  private final List<VirtualSubjectiveScoreCategory> virtualSubjectiveCategories = new LinkedList<>();

  /**
   * @return unmodifiable list
   */
  public List<VirtualSubjectiveScoreCategory> getVirtualSubjectiveCategories() {
    return virtualSubjectiveCategories;
  }

  /**
   * Add a virtual subjective category to the end of the list.
   * Since {@link VirtualSubjectiveScoreCategory} is mutable, this does not check
   * that the names
   * are unique. That needs to be done by a higher level class.
   *
   * @param v the category to add
   */
  public void addVirtualSubjectiveCategory(final VirtualSubjectiveScoreCategory v) {
    virtualSubjectiveCategories.add(v);
  }

  /**
   * Add a virtual subjective category at the specified index.
   * Since {@link VirtualSubjectiveScoreCategory} is mutable, this does not check
   * that the names
   * are unique. That needs to be done by a higher level class.
   *
   * @param v the category to add
   * @param index the index to add the category at
   * @throws IndexOutOfBoundsException if {@code index} is outside the range of
   *           subjective categories
   */
  public void addVirtualSubjectiveCategory(final int index,
                                           final VirtualSubjectiveScoreCategory v)
      throws IndexOutOfBoundsException {
    virtualSubjectiveCategories.add(index, v);
  }

  /**
   * Remove a virtual subjective category.
   *
   * @param v the category to remove
   * @return if the category was removed
   * @see List#remove(Object)
   */
  public boolean removeVirtualSubjectiveCategory(final VirtualSubjectiveScoreCategory v) {
    return virtualSubjectiveCategories.remove(v);
  }

  /**
   * Remove the virtual subjective category at the specified index.
   *
   * @param index the index to remove the element from
   * @return the category that was removed
   * @throws IndexOutOfBoundsException if {@code index} is outside the range of
   *           subjective categories
   */
  public VirtualSubjectiveScoreCategory removeVirtualSubjectiveCategory(final int index)
      throws IndexOutOfBoundsException {
    return virtualSubjectiveCategories.remove(index);
  }

  /**
   * @param name category name
   * @return the category or null if not found
   */
  public @Nullable VirtualSubjectiveScoreCategory getVirtualSubjectiveCategoryByName(final String name) {
    return virtualSubjectiveCategories.stream().filter(c -> c.getName().equals(name)).findAny().orElse(null);
  }

  /**
   * @param title category title
   * @return the category or null if not found
   */
  public @Nullable VirtualSubjectiveScoreCategory getVirtualSubjectiveCategoryByTitle(final String title) {
    return virtualSubjectiveCategories.stream().filter(c -> c.getTitle().equals(title)).findAny().orElse(null);
  }

  private final LinkedList<NonNumericCategory> nonNumericCategories = new LinkedList<>();

  /**
   * @return unmodifiable list
   * @see CategoriesIgnored#getNonNumericCategories(ChallengeDescription,
   *      java.sql.Connection, fll.Tournament)
   */
  public List<NonNumericCategory> getNonNumericCategories() {
    return nonNumericCategories;
  }

  /**
   * @param title category title
   * @return the category or null if not found
   */
  public @Nullable NonNumericCategory getNonNumericCategoryByTitle(final String title) {
    return nonNumericCategories.stream().filter(c -> c.getTitle().equals(title)).findAny().orElse(null);
  }

  /**
   * Add a subjective category to the end of the list.
   * Since {@link ScoreCategory} is mutable, this does not check that the names
   * are unique. That needs to be done by a higher level class.
   *
   * @param v the category to add
   */
  public void addNonNumericCategory(final NonNumericCategory v) {
    nonNumericCategories.add(v);
  }

  /**
   * Add a non-numeric category at the specified index.
   * Since {@link NonNumericCategory} is mutable, this does not check that the
   * titles
   * are unique. That needs to be done by a higher level class.
   *
   * @param v the category to add
   * @param index the index to add the category at
   * @throws IndexOutOfBoundsException if {@code index} is outside the range of
   *           non-numeric categories
   */
  public void addNonNumericCategory(final int index,
                                    final NonNumericCategory v)
      throws IndexOutOfBoundsException {
    nonNumericCategories.add(index, v);
  }

  /**
   * Remove a non-numeric category.
   *
   * @param v the category to remove
   * @return if the category was removed
   * @see List#remove(Object)
   */
  public boolean removeNonNumericCategory(final NonNumericCategory v) {
    return nonNumericCategories.remove(v);
  }

  /**
   * Remove the non-numeric category at the specified index.
   *
   * @param index the index to remove the element from
   * @return the category that was removed
   * @throws IndexOutOfBoundsException if {@code index} is outside the range of
   *           non-numeric categories
   */
  public NonNumericCategory removeNonNumericCategory(final int index) throws IndexOutOfBoundsException {
    return nonNumericCategories.remove(index);
  }

  /**
   * Create the XML for the current state of this challenge description.
   *
   * @return a non-null document.
   */
  public Document toXml() {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();
    final Element fll = document.createElementNS(null, "fll");
    document.appendChild(fll);

    fll.setAttribute(TITLE_ATTRIBUTE, mTitle);
    fll.setAttribute(REVISION_ATTRIBUTE, mRevision);
    fll.setAttribute(REVISION_COMMENT_ATTRIBUTE, mRevisionComment);
    fll.setAttribute(ChallengeParser.WINNER_ATTRIBUTE, mWinner.toString());
    fll.setAttribute(SCHEMA_VERSION_ATTRIBUTE, String.valueOf(ChallengeParser.CURRENT_SCHEMA_VERSION));

    if (null != mCopyright) {
      fll.setAttribute(COPYRIGHT_ATTRIBUTE, mCopyright);
    }

    if (null != mPerformance) {
      final Element performanceElement = mPerformance.toXml(document);
      fll.appendChild(performanceElement);
    }

    for (final SubjectiveScoreCategory cat : mSubjectiveCategories) {
      final Element subjEle = cat.toXml(document);
      fll.appendChild(subjEle);
    }

    for (final VirtualSubjectiveScoreCategory cat : virtualSubjectiveCategories) {
      final Element subjEle = cat.toXml(document);
      fll.appendChild(subjEle);
    }

    for (final NonNumericCategory cat : nonNumericCategories) {
      final Element catEle = cat.toXml(document);
      fll.appendChild(catEle);
    }

    return document;
  }

  /**
   * @param str remove carriage returns and multiple spaces, may be null
   * @return string without the line endings and multiple spaces in a row, will be
   *         the empty string if {@code str} is null
   */
  /* package */ static String removeExtraWhitespace(final @Nullable String str) {
    if (null == str) {
      return "";
    } else {
      return str.replaceAll("\\s+", " ");
    }
  }

  /**
   * @return the highest maximum score all categories
   * @see ScoreCategory#getMaximumScore()
   */
  public double getMaximumScore() {
    return Math.max(getPerformance().getMaximumScore(),
                    getSubjectiveCategories().stream().mapToDouble(ScoreCategory::getMaximumScore).max().orElse(0D));
  }

  /**
   * @param title the title of the category to find
   * @return the award category
   * @throws IllegalArgumentException if a category cannot be found with the
   *           specified title
   */
  public AwardCategory getCategoryByTitle(final String title) {
    @Nullable
    AwardCategory category = getVirtualSubjectiveCategoryByTitle(title);
    if (null != category) {
      return category;
    }
    category = getSubjectiveCategoryByTitle(title);
    if (null != category) {
      return category;
    }
    category = getNonNumericCategoryByTitle(title);
    if (null != category) {
      return category;
    }
    if (title.equals(PerformanceScoreCategory.CATEGORY_TITLE)) {
      return getPerformance();
    }
    if (title.equals(ChampionshipCategory.CHAMPIONSHIP_AWARD_TITLE)) {
      return ChampionshipCategory.INSTANCE;
    }
    if (title.equals(HeadToHeadCategory.AWARD_TITLE)) {
      return HeadToHeadCategory.INSTANCE;
    }

    throw new IllegalArgumentException(String.format("No category can be found with the title '%s'", title));
  }
}
