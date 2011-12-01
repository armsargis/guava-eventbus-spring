This project provides Spring schema-based extension for scanning and registering Guava EventBus event handlers

# How to build

Clone from GIT and then use Gradle:

    $ git clone ...
    $ gradle build (or './gradlew build' if you don't have gradle in your path)

# How to use

Here is simple example:

public interface EventHandler<T> {
    void handle(T event);
}

<strong>implementation:</strong>

    public class AlertEventHandler implements EventHandler<String> {

        private static final Logger LOGGER = LoggerFactory.getLogger(AlertEventHandler.class);

        @Override
        @Subscribe
        public void handle(String event) {
            LOGGER.info("GOT EVENT: {}", event);
        }
    }

<strong>spring definition:</strong>

    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:eventbus="http://sargis.info/eventbus"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
           http://sargis.info/eventbus http://sargis.info/eventbus/eventbus-spring-1.0.xsd">

        <eventbus:handler-scan base-package="info.sargis.spring.eventbus" eventbus-ref="eventBus">
            <eventbus:include type="assignable" expression="info.sargis.spring.eventbus.EventHandler"/>
        </eventbus:handler-scan>

        <bean id="eventBus" class="com.google.common.eventbus.EventBus"/>

    </beans>

<strong>test application:</strong>

    public class GuavaEventBusTest {
        public static void main(String[] args) {
            GenericApplicationContext applicationContext = .... // Starting spring context

            EventBus bean = applicationContext.getBean(EventBus.class);
            bean.post("Hello Guava EventBus");
        }
    }

<strong>and I have output:</strong>

20:17:33.646 [main] INFO  i.s.s.eventbus.AlertEventHandler - GOT EVENT: Hello Guava EventBus

