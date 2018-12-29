/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.text.DecimalFormat;

import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * Some utilties for working with {@link JFormattedTextField}
 */
public final class FormatterUtils {

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
   * @see #createDoubleField(double, double)
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
    def.setValueClass(Integer.class);
    final DecimalFormat format = new DecimalFormat();
    format.setGroupingUsed(true);
    format.setDecimalSeparatorAlwaysShown(true);
    format.setMinimumFractionDigits(2);
    final NumberFormatter disp = new NumberFormatter(format);
    disp.setValueClass(Integer.class);
    final NumberFormatter ed = new NumberFormatter(format);
    ed.setValueClass(Integer.class);
    final DefaultFormatterFactory factory = new DefaultFormatterFactory(def, disp, ed);

    final JFormattedTextField field = new JFormattedTextField(factory);
    field.setValue(Double.valueOf(min));
    field.setInputVerifier(new DoubleVerifier(min, max));
    return field;
  }

}
