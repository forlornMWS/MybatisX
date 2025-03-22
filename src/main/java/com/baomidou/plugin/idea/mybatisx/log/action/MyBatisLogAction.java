package com.baomidou.plugin.idea.mybatisx.log.action;

import com.baomidou.plugin.idea.mybatisx.log.LogUtil;
import com.baomidou.plugin.idea.mybatisx.log.gui.MyBatisLogManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * MyBatisLogAction
 *
 * @author huangxingguang
 */
public class MyBatisLogAction extends DumbAwareAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (Objects.isNull(project)) {
            return;
        }

        if (!project.isOpen() || !project.isInitialized()) {
            return;
        }

        String toolWindowId = LogUtil.getToolWindowId(project);

        if ("EditorPopup".equals(e.getPlace())) {
            final MyBatisLogManager manager = MyBatisLogManager.getInstance(project, toolWindowId);
            if (Objects.nonNull(manager) && manager.getToolWindow(toolWindowId) != null && manager.getToolWindow(toolWindowId).isAvailable()) {
                manager.run();
                manager.getToolWindow(toolWindowId).activate(null);
            }
        }

        rerun(project);
    }

    public void rerun(final Project project) {
        String toolWindowId = LogUtil.getToolWindowId(project);
        final MyBatisLogManager manager = MyBatisLogManager.getInstance(project, toolWindowId);
        if (Objects.nonNull(manager)) {
            Disposer.dispose(manager);
        }
        MyBatisLogManager.createInstance(project, toolWindowId).run();
    }
}
