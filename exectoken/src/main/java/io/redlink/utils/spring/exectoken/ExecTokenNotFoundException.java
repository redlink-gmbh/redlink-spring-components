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

import java.io.IOException;

import org.bson.types.ObjectId;

public class ExecTokenNotFoundException extends IOException {

    private static final long serialVersionUID = -3237299350305694082L;

    private final ObjectId id;
    private final String name;

    public ExecTokenNotFoundException(String name) {
        super("A ExecutionToken[name: " + name + "] is not present");
        this.name = name;
        this.id  = null;
    }
    
    public ExecTokenNotFoundException(ObjectId id, String name) {
        super("A ExecutionToken with the id: " + id + "(name: " + name + ") is not present (maybe expired)");
        this.id = id;
        this.name = name;
    }
    
    public ObjectId getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
}
