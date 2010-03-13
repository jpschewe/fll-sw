/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/**
 * <p>A Properties collection which preserves comments and whitespace
 * present in the input stream from which it was loaded.</p>
 * <p>The class defers the usual work of the <a href="http://java.sun.com/j2se/1.3/docs/api/java/util/Properties.html">java.util.Properties</a>
 * class to there, but it also keeps track of the contents of the
 * input stream from which it was loaded (if applicable), so that it can
 * write out the properties in as close a form as possible to the input.</p>
 * If no changes occur to property values, the output should be the same
 * as the input, except for the leading date stamp, as normal for a
 * properties file. Properties added are appended to the file. Properties
 * whose values are changed are changed in place. Properties that are
 * removed are excised. If the <code>removeComments</code> flag is set,
 * then the comments immediately preceding the property are also removed.</p>
 * <p>If a second set of properties is loaded into an existing set, the
 * lines of the second set are added to the end. Note however, that if a
 * property already stored is present in a stream subsequently loaded, then
 * that property is removed before the new value is set. For example,
 * consider the file</p>
 * <pre> # the first line
 * alpha=one
 *
 * # the second line
 * beta=two</pre>
 * <p>This file is loaded, and then the following is also loaded into the
 * same <code>LayoutPreservingProperties</code> object</p>
 * <pre> # association
 * beta=band
 *
 * # and finally
 * gamma=rays</pre>
 * </p>The resulting collection sequence of logical lines depends on whether
 * or not <code>removeComments</code> was set at the time the second stream
 * is loaded. If it is set, then the resulting list of lines is</p>
 * <pre> # the first line
 * alpha=one
 *
 * # association
 * beta=band
 *
 * # and finally
 * gamma=rays</pre>
 * <p>If the flag is not set, then the comment "the second line" is retained,
 * although the key-value pair <code>beta=two</code> is removed.</p>
 */
public class LayoutPreservingProperties extends Properties {
    private static final String LS = System.getProperty("line.separator");

    /**
     * Logical lines have escaping and line continuation taken care
     * of. Comments and blank lines are logical lines; they are not
     * removed.
     */
    private ArrayList logicalLines = new ArrayList();

    /**
     * Position in the <code>logicalLines</code> list, keyed by property name.
     */
    private HashMap keyedPairLines = new HashMap();

    /**
     * Flag to indicate that, when we remove a property from the file, we
     * also want to remove the comments that precede it.
     */
    private boolean removeComments;

    /**
     * Create a new, empty, Properties collection, with no defaults.
     */
    public LayoutPreservingProperties() {
        super();
    }

    /**
     * Create a new, empty, Properties collection, with the specified defaults.
     * @param defaults the default property values
     */
    public LayoutPreservingProperties(Properties defaults) {
        super(defaults);
    }

    /**
     * Returns <code>true</code> if comments are removed along with
     * properties, or <code>false</code> otherwise. If
     * <code>true</code>, then when a property is removed, the comment
     * preceding it in the original file is removed also.
     * @return <code>true</code> if leading comments are removed when
     * a property is removed; <code>false</code> otherwise
     */
    public boolean isRemoveComments() {
        return removeComments;
    }

    /**
     * Sets the behaviour for comments accompanying properties that
     * are being removed. If <code>true</code>, then when a property
     * is removed, the comment preceding it in the original file is
     * removed also.
     * @param val <code>true</code> if leading comments are to be
     * removed when a property is removed; <code>false</code>
     * otherwise
     */
    public void setRemoveComments(boolean val) {
        removeComments = val;
    }

    public void load(InputStream inStream) throws IOException {
        String s = readLines(inStream);
        byte[] ba = s.getBytes("ISO-8859-1");
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        super.load(bais);
    }

    public Object put(Object key, Object value) throws NullPointerException {
        Object obj = super.put(key, value);
        // the above call will have failed if key or value are null
        innerSetProperty(key.toString(), value.toString());
        return obj;
    }

    public Object setProperty(String key, String value)
        throws NullPointerException {
        Object obj = super.setProperty(key, value);
        // the above call will have failed if key or value are null
        innerSetProperty(key, value);
        return obj;
    }

    /**
     * Store a new key-value pair, or add a new one. The normal
     * functionality is taken care of by the superclass in the call to
     * {@link #setProperty}; this method takes care of this classes
     * extensions.
     * @param key the key of the property to be stored
     * @param value the value to be stored
     */
    private void innerSetProperty(String key, String value) {
        value = escapeValue(value);

        if (keyedPairLines.containsKey(key)) {
            Integer i = (Integer) keyedPairLines.get(key);
            Pair p = (Pair) logicalLines.get(i.intValue());
            p.setValue(value);
        } else {
            key = escapeName(key);
            Pair p = new Pair(key, value);
            p.setNew(true);
            keyedPairLines.put(key, new Integer(logicalLines.size()));
            logicalLines.add(p);
        }
    }

    public void clear() {
        super.clear();
        keyedPairLines.clear();
        logicalLines.clear();
    }

    public Object remove(Object key) {
        Object obj = super.remove(key);
        Integer i = (Integer) keyedPairLines.remove(key);
        if (null != i) {
            if (removeComments) {
                removeCommentsEndingAt(i.intValue());
            }
            logicalLines.set(i.intValue(), null);
        }
        return obj;
    }

    public Object clone() {
        LayoutPreservingProperties dolly =
            (LayoutPreservingProperties) super.clone();
        dolly.keyedPairLines = (HashMap) this.keyedPairLines.clone();
        dolly.logicalLines = (ArrayList) this.logicalLines.clone();
        for (int j = 0; j < dolly.logicalLines.size(); j++) {
            LogicalLine line = (LogicalLine) dolly.logicalLines.get(j);
            if (line instanceof Pair) {
                Pair p = (Pair) line;
                dolly.logicalLines.set(j, p.clone());
            }
            // no reason to clone other lines are they are immutable
        }
        return dolly;
    }

    /**
     * Echo the lines of the properties (including blanks and comments) to the
     * stream.
     * @param out the stream to write to
     */
    public void listLines(PrintStream out) {
        out.println("-- logical lines --");
        Iterator i = logicalLines.iterator();
        while (i.hasNext()) {
            LogicalLine line = (LogicalLine) i.next();
            if (line instanceof Blank) {
                out.println("blank:   \"" + line + "\"");
            }
            else if (line instanceof Comment) {
                out.println("comment: \"" + line + "\"");
            }
            else if (line instanceof Pair) {
                out.println("pair:    \"" + line + "\"");
            }
        }
    }

    /**
     * Save the properties to a file.
     * @param dest the file to write to
     */
    public void saveAs(File dest) throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        store(fos, null);
        fos.close();
    }

    public void store(OutputStream out, String header) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(out, "ISO-8859-1");

        int skipLines = 0;
        int totalLines = logicalLines.size();

        if (header != null) {
            osw.write("#" + header + LS);
            if (totalLines > 0
                && logicalLines.get(0) instanceof Comment
                && header.equals(logicalLines.get(0).toString().substring(1))) {
                skipLines = 1;
            }
        }

        // we may be updatiung a file written by this class, replace
        // the date comment instead of adding a new one and preserving
        // the one written last time
        if (totalLines > skipLines
            && logicalLines.get(skipLines) instanceof Comment) {
            try {
                DateUtils.parseDateFromHeader(logicalLines
                                              .get(skipLines)
                                              .toString().substring(1));
                skipLines++;
            } catch (java.text.ParseException pe) {
                // not an existing date comment
            }
        }
        osw.write("#" + DateUtils.getDateForHeader() + LS);

        boolean writtenSep = false;
        for (Iterator i = logicalLines.subList(skipLines, totalLines).iterator();
             i.hasNext(); ) {
            LogicalLine line = (LogicalLine) i.next();
            if (line instanceof Pair) {
                if (((Pair)line).isNew()) {
                    if (!writtenSep) {
                        osw.write(LS);
                        writtenSep = true;
                    }
                }
                osw.write(line.toString() + LS);
            }
            else if (line != null) {
                osw.write(line.toString() + LS);
            }
        }
        osw.close();
    }

    /**
     * Reads a properties file into an internally maintained
     * collection of logical lines (possibly spanning physcial lines),
     * which make up the comments, blank lines and properties of the
     * file.
     * @param is the stream from which to read the data
     */
    private String readLines(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is, "ISO-8859-1");
        BufferedReader br = new BufferedReader(isr);

        if (logicalLines.size() > 0) {
            // we add a blank line for spacing
            logicalLines.add(new Blank());
        }

        String s = br.readLine();

        boolean continuation = false;
        boolean comment = false;
        StringBuffer fileBuffer = new StringBuffer();
        StringBuffer logicalLineBuffer = new StringBuffer();
        while (s != null) {
            fileBuffer.append(s).append(LS);

            if (continuation) {
                // put in the line feed that was removed
                s = "\n" + s;
            } else {
                // could be a comment, if first non-whitespace is a # or !
                comment = s.matches("^( |\t|\f)*(#|!).*");
            }

            // continuation if not a comment and the line ends is an
            // odd number of backslashes
            if (!comment) {
                continuation = requiresContinuation(s);
            }

            logicalLineBuffer.append(s);

            if (!continuation) {
                LogicalLine line = null;
                if (comment) {
                    line = new Comment(logicalLineBuffer.toString());
                } else if (logicalLineBuffer.toString().trim().length() == 0) {
                    line = new Blank();
                } else {
                    line = new Pair(logicalLineBuffer.toString());
                    String key = unescape(((Pair)line).getName());
                    if (keyedPairLines.containsKey(key)) {
                        // this key is already present, so we remove it and add
                        // the new one
                        remove(key);
                    }
                    keyedPairLines.put(key, new Integer(logicalLines.size()));
                }
                logicalLines.add(line);
                logicalLineBuffer.setLength(0);
            }
            s = br.readLine();
        }
        return fileBuffer.toString();
    }

    /**
     * Returns <code>true</code> if the line represented by
     * <code>s</code> is to be continued on the next line of the file,
     * or <code>false</code> otherwise.
     * @param s the contents of the line to examine
     * @return <code>true</code> if the line is to be continued,
     * <code>false</code> otherwise
     */
    private boolean requiresContinuation(String s) {
        char[] ca = s.toCharArray();
        int i = ca.length - 1;
        while (i > 0 && ca[i] == '\\') {
            i--;
        }
        // trailing backslashes
        int tb = ca.length - i - 1;
        return tb % 2 == 1;
    }

    /**
     * Unescape the string according to the rules for a Properites
     * file, as laid out in the docs for <a
     * href="http://java.sun.com/j2se/1.3/docs/api/java/util/Properties.html">java.util.Properties</a>.
     * @param s the string to unescape (coming from the source file)
     * @return the unescaped string
     */
    private String unescape(String s) {
        /*
         * The following combinations are converted:
         * \n  newline
         * \r  carraige return
         * \f  form feed
         * \t  tab
         * \\  backslash
         * \u0000  unicode character
         * Any other slash is ignored, so
         * \b  becomes 'b'.
         */

        char[] ch = new char[s.length() + 1];
        s.getChars(0, s.length(), ch, 0);
        ch[s.length()] = '\n';
        StringBuffer buffy = new StringBuffer(s.length());
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (c == '\n') {
                // we have hit out end-of-string marker
                break;
            }
            else if (c == '\\') {
                // possibly an escape sequence
                c = ch[++i];
                if (c == 'n')
                    buffy.append('\n');
                else if (c == 'r')
                    buffy.append('\r');
                else if (c == 'f')
                    buffy.append('\f');
                else if (c == 't')
                    buffy.append('\t');
                else if (c == 'u') {
                    // handle unicode escapes
                    c = unescapeUnicode(ch, i+1);
                    i += 4;
                    buffy.append(c);
                }
                else
                    buffy.append(c);
            }
            else {
                buffy.append(c);
            }
        }
        return buffy.toString();
    }

    /**
     * Retrieve the unicode character whose code is listed at position
     * <code>i</code> in the character array <code>ch</code>.
     * @param ch the character array containing the unicode character code
     * @return the character extracted
     */
    private char unescapeUnicode(char[] ch, int i) {
        String s = new String(ch, i, 4);
        return (char) Integer.parseInt(s, 16);
    }

    /**
     * Escape the string <code>s</code> according to the rules in the
     * docs for <a
     * href="http://java.sun.com/j2se/1.3/docs/api/java/util/Properties.html">java.util.Properties</a>.
     * @param s the string to escape
     * @return the escaped string
     */
    private String escapeValue(String s) {
        return escape(s, false);
    }

    /**
     * Escape the string <code>s</code> according to the rules in the
     * docs for <a
     * href="http://java.sun.com/j2se/1.3/docs/api/java/util/Properties.html">java.util.Properties</a>.
     * This method escapes all the whitespace, not just the stuff at
     * the beginning.
     * @param s the string to escape
     * @return the escaped string
     */
    private String escapeName(String s) {
        return escape(s, true);
    }

    /**
     * Escape the string <code>s</code> according to the rules in the
     * docs for <a
     * href="http://java.sun.com/j2se/1.3/docs/api/java/util/Properties.html">java.util.Properties</a>.
     * @param s the string to escape
     * @param escapeAllSpaces if <code>true</code> the method escapes
     * all the spaces, if <code>false</code>, it escapes only the
     * leading whitespace
     * @return the escaped string
     */
    private String escape(String s, boolean escapeAllSpaces) {
        if (s == null) {
            return null;
        }

        char[] ch = new char[s.length()];
        s.getChars(0, s.length(), ch, 0);
        String forEscaping = "\t\f\r\n\\:=#!";
        String escaped = "tfrn\\:=#!";
        StringBuffer buffy = new StringBuffer(s.length());
        boolean leadingSpace = true;
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (c == ' ') {
                if (escapeAllSpaces || leadingSpace) {
                    buffy.append("\\");
                }
            } else {
                leadingSpace = false;
            }
            int p = forEscaping.indexOf(c);
            if (p != -1) {
                buffy.append("\\").append(escaped.substring(p,p+1));
            } else if (c < 0x0020 || c > 0x007e) {
                buffy.append(escapeUnicode(c));
            } else {
                buffy.append(c);
            }
        }
        return buffy.toString();
    }

    /**
     * Return the unicode escape sequence for a character, in the form
     * \u005CuNNNN.
     * @param ch the character to encode
     * @return the unicode escape sequence
     */
    private String escapeUnicode(char ch) {
        StringBuffer buffy = new StringBuffer("\\u");
        String hex = Integer.toHexString((int)ch);
        buffy.append("0000".substring(4-hex.length()));
        buffy.append(hex);
        return buffy.toString();
    }

    /**
     * Remove the comments in the leading up the {@link logicalLines}
     * list leading up to line <code>pos</code>.
     * @param pos the line number to which the comments lead
     */
    private void removeCommentsEndingAt(int pos) {
        /* We want to remove comments preceding this position. Step
         * back counting blank lines (call this range B1) until we hit
         * something non-blank. If what we hit is not a comment, then
         * exit. If what we hit is a comment, then step back counting
         * comment lines (call this range C1). Nullify lines in C1 and
         * B1.
         */

        int end = pos - 1;

        // step pos back until it hits something non-blank
        for (pos = end; pos > 0; pos--) {
            if (!(logicalLines.get(pos) instanceof Blank)) {
                break;
            }
        }

        // if the thing it hits is not a comment, then we have nothing
        // to remove
        if (!(logicalLines.get(pos) instanceof Comment)) {
            return;
        }

        // step back until we hit the start of the comment
        for (; pos >= 0; pos--) {
            if (!(logicalLines.get(pos) instanceof Comment)) {
                break;
            }
        }

        // now we want to delete from pos+1 to end
        for (pos++ ;pos <= end; pos++) {
            logicalLines.set(pos, null);
        }
    }

    /**
     * A logical line of the properties input stream.
     */
    private static abstract class LogicalLine {
        private String text;

        public LogicalLine(String text) {
            this.text = text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String toString() {
            return text;
        }
    }

    /**
     * A blank line of the input stream.
     */
    private static class Blank extends LogicalLine {
        public Blank() {
            super("");
        }
    }

    /**
     * A comment line of the input stream.
     */
    private class Comment extends LogicalLine {
        public Comment(String text) {
            super(text);
        }
    }

    /**
     * A key-value pair from the input stream. This may span more than
     * one physical line, but it is constitutes as a single logical
     * line.
     */
    private static class Pair extends LogicalLine implements Cloneable {
        private String name;
        private String value;
        private boolean added;

        public Pair(String text) {
            super(text);
            parsePair(text);
        }

        public Pair(String name, String value) {
            this(name + "=" + value);
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
            setText(name + "=" + value);
        }

        public boolean isNew() {
            return added;
        }

        public void setNew(boolean val) {
            added = val;
        }

        public Object clone() {
            Object dolly = null;
            try {
                dolly = super.clone();
            }
            catch (CloneNotSupportedException e) {
                // should be fine
                e.printStackTrace();
            }
            return dolly;
        }

        private void parsePair(String text) {
            // need to find first non-escaped '=', ':', '\t' or ' '.
            int pos = findFirstSeparator(text);
            if (pos == -1) {
                // trim leading whitespace only
                name = text;
                value = null;
            }
            else {
                name = text.substring(0, pos);
                value = text.substring(pos+1, text.length());
            }
            // trim leading whitespace only
            name = stripStart(name, " \t\f");
        }

        private String stripStart(String s, String chars) {
            if (s == null) {
                return null;
            }

            int i = 0;
            for (;i < s.length(); i++) {
                if (chars.indexOf(s.charAt(i)) == -1) {
                    break;
                }
            }
            if (i == s.length()) {
                return "";
            }
            return s.substring(i);
        }

        private int findFirstSeparator(String s) {
            // Replace double backslashes with underscores so that they don't
            // confuse us looking for '\t' or '\=', for example, but they also
            // don't change the position of other characters
            s = s.replaceAll("\\\\\\\\", "__");

            // Replace single backslashes followed by separators, so we don't
            // pick them up
            s = s.replaceAll("\\\\=", "__");
            s = s.replaceAll("\\\\:", "__");
            s = s.replaceAll("\\\\ ", "__");
            s = s.replaceAll("\\\\t", "__");

            // Now only the unescaped separators are left
            return indexOfAny(s, " :=\t");
        }

        private int indexOfAny(String s, String chars) {
            if (s == null || chars == null) {
                return -1;
            }

            int p = s.length() + 1;
            for (int i = 0; i < chars.length(); i++) {
                int x = s.indexOf(chars.charAt(i));
                if (x != -1 && x < p) {
                    p = x;
                }
            }
            if (p == s.length() + 1) {
                return -1;
            }
            return p;
        }
    }
}
