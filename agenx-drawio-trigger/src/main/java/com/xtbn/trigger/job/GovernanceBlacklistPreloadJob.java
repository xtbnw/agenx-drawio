package com.xtbn.trigger.job;

import com.xtbn.domain.agent.adapter.repository.IGovernanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GovernanceBlacklistPreloadJob implements ApplicationListener<ApplicationReadyEvent> {
    private final IGovernanceRepository governanceRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        reload();
    }

    @Scheduled(fixedDelay = 300000L, initialDelay = 300000L)
    public void reload() {
        try {
            governanceRepository.preloadBlacklist();
        } catch (Exception e) {
            log.warn("reload governance blacklist failed", e);
        }
    }
}
