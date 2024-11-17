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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RecordBatchResult {

  private final CountDownLatch latch = new CountDownLatch(1);
  private volatile Throwable error;

  public void done(Throwable error) {
    this.error = error;
    latch.countDown();
  }

  public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    return latch.await(timeout, unit);
  }

  public Throwable getError() {
    return error;
  }

}
