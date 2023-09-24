/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.changes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Performs ChangeSet operations on a stream.
 * This class is thread safe and can be used multiple times.
 * It operates on a copy of the ChangeSet. If the ChangeSet changes,
 * a new Performer must be created.
 * 
 * @ThreadSafe
 * @Immutable
 */
public class ChangeSetPerformer {
    private final Set changes;
    
    /**
     * Constructs a ChangeSetPerformer with the changes from this ChangeSet
     * @param changeSet the ChangeSet which operations are used for performing
     */
    public ChangeSetPerformer(final ChangeSet changeSet) {
        changes = changeSet.getChanges();
    }
    
    /**
     * Performs all changes collected in this ChangeSet on the input stream and
     * streams the result to the output stream. Perform may be called more than once.
     * 
     * This method finishes the stream, no other entries should be added
     * after that.
     * 
     * @param in
     *            the InputStream to perform the changes on
     * @param out
     *            the resulting OutputStream with all modifications
     * @throws IOException
     *             if an read/write error occurs
     * @return the results of this operation
     */
    public ChangeSetResults perform(ArchiveInputStream in, ArchiveOutputStream out)
            throws IOException {
        ChangeSetResults results = new ChangeSetResults();
        
        Set workingSet = new LinkedHashSet(changes);
        
        for (Iterator it = workingSet.iterator(); it.hasNext();) {
            Change change = (Change) it.next();

            if (change.type() == Change.TYPE_ADD && change.isReplaceMode()) {
                copyStream(change.getInput(), out, change.getEntry());
                it.remove();
                results.addedFromChangeSet(change.getEntry().getName());
            }
        }

        ArchiveEntry entry = null;
        while ((entry = in.getNextEntry()) != null) {
            boolean copy = true;

            for (Iterator it = workingSet.iterator(); it.hasNext();) {
                Change change = (Change) it.next();

                final int type = change.type();
                final String name = entry.getName();
                if (type == Change.TYPE_DELETE && name != null) {
                    if (name.equals(change.targetFile())) {
                        copy = false;
                        it.remove();
                        results.deleted(name);
                        break;
                    }
                } else if(type == Change.TYPE_DELETE_DIR && name != null) {
                    if (name.startsWith(change.targetFile() + "/")) {
                        copy = false;
                        results.deleted(name);
                        break;
                    }
                }
            }

            if (copy) {
                if (!isDeletedLater(workingSet, entry) && !results.hasBeenAdded(entry.getName())) {
                    copyStream(in, out, entry);
                    results.addedFromStream(entry.getName());
                }
            }
        }
        
        // Adds files which hasn't been added from the original and do not have replace mode on
        for (Iterator it = workingSet.iterator(); it.hasNext();) {
            Change change = (Change) it.next();

            if (change.type() == Change.TYPE_ADD && 
                !change.isReplaceMode() && 
                !results.hasBeenAdded(change.getEntry().getName())) {
                copyStream(change.getInput(), out, change.getEntry());
                it.remove();
                results.addedFromChangeSet(change.getEntry().getName());
            }
        }
        return results;
    }

    /**
     * Checks if an ArchiveEntry is deleted later in the ChangeSet. This is
     * necessary if an file is added with this ChangeSet, but later became
     * deleted in the same set.
     * 
     * @param entry
     *            the entry to check
     * @return true, if this entry has an deletion change later, false otherwise
     */
    private boolean isDeletedLater(Set workingSet, ArchiveEntry entry) {
        String source = entry.getName();

        if (!workingSet.isEmpty()) {
            for (Iterator it = workingSet.iterator(); it.hasNext();) {
                Change change = (Change) it.next();
                final int type = change.type();
                String target = change.targetFile();
                if (type == Change.TYPE_DELETE && source.equals(target)) {
                    return true;
                }

                if (type == Change.TYPE_DELETE_DIR && source.startsWith(target + "/")){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Copies the ArchiveEntry to the Output stream
     * 
     * @param in
     *            the stream to read the data from
     * @param out
     *            the stream to write the data to
     * @param entry
     *            the entry to write
     * @throws IOException
     *             if data cannot be read or written
     */
    private void copyStream(InputStream in, ArchiveOutputStream out,
            ArchiveEntry entry) throws IOException {
        out.putArchiveEntry(entry);
        IOUtils.copy(in, out);
        out.closeArchiveEntry();
    }
}
