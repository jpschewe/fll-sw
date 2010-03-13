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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Reference;

/**
 * The Extension set lists a set of "Optional Packages" /
 * "Extensions".
 *
 * @ant.datatype name="extension-set"
 */
public class ExtensionSet
    extends DataType {
    /**
     * ExtensionAdapter objects representing extensions.
     */
    private final ArrayList extensions = new ArrayList();

    /**
     * Filesets specifying all the extensions wanted.
     */
    private final ArrayList extensionsFilesets = new ArrayList();

    /**
     * Adds an extension that this library requires.
     *
     * @param extensionAdapter an extension that this library requires.
     */
    public void addExtension(final ExtensionAdapter extensionAdapter) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        setChecked(false);
        extensions.add(extensionAdapter);
    }

    /**
     * Adds a set of files about which extensions data will be extracted.
     *
     * @param fileSet a set of files about which extensions data will be extracted.
     */
    public void addLibfileset(final LibFileSet fileSet) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        setChecked(false);
        extensionsFilesets.add(fileSet);
    }

    /**
     * Adds a set of files about which extensions data will be extracted.
     *
     * @param fileSet a set of files about which extensions data will be extracted.
     */
    public void addFileset(final FileSet fileSet) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        setChecked(false);
        extensionsFilesets.add(fileSet);
    }

    /**
     * Extract a set of Extension objects from the ExtensionSet.
     *
     * @param proj the project instance.
     * @return an array containing the Extensions from this set
     * @throws BuildException if an error occurs
     */
    public Extension[] toExtensions(final Project proj)
        throws BuildException {
        if (isReference()) {
            return ((ExtensionSet) getCheckedRef()).toExtensions(proj);
        }
        dieOnCircularReference();
        final ArrayList extensionsList = ExtensionUtil.toExtensions(extensions);
        ExtensionUtil.extractExtensions(proj, extensionsList, extensionsFilesets);
        return (Extension[]) extensionsList.toArray(new Extension[extensionsList.size()]);
    }

    /**
     * Makes this instance in effect a reference to another ExtensionSet
     * instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     *
     * @param reference the reference to which this instance is associated
     * @exception BuildException if this instance already has been configured.
     */
    public void setRefid(final Reference reference)
        throws BuildException {
        if (!extensions.isEmpty() || !extensionsFilesets.isEmpty()) {
            throw tooManyAttributes();
        }
        super.setRefid(reference);
    }

    protected synchronized void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            for (Iterator i = extensions.iterator(); i.hasNext(); ) {
                pushAndInvokeCircularReferenceCheck((ExtensionAdapter) i.next(),
                                                    stk, p);
            }
            for (Iterator i = extensionsFilesets.iterator(); i.hasNext(); ) {
                pushAndInvokeCircularReferenceCheck((FileSet) i.next(), stk, p);
            }
            setChecked(true);
        }
    }

    /**
     * @see java.lang.Object#toString()
     * @return the extensions in a string.
     */
    public String toString() {
        return "ExtensionSet" + Arrays.asList(toExtensions(getProject()));
    }
}
