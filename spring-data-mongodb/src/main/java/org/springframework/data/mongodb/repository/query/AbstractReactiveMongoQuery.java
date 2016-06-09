/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.mongodb.repository.query;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.ReactiveMongoQueryExecution.CollectionExecution;
import org.springframework.data.mongodb.repository.query.ReactiveMongoQueryExecution.DeleteExecution;
import org.springframework.data.mongodb.repository.query.ReactiveMongoQueryExecution.PagedExecution;
import org.springframework.data.mongodb.repository.query.ReactiveMongoQueryExecution.ResultProcessingConverter;
import org.springframework.data.mongodb.repository.query.ReactiveMongoQueryExecution.ResultProcessingExecution;
import org.springframework.data.mongodb.repository.query.ReactiveMongoQueryExecution.SingleEntityExecution;
import org.springframework.data.mongodb.repository.query.ReactiveMongoQueryExecution.SlicedExecution;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.util.Assert;

/**
 * Base class for reactive {@link RepositoryQuery} implementations for Mongo.
 *
 * @author Mark Paluch
 */
public abstract class AbstractReactiveMongoQuery implements RepositoryQuery {

	private final MongoQueryMethod method;
	private final ReactiveMongoOperations operations;
	private final EntityInstantiators instantiators;

	/**
	 * Creates a new {@link AbstractReactiveMongoQuery} from the given {@link MongoQueryMethod} and
	 * {@link MongoOperations}.
	 *
	 * @param method must not be {@literal null}.
	 * @param operations must not be {@literal null}.
	 */
	public AbstractReactiveMongoQuery(MongoQueryMethod method, ReactiveMongoOperations operations) {

		Assert.notNull(method, "MongoQueryMethod must not be null!");
		Assert.notNull(operations, "ReactiveMongoOperations must not be null!");

		this.method = method;
		this.operations = operations;
		this.instantiators = new EntityInstantiators();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
	 */
	public MongoQueryMethod getQueryMethod() {
		return method;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
	 */
	public Object execute(Object[] parameters) {

		MongoParameterAccessor accessor = new MongoParametersParameterAccessor(method, parameters);
		Query query = createQuery(new ConvertingParameterAccessor(operations.getConverter(), accessor));

		applyQueryMetaAttributesWhenPresent(query);

		ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);
		String collection = method.getEntityInformation().getCollectionName();

		ReactiveMongoQueryExecution execution = getExecution(query, accessor,
				new ResultProcessingConverter(processor, operations, instantiators));

		return execution.execute(query, processor.getReturnedType().getDomainType(), collection);
	}

	/**
	 * Returns the execution instance to use.
	 *
	 * @param query must not be {@literal null}.
	 * @param accessor must not be {@literal null}.
	 * @param resultProcessing must not be {@literal null}.
	 * @return
	 */
	private ReactiveMongoQueryExecution getExecution(Query query, MongoParameterAccessor accessor,
			Converter<Object, Object> resultProcessing) {
		return new ResultProcessingExecution(getExecutionToWrap(query, accessor), resultProcessing);
	}

	private ReactiveMongoQueryExecution getExecutionToWrap(Query query, MongoParameterAccessor accessor) {

		if (isDeleteQuery()) {
			return new DeleteExecution(operations, method);
		} else if (method.isSliceQuery()) {
			return new SlicedExecution(operations, accessor.getPageable());
		} else if (method.isCollectionQuery()) {
			return new CollectionExecution(operations, accessor.getPageable());
		} else if (method.isPageQuery()) {
			return new PagedExecution(operations, accessor.getPageable());
		} else {
			return new SingleEntityExecution(operations, isCountQuery());
		}
	}

	Query applyQueryMetaAttributesWhenPresent(Query query) {

		if (method.hasQueryMetaAttributes()) {
			query.setMeta(method.getQueryMetaAttributes());
		}

		return query;
	}

	/**
	 * Creates a {@link Query} instance using the given {@link ConvertingParameterAccessor}. Will delegate to
	 * {@link #createQuery(ConvertingParameterAccessor)} by default but allows customization of the count query to be
	 * triggered.
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	protected Query createCountQuery(ConvertingParameterAccessor accessor) {
		return applyQueryMetaAttributesWhenPresent(createQuery(accessor));
	}

	/**
	 * Creates a {@link Query} instance using the given {@link ParameterAccessor}
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	protected abstract Query createQuery(ConvertingParameterAccessor accessor);

	/**
	 * Returns whether the query should get a count projection applied.
	 *
	 * @return
	 */
	protected abstract boolean isCountQuery();

	/**
	 * Return weather the query should delete matching documents.
	 *
	 * @return
	 * @since 1.5
	 */
	protected abstract boolean isDeleteQuery();
}
