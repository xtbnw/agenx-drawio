package com.xtbn.domain.agent.adapter.port.registry;

import java.util.Map;

public interface IBeanRegistry {
    <T> void registerBean(String beanName, Class<T> beanClass, T beanInstance);

    void unregisterBean(String beanName);

    boolean containsBean(String beanName);

    <T> T getBean(String beanName, Class<T> requiredType);

    Object getBean(String beanName);

    <T> T getBean(Class<T> requiredType);

    <T> Map<String, T> getBeansOfType(Class<T> requiredType);
}
