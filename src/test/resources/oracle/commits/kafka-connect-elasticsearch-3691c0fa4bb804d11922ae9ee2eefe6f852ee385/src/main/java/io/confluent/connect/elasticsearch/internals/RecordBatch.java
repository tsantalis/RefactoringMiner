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
import java.util.List;

public class RecordBatch {
  private final List<ESRequest> requests;
  private final RecordBatchResult result;
  private volatile long lastAttemptMs;
  private boolean retry;
  private volatile int attempts;


  public RecordBatch(long now) {
    this.requests = new ArrayList<>();
    this.result = new RecordBatchResult();
    this.attempts = 0;
    this.lastAttemptMs = now;
  }

  public void add(ESRequest request) {
    requests.add(request);
  }

  public List<ESRequest> requests() {
    return requests;
  }

  public RecordBatchResult result() {
    return result;
  }

  public int size() {
    return requests.size();
  }

  public boolean inRetry()  {
    return retry;
  }

  public void setRetry() {
    this.retry = true;
  }

  public long getLastAttemptMs() {
    return lastAttemptMs;
  }

  public void setLastAttemptMs(long lastAttemptMs) {
    this.lastAttemptMs = lastAttemptMs;
  }

  public int getAttempts() {
    return attempts;
  }

  public void incrementAttempts() {
    attempts++;
  }
}
