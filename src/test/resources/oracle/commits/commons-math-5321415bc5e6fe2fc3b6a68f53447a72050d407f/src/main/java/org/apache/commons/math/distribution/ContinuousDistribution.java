/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.math.distribution;

import org.apache.commons.math.MathException;

/**
 * Base interface for continuous distributions.
 *
 * @version $Revision$ $Date$
 */
public interface ContinuousDistribution extends Distribution {
    /**
     * For a distribution, {@code X}, compute {@code x} such that
     * {@code P(X < x) = p}.
     *
     * @param p Cumulative probability.
     * @return {@code x} such that {@code P(X < x) = p}.
     * @throws MathException if the inverse cumulative probability cannot be
     * computed due to convergence or other numerical errors.
     */
    double inverseCumulativeProbability(double p) throws MathException;

    /**
     * Probability density for a particular point.
     *
     * @param x Point at which the density should be computed.
     * @return the pdf at point {@code x}.
     */
    double density(double x);
}
