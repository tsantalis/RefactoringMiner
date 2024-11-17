package gwt.material.design.client.base.validator;

/*
 * #%L
 * GwtBootstrap3
 * %%
 * Copyright (C) 2015 GwtBootstrap3
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * These are convenience methods to automatically add the {@link BlankValidator}.
 *
 * @author Steven Jardine
 */
public interface HasBlankValidator {

    /**
     * @return the allow blank
     */
    boolean isAllowBlank();

    /**
     * @param allowBlank the new allow blank
     */
    void setAllowBlank(boolean allowBlank);
}
