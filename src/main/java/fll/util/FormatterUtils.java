/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * Some utilties for working with {@link JFormattedTextField}.
 */
public final class FormatterUtils {

  static final Color INVALID_COLOR = Color.red;

  static final Color VALID_COLOR = Color.black;

  private FormatterUtils() {
  }

  /**
   * Create a {@link JFormattedTextField} for editing integers.
   * 
   * @param min minimum value
   * @param max maximum value
   * @return text field for editing integers
   */
  public static JFormattedTextField createIntegerField(final int min,
                                                       final int max) {

    final NumberFormatter def = new NumberFormatter();
    def.setValueClass(Integer.class);
    final NumberFormatter disp = new NumberFormatter(new DecimalFormat("#,###,##0"));
    disp.setValueClass(Integer.class);
    final NumberFormatter ed = new NumberFormatter(new DecimalFormat("#,###,##0"));
    ed.setValueClass(Integer.class);
    final DefaultFormatterFactory factory = new DefaultFormatterFactory(def, disp, ed);

    final JFormattedTextField field = new JFormattedTextField(factory);
    field.setValue(Integer.valueOf(min));
    field.setInputVerifier(new IntegerVerifier(min, max));
    return field;
  }

  /**
   * No limits on the range.
   * 
   * @return {@link #createDoubleField(double, double)}
   */
  public static JFormattedTextField createDoubleField() {
    return createDoubleField(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  /**
   * Create a {@link JFormattedTextField} for editing doubles.
   * 
   * @param min minimum value
   * @param max maximum value
   * @return text field for editing doubles
   */
  public static JFormattedTextField createDoubleField(final double min,
                                                      final double max) {

    final NumberFormatter def = new NumberFormatter();
    def.setValueClass(Double.class);
    final DecimalFormat format = new DecimalFormat();
    format.setGroupingUsed(true);
    format.setDecimalSeparatorAlwaysShown(true);
    format.setMinimumFractionDigits(2);
    final NumberFormatter disp = new NumberFormatter(format);
    disp.setValueClass(Double.class);
    final NumberFormatter ed = new NumberFormatter(format);
    ed.setValueClass(Double.class);
    final DefaultFormatterFactory factory = new DefaultFormatterFactory(def, disp, ed);

    final JFormattedTextField field = new JFormattedTextField(factory);
    field.setValue(Double.valueOf(min));
    field.setInputVerifier(new DoubleVerifier(min, max));

    final int numColumns;
    if (Double.isFinite(max)) {
      numColumns = format.format(max).length()
          + 2;
    } else {
      // just pick a reasonable number
      numColumns = 10;
    }

    field.setColumns(numColumns);
    field.setMaximumSize(field.getPreferredSize());

    return field;
  }

  /**
   * Create a {@link JFormattedTextField} for editing strings.
   * This allows editing string fields by inserting text rather than overwrite.
   * The default value is the empty string.
   * 
   * @return text field for editing strings
   */
  public static JFormattedTextField createStringField() {
    final DefaultFormatter format = new DefaultFormatter();
    format.setOverwriteMode(false);
    format.setValueClass(String.class);

    final JFormattedTextField field = new JFormattedTextField(format);
    field.setValue("");
    return field;
  }

  /**
   * Create a {@link JFormattedTextField} for editing database names.
   * The default value is the empty string.
   * 
   * @return text field for editing database names
   */
  public static JFormattedTextField createDatabaseNameField() {
    return createFieldForPattern(DatabaseNameCellEditor.DATABASE_NAME_PATTERN);
  }

  /**
   * Create a field that is formatted with {@link RegexFormatter}.
   * 
   * @param pattern {@link RegexFormatter#getPattern()}
   * @return text field for editing strings matching the specified {@code pattern}
   */
  public static JFormattedTextField createFieldForPattern(final Pattern pattern) {
    final RegexFormatter format = new RegexFormatter(pattern);
    format.setOverwriteMode(false);
    format.setValueClass(String.class);

    final JFormattedTextField field = new JFormattedTextField(format);
    field.setValue("");
    return field;
  }

}
