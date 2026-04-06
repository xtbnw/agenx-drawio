(function () {
  const { createElement: h, useEffect, useMemo, useRef, useState } = React;

  const API_BASE = "/api/v1";
  const STORAGE_KEYS = {
    userId: "agent-chat-user-id",
    agentId: "agent-chat-agent-id",
    sessionId: "agent-chat-session-id",
    sidebarCollapsed: "agent-chat-sidebar-collapsed"
  };
  const QUICK_PROMPTS = [
    { id: "intro", label: "自我介绍", value: "请先做一个简短的自我介绍。" },
    { id: "ability", label: "能力说明", value: "请告诉我你能帮我做什么。" },
    { id: "case", label: "测试建议", value: "给我一个适合测试当前 Agent 的示例问题。" }
  ];

  marked.setOptions({ gfm: true, breaks: true });

  function App() {
    const [agents, setAgents] = useState([]);
    const [selectedAgentId, setSelectedAgentId] = useState(localStorage.getItem(STORAGE_KEYS.agentId) || "");
    const [userId, setUserId] = useState(localStorage.getItem(STORAGE_KEYS.userId) || generateUserId());
    const [sessionId, setSessionId] = useState(localStorage.getItem(STORAGE_KEYS.sessionId) || "");
    const [sessions, setSessions] = useState([]);
    const [messages, setMessages] = useState([]);
    const [composerValue, setComposerValue] = useState("");
    const [activePromptId, setActivePromptId] = useState("");
    const [status, setStatus] = useState({ type: "ready", text: "准备就绪" });
    const [isSending, setIsSending] = useState(false);
    const [sidebarCollapsed, setSidebarCollapsed] = useState(localStorage.getItem(STORAGE_KEYS.sidebarCollapsed) === "true");
    const [userPopoverOpen, setUserPopoverOpen] = useState(false);

    const chatScrollRef = useRef(null);
    const textareaRef = useRef(null);
    const userPopoverRef = useRef(null);
    const selectedAgent = agents.find((agent) => agent.agentId === selectedAgentId) || null;
    const activeSession = sessions.find((item) => item.sessionId === sessionId) || null;

    useEffect(() => {
      loadAgents();
    }, []);

    useEffect(() => {
      localStorage.setItem(STORAGE_KEYS.userId, userId);
      refreshSessions(true);
    }, [userId]);

    useEffect(() => {
      if (selectedAgentId) {
        localStorage.setItem(STORAGE_KEYS.agentId, selectedAgentId);
      } else {
        localStorage.removeItem(STORAGE_KEYS.agentId);
      }
    }, [selectedAgentId]);

    useEffect(() => {
      if (sessionId) {
        localStorage.setItem(STORAGE_KEYS.sessionId, sessionId);
      } else {
        localStorage.removeItem(STORAGE_KEYS.sessionId);
      }
    }, [sessionId]);

    useEffect(() => {
      localStorage.setItem(STORAGE_KEYS.sidebarCollapsed, String(sidebarCollapsed));
    }, [sidebarCollapsed]);

    useEffect(() => {
      if (chatScrollRef.current) {
        chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
      }
    }, [messages]);

    useEffect(() => {
      autoResizeTextarea(textareaRef.current);
    }, [composerValue]);

    useEffect(() => {
      function handlePointerDown(event) {
        if (userPopoverRef.current && !userPopoverRef.current.contains(event.target)) {
          setUserPopoverOpen(false);
        }
      }

      document.addEventListener("pointerdown", handlePointerDown);
      return () => document.removeEventListener("pointerdown", handlePointerDown);
    }, []);

    async function loadAgents() {
      setStatus({ type: "loading", text: "加载中..." });
      try {
        const result = await requestJson(`${API_BASE}/query_ai_agent_config_list`);
        const nextAgents = Array.isArray(result.data) ? result.data : [];
        const nextSelected = nextAgents.find((agent) => agent.agentId === selectedAgentId) || nextAgents[0] || null;
        setAgents(nextAgents);
        setSelectedAgentId(nextSelected ? nextSelected.agentId : "");
        setStatus({ type: nextAgents.length ? "ready" : "error", text: nextAgents.length ? "已连接" : "暂无 Agent" });
      } catch (error) {
        setAgents([]);
        setSelectedAgentId("");
        setStatus({ type: "error", text: error.message || "加载失败" });
      }
    }

    async function refreshSessions(silent) {
      if (!userId) {
        setSessions([]);
        return;
      }
      if (!silent) {
        setStatus({ type: "loading", text: "加载会话中..." });
      }
      try {
        const result = await requestJson(`${API_BASE}/session_list?userId=${encodeURIComponent(userId)}`);
        const nextSessions = Array.isArray(result.data) ? result.data : [];
        setSessions(nextSessions);

        if (sessionId && !messages.length) {
          const cached = nextSessions.find((item) => item.sessionId === sessionId);
          if (cached) {
            await loadSessionDetail(sessionId, true);
          } else if (!silent) {
            setSessionId("");
            setMessages([]);
          }
        }

        if (!silent) {
          setStatus({ type: "ready", text: "已同步历史会话" });
        }
      } catch (error) {
        if (!silent) {
          setStatus({ type: "error", text: error.message || "加载历史会话失败" });
        }
      }
    }

    async function createSession(agentId, nextUserId) {
      const result = await requestJson(`${API_BASE}/create_session`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          agentId,
          userId: nextUserId
        })
      });

      return result && result.data ? result.data.sessionId || "" : "";
    }

    async function loadSessionDetail(nextSessionId, silent) {
      if (!nextSessionId) {
        return;
      }
      if (!silent) {
        setStatus({ type: "loading", text: "加载会话中..." });
      }
      const result = await requestJson(`${API_BASE}/session_detail?userId=${encodeURIComponent(userId)}&sessionId=${encodeURIComponent(nextSessionId)}`);
      const detail = result.data || {};
      setSessionId(detail.sessionId || nextSessionId);
      if (detail.agentId) {
        setSelectedAgentId(detail.agentId);
      }
      setMessages((detail.messages || []).map(historyMessageToBubble));
      if (!silent) {
        setStatus({ type: "ready", text: "已恢复历史会话" });
      }
    }

    async function switchSession(nextSession) {
      setStatus({ type: "loading", text: "切换会话中..." });
      try {
        const result = await requestJson(`${API_BASE}/switch_session`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            userId,
            sessionId: nextSession.sessionId
          })
        });

        const detail = result.data || {};
        setSessionId(detail.sessionId || nextSession.sessionId);
        setSelectedAgentId(detail.agentId || selectedAgentId);
        setMessages((detail.messages || []).map(historyMessageToBubble));
        setStatus({ type: "ready", text: "已切换历史会话" });
        if (window.innerWidth <= 980) {
          setSidebarCollapsed(true);
        }
      } catch (error) {
        setStatus({ type: "error", text: error.message || "切换会话失败" });
      }
    }

    async function startNewSession() {
      if (!selectedAgentId) {
        setStatus({ type: "error", text: "请先选择 Agent" });
        return;
      }

      setStatus({ type: "loading", text: "创建会话中..." });
      try {
        const nextSessionId = await createSession(selectedAgentId, userId);
        setSessionId(nextSessionId);
        setMessages([]);
        setUserPopoverOpen(false);
        await refreshSessions(true);
        setStatus({ type: "ready", text: "新会话已创建" });
      } catch (error) {
        setStatus({ type: "error", text: error.message || "创建会话失败" });
      }
    }

    async function handleAgentSelect(agentId) {
      setSelectedAgentId(agentId);
      setSessionId("");
      setMessages([]);
      setStatus({ type: "ready", text: "已切换 Agent，发送消息或创建新会话继续" });
      if (window.innerWidth <= 980) {
        setSidebarCollapsed(true);
      }
    }

    async function handleSend() {
      const message = composerValue.trim();
      const currentUserId = userId.trim();
      let currentSessionId = sessionId.trim();

      if (isSending) {
        return;
      }

      if (!selectedAgent) {
        setStatus({ type: "error", text: "请选择 Agent" });
        return;
      }

      if (!currentUserId) {
        setStatus({ type: "error", text: "请输入 userId" });
        return;
      }

      if (!message) {
        setStatus({ type: "error", text: "请输入消息" });
        return;
      }

      const userMessage = createMessage("user", message);
      const assistantMessage = createMessage("assistant", "");

      setMessages((prev) => prev.concat([userMessage, { ...assistantMessage, loading: true }]));
      setComposerValue("");
      setActivePromptId("");
      setIsSending(true);
      setStatus({ type: "loading", text: "回复中..." });

      try {
        if (!currentSessionId) {
          currentSessionId = await createSession(selectedAgent.agentId, currentUserId);
          setSessionId(currentSessionId);
          await refreshSessions(true);
        }

        try {
          await streamChat({
            agentId: selectedAgent.agentId,
            userId: currentUserId,
            sessionId: currentSessionId,
            message
          }, {
            onMessage(payload) {
              if (!payload) {
                return;
              }
              setMessages((prev) => updateAssistantWorkflowMessage(prev, assistantMessage.id, payload));
              setStatus({
                type: "loading",
                text: payload.finalResponse ? "整理结果中..." : "执行工作流中"
              });
            },
            onError(messageText) {
              throw new Error(messageText || "流式请求失败");
            }
          });
        } catch (error) {
          const errorMessage = error && error.message ? error.message : "";
          if (!/Session not found/i.test(errorMessage)) {
            throw error;
          }

          currentSessionId = await createSession(selectedAgent.agentId, currentUserId);
          setSessionId(currentSessionId);

          await streamChat({
            agentId: selectedAgent.agentId,
            userId: currentUserId,
            sessionId: currentSessionId,
            message
          }, {
            onMessage(payload) {
              if (!payload) {
                return;
              }
              setMessages((prev) => updateAssistantWorkflowMessage(prev, assistantMessage.id, payload));
              setStatus({
                type: "loading",
                text: payload.finalResponse ? "整理结果中..." : "执行工作流中"
              });
            },
            onError(messageText) {
              throw new Error(messageText || "流式请求失败");
            }
          });
        }

        setMessages((prev) => finalizeAssistantWorkflowMessage(prev, assistantMessage.id));
        await refreshSessions(true);
        setStatus({ type: "ready", text: "已完成" });
      } catch (error) {
        setMessages((prev) => updateMessage(prev, assistantMessage.id, {
          content: `请求失败：${error.message || "未知错误"}`,
          loading: false,
          workflowSteps: []
        }));
        setStatus({ type: "error", text: "请求失败" });
      } finally {
        setIsSending(false);
      }
    }

    const userInitial = useMemo(() => (userId || "U").trim().slice(0, 1).toUpperCase(), [userId]);

    return h("div", { className: "app-frame" },
      h("div", { className: "workspace" },
        h(Sidebar, {
          collapsed: sidebarCollapsed,
          agents,
          sessions,
          selectedAgentId,
          sessionId,
          userId,
          userInitial,
          userPopoverOpen,
          userPopoverRef,
          onToggleSidebar: () => setSidebarCollapsed((prev) => !prev),
          onNewSession: startNewSession,
          onSelectAgent: handleAgentSelect,
          onSelectSession: switchSession,
          onToggleUserPopover: () => setUserPopoverOpen((prev) => !prev)
        }),
        h(MainArea, {
          selectedAgent,
          activeSession,
          messages,
          status,
          composerValue,
          activePromptId,
          isSending,
          chatScrollRef,
          textareaRef,
          onComposerChange: setComposerValue,
          onPromptClick: (prompt) => {
            setComposerValue(prompt.value);
            setActivePromptId(prompt.id);
          },
          onSend: handleSend
        })
      )
    );
  }

  function Sidebar(props) {
    return h("aside", { className: `sidebar ${props.collapsed ? "collapsed" : ""}` },
      h("div", { className: "sidebar-top" },
        h("button", {
          className: "toggle-btn",
          type: "button",
          onClick: props.onToggleSidebar,
          title: props.collapsed ? "展开导航" : "收起导航"
        }, h(MenuIcon))
      ),
      h("button", {
        className: "session-btn",
        type: "button",
        onClick: props.onNewSession,
        title: "新会话"
      }, h(PlusIcon), h("span", { className: "session-label" }, "新会话")),
      h("div", { className: "agent-section" },
        h("div", { className: "agent-section-title" }, "可用智能体"),
        h("div", { className: "agent-list" },
          props.agents.length
            ? props.agents.map((agent) => h("button", {
                key: agent.agentId,
                className: `agent-btn ${agent.agentId === props.selectedAgentId ? "active" : ""}`,
                type: "button",
                onClick: () => props.onSelectAgent(agent.agentId),
                title: agent.agentName || agent.agentId
              },
                h("span", { className: "agent-avatar" }, getAvatarText(agent.agentName || agent.agentId)),
                h("div", { className: "agent-meta" },
                  h("p", { className: "agent-name" }, agent.agentName || agent.agentId),
                  h("p", { className: "agent-desc" }, agent.agentDesc || "暂无描述")
                )
              ))
            : h("div", { className: "agent-btn" },
                h("span", { className: "agent-avatar" }, "?"),
                h("div", { className: "agent-meta" }, h("p", { className: "agent-name" }, "暂无 Agent"))
              )
        )
      ),
      h("div", { className: "history-section" },
        h("div", { className: "agent-section-title" }, "历史会话"),
        h("div", { className: "history-list" },
          props.sessions.length
            ? props.sessions.map((session) => h("button", {
                key: session.sessionId,
                className: `history-btn ${session.sessionId === props.sessionId ? "active" : ""}`,
                type: "button",
                onClick: () => props.onSelectSession(session),
                title: session.title || session.sessionId
              },
                h("div", { className: "history-head" },
                  h("span", { className: "history-title" }, session.title || "新会话"),
                  h("span", { className: "history-time" }, formatDateTime(session.lastMessageAt || session.updatedAt))
                ),
                h("div", { className: "history-agent" }, session.agentName || session.agentId || "未知 Agent"),
                h("div", { className: "history-preview" }, session.lastMessagePreview || "暂无消息")
              ))
            : h("div", { className: "history-empty" }, "暂无历史会话")
        )
      ),
      h("div", { className: "sidebar-footer", ref: props.userPopoverRef },
        props.userPopoverOpen && h("div", { className: "user-popover" },
          h("p", { className: "popover-label" }, "Current userId"),
          h("p", { className: "popover-value" }, props.userId)
        ),
        h("div", { className: "user-anchor" },
          h("button", {
            className: "user-btn",
            type: "button",
            onClick: props.onToggleUserPopover,
            title: "用户信息"
          }, props.userInitial),
          h("div", { className: "user-meta" },
            h("div", { className: "user-name" }, "用户"),
            h("div", { className: "user-sub" }, "点击查看 userId")
          )
        )
      )
    );
  }

  function MainArea(props) {
    return h("main", { className: "main-shell" },
      h("header", { className: "chat-header" },
        h("div", null,
          h("div", { className: "chat-title" }, props.selectedAgent ? (props.selectedAgent.agentName || props.selectedAgent.agentId) : "请选择 Agent"),
          props.activeSession ? h("div", { className: "chat-subtitle" }, props.activeSession.title || "当前会话") : null
        ),
        h("div", { className: `chat-status ${props.status.type}` }, props.status.text)
      ),
      h("section", { className: "chat-panel" },
        h("div", { className: "chat-scroll", ref: props.chatScrollRef },
          props.messages.length
            ? h("div", { className: "message-list" },
                props.messages.map((message) => h(MessageBubble, { key: message.id, message }))
              )
            : h("div", { className: "empty-chat" },
                h("div", { className: "empty-card" },
                  h("p", { className: "empty-title" }, "开始一段新对话")
                )
              )
        )
      ),
      h(Composer, {
        value: props.composerValue,
        activePromptId: props.activePromptId,
        isSending: props.isSending,
        textareaRef: props.textareaRef,
        onChange: props.onComposerChange,
        onPromptClick: props.onPromptClick,
        onSend: props.onSend
      })
    );
  }

  function Composer(props) {
    return h("section", { className: "composer" },
      h("div", { className: "composer-shell" },
        h("div", { className: "composer-tools" },
          QUICK_PROMPTS.map((prompt) => h("button", {
            key: prompt.id,
            className: `composer-action ${props.activePromptId === prompt.id ? "active" : ""}`,
            type: "button",
            onClick: () => props.onPromptClick(prompt)
          }, prompt.label))
        ),
        h("div", { className: "composer-main" },
          h("textarea", {
            ref: props.textareaRef,
            className: "composer-textarea",
            rows: 1,
            value: props.value,
            placeholder: "输入消息...",
            onChange: (event) => props.onChange(event.target.value),
            onKeyDown: (event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                props.onSend();
              }
            }
          }),
          h("button", {
            className: "send-btn",
            type: "button",
            onClick: props.onSend,
            disabled: props.isSending,
            title: props.isSending ? "生成中" : "发送"
          }, h(SendIcon))
        )
      )
    );
  }

  function MessageBubble(props) {
    const message = props.message;
    const markdownHtml = message.role === "assistant" && !message.loading
      ? renderMarkdown(message.content || "")
      : "";

    return h("div", { className: `message-row ${message.role}` },
      h("article", { className: "message-bubble" },
        h("div", { className: "message-meta" }, `${message.role === "user" ? "用户" : "Agent"} · ${formatTime(message.timestamp)}`),
        message.role === "assistant"
          ? h("div", null,
              Array.isArray(message.workflowSteps) && message.workflowSteps.length
                ? h("div", { className: "workflow-panel" },
                    message.workflowSteps.map((step) => h("div", {
                      key: step.key,
                      className: `workflow-step ${step.status}`
                    },
                      h("div", { className: "workflow-step-head" },
                        h("span", { className: "workflow-step-name" }, step.name),
                        h("span", { className: `workflow-step-badge ${step.status}` }, readWorkflowStatusLabel(step.status))
                      ),
                      step.detail ? h("div", { className: "workflow-step-detail", title: step.detail }, step.detail) : null
                    ))
                  )
                : null,
              markdownHtml
                ? h("div", { className: "message-body", dangerouslySetInnerHTML: { __html: markdownHtml } })
                : message.loading
                  ? h("div", { className: "typing" }, h("span"), h("span"), h("span"))
                  : h("div", { className: "workflow-empty" }, "等待工作流结果...")
            )
          : message.loading
            ? h("div", { className: "typing" }, h("span"), h("span"), h("span"))
            : h("div", { className: "message-body" }, message.content)
      )
    );
  }

  function MenuIcon() {
    return h("svg", { viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: "1.8", strokeLinecap: "round" },
      h("path", { d: "M5 7h14M5 12h14M5 17h14" })
    );
  }

  function PlusIcon() {
    return h("svg", { viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: "1.8", strokeLinecap: "round" },
      h("path", { d: "M12 5v14M5 12h14" })
    );
  }

  function SendIcon() {
    return h("svg", { viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: "1.8", strokeLinecap: "round", strokeLinejoin: "round" },
      h("path", { d: "M22 2 11 13" }),
      h("path", { d: "m22 2-7 20-4-9-9-4Z" })
    );
  }

  async function streamChat(payload, handlers) {
    const response = await fetch(`${API_BASE}/chat_stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!response.ok || !response.body) {
      throw new Error(`HTTP ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";

    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const parsed = consumeSseBuffer(buffer);
      buffer = parsed.rest;
      parsed.events.forEach((event) => dispatchSseEvent(event, handlers));
    }

    const tail = decoder.decode();
    if (tail) {
      buffer += tail;
    }

    if (buffer.trim()) {
      const parsed = consumeSseBuffer(`${buffer}\n\n`);
      parsed.events.forEach((event) => dispatchSseEvent(event, handlers));
    }
  }

  function dispatchSseEvent(event, handlers) {
    const payload = safeJsonParse(event.data);
    if (event.event === "error") {
      if (handlers.onError) {
        handlers.onError(payload && payload.message);
      }
      return;
    }

    if (event.event === "message" && handlers.onMessage) {
      handlers.onMessage(payload);
    }
  }

  function consumeSseBuffer(buffer) {
    const normalized = buffer.replace(/\r\n/g, "\n");
    const blocks = normalized.split("\n\n");
    const rest = blocks.pop() || "";
    return {
      events: blocks.map(parseSseEvent).filter(Boolean),
      rest
    };
  }

  function parseSseEvent(block) {
    const lines = block.split("\n");
    let eventName = "message";
    const dataLines = [];

    lines.forEach((line) => {
      if (!line || line.startsWith(":")) {
        return;
      }

      if (line.startsWith("event:")) {
        eventName = line.slice(6).trim();
        return;
      }

      if (line.startsWith("data:")) {
        dataLines.push(line.slice(5).trimStart());
      }
    });

    if (!dataLines.length) {
      return null;
    }

    return { event: eventName, data: dataLines.join("\n") };
  }

  function requestJson(url, options) {
    return fetch(url, options).then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      if (!data || data.code !== "0000") {
        throw new Error((data && data.info) || "接口返回异常");
      }

      return data;
    });
  }

  function renderMarkdown(value) {
    const html = marked.parse(value || "");
    return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } });
  }

  function finalizeStreamContent(content) {
    if (typeof content === "string" && content.trim()) {
      return content.trim();
    }

    return "Agent 未返回可展示内容。";
  }

  function updateMessage(messages, messageId, patch) {
    return messages.map((message) => message.id === messageId ? { ...message, ...patch } : message);
  }

  function updateAssistantWorkflowMessage(messages, messageId, payload) {
    return messages.map((message) => {
      if (message.id !== messageId) {
        return message;
      }

      const nextSteps = appendWorkflowStep(message.workflowSteps || [], payload);
      const nextContent = payload.finalResponse
        ? finalizeStreamContent(payload.content || message.content)
        : message.content;

      return {
        ...message,
        content: nextContent,
        loading: false,
        workflowSteps: nextSteps
      };
    });
  }

  function finalizeAssistantWorkflowMessage(messages, messageId) {
    return messages.map((message) => {
      if (message.id !== messageId) {
        return message;
      }

      const steps = (message.workflowSteps || []).map((step) => ({
        ...step,
        status: step.status === "running" ? "done" : step.status
      }));

      return {
        ...message,
        content: finalizeStreamContent(message.content),
        loading: false,
        workflowSteps: steps
      };
    });
  }

  function appendWorkflowStep(steps, payload) {
    const author = normalizeAuthor(payload.author);
    const detail = summarizeWorkflowDetail(payload);
    const status = payload.finalResponse ? "done" : payload.partial ? "running" : (payload.turnComplete ? "done" : "running");

    if (!author) {
      return steps;
    }

    const nextSteps = steps.map((step) => step.status === "running" ? { ...step, status: "done" } : step);
    nextSteps.push({
      key: payload.id || `${author}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      name: author,
      detail,
      status
    });

    return nextSteps;
  }

  function createMessage(role, content) {
    return {
      id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      role,
      content,
      loading: false,
      timestamp: new Date(),
      workflowSteps: []
    };
  }

  function historyMessageToBubble(message) {
    return {
      id: message.messageId || `history-${Date.now()}-${Math.random().toString(16).slice(2)}`,
      role: message.role === "user" ? "user" : "assistant",
      content: message.content || "",
      loading: false,
      timestamp: new Date(message.createdAt || Date.now()),
      workflowSteps: []
    };
  }

  function autoResizeTextarea(textarea) {
    if (!textarea) {
      return;
    }

    textarea.style.height = "0px";
    textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
  }

  function safeJsonParse(value) {
    try {
      return JSON.parse(value);
    } catch (error) {
      return null;
    }
  }

  function formatTime(value) {
    const date = value instanceof Date ? value : new Date(value);
    return new Intl.DateTimeFormat("zh-CN", {
      hour: "2-digit",
      minute: "2-digit"
    }).format(date);
  }

  function formatDateTime(value) {
    if (!value) {
      return "";
    }
    return new Intl.DateTimeFormat("zh-CN", {
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    }).format(new Date(value));
  }

  function generateUserId() {
    return `user_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  }

  function getAvatarText(value) {
    return (value || "?").trim().slice(0, 1).toUpperCase();
  }

  function normalizeAuthor(value) {
    return typeof value === "string" && value.trim() ? value.trim() : "Workflow";
  }

  function summarizeWorkflowDetail(payload) {
    const content = typeof payload.content === "string" ? payload.content.trim() : "";
    if (!content) {
      if (payload.finalResponse) {
        return "已生成最终结果";
      }
      if (payload.turnComplete) {
        return "当前步骤已结束";
      }
      return "正在处理中";
    }

    const compact = content.replace(/\s+/g, " ");
    return compact.length > 120 ? `${compact.slice(0, 120)}...` : compact;
  }

  function readWorkflowStatusLabel(status) {
    if (status === "done") {
      return "完成";
    }
    if (status === "running") {
      return "进行中";
    }
    return "等待中";
  }

  const root = ReactDOM.createRoot(document.getElementById("app"));
  root.render(h(App));
})();
