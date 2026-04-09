package com.xtbn.domain.agent.service.assmble.component.tool.skill.impl;

import com.xtbn.domain.agent.model.valobj.AgentConfigVO;
import com.xtbn.domain.agent.service.assmble.component.tool.skill.IToolSkillCreateService;
import com.xtbn.domain.agent.service.assmble.component.tool.ToolCallbackFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DefaultToolSkillCreateService implements IToolSkillCreateService {
    @Resource
    private ToolCallbackFactory toolCallbackFactory;

    @Override
    public ToolCallback[] buildToolCallback(AgentConfigVO.AgentRuntime.ChatModel.ToolSkill toolSkill) throws Exception {
        String type = toolSkill.getType();
        String path = toolSkill.getPath();

        List<ToolCallback> toolCallbackList = new ArrayList<>();
        // 文件系统目录
        if ("directory".equals(type)) {
            Path rootPath = Paths.get(path);
            // 不存在 or 不是目录
            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                return new ToolCallback[0];
            }
            // 判断是否为空
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
                if (!stream.iterator().hasNext()) {
                    return new ToolCallback[0];
                }
            }
            ToolCallback toolCallback = SkillsTool.builder()
                    .addSkillsDirectory(path)
                    .build();

            toolCallbackList.add(toolCallback);
        }

        // classpath 资源目录
        if ("resource".equals(type)) {
            ClassLoader classLoader = getClass().getClassLoader();
            URL url = classLoader.getResource(path);
            if (url == null) {
                return new ToolCallback[0];
            }
            if ("file".equals(url.getProtocol())) {
                Path rootPath = Paths.get(url.toURI());
                if (!Files.isDirectory(rootPath)) {
                    return new ToolCallback[0];
                }
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
                    if (!stream.iterator().hasNext()) {
                        return new ToolCallback[0];
                    }
                }
            }
            ToolCallback toolCallback = SkillsTool.builder()
                    .addSkillsResource(new ClassPathResource(path))
                    .build();
            toolCallbackList.add(toolCallback);
        }
        return toolCallbackFactory.wrapWithLogging("skill", path, toolCallbackList.toArray(new ToolCallback[0]));
    }
}
