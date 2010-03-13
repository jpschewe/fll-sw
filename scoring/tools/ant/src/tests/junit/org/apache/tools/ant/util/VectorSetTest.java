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

import java.util.Arrays;

import junit.framework.TestCase;

public class VectorSetTest extends TestCase {

    private static final Object O = new Object();
    private VectorSet v = new VectorSet();

    public void testAdd() {
        assertTrue(v.add(O));
        assertFalse(v.add(O));
        assertEquals(1, v.size());
    }

    public void testAdd2() {
        v.add(0, O);
        v.add(1, O);
        assertEquals(1, v.size());
    }

    public void testAddElement() {
        v.addElement(O);
        v.addElement(O);
        assertEquals(1, v.size());
    }

    public void testAddAll() {
        assertTrue(v.addAll(Arrays.asList(new Object[] {O, O})));
        assertEquals(1, v.size());
    }

    public void testAddAll2() {
        assertTrue(v.addAll(0, Arrays.asList(new Object[] {O, O})));
        assertEquals(1, v.size());
    }

    public void testClear() {
        v.add(O);
        v.clear();
        assertEquals(0, v.size());
    }
        
    public void testClone() {
        v.add(O);
        Object o = v.clone();
        assertTrue(o instanceof VectorSet);
        VectorSet vs = (VectorSet) o;
        assertEquals(1, vs.size());
        assertTrue(vs.contains(O));
    }

    public void testContains() {
        assertFalse(v.contains(O));
        v.add(O);
        assertTrue(v.contains(O));
        assertFalse(v.contains(null));
    }

    public void testContainsAll() {
        assertFalse(v.containsAll(Arrays.asList(new Object[] {O, O})));
        v.add(O);
        assertTrue(v.containsAll(Arrays.asList(new Object[] {O, O})));
        assertFalse(v.containsAll(Arrays.asList(new Object[] {O, null})));
    }

    public void testInsertElementAt() {
        v.insertElementAt(O, 0);
        v.insertElementAt(O, 1);
        assertEquals(1, v.size());
    }

    public void testRemoveIndex() {
        v.add(O);
        assertSame(O, v.remove(0));
        assertEquals(0, v.size());
        try {
            v.remove(0);
            fail("expected an AIOBE");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    public void testRemoveObject() {
        v.add(O);
        assertTrue(v.remove(O));
        assertEquals(0, v.size());
        assertFalse(v.remove(O));
    }

    public void testRemoveAll() {
        v.add(O);
        assertTrue(v.removeAll(Arrays.asList(new Object[] {O, O})));
        assertEquals(0, v.size());
        assertFalse(v.removeAll(Arrays.asList(new Object[] {O, O})));
    }

    public void testRemoveAllElements() {
        v.add(O);
        v.removeAllElements();
        assertEquals(0, v.size());
    }
        
    public void testRemoveElement() {
        v.add(O);
        assertTrue(v.removeElement(O));
        assertEquals(0, v.size());
        assertFalse(v.removeElement(O));
    }

    public void testRemoveElementAt() {
        v.add(O);
        v.removeElementAt(0);
        assertEquals(0, v.size());
        try {
            v.removeElementAt(0);
            fail("expected an AIOBE");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    public void testRemoveRange() {
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        v.addAll(Arrays.asList(new Object[] {O, a, b, c}));
        v.removeRange(1, 3);
        assertEquals(2, v.size());
        assertTrue(v.contains(O));
        assertTrue(v.contains(c));
    }

    public void testRetainAll() {
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        v.addAll(Arrays.asList(new Object[] {O, a, b, c}));
        assertEquals(0, v.indexOf(O));
        v.retainAll(Arrays.asList(new Object[] {c, O}));
        assertEquals(2, v.size());
        assertTrue(v.contains(O));
        assertTrue(v.contains(c));
        assertEquals(0, v.indexOf(O));
    }

    public void testSet() {
        v.add(O);
        Object a = new Object();
        assertSame(O, v.set(0, a));
        assertSame(a, v.get(0));
        assertEquals(1, v.size());
    }

    public void testSetElementAt() {
        v.add(O);
        Object a = new Object();
        v.setElementAt(a, 0);
        assertSame(a, v.get(0));
        assertEquals(1, v.size());
    }
}
