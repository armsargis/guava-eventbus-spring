/*
 * Copyright (C) 2011 Sargis Harutyunyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.sargis.eventbus.config;

import com.google.common.eventbus.EventBus;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Set;

public class EventBusBuilder implements InitializingBean, DisposableBean {

    private EventBus eventBus;

    private Set<Object> handlers;

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Object handler : handlers) {
            eventBus.register(handler);
        }
    }

    @Override
    public void destroy() throws Exception {
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setHandlers(Set<Object> handlers) {
        this.handlers = handlers;
    }

}
