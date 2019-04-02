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
package io.redlink.utils.spring.exectoken;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.redlink.utils.spring.exectoken.repo.ExecTokenRepository;
import io.redlink.utils.spring.exectoken.repo.Token;

public final class ExecutionToken implements AutoCloseable {

    /**
     * The default expire duration (<code>5 minutes</code>)
     */
    public static final Duration DEFAULT_EXPIRE_DURATION = Duration.ofMinutes(5);
    
    @JsonIgnore
    private Token lockToken;
    
    @JsonIgnore
    private final Duration expireDuration;

    @JsonIgnore
    private final ExecTokenRepository lockRepo;
    
    private boolean releaseOnClose = true;
    
    ExecutionToken(ExecTokenRepository lockRepo, Duration expireDuration, Token token) {
        this.lockRepo = lockRepo;
        this.expireDuration = expireDuration;
        this.lockToken = token;
    }

    public Date getObtained(){
        return lockToken.getCreated();
    }
    
    public Date getDate(){
        return lockToken.getModified();
    }
    
    public Date getExpires() {
        return lockToken.getExpires();
    }
    
    public ObjectId getId() {
        return lockToken.getId();
    }
    
    public String getName() {
        return lockToken.getName();
    }
    
    public Map<String, Object> getProperties() {
        return lockToken.getProperties();
    }
    
    public void setReleaseOnClose(boolean releaseOnClose) {
        this.releaseOnClose = releaseOnClose;
    }
    
    public boolean isReleaseOnClose() {
        return releaseOnClose;
    }
    
    Token getLockToken() {
        return lockToken;
    }
    /**
     * Renews this lock by updating the {@link ExecutionToken#getExpires() expiration date}
     * @throws ExecTokenNotFoundException if the token was already expired
     */
    public void renew() throws ExecTokenNotFoundException {
        Token renewed = lockRepo.renew(lockToken, expireDuration);
        if(renewed == null){
            throw new ExecTokenNotFoundException(lockToken.getId(), lockToken.getName());
        } else {
            lockToken = renewed;
        }
    }

    @Override
    public void close() throws IOException {
        if(!releaseOnClose){
            return;
        }
        if(lockRepo != null){
            lockRepo.releaseQuietly(lockToken);
        } else {
            throw new IOException("Unable to release Lock as LockRepository is not available");
        }
        
    }
}
