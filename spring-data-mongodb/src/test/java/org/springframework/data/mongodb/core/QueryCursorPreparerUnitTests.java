/*
 * Copyright 2011-2014 the original author or authors.
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

import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.mongodb.core.query.Criteria.*;
import static org.springframework.data.mongodb.core.query.Query.*;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate.QueryCursorPreparer;
import org.springframework.data.mongodb.core.query.Meta;
import org.springframework.data.mongodb.core.query.Query;

import com.mongodb.client.FindIterable;

/**
 * Unit tests for {@link QueryCursorPreparer}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryCursorPreparerUnitTests {

	@Mock MongoDbFactory factory;
	@Mock MongoExceptionTranslator exceptionTranslatorMock;
	@Mock FindIterable<Document> cursor;

	@Mock FindIterable<Document> cursorToUse;

	@Before
	public void setUp() {

		when(factory.getExceptionTranslator()).thenReturn(exceptionTranslatorMock);
		when(cursor.batchSize(anyInt())).thenReturn(cursor);
		when(cursor.filter(any(Document.class))).thenReturn(cursor);
		when(cursor.limit(anyInt())).thenReturn(cursor);
		when(cursor.modifiers(any(Document.class))).thenReturn(cursor);
	}

	/**
	 * @see DATAMONGO-185
	 */
	@Test
	public void appliesHintsCorrectly() {

		Query query = query(where("foo").is("bar")).withHint("hint");

		pepare(query);

		ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
		verify(cursor).modifiers(captor.capture());
		assertThat(captor.getValue(), equalTo(new Document("$hint", "hint")));
	}

	/**
	 * @see DATAMONGO-957
	 */
	@Test
	public void doesNotApplyMetaWhenEmpty() {

		Query query = query(where("foo").is("bar"));
		query.setMeta(new Meta());

		pepare(query);

		verify(cursorToUse, never()).modifiers(any(Document.class));
	}

	/**
	 * @see DATAMONGO-957
	 */
	@Test
	public void appliesMaxScanCorrectly() {

		Query query = query(where("foo").is("bar")).maxScan(100);

		pepare(query);

		ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
		verify(cursor).modifiers(captor.capture());
		assertThat(captor.getValue(), equalTo(new Document("$maxScan", 100L)));
	}

	/**
	 * @see DATAMONGO-957
	 */
	@Test
	public void appliesMaxTimeCorrectly() {

		Query query = query(where("foo").is("bar")).maxTime(1, TimeUnit.SECONDS);

		pepare(query);

		ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
		verify(cursor).modifiers(captor.capture());
		assertThat(captor.getValue(), equalTo(new Document("$maxTimeMS", 1000L)));
	}

	/**
	 * @see DATAMONGO-957
	 */
	@Test
	public void appliesCommentCorrectly() {

		Query query = query(where("foo").is("bar")).comment("spring data");

		pepare(query);

		ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
		verify(cursor).modifiers(captor.capture());
		assertThat(captor.getValue(), equalTo(new Document("$comment", "spring data")));
	}

	/**
	 * @see DATAMONGO-957
	 */
	@Test
	public void appliesSnapshotCorrectly() {

		Query query = query(where("foo").is("bar")).useSnapshot();

		pepare(query);

		ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
		verify(cursor).modifiers(captor.capture());
		assertThat(captor.getValue(), equalTo(new Document("$snapshot", true)));
	}

	private FindIterable<Document> pepare(Query query) {

		CursorPreparer preparer = new MongoTemplate(factory).new QueryCursorPreparer(query, null);
		return preparer.prepare(cursor);
	}
}
