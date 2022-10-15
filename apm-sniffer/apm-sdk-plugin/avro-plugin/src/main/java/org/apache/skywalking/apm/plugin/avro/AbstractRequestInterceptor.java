/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.avro;

import java.io.IOException;
import java.lang.reflect.Method;
import org.apache.avro.Protocol;
import org.apache.avro.ipc.Requestor;
import org.apache.avro.ipc.Transceiver;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;

public abstract class AbstractRequestInterceptor implements InstanceConstructorInterceptor, InstanceMethodsAroundInterceptor {
    private static final ILog LOGGER = LogManager.getLogger(GenericRequestorInterceptor.class);

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        if (objInst.getSkyWalkingDynamicField() == null) {
            Requestor requestor = (Requestor) objInst;
            requestor.addRPCPlugin(new SWClientRPCPlugin());

            Protocol protocol = (Protocol) allArguments[0];
            Transceiver transceiver = (Transceiver) allArguments[1];
            try {
                objInst.setSkyWalkingDynamicField(new AvroInstance(protocol.getNamespace() + "." + protocol.getName() + ".", transceiver
                    .getRemoteName()));
            } catch (IOException e) {
                objInst.setSkyWalkingDynamicField(new AvroInstance("Undefined", "Undefined"));
                LOGGER.error("Failed to get Avro Remote Client Information.", e);
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(t);
        }
    }
}
