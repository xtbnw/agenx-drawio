package com.xtbn.config;

import com.xtbn.infrastructure.adapter.safety.GovernanceScriptExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class GovernanceConfig {

    @Bean
    public DefaultRedisScript<Long> governanceTokenBucketScript() {
        return script("lua/governance_token_bucket.lua");
    }

    @Bean
    public DefaultRedisScript<Long> governanceConcurrencyAcquireScript() {
        return script("lua/governance_concurrency_acquire.lua");
    }

    @Bean
    public DefaultRedisScript<Long> governanceConcurrencyReleaseScript() {
        return script("lua/governance_concurrency_release.lua");
    }

//    @Bean(destroyMethod = "shutdown")
//    public RedissonClient redissonClient(RedisProperties redisProperties) {
//        Config config = new Config();
//        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
//        config.useSingleServer()
//                .setAddress(address)
//                .setDatabase(redisProperties.getDatabase());
//        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
//            config.useSingleServer().setPassword(redisProperties.getPassword());
//        }
//        return Redisson.create(config);
//    }

    @Bean
    public GovernanceScriptExecutor governanceScriptExecutor(StringRedisTemplate stringRedisTemplate,
                                                             @Qualifier("governanceTokenBucketScript") DefaultRedisScript<Long> governanceTokenBucketScript,
                                                             @Qualifier("governanceConcurrencyAcquireScript") DefaultRedisScript<Long> governanceConcurrencyAcquireScript,
                                                             @Qualifier("governanceConcurrencyReleaseScript") DefaultRedisScript<Long> governanceConcurrencyReleaseScript) {
        return new GovernanceScriptExecutor(
                stringRedisTemplate,
                governanceTokenBucketScript,
                governanceConcurrencyAcquireScript,
                governanceConcurrencyReleaseScript
        );
    }

    private DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
