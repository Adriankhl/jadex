/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jadex.commons.beans;

import jadex.commons.beans.Encoder;
import jadex.commons.beans.Expression;
import jadex.commons.beans.PersistenceDelegate;

class NullPersistenceDelegate extends PersistenceDelegate {
    @Override
    protected Expression instantiate(Object oldInstance, Encoder out) {
        return null;
    }

    @Override
    /*
     * It's unnecessary to do anything for initialization, because two mutatable
     * strings are actually equivalent already.
     */
    protected void initialize(Class<?> type, Object oldInstance,
            Object newInstance, Encoder out) {
        // do nothing
    }

    @Override
    public void writeObject(Object oldInstance, Encoder out) {
        // do nothing
    }
}
