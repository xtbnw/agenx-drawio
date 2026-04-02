package com.xtbn.api;

import com.xtbn.api.dto.*;
import com.xtbn.api.response.Response;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface IAgentService {

    Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList();

    Response<CreateSessionResponseDTO> createSession(CreateSessionRequestDTO requestDTO);

    Response<ChatResponseDTO> chat(ChatRequestDTO requestDTO);

    Flux<ServerSentEvent<Map<String, Object>>> chatStream(ChatRequestDTO requestDTO);

}
