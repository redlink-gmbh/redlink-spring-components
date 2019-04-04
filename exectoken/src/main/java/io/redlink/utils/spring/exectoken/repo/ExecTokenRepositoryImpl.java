/*
 * Copyright 2019 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.redlink.utils.spring.exectoken.repo;

import static io.redlink.utils.spring.exectoken.repo.ExecTokenRepository.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.WriteResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import io.redlink.utils.spring.exectoken.ExecTokenNotFoundException;
import io.redlink.utils.spring.exectoken.ExecutionLockedException;

public class ExecTokenRepositoryImpl implements ExecTokenRepositoryCustom {

    
    private final MongoTemplate mongoTemplate;

    public ExecTokenRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    @SuppressWarnings("squid:S2583")
    public Token obtain(String name, Duration expireDuration) throws ExecutionLockedException {
        if(name == null || name.length() < 1){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor blank!");
        }
        if(expireDuration == null || expireDuration.isNegative() || expireDuration.isZero()){
            throw new IllegalArgumentException("The expireDuration MUST NOT be NULL, negative nor zero!");
        }
        Date now = new Date();
        Date expires = new Date(now.getTime() + expireDuration.toMillis());
        Query query = queryByName(name);
        
        //we have different upsert APIs for different supported Spring versions :(
        Object result = mongoTemplate.upsert(
                query, 
                new Update()
                    .setOnInsert(FIELD_CREATED, now)
                    .setOnInsert(FIELD_MODIFIED, now)
                    .setOnInsert(FIELD_EXPIRES, expires)
                    .setOnInsert(FIELD_PROPERTIES, Collections.emptyMap()),
                Token.class);
        final Object upsertedId;
        if(result instanceof WriteResult) { //spring-data-mongodb < 2
            upsertedId = ((WriteResult)result).getUpsertedId();
        } else if(result instanceof UpdateResult) { //spring-data-mongodb 2+
            upsertedId = ((UpdateResult)result).getUpsertedId();
        } else {
            throw new IllegalStateException("Unexpected return type of Upsert Request (type: " 
                    + result.getClass().getName() + ")!"); 
        }
        if(upsertedId != null){
            return mongoTemplate.findById(upsertedId, Token.class);
        } else {
            throw new ExecutionLockedException(mongoTemplate.findOne(queryByName(name), Token.class));
        }
    }

    private Query queryByName(String name) {
        return Query.query(Criteria.where("name").is(name));
    }

    @Override
    public void release(Token lock) throws ExecTokenNotFoundException {
        if(!releaseInternal(lock)){
            throw new ExecTokenNotFoundException(lock.getId(), lock.getName());
        }
    }

    @Override
    public void releaseQuietly(Token lock) {
        releaseInternal(lock);
        
    }

    private boolean releaseInternal(Token lock) {
        if(lock == null || lock.getId() == null){
            throw new IllegalArgumentException("The parsed Lock MUST NOT be NULL and MUST have an ID");
        }
        Object result = mongoTemplate.remove(lock);
        if(result instanceof WriteResult) { //spring-data-mongodb < 2
            return ((WriteResult)result).getN() > 0;
        } else if(result instanceof DeleteResult) { //spring-data-mongodb 2+
            return ((DeleteResult)result).getDeletedCount() > 0;
        } else {
            throw new IllegalStateException("Unexpected return type of Upsert Request (type: " 
                    + result.getClass().getName() + ")!"); 
        }
    }

    @Override
    public Token renew(Token lock, Duration expireDuration)  {
        if(expireDuration == null || expireDuration.isNegative() || expireDuration.isZero()){
            throw new IllegalArgumentException("The parsed expireDuration MUST NOT be NULL, negative nor zero!");
        }
        if(lock == null || lock.getId() == null){
            throw new IllegalArgumentException("The parsed Lock MUST NOT be NULL and MUST have an ID");
        }
        Date now = new Date();
        Date expires = new Date(now.getTime() + expireDuration.toMillis());
        return mongoTemplate.findAndModify(
                Query.query(Criteria.where(FIELD_ID).is(lock.getId())), 
                Update.update(FIELD_MODIFIED, now)
                    .set(FIELD_EXPIRES, expires),
                FindAndModifyOptions.options().returnNew(true),
                Token.class);
    }
    
}
