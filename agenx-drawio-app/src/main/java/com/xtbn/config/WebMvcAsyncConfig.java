package com.xtbn.config;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class WebMvcAsyncConfig implements WebMvcConfigurer {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        AsyncTaskExecutor taskExecutor = new TaskExecutorAdapter(threadPoolExecutor);
        configurer.setTaskExecutor(taskExecutor);
        configurer.setDefaultTimeout(3 * 60 * 1000L);
    }
}
