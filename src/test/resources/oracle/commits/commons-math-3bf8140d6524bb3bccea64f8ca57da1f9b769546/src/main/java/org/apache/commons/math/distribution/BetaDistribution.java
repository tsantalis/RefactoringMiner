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

/**
 * Computes the cumulative, inverse cumulative and density functions for the beta distribuiton.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Beta_distribution">Beta_distribution</a>
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface BetaDistribution extends ContinuousDistribution {
     /**
      * Access the alpha shape parameter.
      *
      * @return alpha.
      */
     double getAlpha();

     /**
      * Access the beta shape parameter.
      *
      * @return beta.
      */
     double getBeta();

     /**
      * Return the probability density for a particular point.
      *
      * @param x  Point at which the density should be computed.
      * @return the pdf at point {@code x}.
      */
     double density(double x);
}
