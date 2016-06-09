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
package org.springframework.data.mongodb.core;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.geo.GeoResults;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapreduce.GroupBy;
import org.springframework.data.mongodb.core.mapreduce.GroupByResults;
import org.springframework.data.mongodb.core.mapreduce.MapReduceOptions;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.CloseableIterator;

import com.mongodb.CommandResult;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import com.mongodb.WriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.Success;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface that specifies a basic set of MongoDB operations executed in a reactive way. Implemented by {@link ReactiveMongoTemplate}. Not often used but
 * a useful option for extensibility and testability (as it can be easily mocked, stubbed, or be the target of a JDK
 * proxy). Commands issued using {@link ReactiveMongoOperations} are lazily executed at the time a subscriber subscribes to the {@link Publisher}.
 *
 * TODO:
 * - adopt MongoDB 3 findOneAndReplace/findOneAndDelete/findOneAndUpdate
 * - Reactive Mongo Converter?
 *
 *
 * @author Mark Paluch
 * @see Flux
 * @see Mono
 * @see http://projectreactor.io/docs/
 *
 */
public interface ReactiveMongoOperations {

	/**
	 * Execute the a MongoDB command expressed as a JSON string. This will call the method JSON.parse that is part of the
	 * MongoDB driver to convert the JSON string to a DBObject. Any errors that result from executing this command will be
	 * converted into Spring's DAO exception hierarchy.
	 *
	 * @param jsonCommand a MongoDB command expressed as a JSON string.
	 */
	Flux<Document> executeCommand(String jsonCommand);

	/**
	 * Execute a MongoDB command. Any errors that result from executing this command will be converted into Spring's DAO
	 * exception hierarchy.
	 *
	 * @param command a MongoDB command
	 */
	Flux<Document> executeCommand(Document command);

	/**
	 * Execute a MongoDB command. Any errors that result from executing this command will be converted into Spring's data
	 * access exception hierarchy.
	 *
	 * @param command a MongoDB command, must not be {@literal null}.
	 * @param readPreference read preferences to use, can be {@literal null}.
	 * @return
	 */
	Flux<Document> executeCommand(Document command, ReadPreference readPreference);

	/**
	 * Executes a {@link ReactiveDbCallback} translating any exceptions as necessary.
	 * <p/>
	 * Allows for returning a result object, that is a domain object or a collection of domain objects.
	 *
	 * @param <T> return type
	 * @param action callback object that specifies the MongoDB actions to perform on the passed in DB instance.
	 * @return a result object returned by the action or <tt>null</tt>
	 */
	<T> Flux<T> execute(ReactiveDbCallback<T> action);

	/**
	 * Executes the given {@link ReactiveCollectionCallback} on the entity collection of the specified class.
	 * <p/>
	 * Allows for returning a result object, that is a domain object or a collection of domain objects.
	 *
	 * @param entityClass class that determines the collection to use
	 * @param <T> return type
	 * @param action callback object that specifies the MongoDB action
	 * @return a result object returned by the action or <tt>null</tt>
	 */
	<T> Flux<T> execute(Class<?> entityClass, ReactiveCollectionCallback<T> action);

	/**
	 * Executes the given {@link ReactiveCollectionCallback} on the collection of the given name.
	 * <p/>
	 * Allows for returning a result object, that is a domain object or a collection of domain objects.
	 *
	 * @param <T> return type
	 * @param collectionName the name of the collection that specifies which DBCollection instance will be passed into
	 * @param action callback object that specifies the MongoDB action the callback action.
	 * @return a result object returned by the action or <tt>null</tt>
	 */
	<T> Flux<T> execute(String collectionName, ReactiveCollectionCallback<T> action);

	/**
	 * Create an uncapped collection with a name based on the provided entity class.
	 *
	 * @param entityClass class that determines the collection to create
	 * @return the created collection
	 */
	<T> Mono<MongoCollection<Document>> createCollection(Class<T> entityClass);

	/**
	 * Create a collection with a name based on the provided entity class using the options.
	 *
	 * @param entityClass class that determines the collection to create
	 * @param collectionOptions options to use when creating the collection.
	 * @return the created collection
	 */
	<T> Mono<MongoCollection<Document>> createCollection(Class<T> entityClass, CollectionOptions collectionOptions);

	/**
	 * Create an uncapped collection with the provided name.
	 *
	 * @param collectionName name of the collection
	 * @return the created collection
	 */
	Mono<MongoCollection<Document>> createCollection(String collectionName);

	/**
	 * Create a collection with the provided name and options.
	 *
	 * @param collectionName name of the collection
	 * @param collectionOptions options to use when creating the collection.
	 * @return the created collection
	 */
	Mono<MongoCollection<Document>> createCollection(String collectionName, CollectionOptions collectionOptions);

	/**
	 * A set of collection names.
	 *
	 * @return list of collection names
	 */
	Flux<String> getCollectionNames();

	/**
	 * Get a collection by name, creating it if it doesn't exist.
	 * <p/>
	 * Translate any exceptions as necessary.
	 *
	 * @param collectionName name of the collection
	 * @return an existing collection or a newly created one.
	 */
	MongoCollection<Document> getCollection(String collectionName);

	/**
	 * Check to see if a collection with a name indicated by the entity class exists.
	 * <p/>
	 * Translate any exceptions as necessary.
	 *
	 * @param entityClass class that determines the name of the collection
	 * @return true if a collection with the given name is found, false otherwise.
	 */
	<T> Mono<Boolean> collectionExists(Class<T> entityClass);

	/**
	 * Check to see if a collection with a given name exists.
	 * <p/>
	 * Translate any exceptions as necessary.
	 *
	 * @param collectionName name of the collection
	 * @return true if a collection with the given name is found, false otherwise.
	 */
	Mono<Boolean> collectionExists(String collectionName);

	/**
	 * Drop the collection with the name indicated by the entity class.
	 * <p/>
	 * Translate any exceptions as necessary.
	 *
	 * @param entityClass class that determines the collection to drop/delete.
	 */
	<T> Mono<Void> dropCollection(Class<T> entityClass);

	/**
	 * Drop the collection with the given name.
	 * <p/>
	 * Translate any exceptions as necessary.
	 *
	 * @param collectionName name of the collection to drop/delete.
	 */
	Mono<Void> dropCollection(String collectionName);

	/**
	 * Query for a list of objects of type T from the collection used by the entity class.
	 * <p/>
	 * The object is converted from the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * If your collection does not contain a homogeneous collection of types, this operation will not be an efficient way
	 * to map objects since the test for class type is done in the client and not on the server.
	 *
	 * @param entityClass the parameterized type of the returned list
	 * @return the converted collection
	 */
	<T> Flux<T> findAll(Class<T> entityClass);

	/**
	 * Query for a list of objects of type T from the specified collection.
	 * <p/>
	 * The object is converted from the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * If your collection does not contain a homogeneous collection of types, this operation will not be an efficient way
	 * to map objects since the test for class type is done in the client and not on the server.
	 *
	 * @param entityClass the parameterized type of the returned list.
	 * @param collectionName name of the collection to retrieve the objects from
	 * @return the converted collection
	 */
	<T> Flux<T> findAll(Class<T> entityClass, String collectionName);

	/**
	 * Map the results of an ad-hoc query on the collection for the entity class to a single instance of an object of the
	 * specified type.
	 * <p/>
	 * The object is converted from the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * The query is specified as a {@link Query} which can be created either using the {@link BasicQuery} or the more
	 * feature rich {@link Query}.
	 *
	 * @param query the query class that specifies the criteria used to find a record and also an optional fields
	 *          specification
	 * @param entityClass the parameterized type of the returned list.
	 * @return the converted object
	 */
	<T> Mono<T> findOne(Query query, Class<T> entityClass);

	/**
	 * Map the results of an ad-hoc query on the specified collection to a single instance of an object of the specified
	 * type.
	 * <p/>
	 * The object is converted from the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * The query is specified as a {@link Query} which can be created either using the {@link BasicQuery} or the more
	 * feature rich {@link Query}.
	 *
	 * @param query the query class that specifies the criteria used to find a record and also an optional fields
	 *          specification
	 * @param entityClass the parameterized type of the returned list.
	 * @param collectionName name of the collection to retrieve the objects from
	 * @return the converted object
	 */
	<T> Mono<T> findOne(Query query, Class<T> entityClass, String collectionName);

	/**
	 * Determine result of given {@link Query} contains at least one element.
	 *
	 * @param query the {@link Query} class that specifies the criteria used to find a record.
	 * @param collectionName name of the collection to check for objects.
	 * @return
	 */
	Mono<Boolean> exists(Query query, String collectionName);

	/**
	 * Determine result of given {@link Query} contains at least one element.
	 *
	 * @param query the {@link Query} class that specifies the criteria used to find a record.
	 * @param entityClass the parameterized type.
	 * @return
	 */
	Mono<Boolean> exists(Query query, Class<?> entityClass);

	/**
	 * Determine result of given {@link Query} contains at least one element.
	 *
	 * @param query the {@link Query} class that specifies the criteria used to find a record.
	 * @param entityClass the parameterized type.
	 * @param collectionName name of the collection to check for objects.
	 * @return
	 */
	Mono<Boolean> exists(Query query, Class<?> entityClass, String collectionName);

	/**
	 * Map the results of an ad-hoc query on the collection for the entity class to a List of the specified type.
	 * <p/>
	 * The object is converted from the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * The query is specified as a {@link Query} which can be created either using the {@link BasicQuery} or the more
	 * feature rich {@link Query}.
	 *
	 * @param query the query class that specifies the criteria used to find a record and also an optional fields
	 *          specification
	 * @param entityClass the parameterized type of the returned list.
	 * @return the List of converted objects
	 */
	<T> Flux<T> find(Query query, Class<T> entityClass);

	/**
	 * Map the results of an ad-hoc query on the specified collection to a List of the specified type.
	 * <p/>
	 * The object is converted from the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * The query is specified as a {@link Query} which can be created either using the {@link BasicQuery} or the more
	 * feature rich {@link Query}.
	 *
	 * @param query the query class that specifies the criteria used to find a record and also an optional fields
	 *          specification
	 * @param entityClass the parameterized type of the returned list.
	 * @param collectionName name of the collection to retrieve the objects from
	 * @return the List of converted objects
	 */
	<T> Flux<T> find(Query query, Class<T> entityClass, String collectionName);

	/**
	 * Returns a document with the given id mapped onto the given class. The collection the query is ran against will be
	 * derived from the given target class as well.
	 *
	 * @param <T>
	 * @param id the id of the document to return.
	 * @param entityClass the type the document shall be converted into.
	 * @return the document with the given id mapped onto the given target class.
	 */
	<T> Mono<T> findById(Object id, Class<T> entityClass);

	/**
	 * Returns the document with the given id from the given collection mapped onto the given target class.
	 *
	 * @param id the id of the document to return
	 * @param entityClass the type to convert the document to
	 * @param collectionName the collection to query for the document
	 * @param <T>
	 * @return
	 */
	<T> Mono<T> findById(Object id, Class<T> entityClass, String collectionName);

	/**
	 * Triggers <a href="http://docs.mongodb.org/manual/reference/method/db.collection.findAndModify/">findAndModify
	 * <a/> to apply provided {@link Update} on documents matching {@link Criteria} of given {@link Query}.
	 *
	 * @param query the {@link Query} class that specifies the {@link Criteria} used to find a record and also an optional
	 *          fields specification.
	 * @param update the {@link Update} to apply on matching documents.
	 * @param entityClass the parameterized type.
	 * @return
	 */
	<T> Mono<T> findAndModify(Query query, Update update, Class<T> entityClass);

	/**
	 * Triggers <a href="http://docs.mongodb.org/manual/reference/method/db.collection.findAndModify/">findAndModify
	 * <a/> to apply provided {@link Update} on documents matching {@link Criteria} of given {@link Query}.
	 *
	 * @param query the {@link Query} class that specifies the {@link Criteria} used to find a record and also an optional
	 *          fields specification.
	 * @param update the {@link Update} to apply on matching documents.
	 * @param entityClass the parameterized type.
	 * @param collectionName the collection to query.
	 * @return
	 */
	<T> Mono<T> findAndModify(Query query, Update update, Class<T> entityClass, String collectionName);

	/**
	 * Triggers <a href="http://docs.mongodb.org/manual/reference/method/db.collection.findAndModify/">findAndModify
	 * <a/> to apply provided {@link Update} on documents matching {@link Criteria} of given {@link Query} taking
	 * {@link FindAndModifyOptions} into account.
	 *
	 * @param query the {@link Query} class that specifies the {@link Criteria} used to find a record and also an optional
	 *          fields specification.
	 * @param update the {@link Update} to apply on matching documents.
	 * @param options the {@link FindAndModifyOptions} holding additional information.
	 * @param entityClass the parameterized type.
	 * @return
	 */
	<T> Mono<T> findAndModify(Query query, Update update, FindAndModifyOptions options, Class<T> entityClass);

	/**
	 * Triggers <a href="http://docs.mongodb.org/manual/reference/method/db.collection.findAndModify/">findAndModify
	 * <a/> to apply provided {@link Update} on documents matching {@link Criteria} of given {@link Query} taking
	 * {@link FindAndModifyOptions} into account.
	 *
	 * @param query the {@link Query} class that specifies the {@link Criteria} used to find a record and also an optional
	 *          fields specification.
	 * @param update the {@link Update} to apply on matching documents.
	 * @param options the {@link FindAndModifyOptions} holding additional information.
	 * @param entityClass the parameterized type.
	 * @param collectionName the collection to query.
	 * @return
	 */
	<T> Mono<T> findAndModify(Query query, Update update, FindAndModifyOptions options, Class<T> entityClass,
						String collectionName);

	/**
	 * Map the results of an ad-hoc query on the collection for the entity type to a single instance of an object of the
	 * specified type. The first document that matches the query is returned and also removed from the collection in the
	 * database.
	 * <p/>
	 * The object is converted from the MongoDB native representation using an instance of {@see MongoConverter}.
	 * <p/>
	 * The query is specified as a {@link Query} which can be created either using the {@link BasicQuery} or the more
	 * feature rich {@link Query}.
	 *
	 * @param query the query class that specifies the criteria used to find a record and also an optional fields
	 *          specification
	 * @param entityClass the parameterized type of the returned list.
	 * @return the converted object
	 */
	<T> Mono<T> findAndRemove(Query query, Class<T> entityClass);

	/**
	 * Map the results of an ad-hoc query on the specified collection to a single instance of an object of the specified
	 * type. The first document that matches the query is returned and also removed from the collection in the database.
	 * <p/>
	 * The object is converted from the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * The query is specified as a {@link Query} which can be created either using the {@link BasicQuery} or the more
	 * feature rich {@link Query}.
	 *
	 * @param query the query class that specifies the criteria used to find a record and also an optional fields
	 *          specification
	 * @param entityClass the parameterized type of the returned list.
	 * @param collectionName name of the collection to retrieve the objects from
	 * @return the converted object
	 */
	<T> Mono<T> findAndRemove(Query query, Class<T> entityClass, String collectionName);

	/**
	 * Returns the number of documents for the given {@link Query} by querying the collection of the given entity class.
	 *
	 * @param query
	 * @param entityClass must not be {@literal null}.
	 * @return
	 */
	Mono<Long> count(Query query, Class<?> entityClass);

	/**
	 * Returns the number of documents for the given {@link Query} querying the given collection. The given {@link Query}
	 * must solely consist of document field references as we lack type information to map potential property references
	 * onto document fields. TO make sure the query gets mapped, use {@link #count(Query, Class, String)}.
	 *
	 * @param query
	 * @param collectionName must not be {@literal null} or empty.
	 * @return
	 * @see #count(Query, Class, String)
	 */
	Mono<Long> count(Query query, String collectionName);

	/**
	 * Returns the number of documents for the given {@link Query} by querying the given collection using the given entity
	 * class to map the given {@link Query}.
	 *
	 * @param query
	 * @param entityClass must not be {@literal null}.
	 * @param collectionName must not be {@literal null} or empty.
	 * @return
	 */
	Mono<Long> count(Query query, Class<?> entityClass, String collectionName);

	/**
	 * Insert the object into the collection for the entity type of the object to save.
	 * <p/>
	 * The object is converted to the MongoDB native representation using an instance of {@see MongoConverter}.
	 * <p/>
	 * If you object has an "Id' property, it will be set with the generated Id from MongoDB. If your Id property is a
	 * String then MongoDB ObjectId will be used to populate that string. Otherwise, the conversion from ObjectId to your
	 * property type will be handled by Spring's BeanWrapper class that leverages Type Conversion API. See
	 * <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/validation.html#core-convert" >
	 * Spring's Type Conversion"</a> for more details.
	 * <p/>
	 * <p/>
	 * Insert is used to initially store the object into the database. To update an existing object use the save method.
	 *
	 * @param objectToSave the object to store in the collection.
	 */
	Mono<Void> insert(Object objectToSave);

	/**
	 * Insert the object into the specified collection.
	 * <p/>
	 * The object is converted to the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * Insert is used to initially store the object into the database. To update an existing object use the save method.
	 *
	 * @param objectToSave the object to store in the collection
	 * @param collectionName name of the collection to store the object in
	 */
	Mono<Void> insert(Object objectToSave, String collectionName);

	/**
	 * Insert a Collection of objects into a collection in a single batch write to the database.
	 *
	 * @param batchToSave the list of objects to save.
	 * @param entityClass class that determines the collection to use
	 */
	Mono<Void> insert(Collection<? extends Object> batchToSave, Class<?> entityClass);

	/**
	 * Insert a list of objects into the specified collection in a single batch write to the database.
	 *
	 * @param batchToSave the list of objects to save.
	 * @param collectionName name of the collection to store the object in
	 */
	Mono<Void> insert(Collection<? extends Object> batchToSave, String collectionName);

	/**
	 * Insert a mixed Collection of objects into a database collection determining the collection name to use based on the
	 * class.
	 *
	 * @param objectsToSave the list of objects to save.
	 */
	Mono<Void> insertAll(Collection<? extends Object> objectsToSave);

	/**
	 * Insert the object into the collection for the entity type of the object to save.
	 * <p/>
	 * The object is converted to the MongoDB native representation using an instance of {@see MongoConverter}.
	 * <p/>
	 * If you object has an "Id' property, it will be set with the generated Id from MongoDB. If your Id property is a
	 * String then MongoDB ObjectId will be used to populate that string. Otherwise, the conversion from ObjectId to your
	 * property type will be handled by Spring's BeanWrapper class that leverages Type Conversion API. See
	 * <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/validation.html#core-convert" >
	 * Spring's Type Conversion"</a> for more details.
	 * <p/>
	 * <p/>
	 * Insert is used to initially store the object into the database. To update an existing object use the save method.
	 *
	 * @param objectToSave the object to store in the collection.
	 */
	Mono<Void> insert(Mono<? extends Object> objectToSave);

	/**
	 * Insert a Collection of objects into a collection in a single batch write to the database.
	 *
	 * @param batchToSave the publisher which provides objects to save.
	 * @param entityClass class that determines the collection to use
	 */
	Mono<Void> insert(Publisher<? extends Object> batchToSave, Class<?> entityClass);

	/**
	 * Insert a list of objects into the specified collection in a single batch write to the database.
	 *
	 * @param batchToSave the publisher which provides objects to save.
	 * @param collectionName name of the collection to store the object in
	 */
	Mono<Void> insert(Publisher<? extends Object> batchToSave, String collectionName);

	/**
	 * Insert a mixed Collection of objects into a database collection determining the collection name to use based on the
	 * class.
	 *
	 * @param objectsToSave the publisher which provides objects to save.
	 */
	Mono<Void> insertAll(Publisher<? extends Object> objectsToSave);

	/**
	 * Save the object to the collection for the entity type of the object to save. This will perform an insert if the
	 * object is not already present, that is an 'upsert'.
	 * <p/>
	 * The object is converted to the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * If you object has an "Id' property, it will be set with the generated Id from MongoDB. If your Id property is a
	 * String then MongoDB ObjectId will be used to populate that string. Otherwise, the conversion from ObjectId to your
	 * property type will be handled by Spring's BeanWrapper class that leverages Type Conversion API. See
	 * <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/validation.html#core-convert" >
	 * Spring's Type Conversion"</a> for more details.
	 *
	 * @param objectToSave the object to store in the collection
	 */
	Mono<Void> save(Object objectToSave);

	/**
	 * Save the object to the specified collection. This will perform an insert if the object is not already present, that
	 * is an 'upsert'.
	 * <p/>
	 * The object is converted to the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * If you object has an "Id' property, it will be set with the generated Id from MongoDB. If your Id property is a
	 * String then MongoDB ObjectId will be used to populate that string. Otherwise, the conversion from ObjectId to your
	 * property type will be handled by Spring's BeanWrapper class that leverages Type Cobnversion API. See <a
	 * http://docs.spring.io/spring/docs/current/spring-framework-reference/html/validation.html#core-convert">Spring's
	 * Type Conversion"</a> for more details.
	 *
	 * @param objectToSave the object to store in the collection
	 * @param collectionName name of the collection to store the object in
	 */
	Mono<Void> save(Object objectToSave, String collectionName);

		/**
	 * Save the object to the collection for the entity type of the object to save. This will perform an insert if the
	 * object is not already present, that is an 'upsert'.
	 * <p/>
	 * The object is converted to the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * If you object has an "Id' property, it will be set with the generated Id from MongoDB. If your Id property is a
	 * String then MongoDB ObjectId will be used to populate that string. Otherwise, the conversion from ObjectId to your
	 * property type will be handled by Spring's BeanWrapper class that leverages Type Conversion API. See
	 * <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/validation.html#core-convert" >
	 * Spring's Type Conversion"</a> for more details.
	 *
	 * @param objectToSave the object to store in the collection
	 */
	Mono<Void> save(Mono<? extends Object> objectToSave);

	/**
	 * Save the object to the specified collection. This will perform an insert if the object is not already present, that
	 * is an 'upsert'.
	 * <p/>
	 * The object is converted to the MongoDB native representation using an instance of {@see MongoConverter}. Unless
	 * configured otherwise, an instance of MappingMongoConverter will be used.
	 * <p/>
	 * If you object has an "Id' property, it will be set with the generated Id from MongoDB. If your Id property is a
	 * String then MongoDB ObjectId will be used to populate that string. Otherwise, the conversion from ObjectId to your
	 * property type will be handled by Spring's BeanWrapper class that leverages Type Cobnversion API. See <a
	 * http://docs.spring.io/spring/docs/current/spring-framework-reference/html/validation.html#core-convert">Spring's
	 * Type Conversion"</a> for more details.
	 *
	 * @param objectToSave the object to store in the collection
	 * @param collectionName name of the collection to store the object in
	 */
	Mono<Void> save(Mono<? extends Object> objectToSave, String collectionName);

	/**
	 * Performs an upsert. If no document is found that matches the query, a new document is created and inserted by
	 * combining the query document and the update document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be upserted
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing object
	 * @param entityClass class that determines the collection to use
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> upsert(Query query, Update update, Class<?> entityClass);

	/**
	 * Performs an upsert. If no document is found that matches the query, a new document is created and inserted by
	 * combining the query document and the update document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be updated
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing
	 *          object.
	 * @param collectionName name of the collection to update the object in
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> upsert(Query query, Update update, String collectionName);

	/**
	 * Performs an upsert. If no document is found that matches the query, a new document is created and inserted by
	 * combining the query document and the update document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be upserted
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing object
	 * @param entityClass class of the pojo to be operated on
	 * @param collectionName name of the collection to update the object in
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> upsert(Query query, Update update, Class<?> entityClass, String collectionName);

	/**
	 * Updates the first object that is found in the collection of the entity class that matches the query document with
	 * the provided update document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be updated
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing
	 *          object.
	 * @param entityClass class that determines the collection to use
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> updateFirst(Query query, Update update, Class<?> entityClass);

	/**
	 * Updates the first object that is found in the specified collection that matches the query document criteria with
	 * the provided updated document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be updated
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing
	 *          object.
	 * @param collectionName name of the collection to update the object in
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> updateFirst(Query query, Update update, String collectionName);

	/**
	 * Updates the first object that is found in the specified collection that matches the query document criteria with
	 * the provided updated document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be updated
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing
	 *          object.
	 * @param entityClass class of the pojo to be operated on
	 * @param collectionName name of the collection to update the object in
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> updateFirst(Query query, Update update, Class<?> entityClass, String collectionName);

	/**
	 * Updates all objects that are found in the collection for the entity class that matches the query document criteria
	 * with the provided updated document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be updated
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing
	 *          object.
	 * @param entityClass class that determines the collection to use
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> updateMulti(Query query, Update update, Class<?> entityClass);

	/**
	 * Updates all objects that are found in the specified collection that matches the query document criteria with the
	 * provided updated document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be updated
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing
	 *          object.
	 * @param collectionName name of the collection to update the object in
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> updateMulti(Query query, Update update, String collectionName);

	/**
	 * Updates all objects that are found in the collection for the entity class that matches the query document criteria
	 * with the provided updated document.
	 *
	 * @param query the query document that specifies the criteria used to select a record to be updated
	 * @param update the update document that contains the updated object or $ operators to manipulate the existing
	 *          object.
	 * @param entityClass class of the pojo to be operated on
	 * @param collectionName name of the collection to update the object in
	 * @return the WriteResult which lets you access the results of the previous write.
	 */
	Mono<UpdateResult> updateMulti(final Query query, final Update update, Class<?> entityClass, String collectionName);

	/**
	 * Remove the given object from the collection by id.
	 *
	 * @param object
	 */
	Mono<DeleteResult> remove(Object object);

	/**
	 * Removes the given object from the given collection.
	 *
	 * @param object
	 * @param collection must not be {@literal null} or empty.
	 */
	Mono<DeleteResult> remove(Object object, String collection);

	/**
	 * Remove the given object from the collection by id.
	 *
	 * @param object
	 */
	Mono<DeleteResult> remove(Mono<? extends Object> object);

	/**
	 * Removes the given object from the given collection.
	 *
	 * @param object
	 * @param collection must not be {@literal null} or empty.
	 */
	Mono<DeleteResult> remove(Mono<? extends Object> object, String collection);

	/**
	 * Remove all documents that match the provided query document criteria from the the collection used to store the
	 * entityClass. The Class parameter is also used to help convert the Id of the object if it is present in the query.
	 *
	 * @param query
	 * @param entityClass
	 */
	Mono<DeleteResult> remove(Query query, Class<?> entityClass);

	/**
	 * Remove all documents that match the provided query document criteria from the the collection used to store the
	 * entityClass. The Class parameter is also used to help convert the Id of the object if it is present in the query.
	 *
	 * @param query
	 * @param entityClass
	 * @param collectionName
	 */
	Mono<DeleteResult> remove(Query query, Class<?> entityClass, String collectionName);

	/**
	 * Remove all documents from the specified collection that match the provided query document criteria. There is no
	 * conversion/mapping done for any criteria using the id field.
	 *
	 * @param query the query document that specifies the criteria used to remove a record
	 * @param collectionName name of the collection where the objects will removed
	 */
	Mono<DeleteResult> remove(Query query, String collectionName);

	/**
	 * Returns and removes all documents form the specified collection that match the provided query.
	 *
	 * @param query
	 * @param collectionName
	 * @return
	 */
	<T> Flux<T> findAllAndRemove(Query query, String collectionName);

	/**
	 * Returns and removes all documents matching the given query form the collection used to store the entityClass.
	 *
	 * @param query
	 * @param entityClass
	 * @return
	 */
	<T> Flux<T> findAllAndRemove(Query query, Class<T> entityClass);

	/**
	 * Returns and removes all documents that match the provided query document criteria from the the collection used to
	 * store the entityClass. The Class parameter is also used to help convert the Id of the object if it is present in
	 * the query.
	 *
	 * @param query
	 * @param entityClass
	 * @param collectionName
	 * @return
	 */
	<T> Flux<T> findAllAndRemove(Query query, Class<T> entityClass, String collectionName);

	/**
	 * Returns the underlying {@link MongoConverter}.
	 *
	 * @return
	 */
	MongoConverter getConverter();

}
