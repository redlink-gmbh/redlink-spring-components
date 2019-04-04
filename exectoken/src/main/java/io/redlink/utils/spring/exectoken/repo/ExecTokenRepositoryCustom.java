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

import java.time.Duration;

import io.redlink.utils.spring.exectoken.ExecTokenNotFoundException;
import io.redlink.utils.spring.exectoken.ExecutionLockedException;

public interface ExecTokenRepositoryCustom {
    
    public Token obtain(String name, Duration expireDuration) throws ExecutionLockedException;

    public void release(Token lock) throws ExecTokenNotFoundException;
    
    public void releaseQuietly(Token lock);

    public Token renew(Token lock, Duration expireDuration);

}
