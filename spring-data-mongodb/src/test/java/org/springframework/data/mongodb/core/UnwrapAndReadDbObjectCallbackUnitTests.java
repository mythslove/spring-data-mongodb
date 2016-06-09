/*
 * Copyright 2013 the original author or authors.
 *
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
 */
package org.springframework.data.mongodb.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate.UnwrapAndReadDbObjectCallback;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Unit tests for {@link UnwrapAndReadDbObjectCallback}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class UnwrapAndReadDbObjectCallbackUnitTests {

	@Mock MongoDbFactory factory;
	@Mock MongoExceptionTranslator exceptionTranslatorMock;

	UnwrapAndReadDbObjectCallback<Target> callback;

	@Before
	public void setUp() {

		when(factory.getExceptionTranslator()).thenReturn(exceptionTranslatorMock);

		MongoTemplate template = new MongoTemplate(factory);
		MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(factory),
				new MongoMappingContext());

		this.callback = template.new UnwrapAndReadDbObjectCallback<Target>(converter, Target.class, "collection-1");
	}

	@Test
	public void usesFirstLevelValues() {

		Target target = callback.doWith(new Document("foo", "bar"));

		assertThat(target.id, is(nullValue()));
		assertThat(target.foo, is("bar"));
	}

	@Test
	public void unwrapsUnderscoreIdIfBasicDocument() {

		Target target = callback.doWith(new Document("_id", new Document("foo", "bar")));

		assertThat(target.id, is(nullValue()));
		assertThat(target.foo, is("bar"));
	}

	@Test
	public void firstLevelPropertiesTrumpNestedOnes() {

		Target target = callback.doWith(new Document("_id", new Document("foo", "bar")).append("foo", "foobar"));

		assertThat(target.id, is(nullValue()));
		assertThat(target.foo, is("foobar"));
	}

	@Test
	public void keepsUnderscoreIdIfScalarValue() {

		Target target = callback.doWith(new Document("_id", "bar").append("foo", "foo"));

		assertThat(target.id, is("bar"));
		assertThat(target.foo, is("foo"));
	}

	static class Target {

		String id;
		String foo;
	}
}
