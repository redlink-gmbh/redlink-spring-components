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
package io.redlink.utils.spring.exectoken;

import java.time.Duration;

import org.springframework.stereotype.Service;

import io.redlink.utils.spring.exectoken.repo.ExecTokenRepository;
import io.redlink.utils.spring.exectoken.repo.Token;

@Service
public class ExecutionTokenService {

    
    private final ExecTokenRepository lockRepo;
    
    public ExecutionTokenService(ExecTokenRepository lockRepo){
        this.lockRepo = lockRepo;
    }
    
    
    public ExecutionToken obtain(String name) throws ExecutionLockedException{
        if(name == null || name.length() < 1){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor blank!");
        }
        return new ExecutionToken(lockRepo, ExecutionToken.DEFAULT_EXPIRE_DURATION, 
                lockRepo.obtain(name, ExecutionToken.DEFAULT_EXPIRE_DURATION));
    }
    
    public ExecutionToken obtain(String name, Duration expireDuration) throws ExecutionLockedException {
        if(name == null || name.length() < 1){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor blank!");
        }
        if(expireDuration == null){
            expireDuration = ExecutionToken.DEFAULT_EXPIRE_DURATION;
        } else if(expireDuration.isNegative() || expireDuration.isZero()){
            throw new IllegalArgumentException("The expire duration MUST NOT be negative or zero!");
        }
        return new ExecutionToken(lockRepo, expireDuration, lockRepo.obtain(name, expireDuration));
    }

    public boolean isLocked(String name){
        if(name == null || name.length() < 1){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor blank!");
        }
        return lockRepo.existsByName(name);
    }

    public Token getLockToken(String name){
        if(name == null || name.length() < 1){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor blank!");
        }
        return lockRepo.findByName(name);
    }
    
    
}
