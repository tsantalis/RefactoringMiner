/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.tracking;

import java.util.Collections;
import java.util.List;

public final class FlushEvent {

    private final List<Record> records;

    public FlushEvent(final List<Record> records) {
        if (records != null) {
            this.records = records;
        } else {
            this.records = Collections.emptyList();
        }
    }

    public List<Record> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        return "FlushEvent [records.size=" + records.size() + "]";
    }

}
