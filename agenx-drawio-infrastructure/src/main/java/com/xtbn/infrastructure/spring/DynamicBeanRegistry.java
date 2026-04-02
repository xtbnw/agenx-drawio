package com.xtbn.infrastructure.spring;

import com.xtbn.domain.agent.adapter.port.registry.IBeanRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DynamicBeanRegistry implements IBeanRegistry {

    @Resource
    private ApplicationContext applicationContext;

    public synchronized <T> void registerBean(String beanName, Class<T> beanClass, T beanInstance) {
        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        BeanDefinitionBuilder beanDefinitionBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(beanClass, () -> beanInstance);
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);

        if (beanFactory.containsBeanDefinition(beanName)) {
            beanFactory.removeBeanDefinition(beanName);
            log.info("移除已存在 Bean: {}", beanName);
        }

        beanFactory.registerBeanDefinition(beanName, beanDefinition);
        log.info("成功注册 Bean: {}", beanName);
    }

    public synchronized void unregisterBean(String beanName) {
        DefaultListableBeanFactory beanFactory =
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        if (beanFactory.containsBeanDefinition(beanName)) {
            beanFactory.removeBeanDefinition(beanName);
            log.info("成功移除 Bean: {}", beanName);
        }
    }

    public boolean containsBean(String beanName) {
        return applicationContext.containsBean(beanName);
    }

    public <T> T getBean(String beanName, Class<T> requiredType) {
        return applicationContext.getBean(beanName, requiredType);
    }

    public Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    public <T> T getBean(Class<T> requiredType) {
        return applicationContext.getBean(requiredType);
    }

    public <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
        return BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, requiredType);
    }
}