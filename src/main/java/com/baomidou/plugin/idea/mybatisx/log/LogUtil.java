package com.baomidou.plugin.idea.mybatisx.log;

import com.baomidou.plugin.idea.mybatisx.log.gui.MyBatisLogExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class LogUtil {

    public static final String TOOL_WINDOW_SERVICES = "Services";
    public static final String TOOL_WINDOW_RUN = "Run";
    public static final String TOOL_WINDOW_DEBUG = "Debug";

    public static String getToolWindowId(Project project) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return performGetToolWindowId(project);
        }
        return MyBatisLogExecutor.TOOL_WINDOW_ID; // 直接返回默认值
    }

    @NotNull
    private static String performGetToolWindowId(Project project) {
        if (project == null) {
            return MyBatisLogExecutor.TOOL_WINDOW_ID;
        }

        // 只有在确定是 EDT 线程时才调用
        if (ApplicationManager.getApplication().isDispatchThread()) {
            String toolWindowId = ToolWindowManager.getInstance(project).getLastActiveToolWindowId();
            if (StringUtils.isBlank(toolWindowId) ||
                (!toolWindowId.equals(TOOL_WINDOW_SERVICES) && !toolWindowId.equals(TOOL_WINDOW_RUN) && !toolWindowId.equals(TOOL_WINDOW_DEBUG))) {
                toolWindowId = MyBatisLogExecutor.TOOL_WINDOW_ID;
            }
            return toolWindowId;
        }

        return MyBatisLogExecutor.TOOL_WINDOW_ID;
    }
}
