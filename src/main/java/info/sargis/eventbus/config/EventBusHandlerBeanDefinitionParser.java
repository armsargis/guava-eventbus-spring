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

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Pattern;

public class EventBusHandlerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private static final String BASE_PACKAGE_ATTRIBUTE = "base-package";

    private static final String EXCLUDE_FILTER_ELEMENT = "exclude";

    private static final String INCLUDE_FILTER_ELEMENT = "include";

    private static final String FILTER_TYPE_ATTRIBUTE = "type";

    private static final String FILTER_EXPRESSION_ATTRIBUTE = "expression";

    public static final String XSD_ATTR_EVENTBUS = "eventbus-ref";

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        builder.addPropertyValue("eventBus", getEventBusBeanDefintion(element));
        builder.addPropertyValue("handlers", getHandlers(element, parserContext));
    }

    @Override
    protected boolean shouldGenerateId() {
        return true;
    }

    @Override
    protected String getBeanClassName(Element element) {
        return "info.sargis.eventbus.config.EventBusBuilder";
    }

    private RuntimeBeanReference getEventBusBeanDefintion(Element element) {
        return new RuntimeBeanReference(getEventBusRefName(element));
    }

    protected Set<RuntimeBeanReference> getHandlers(Element element, ParserContext parserContext) {

        String[] basePackages = StringUtils.tokenizeToStringArray(
                element.getAttribute(BASE_PACKAGE_ATTRIBUTE), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS
        );

        ClassPathBeanDefinitionScanner scanner = configureScanner(parserContext, element);
        Set<RuntimeBeanReference> candidates = new ManagedSet<RuntimeBeanReference>(32);

        for (String basePackage : basePackages) {
            Set<BeanDefinition> components = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition component : components) {
                if (isEventBusHandlerCandidate(component, parserContext)) {
                    candidates.add(defineRuntimeBeanReference(parserContext, component));
                } else {
                    logger.warn(String.format(
                            "Found EventBus handler candidate: %s, but without @Subscribe annotation on any public method", component
                    ));
                }
            }
        }

        return candidates;
    }

    private boolean isEventBusHandlerCandidate(BeanDefinition component, ParserContext parserContext) {
        try {
            String beanClassName = component.getBeanClassName();
            Class<?> beanClass = Class.forName(beanClassName);

            for (Method method : beanClass.getMethods()) {
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                if (annotation != null) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error("", e);
        }

        return false;
    }

    private String getEventBusRefName(Element element) {
        return element.getAttribute(XSD_ATTR_EVENTBUS);
    }

    private RuntimeBeanReference defineRuntimeBeanReference(ParserContext parserContext, BeanDefinition beanDefinition) {
        String generatedBeanName = getGeneratedBeanName(parserContext, beanDefinition);

        parserContext.getRegistry().registerBeanDefinition(generatedBeanName, beanDefinition);
        return new RuntimeBeanReference(generatedBeanName);
    }

    private String getGeneratedBeanName(ParserContext parserContext, BeanDefinition component) {
        return parserContext.getReaderContext().generateBeanName(component);
    }

    protected ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
        XmlReaderContext readerContext = parserContext.getReaderContext();

        // Delegate bean definition registration to scanner class.
        ClassPathBeanDefinitionScanner scanner = createScanner(readerContext, false);
        scanner.setResourceLoader(readerContext.getResourceLoader());
        scanner.setBeanDefinitionDefaults(parserContext.getDelegate().getBeanDefinitionDefaults());
        scanner.setAutowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns());

        parseTypeFilters(element, scanner, readerContext, parserContext);

        return scanner;
    }

    protected ClassPathBeanDefinitionScanner createScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
        return new ClassPathBeanDefinitionScanner(readerContext.getRegistry(), useDefaultFilters);
    }

    protected void parseTypeFilters(Element element, ClassPathBeanDefinitionScanner scanner, XmlReaderContext readerContext, ParserContext parserContext) {

        // Parse exclude and include filter elements.
        ClassLoader classLoader = scanner.getResourceLoader().getClassLoader();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String localName = parserContext.getDelegate().getLocalName(node);
                try {
                    if (INCLUDE_FILTER_ELEMENT.equals(localName)) {
                        TypeFilter typeFilter = createTypeFilter((Element) node, classLoader);
                        scanner.addIncludeFilter(typeFilter);
                    } else if (EXCLUDE_FILTER_ELEMENT.equals(localName)) {
                        TypeFilter typeFilter = createTypeFilter((Element) node, classLoader);
                        scanner.addExcludeFilter(typeFilter);
                    }
                } catch (Exception ex) {
                    readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected TypeFilter createTypeFilter(Element element, ClassLoader classLoader) {
        String filterType = element.getAttribute(FILTER_TYPE_ATTRIBUTE);
        String expression = element.getAttribute(FILTER_EXPRESSION_ATTRIBUTE);
        try {
            if ("annotation".equals(filterType)) {
                return new AnnotationTypeFilter((Class<Annotation>) classLoader.loadClass(expression));
            } else if ("assignable".equals(filterType)) {
                return new AssignableTypeFilter(classLoader.loadClass(expression));
            } else if ("regex".equals(filterType)) {
                return new RegexPatternTypeFilter(Pattern.compile(expression));
            } else if ("custom".equals(filterType)) {
                Class filterClass = classLoader.loadClass(expression);
                if (!TypeFilter.class.isAssignableFrom(filterClass)) {
                    throw new IllegalArgumentException(
                            "Class is not assignable to [" + TypeFilter.class.getName() + "]: " + expression);
                }
                return (TypeFilter) BeanUtils.instantiateClass(filterClass);
            } else {
                throw new IllegalArgumentException("Unsupported filter type: " + filterType);
            }
        } catch (ClassNotFoundException ex) {
            throw new FatalBeanException("Type filter class not found: " + expression, ex);
        }
    }

}
