package org.rapidoid.dao;

/*
 * #%L
 * rapidoid-dao
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
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

import org.rapidoid.test.TestCommons;
import org.testng.annotations.Test;

class PersonService extends DAO<Person> {}

/**
 * @author Nikolche Mihajlovski
 * @since 3.0.0
 */
public class DAOTest extends TestCommons {

	@Test
	public void testEntityTypeInference() {
		PersonService service = new PersonService();

		// exercise the entity type inference
		eq(service.getEntityType(), Person.class);
	}

}
