package com.xtbn.domain.agent.service.assmble.component.memory;

import com.google.adk.memory.BaseMemoryService;
import com.google.adk.memory.MemoryEntry;
import com.google.adk.memory.SearchMemoryResponse;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.xtbn.domain.agent.adapter.repository.IMemoryRepository;
import com.xtbn.domain.agent.model.entity.StoredMemoryEntity;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemoryService implements BaseMemoryService {

    private final IMemoryRepository repository;

    @Override
    public Completable addSessionToMemory(Session session) {
        return Completable.fromAction(() -> {
            long now = Instant.now().toEpochMilli();
            StoredMemoryEntity record = new StoredMemoryEntity();
            record.setAppName(session.appName());
            record.setUserId(session.userId());
            record.setSessionId(session.id());
            record.setMemoryText(buildMemoryText(session));
            record.setSource("session_summary");
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            repository.upsertMemory(record);
        });
    }

    @Override
    public Single<SearchMemoryResponse> searchMemory(String appName, String userId, String query) {
        return Single.fromCallable(() -> SearchMemoryResponse.builder()
                .memories(repository.searchMemories(appName, userId, query).stream()
                        .map(record -> MemoryEntry.builder()
                                .author(record.getSource())
                                .timestamp(Instant.ofEpochMilli(record.getUpdatedAt()))
                                .content(Content.fromParts(Part.fromText(record.getMemoryText() == null ? "" : record.getMemoryText())))
                                .build())
                        .collect(Collectors.toList()))
                .build());
    }

    private String buildMemoryText(Session session) {
        List<String> lines = session.events().stream()
                .map(event -> {
                    String content = event.stringifyContent();
                    if (content == null || content.trim().isEmpty()) {
                        return null;
                    }
                    String author = event.author() == null ? "assistant" : event.author();
                    return author + ": " + content.trim();
                })
                .filter(line -> line != null && !line.isBlank())
                .collect(Collectors.toList());

        if (lines.isEmpty()) {
            return "";
        }

        if (lines.size() <= 12) {
            return String.join("\n", lines);
        }

        return String.join("\n", lines.subList(lines.size() - 12, lines.size()));
    }
}
