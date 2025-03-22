package com.baomidou.plugin.idea.mybatisx.log.gui;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.baomidou.plugin.idea.mybatisx.log.Icons;

/**
 * MyBatisLogExecutor
 *
 * @author huangxingguang
 */
public class MyBatisLogExecutor extends Executor {

    public static final String TOOL_WINDOW_ID = "MyBatis Log";

    @Override
    public @NotNull String getToolWindowId() {
        return TOOL_WINDOW_ID;
    }

    @Override
    public @NotNull Icon getToolWindowIcon() {
        return getIcon();
    }

    @Override
    public @NotNull Icon getIcon() {
        return Icons.MY_BATIS;
    }

    @Override
    public Icon getDisabledIcon() {
        return Icons.MY_BATIS;
    }

    @Override
    public String getDescription() {
        return "MyBatis log";
    }

    @NotNull
    @Override
    public String getActionName() {
        return "MyBatis Log";
    }

    @NotNull
    @Override
    public String getId() {
        return TOOL_WINDOW_ID;
    }

    @NotNull
    @Override
    public String getStartActionText() {
        return "MyBatis Log";
    }

    @Override
    public String getContextActionId() {
        return getDescription();
    }

    @Override
    public String getHelpId() {
        return TOOL_WINDOW_ID;
    }

    public static Executor getInstance(String toolWindowId) {
        if ("Services".equals(toolWindowId)) {
            return MyBatisLogExecutor.getInstance(TOOL_WINDOW_ID);
        }
        return ExecutorRegistry.getInstance().getExecutorById(toolWindowId);
    }
}
