package com.xtbn.trigger.http;

import com.google.adk.events.Event;
import com.xtbn.api.IAgentService;
import com.xtbn.api.dto.AiAgentConfigResponseDTO;
import com.xtbn.api.dto.ChatRequestDTO;
import com.xtbn.api.dto.ChatResponseDTO;
import com.xtbn.api.dto.CreateSessionRequestDTO;
import com.xtbn.api.dto.CreateSessionResponseDTO;
import com.xtbn.api.dto.HistoryMessageDTO;
import com.xtbn.api.dto.SessionDetailResponseDTO;
import com.xtbn.api.dto.SessionListItemDTO;
import com.xtbn.api.dto.SwitchSessionRequestDTO;
import com.xtbn.api.dto.SwitchSessionResponseDTO;
import com.xtbn.api.response.Response;
import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.chat.model.valobj.ChatSessionDetailVO;
import com.xtbn.domain.chat.service.IChatService;
import com.xtbn.types.enums.ResponseCode;
import com.xtbn.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@CrossOrigin(origins = "*")
public class AgentServiceController implements IAgentService {

    @Resource
    private IChatService chatService;

    @RequestMapping(value = "query_ai_agent_config_list", method = RequestMethod.GET)
    @Override
    public Response<List<AiAgentConfigResponseDTO>> queryAiAgentConfigList() {
        try {
            List<AgentConfigVO.RootAgent> agentConfigs = chatService.queryAgentConfigList();

            List<AiAgentConfigResponseDTO> responseDTOS = agentConfigs.stream().map(agentConfig -> {
                AiAgentConfigResponseDTO responseDTO = new AiAgentConfigResponseDTO();
                responseDTO.setAgentId(agentConfig.getRootAgentId());
                responseDTO.setAgentName(agentConfig.getRootAgentName());
                responseDTO.setAgentDesc(agentConfig.getRootAgentDesc());
                return responseDTO;
            }).collect(Collectors.toList());

            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTOS)
                    .build();

        } catch (AppException e) {
            log.error("query ai agent config list error", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("query ai agent config list failed", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "create_session", method = RequestMethod.POST)
    @Override
    public Response<CreateSessionResponseDTO> createSession(@RequestBody CreateSessionRequestDTO requestDTO) {
        try {
            boolean refresh = Boolean.TRUE.equals(requestDTO.getRefresh());
            String sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId(), refresh);

            CreateSessionResponseDTO responseDTO = new CreateSessionResponseDTO();
            responseDTO.setSessionId(sessionId);

            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("create session app error", e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("create session failed", e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "chat", method = RequestMethod.POST)
    @Override
    public Response<ChatResponseDTO> chat(@RequestBody ChatRequestDTO requestDTO) {
        try {
            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }

            String finalMessage = chatService.handleFinalMessage(
                    requestDTO.getAgentId(),
                    requestDTO.getUserId(),
                    sessionId,
                    requestDTO.getMessage()
            );

            ChatResponseDTO responseDTO = new ChatResponseDTO();
            responseDTO.setContent(finalMessage);

            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("chat app error", e);
            return Response.<ChatResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("chat failed", e);
            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "chat_stream", method = RequestMethod.POST, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Override
    public Flux<ServerSentEvent<Map<String, Object>>> chatStream(@RequestBody ChatRequestDTO requestDTO) {
        return chatService.handleMessageStream(
                        requestDTO.getAgentId(),
                        requestDTO.getUserId(),
                        requestDTO.getSessionId(),
                        requestDTO.getMessage()
                )
                .map(event -> ServerSentEvent.<Map<String, Object>>builder()
                        .event("message")
                        .data(buildStreamEvent(event))
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<Map<String, Object>>builder()
                        .event("done")
                        .data(Map.of("done", true))
                        .build()))
                .onErrorResume(throwable -> {
                    log.error("chat stream failed", throwable);
                    return Flux.just(ServerSentEvent.<Map<String, Object>>builder()
                            .event("error")
                            .data(Map.of("message", throwable.getMessage() == null ? "stream error" : throwable.getMessage()))
                            .build());
                });
    }

    @RequestMapping(value = "session_list", method = RequestMethod.GET)
    @Override
    public Response<List<SessionListItemDTO>> querySessionList(@RequestParam("userId") String userId) {
        try {
            List<SessionListItemDTO> items = chatService.querySessionList(userId).stream().map(session -> {
                SessionListItemDTO item = new SessionListItemDTO();
                item.setSessionId(session.getSessionId());
                item.setTitle(session.getTitle());
                item.setAgentId(session.getAgentId());
                item.setAgentName(session.getAgentName());
                item.setLastMessagePreview(session.getLastMessagePreview());
                item.setMessageCount(session.getMessageCount());
                item.setCreatedAt(session.getCreatedAt());
                item.setUpdatedAt(session.getUpdatedAt());
                item.setLastMessageAt(session.getLastMessageAt());
                return item;
            }).collect(Collectors.toList());

            return Response.<List<SessionListItemDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(items)
                    .build();
        } catch (AppException e) {
            log.error("query session list app error", e);
            return Response.<List<SessionListItemDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("query session list failed", e);
            return Response.<List<SessionListItemDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "session_detail", method = RequestMethod.GET)
    @Override
    public Response<SessionDetailResponseDTO> querySessionDetail(@RequestParam("userId") String userId, @RequestParam("sessionId") String sessionId) {
        try {
            return Response.<SessionDetailResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(toSessionDetailResponse(chatService.querySessionDetail(userId, sessionId)))
                    .build();
        } catch (AppException e) {
            log.error("query session detail app error", e);
            return Response.<SessionDetailResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("query session detail failed", e);
            return Response.<SessionDetailResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "switch_session", method = RequestMethod.POST)
    @Override
    public Response<SwitchSessionResponseDTO> switchSession(@RequestBody SwitchSessionRequestDTO requestDTO) {
        try {
            SessionDetailResponseDTO detail = toSessionDetailResponse(chatService.querySessionDetail(requestDTO.getUserId(), requestDTO.getSessionId()));
            SwitchSessionResponseDTO responseDTO = new SwitchSessionResponseDTO();
            responseDTO.setSessionId(detail.getSessionId());
            responseDTO.setTitle(detail.getTitle());
            responseDTO.setAgentId(detail.getAgentId());
            responseDTO.setAgentName(detail.getAgentName());
            responseDTO.setCreatedAt(detail.getCreatedAt());
            responseDTO.setUpdatedAt(detail.getUpdatedAt());
            responseDTO.setLastMessageAt(detail.getLastMessageAt());
            responseDTO.setMessageCount(detail.getMessageCount());
            responseDTO.setMessages(detail.getMessages());
            return Response.<SwitchSessionResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("switch session app error", e);
            return Response.<SwitchSessionResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("switch session failed", e);
            return Response.<SwitchSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    private Map<String, Object> buildStreamEvent(Event event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", event.id());
        payload.put("author", event.author());
        payload.put("content", event.stringifyContent());
        payload.put("partial", event.partial().orElse(false));
        payload.put("turnComplete", event.turnComplete().orElse(false));
        payload.put("finalResponse", event.finalResponse());
        payload.put("errorMessage", event.errorMessage().orElse(null));
        payload.put("timestamp", event.timestamp());
        return payload;
    }

    private SessionDetailResponseDTO toSessionDetailResponse(ChatSessionDetailVO sessionDetailVO) {
        SessionDetailResponseDTO responseDTO = new SessionDetailResponseDTO();
        responseDTO.setSessionId(sessionDetailVO.getSessionId());
        responseDTO.setTitle(sessionDetailVO.getTitle());
        responseDTO.setAgentId(sessionDetailVO.getAgentId());
        responseDTO.setAgentName(sessionDetailVO.getAgentName());
        responseDTO.setCreatedAt(sessionDetailVO.getCreatedAt());
        responseDTO.setUpdatedAt(sessionDetailVO.getUpdatedAt());
        responseDTO.setLastMessageAt(sessionDetailVO.getLastMessageAt());
        responseDTO.setMessageCount(sessionDetailVO.getMessageCount());
        responseDTO.setMessages(sessionDetailVO.getMessages().stream().map(message -> {
            HistoryMessageDTO dto = new HistoryMessageDTO();
            dto.setMessageId(message.getMessageId());
            dto.setRole(message.getRole());
            dto.setContent(message.getContent());
            dto.setCreatedAt(message.getCreatedAt());
            return dto;
        }).collect(Collectors.toList()));
        return responseDTO;
    }
}
