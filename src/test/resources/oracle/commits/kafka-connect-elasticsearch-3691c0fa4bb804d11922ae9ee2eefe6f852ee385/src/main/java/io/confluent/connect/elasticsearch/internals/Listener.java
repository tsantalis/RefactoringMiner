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

/**
 * A listener for the execution.
 */
public interface Listener {

  /**
   * Callback before the batch is executed.
   */
  void beforeBulk(long executionId, RecordBatch batch);

  /**
   * Callback after a successful execution of a batch.
   */
  void afterBulk(long executionId, RecordBatch batch, Response response);

  /**
   * Callback after a failed execution of a batch.
   *
   * Note that in case an instance of <code>InterruptedException</code> is passed, which means that
   * request processing has been cancelled externally, the thread's interruption status has been
   * restored prior to calling this method.
   */
  void afterBulk(long executionId, RecordBatch batch, Throwable failure);
}
