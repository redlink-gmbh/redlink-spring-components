/**
 *    Copyright 2019 redlink GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.redlink.utils.spring.exectoken.repo;

import static io.redlink.utils.spring.exectoken.repo.ExecTokenRepository.*;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import io.redlink.utils.spring.exectoken.repo.ExecTokenRepository;

@Document(collection="execToken")
public final class Token {

    /**
     * The default expire duration (<code>5min</code>)
     */
    public static final Duration DEFAULT_EXPIRE_DURATION = Duration.ofMinutes(5);
    
    @Id
    private final ObjectId id;
    @Field(FIELD_NAME)
    @Indexed(unique=true)
    private final String name;
    @Field(FIELD_CREATED)
    private final Date created;
    @Indexed(direction=IndexDirection.DESCENDING)
    @Field(FIELD_MODIFIED)
    private final Date modified;
    @Field(FIELD_EXPIRES)
    @Indexed(expireAfterSeconds=0)
    private final Date expires;
    @Field(FIELD_PROPERTIES)
    private Map<String,Object> properties = new HashMap<>();
    
    @Transient
    private ExecTokenRepository lockRepo;
    
    @PersistenceConstructor
    protected Token(ObjectId id, String name, Date created, Date modified, Date expires) {
        this.id = id;
        this.name = name;
        this.created = created;
        this.modified = modified;
        this.expires = expires;
    }

    public Date getCreated() {
        return created;
    }
    
    public Date getModified() {
        return modified;
    }
    
    public Date getExpires() {
        return expires;
    }
    
    public ObjectId getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties == null ? new HashMap<String, Object>() : properties;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Token other = (Token) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Token [id=" + id + ", name=" + name + ", created=" + created + ", modified=" + modified + ", expires="
                + expires + ", properties=" + properties + "]";
    }
    
}
