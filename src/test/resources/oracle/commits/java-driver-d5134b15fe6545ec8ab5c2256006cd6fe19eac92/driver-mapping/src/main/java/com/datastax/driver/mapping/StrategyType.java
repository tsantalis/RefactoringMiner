/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.datastax.driver.mapping;

/**
 * Defines strategies for object persistence.
 * <p/>
 * For Saving an object there are two strategies :
 * <ul>
 * <li>ALL_FIELDS : If a value of a field is null, it will be stored as NULL
 * into the database. This strategy is the default.</li>
 * <li>NOT_NULL_FIELDS_ONLY : If a value of a field is null, the field will be
 * ignored during the save operation. It means that
 * when updating an object into the database if a field
 * is null and a value is already present for this row,
 * the previous value will be kept and only non-null
 * fields will be updated. This can cause consistency
 * issues so users should be warned of the potential
 * issues using this feature</li>
 * </ul>
 */
public enum StrategyType {
    ALL_FIELDS, NOT_NULL_FIELDS_ONLY
}
