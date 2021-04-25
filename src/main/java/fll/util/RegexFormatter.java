/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFormattedTextField;
import javax.swing.text.DefaultFormatter;

/**
 * Allows one to use regular expressions to specify the format of a
 * {@link JFormattedTextField}.
 * Based on code from http://www.oracle.com/technetwork/java/reftf-138955.html
 * and
 * http://www.java2s.com/Tutorial/Java/0240__Swing/RegexFormatterwithaJFormattedTextField.htm.
 */
public class RegexFormatter extends DefaultFormatter {
  private final Pattern pattern;

  /**
   * Creates a regular expression based AbstractFormatter.
   * {@code pattern} specifies the regular expression that will be used
   * to determine if a value is legal.
   * 
   * @param pattern {@link #getPattern()}
   */
  public RegexFormatter(final Pattern pattern) {
    this.pattern = pattern;
  }

  /**
   * @return the pattern used to determine if a value is valid.
   */
  public Pattern getPattern() {
    return pattern;
  }

  /**
   * Parses text returning an arbitrary Object. Some formatters
   * may return null.
   * If a Pattern has been specified and the text completely
   * matches the regular expression this will invoke setMatcher.
   * 
   * @throws ParseException
   *           if there is an error in the conversion
   * @param text
   *          String to convert
   * @return Object representation of text
   */
  public Object stringToValue(String text) throws ParseException {
    final Pattern pattern = getPattern();

    if (pattern != null) {
      final Matcher matcher = pattern.matcher(text);

      if (matcher.matches()) {
        return super.stringToValue(text);
      }
      throw new ParseException("Pattern did not match", 0);
    }
    return text;
  }
}