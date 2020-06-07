/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.Color;
import java.awt.geom.Dimension2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.xmlgraphics.java2d.Dimension2DDouble;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;

/**
 * Utilities for working with Apache FOP.
 */
public final class FOPUtils {

  /**
   * Standard padding for most report tables.
   */
  public static final double TABLE_CELL_STANDARD_PADDING = 2;

  private FOPUtils() {
  }

  /**
   * Namespace for XSL-FO.
   */
  public static final String XSL_FO_NAMESPACE = "http://www.w3.org/1999/XSL/Format";

  /**
   * Prefix with colon.
   */
  public static final String XSL_FO_PREFIX = "fo:";

  /**
   * Static content tag.
   */
  public static final String STATIC_CONTENT_TAG = "static-content";

  /**
   * Create the root element for an XSL-FO document.
   * This ensures that the namespace is set properly.
   * 
   * @param document used to create the element
   * @return the root element
   */
  public static Element createRoot(final Document document) {
    final Element rootElement = createXslFoElement(document, "root");
    rootElement.setAttribute("xmlns:fo", XSL_FO_NAMESPACE);

    // get rid of warnings about not being able to find fonts
    rootElement.setAttribute("font-family", "Times");

    return rootElement;
  }

  /**
   * Helper to create elements with correct prefix.
   * 
   * @param document used to create the element
   * @param elementName the name of the element
   * @return the new element
   */
  public static Element createXslFoElement(final Document document,
                                           final String elementName) {
    final Element ele = document.createElementNS(XSL_FO_NAMESPACE, String.format("%s%s", XSL_FO_PREFIX, elementName));
    return ele;
  }

  /**
   * 8.5" x 11" page.
   */
  public static final Dimension2D PAGE_LETTER_SIZE = new Dimension2DDouble(8.5, 11);

  /**
   * 0.5" on each side.
   */
  public static final Margins STANDARD_MARGINS = new Margins(0.2, 0.2, 0.5, 0.5);

  /**
   * Footer with enough room to put the page number.
   */
  public static final double STANDARD_FOOTER_HEIGHT = 0.3;

  /**
   * @param document used to create the elements
   * @param name the name of the page master
   * @see #createSimplePageMaster(Document, Element, String, double, double,
   *      double, double, double, double, double, double)
   * @throws IllegalArgumentException see {#link
   *           {@link #createSimplePageMaster(Document, Element, String, double, double, double, double, double, double, double, double)}
   * @see #STANDARD_MARGINS
   * @see #PAGE_LETTER_SIZE
   * @see #STANDARD_FOOTER_HEIGHT
   * @return the page master element
   */
  public static Element createSimplePageMaster(final Document document,
                                               final String name)
      throws IllegalArgumentException {
    return createSimplePageMaster(document, name, PAGE_LETTER_SIZE, STANDARD_MARGINS, 0, STANDARD_FOOTER_HEIGHT);
  }

  /**
   * @param document used to create the elements
   * @param name the name of the page master
   * @param pageSize page size in inches
   * @param margins margins in inches
   * @param headerHeight height in inches
   * @param footerHeight height in inches
   * @return the page master element
   * @throws IllegalArgumentException if there is not enough room for the header
   *           or footer
   */
  public static Element createSimplePageMaster(final Document document,
                                               final String name,
                                               final Dimension2D pageSize,
                                               final Margins margins,
                                               final double headerHeight,
                                               final double footerHeight)
      throws IllegalArgumentException {
    final Element simplePageMaster = createXslFoElement(document, "simple-page-master");
    simplePageMaster.setAttribute("master-name", name);
    simplePageMaster.setAttribute("page-height", String.format("%fin", pageSize.getHeight()));
    simplePageMaster.setAttribute("page-width", String.format("%fin", pageSize.getWidth()));

    simplePageMaster.setAttribute("margin-top", String.format("%fin", margins.getTop()));
    simplePageMaster.setAttribute("margin-bottom", String.format("%fin", margins.getBottom()));
    simplePageMaster.setAttribute("margin-left", String.format("%fin", margins.getLeft()));
    simplePageMaster.setAttribute("margin-right", String.format("%fin", margins.getRight()));

    final Element body = createXslFoElement(document, "region-body");
    body.setAttribute("margin-top", String.format("%fin", headerHeight));
    body.setAttribute("margin-bottom", String.format("%fin", footerHeight));
    simplePageMaster.appendChild(body);

    final Element header = createXslFoElement(document, "region-before");
    header.setAttribute("extent", String.format("%sin", headerHeight));
    simplePageMaster.appendChild(header);

    final Element footer = createXslFoElement(document, "region-after");
    footer.setAttribute("extent", String.format("%sin", footerHeight));
    simplePageMaster.appendChild(footer);

    return simplePageMaster;
  }

  /**
   * Used for counting pages.
   */
  public static final String PAGE_SEQUENCE_NAME = "seq1";

  /**
   * Tag for a page sequence element.
   */
  public static final String PAGE_SEQUENCE_TAG = "page-sequence";

  /**
   * Attribute to use in a page sequence to reference a page master.
   */
  public static final String MASTER_REFERENCE_ATTR = "master-reference";

  /**
   * Creates a page sequence.
   * 
   * @param document used to create elements
   * @param pageMasterName the name of the page master from
   *          {@link #createSimplePageMaster(Document, Element, String, double, double, double, double, double, double, double, double)}.
   * @return the page sequence element to be added to the root element
   */
  public static Element createPageSequence(final Document document,
                                           final String pageMasterName) {
    final Element ele = createXslFoElement(document, PAGE_SEQUENCE_TAG);
    ele.setAttribute(MASTER_REFERENCE_ATTR, pageMasterName);
    return ele;
  }

  /**
   * Simple footer with page numbers. Font size is set to 10pt.
   * This needs the id {@link #PAGE_SEQUENCE_NAME} in the page sequence.
   * 
   * @param document used to create elements
   * @return the element to be added to the page sequence
   * @see #PAGE_SEQUENCE_NAME
   */
  public static Element createSimpleFooter(final Document document) {
    final Element staticContent = createXslFoElement(document, "static-content");
    staticContent.setAttribute("flow-name", "xsl-region-after");
    staticContent.setAttribute("font-size", "10pt");

    final Element block = createXslFoElement(document, "block");
    block.setAttribute("text-align", "end");
    block.appendChild(document.createTextNode("Page "));
    block.appendChild(createXslFoElement(document, "page-number"));
    block.appendChild(document.createTextNode(" of "));
    final Element lastPage = createXslFoElement(document, "page-number-citation-last");
    lastPage.setAttribute("ref-id", PAGE_SEQUENCE_NAME);
    block.appendChild(lastPage);

    staticContent.appendChild(block);

    return staticContent;
  }

  /**
   * @param document used to create elements
   * @return the body element to be added to the page sequence
   */
  public static Element createBody(final Document document) {
    final Element body = createXslFoElement(document, "flow");
    body.setAttribute("flow-name", "xsl-region-body");
    return body;
  }

  /**
   * Create a basic table that takes 100% of the width.
   * 
   * @param document used to create elements
   * @return the table element
   */
  public static Element createBasicTable(final Document document) {
    final Element table = createXslFoElement(document, TABLE_TAG);
    table.setAttribute("table-layout", "fixed");
    table.setAttribute("width", "100%");
    return table;
  }

  /**
   * Tag name for table columns.
   */
  public static final String TABLE_COLUMN_TAG = "table-column";

  /**
   * Tag name for table element.
   */
  public static final String TABLE_TAG = "table";

  /**
   * Count the number of columns in the table.
   * 
   * @param table the table element
   * @return the number of columns
   * @throws IllegalArgumentException if the element is not a table
   * @see #TABLE_TAG
   */
  public static int columnsInTable(final Element table) {
    if (!TABLE_TAG.equals(table.getLocalName())) {
      throw new IllegalArgumentException("Not a table: "
          + table.getTagName());
    }

    final int columnCount = table.getElementsByTagNameNS(XSL_FO_NAMESPACE, FOPUtils.TABLE_COLUMN_TAG).getLength();
    return columnCount;
  }

  /**
   * Define a table column.
   * 
   * @param document used to create elements
   * @param proportionalWidth the relative width
   * @return the column element
   */
  public static Element createTableColumn(final Document document,
                                          final int proportionalWidth) {
    final Element ele = createXslFoElement(document, TABLE_COLUMN_TAG);
    ele.setAttribute("column-width", String.format("proportional-column-width(%d)", proportionalWidth));
    return ele;
  }

  /**
   * Create the table header element with bold text.
   * 
   * @param document used to create elements
   * @return the table header element
   */
  public static Element createTableHeader(final Document document) {
    final Element ele = createXslFoElement(document, "table-header");
    ele.setAttribute("font-weight", "bold");
    return ele;
  }

  /**
   * Center text alignment.
   */
  public static final String TEXT_ALIGN_CENTER = "center";

  /**
   * Right text alignment.
   */
  public static final String TEXT_ALIGN_RIGHT = "right";

  /**
   * Left text alignment.
   */
  public static final String TEXT_ALIGN_LEFT = "left";

  /**
   * Block container tag.
   */
  public static final String BLOCK_CONTAINER_TAG = "block-container";

  /**
   * Block tag.
   */
  public static final String BLOCK_TAG = "block";

  /**
   * Create a basic table cell.
   * Borders are not set.
   * 
   * @param document used to create elements
   * @param text the text to put in the cell
   * @param textAlignment CSS text alignment attribute, may be null
   * @return the table cell
   */
  public static Element createTableCell(final Document document,
                                        final String textAlignment,
                                        final String text) {
    return createTableCell(document, textAlignment, text, 0, false);
  }

  /**
   * Rotation is set to 0.
   * 
   * @param document see
   *          {@link #createNoWrapTableCell(Document, String, String, int)
   * @param textAlignment see
   *          {@link #createNoWrapTableCell(Document, String, String, int)
   * @param text see {@link #createNoWrapTableCell(Document, String, String, int)
   * @return see {@link #createNoWrapTableCell(Document, String, String, int)
   */
  public static Element createNoWrapTableCell(final Document document,
                                              final String textAlignment,
                                              final String text) {
    return createNoWrapTableCell(document, textAlignment, text, 0);
  }

  private static final List<Integer> VALID_ROTATIONS = Collections.unmodifiableList(Arrays.asList(0, 90, 180, 270, -90,
                                                                                                  -180, -270));

  /**
   * Create a table cell that hides text that is too long for the cell.
   * Borders are not set.
   * 
   * @param document used to create elements
   * @param text the text to put in the cell
   * @param textAlignment CSS text alignment attribute, may be null
   * @param rotation rotation in degrees, must be one of 0, 90, 180, 270, -90,
   *          -180, -270
   * @return the table cell
   * @throws IllegalArgumentException if the rotation is invalid
   * @see #createTableCell(Document, String, String, int, boolean)
   */
  public static Element createNoWrapTableCell(final Document document,
                                              final String textAlignment,
                                              final String text,
                                              final int rotation)
      throws IllegalArgumentException {
    return createTableCell(document, textAlignment, text, rotation, true);
  }

  /**
   * @param document used to create elements
   * @param textAlign CSS text alignment attribute, may be null
   * @param text the text to put in the cell
   * @param rotation rotation in degrees, must be one of 0, 90, 180, 270, -90,
   *          -180, -270
   * @param noWrap true if the text should not be wrapped, overflow is hidden
   * @return the table cell
   * @throws IllegalArgumentException if the rotation is invalid
   */
  public static Element createTableCell(final Document document,
                                        final String textAlign,
                                        final String text,
                                        final int rotation,
                                        final boolean noWrap) {
    if (!VALID_ROTATIONS.contains(rotation)) {
      throw new IllegalArgumentException(String.format("%d is not a valid rotation. Valid rotations are %s", rotation,
                                                       VALID_ROTATIONS.stream().map(String::valueOf)
                                                                      .collect(Collectors.joining(","))));
    }

    final Element cell = createXslFoElement(document, "table-cell");

    final Element blockContainer = createXslFoElement(document, BLOCK_CONTAINER_TAG);
    if (noWrap) {
      blockContainer.setAttribute("overflow", "hidden");
      blockContainer.setAttribute("wrap-option", "no-wrap");
    }

    if (rotation != 0) {
      throw new UnsupportedOperationException("Rotations aren't properly supported. One needs to create an SVG and rotate the text there.");

      // width and height are relative to the orientation
      // when rotated by 90 degrees, the height is left to right
      // blockContainer.setAttribute("reference-orientation",
      // String.valueOf(rotation));
    }
    cell.appendChild(blockContainer);

    final Element block = createXslFoElement(document, BLOCK_TAG);
    blockContainer.appendChild(block);
    if (null != textAlign) {
      block.setAttribute("text-align", textAlign);
    }
    block.appendChild(document.createTextNode(text));

    return cell;
  }

  /**
   * Set a top solid, black border of the specified width.
   * 
   * @param element the element to set the border on
   * @param width width in points
   */
  public static void addTopBorder(final Element element,
                                  final double width) {
    addTopBorder(element, width, "black");
  }

  /**
   * Set a top solid border with the specified width and color.
   * 
   * @param element the element to set the border on
   * @param width width in points
   * @param color a valid color string
   */
  public static void addTopBorder(final Element element,
                                  final double width,
                                  final String color) {
    addBorder(element, width, color, "top");
  }

  /**
   * Set a bottom solid, black border of the specified width.
   * 
   * @param element the element to set the border on
   * @param width width in points
   */
  public static void addBottomBorder(final Element element,
                                     final double width) {
    addBottomBorder(element, width, "black");
  }

  /**
   * Set a top solid border with the specified width and color.
   * 
   * @param element the element to set the border on
   * @param width width in points
   * @param color a valid color string
   */
  public static void addBottomBorder(final Element element,
                                     final double width,
                                     final String color) {
    addBorder(element, width, color, "bottom");
  }

  /**
   * Set a left solid, black border of the specified width.
   * 
   * @param element the element to set the border on
   * @param width width in points
   */
  public static void addLeftBorder(final Element element,
                                   final double width) {
    addLeftBorder(element, width, "black");
  }

  /**
   * Set a left solid border with the specified width and color.
   * 
   * @param element the element to set the border on
   * @param width width in points
   * @param color a valid color string
   */
  public static void addLeftBorder(final Element element,
                                   final double width,
                                   final String color) {
    addBorder(element, width, color, "left");
  }

  /**
   * Set a right solid, black border of the specified width.
   * 
   * @param element the element to set the border on
   * @param width width in points
   */
  public static void addRightBorder(final Element element,
                                    final double width) {
    addRightBorder(element, width, "black");
  }

  /**
   * Set a right solid border with the specified width and color.
   * 
   * @param element the element to set the border on
   * @param width width in points
   * @param color a valid color string
   */
  public static void addRightBorder(final Element element,
                                    final double width,
                                    final String color) {
    addBorder(element, width, color, "right");
  }

  private static void addBorder(final Element element,
                                final double width,
                                final String color,
                                final String side) {
    element.setAttribute(String.format("border-%s", side), String.format("%fpt solid %s", width, color));
  }

  /**
   * Set all borders.
   * 
   * @param element element to add the borders to
   * @param topWidth {@link #addTopBorder(Element, double)}
   * @param bottomWidth {@link #addBottomBorder(Element, double)}
   * @param leftWidth {@link #addLeftBorder(Element, double)}
   * @param rightWidth {@link #addRightBorder(Element, double)}
   */
  public static void addBorders(final Element element,
                                final double topWidth,
                                final double bottomWidth,
                                final double leftWidth,
                                final double rightWidth) {
    addTopBorder(element, topWidth);
    addBottomBorder(element, bottomWidth);
    addLeftBorder(element, leftWidth);
    addRightBorder(element, rightWidth);
  }

  /**
   * Create a factory that looks in the current directory to resolve relative
   * URLs.
   * 
   * @return factory
   */
  public static FopFactory createSimpleFopFactory() {
    return FopFactory.newInstance(Paths.get(".").toUri());
  }

  /**
   * @param fopFactory the factory to use
   * @param xslfo the document to render
   * @param out where to render the document
   * @throws IOException if there is an error writing
   * @throws FOPException if there is an error configuring FOP
   * @throws TransformerException if there is an error in the input document
   */
  public static void renderPdf(final FopFactory fopFactory,
                               final Document xslfo,
                               final OutputStream out)
      throws IOException, FOPException, TransformerException {
    final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

    // Setup JAXP using identity transformer
    final TransformerFactory factory = TransformerFactory.newInstance();
    final Transformer transformer = factory.newTransformer(); // identity transformer

    // Setup input and output for XSLT transformation
    final Source src = new DOMSource(xslfo);

    // Resulting SAX events (the generated FO) must be piped through to FOP
    final Result res = new SAXResult(fop.getDefaultHandler());

    // Start XSLT transformation and FOP processing
    transformer.transform(src, res);
  }

  /**
   * @param document used to create elements
   * @return block containing a blank line
   */
  public static Element createBlankLine(final Document document) {
    final Element block = createXslFoElement(document, "block");
    final Element leader = createXslFoElement(document, "leader");
    block.appendChild(leader);
    return block;
  }

  /**
   * @param document used to create elements
   * @param thickness the thickness of the line in points
   * @return block containing the horizontal line
   */
  public static Element createHorizontalLine(final Document document,
                                             final int thickness) {
    final Element lineBlock = FOPUtils.createXslFoElement(document, "block");

    final Element line = FOPUtils.createXslFoElement(document, "leader");
    line.setAttribute("leader-pattern", "rule");
    line.setAttribute("leader-length", "100%");
    line.setAttribute("rule-style", "solid");
    line.setAttribute("rule-thickness", String.format("%dpt", thickness));
    lineBlock.appendChild(line);

    return lineBlock;
  }

  /**
   * Tag for leader.
   */
  public static final String LEADER_TAG = "leader";

  /**
   * Add space between elements in a block. This is done by adding a
   * {@link #LEADER_TAG} with spaces.
   *
   * @param document used to create elements
   * @return the space
   */
  public static Element createHorizontalSpace(final Document document) {
    final Element space = FOPUtils.createXslFoElement(document, FOPUtils.LEADER_TAG);
    space.setAttribute("leader-pattern", "space");
    return space;
  }

  /**
   * Convert {@code c} to a value that is meant to be used inside XSL-FO.
   * 
   * @param c color
   * @return string
   */
  public static String renderColor(final Color c) {
    return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
  }

  /**
   * Tag used to create a table row.
   */
  public static final String TABLE_ROW_TAG = "table-row";

  /**
   * Create a table row that cannot span pages.
   * 
   * @param document used to create elements
   * @return the row element
   */
  public static Element createTableRow(final Document document) {
    return createTableRow(document, false);
  }

  /**
   * Create a table row.
   * 
   * @param document used to create elements
   * @param allowPageBreak if true, allow the row to span pages
   * @return the row element
   */
  public static Element createTableRow(final Document document,
                                       final boolean allowPageBreak) {
    final Element row = FOPUtils.createXslFoElement(document, TABLE_ROW_TAG);
    if (!allowPageBreak) {
      row.setAttribute("keep-together.within-page", "always");
    }
    return row;
  }

  /**
   * Page margins.
   */
  public static final class Margins implements Serializable {

    /**
     * @param top see {@link #getTop()}
     * @param bottom see {@link #getBottom()}
     * @param left see {@link #getLeft()}
     * @param right see {@link #getRight()}
     */
    public Margins(final double top,
                   final double bottom,
                   final double left,
                   final double right) {
      this.top = top;
      this.bottom = bottom;
      this.left = left;
      this.right = right;
    }

    private final double top;

    /**
     * @return top margin
     */
    public double getTop() {
      return top;
    }

    private final double bottom;

    /**
     * @return bottom margin
     */
    public double getBottom() {
      return bottom;
    }

    private final double left;

    /**
     * @return left margin
     */
    public double getLeft() {
      return left;
    }

    private final double right;

    /**
     * @return right margin
     */
    public double getRight() {
      return right;
    }

    @Override
    public int hashCode() {
      return Objects.hash(top, bottom, left, right);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      } else if (null == o) {
        return false;
      } else if (o.getClass().equals(this.getClass())) {
        final Margins other = (Margins) o;
        return Utilities.doubleExactEquals(this.getTop(), other.getTop())
            && Utilities.doubleExactEquals(this.getBottom(), other.getBottom())
            && Utilities.doubleExactEquals(this.getLeft(), other.getLeft())
            && Utilities.doubleExactEquals(this.getRight(), other.getRight());
      } else {
        return false;
      }
    }

  }

  /**
   * Keep {@code ele} with the previous element on the same page when possible.
   * 
   * @param ele the element to keep with previous
   */
  public static void keepWithPrevious(final Element ele) {
    ele.setAttribute("keep-with-previous", "50");
  }

  /**
   * Force {@code ele} to be kept with the previous element on the same page.
   * 
   * @param ele the elemnt to keep with previous
   */
  public static void keepWithPreviousAlways(final Element ele) {
    ele.setAttribute("keep-with-previous", "always");
  }

  private static void addPadding(final Element element,
                                 final double width,
                                 final String side) {
    element.setAttribute(String.format("padding-%s", side), String.format("%fpt", width));
  }

  /**
   * Set a top padding with the specified width.
   * 
   * @param element the element to set the padding on
   * @param width width in points
   */
  public static void addTopPadding(final Element element,
                                   final double width) {
    addPadding(element, width, "top");
  }

  /**
   * Set a bottom padding with the specified width.
   * 
   * @param element the element to set the padding on
   * @param width width in points
   */
  public static void addBottomPadding(final Element element,
                                      final double width) {
    addPadding(element, width, "bottom");
  }

  /**
   * Set a left padding with the specified width.
   * 
   * @param element the element to set the padding on
   * @param width width in points
   */
  public static void addLeftPadding(final Element element,
                                    final double width) {
    addPadding(element, width, "left");
  }

  /**
   * Set a right padding with the specified width.
   * 
   * @param element the element to set the padding on
   * @param width width in points
   */
  public static void addRightPadding(final Element element,
                                     final double width) {
    addPadding(element, width, "right");
  }

  /**
   * Set all padding.
   * 
   * @param element element to add the borders to
   * @param topWidth {@link #addTopPadding(Element, double)}
   * @param bottomWidth {@link #addBottomPadding(Element, double)}
   * @param leftWidth {@link #addLeftPadding(Element, double)}
   * @param rightWidth {@link #addRightPadding(Element, double)}
   */
  public static void addPadding(final Element element,
                                final double topWidth,
                                final double bottomWidth,
                                final double leftWidth,
                                final double rightWidth) {
    addTopPadding(element, topWidth);
    addBottomPadding(element, bottomWidth);
    addLeftPadding(element, leftWidth);
    addRightPadding(element, rightWidth);
  }

}
