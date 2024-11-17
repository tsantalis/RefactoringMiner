/**
 * Copyright 2016 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 **/

package io.confluent.connect.elasticsearch.internals;

import java.util.ArrayList;
import java.util.IdentityHashMap;

public class IncompleteRecordBatches {

  private final IdentityHashMap<RecordBatch, RecordBatch> incomplete;

  public IncompleteRecordBatches() {
    this.incomplete = new IdentityHashMap<>();
  }

  public void add(RecordBatch batch) {
    synchronized (incomplete) {
      this.incomplete.put(batch, batch);
    }
  }

  public void remove(RecordBatch batch) {
    synchronized (incomplete) {
      RecordBatch removed = this.incomplete.remove(batch);
      if (removed == null)
        throw new IllegalStateException("Remove from the incomplete set failed. This should be impossible.");
    }
  }

  public Iterable<RecordBatch> all() {
    synchronized (incomplete) {
      return new ArrayList<>(this.incomplete.keySet());
    }
  }

  public int numIncompletes() {
    synchronized (incomplete) {
      return incomplete.size();
    }
  }
}
