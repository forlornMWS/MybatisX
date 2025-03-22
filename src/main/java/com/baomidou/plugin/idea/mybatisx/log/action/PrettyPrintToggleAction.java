package com.baomidou.plugin.idea.mybatisx.log.action;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.baomidou.plugin.idea.mybatisx.log.Icons;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * PrettyPrintToggleAction
 * @author huangxingguang
 */
public class PrettyPrintToggleAction extends ToggleAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }

    public PrettyPrintToggleAction() {
        super("Pretty Print", "Pretty print", Icons.PRETTY_PRINT);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        if (Objects.isNull(e.getProject())) {
            return false;
        }
        return PropertiesComponent.getInstance(e.getProject()).getBoolean(PrettyPrintToggleAction.class.getName());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        if (Objects.isNull(e.getProject())) {
            return;
        }

        PropertiesComponent.getInstance(e.getProject()).setValue(PrettyPrintToggleAction.class.getName(), state);

    }

}
