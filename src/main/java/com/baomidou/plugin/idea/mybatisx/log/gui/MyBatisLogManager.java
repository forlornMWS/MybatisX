package com.baomidou.plugin.idea.mybatisx.log.gui;

import com.baomidou.plugin.idea.mybatisx.log.BasicFormatter;
import com.baomidou.plugin.idea.mybatisx.log.Icons;
import com.baomidou.plugin.idea.mybatisx.log.LogUtil;
import com.baomidou.plugin.idea.mybatisx.log.action.ClearAllAction;
import com.baomidou.plugin.idea.mybatisx.log.action.JumpSqlAction;
import com.baomidou.plugin.idea.mybatisx.log.action.NextSqlAction;
import com.baomidou.plugin.idea.mybatisx.log.action.PrettyPrintToggleAction;
import com.baomidou.plugin.idea.mybatisx.log.action.PreviousSqlAction;
import com.baomidou.plugin.idea.mybatisx.log.action.RerunAction;
import com.baomidou.plugin.idea.mybatisx.log.action.SettingsAction;
import com.baomidou.plugin.idea.mybatisx.log.action.StopAction;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.messages.MessageBusConnection;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.baomidou.plugin.idea.mybatisx.log.MyBatisLogConsoleFilter.KEYWORDS_KEY;
import static com.baomidou.plugin.idea.mybatisx.log.MyBatisLogConsoleFilter.PARAMETERS_KEY;
import static com.baomidou.plugin.idea.mybatisx.log.MyBatisLogConsoleFilter.PREPARING_KEY;

/**
 * MyBatisLogManager
 *
 * @author huangxingguang
 */
@Getter
@Setter
public class MyBatisLogManager implements Disposable {

    private static final Key<MyBatisLogManager> KEY = Key.create(MyBatisLogManager.class.getName());
    private static final BasicFormatter FORMATTER = new BasicFormatter();

    private final Map<Integer, ConsoleViewContentType> consoleViewContentTypes = new ConcurrentHashMap<>();

    private final ConsoleViewImpl consoleView;
    private final Project project;
    private final RunContentDescriptor descriptor;

    private final AtomicInteger counter;

    private volatile String preparing;

    private volatile String parameters;

    private volatile boolean running = false;

    private final List<String> keywords = new ArrayList<>(0);

    private MyBatisLogManager(@NotNull Project project, String toolWindowId) {
        this.project = project;

        this.consoleView = createConsoleView();

        final JPanel panel = createConsolePanel(this.consoleView);

        RunnerLayoutUi layoutUi = getRunnerLayoutUi();

        Content content = layoutUi.createContent(UUID.randomUUID().toString(), panel, "SQL", Icons.MY_BATIS, panel);

        content.setCloseable(false);

        layoutUi.addContent(content);

        layoutUi.getOptions().setLeftToolbar(createActionToolbar(), "RunnerToolbar");

        final MessageBusConnection messageBusConnection = project.getMessageBus().connect();

        this.counter = new AtomicInteger();
        this.descriptor = getRunContentDescriptor(layoutUi);

        Disposer.register(this, consoleView);
        Disposer.register(this, content);
        Disposer.register(this, layoutUi.getContentManager());
        Disposer.register(this, messageBusConnection);
        Disposer.register(project, this);

        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        this.preparing = propertiesComponent.getValue(PREPARING_KEY, "Preparing: ");
        this.parameters = propertiesComponent.getValue(PARAMETERS_KEY, "Parameters: ");
        resetKeywords(propertiesComponent.getValue(KEYWORDS_KEY, StringUtils.EMPTY));

        ToolWindow toolWindow = getToolWindow(toolWindowId);
        messageBusConnection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {

            @Override
            public void stateChanged() {
                if (Objects.nonNull(toolWindow) && !toolWindow.isAvailable()) {
                    Disposer.dispose(MyBatisLogManager.this);
                }
            }
        });

        Executor executor = MyBatisLogExecutor.getInstance(toolWindowId);
        if (executor != null) {
            // 对于 "Services" 需要特殊处理
            if ("Services".equals(toolWindowId)) {
                ToolWindow servicesToolWindow = ToolWindowManager.getInstance(project).getToolWindow("Services");
                if (servicesToolWindow != null) {
                    // 获取内容管理器
                    ContentManager contentManager = servicesToolWindow.getContentManager();

                    Content[] contents = contentManager.getContents();
                    for (Content content1 : contents) {
                        if (content1.getDisplayName() != null && content1.getDisplayName().equals("MyBatis Log")) {
                            // 如果已经存在，则直接选中
                            contentManager.setSelectedContent(content1);
                            content1.setComponent(descriptor.getComponent());
                            return;
                        }
                    }
                    // 创建新的内容页签
                    Content newContent = contentManager.getFactory().createContent(
                        descriptor.getComponent(),
                        "MyBatis Log",
                        false
                    );

                    // 添加到现有的 Services 内容管理器
                    contentManager.addContent(newContent);

                    // 选中新创建的内容页签
                    contentManager.setSelectedContent(newContent);

                    // 激活 Services 工具窗口
                    servicesToolWindow.activate(null);
                }
            } else {
                // 对于Run工具窗口，保持原有逻辑
                ExecutionManager.getInstance(project).getContentManager().showRunContent(executor, descriptor);

                if (Objects.nonNull(toolWindow)) {
                    toolWindow.activate(null);
                }
            }
        }

    }

    private ConsoleViewImpl createConsoleView() {
        TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        final ConsoleViewImpl console = (ConsoleViewImpl) consoleBuilder.getConsole();
        // init editor
        console.getComponent();

        final Editor editor = console.getEditor();
        editor.getDocument().addDocumentListener(new RangeHighlighterDocumentListener(editor));

        return console;
    }

    private ActionGroup createActionToolbar() {

        final ConsoleViewImpl consoleView = this.consoleView;

        final DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new RerunAction());
        actionGroup.add(new StopAction(this));
        actionGroup.add(new SettingsAction(this));
        actionGroup.addSeparator();
        actionGroup.add(new PreviousSqlAction(consoleView));
        actionGroup.add(new NextSqlAction(consoleView));
        actionGroup.addSeparator();

        actionGroup.add(new ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
            @Nullable
            @Override
            protected Editor getEditor(@NotNull AnActionEvent e) {
                return consoleView.getEditor();
            }
        });

        actionGroup.add(new ScrollToTheEndToolbarAction(consoleView.getEditor()));
        actionGroup.add(new PrettyPrintToggleAction());
        actionGroup.addSeparator();
        actionGroup.add(new ClearAllAction(consoleView));
        actionGroup.addSeparator();
        return actionGroup;
    }

    private JPanel createConsolePanel(ConsoleView consoleView) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(consoleView.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    private RunContentDescriptor getRunContentDescriptor(RunnerLayoutUi layoutUi) {
        RunContentDescriptor descriptor = new RunContentDescriptor(new RunProfile() {
            @Nullable
            @Override
            public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
                return null;
            }

            @NotNull
            @Override
            public String getName() {
                return "SQL";
            }

            @Override
            @Nullable
            public Icon getIcon() {
                return null;
            }
        }, new DefaultExecutionResult(), layoutUi);
        descriptor.setExecutionId(System.nanoTime());

        return descriptor;
    }

    private RunnerLayoutUi getRunnerLayoutUi() {

        return RunnerLayoutUi.Factory.getInstance(project).create("MyBatis Log", "MyBatis Log", "MyBatis Log", project);
    }

    public void println(String logPrefix, String sql,int rgb) {

        final ConsoleViewContentType consoleViewContentType = consoleViewContentTypes.computeIfAbsent(rgb, k -> new ConsoleViewContentType(String.valueOf(rgb), new TextAttributes(new JBColor(rgb, rgb), null, null, null, Font.PLAIN)));

        consoleView.print(String.format("-- %s -- %s\n", counter.incrementAndGet(), logPrefix), ConsoleViewContentType.USER_INPUT);

        consoleView.print(String.format("%s\n", isFormat() ? FORMATTER.format(sql) : StringUtils.removeEnd(sql, "\n")), consoleViewContentType);

    }

    private boolean isFormat() {
        return PropertiesComponent.getInstance(project).getBoolean(PrettyPrintToggleAction.class.getName());
    }

    public void run() {

        if (running) {
            return;
        }

        running = true;

    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;

    }

    @Nullable
    public static MyBatisLogManager getInstance(@NotNull Project project, String toolWindowId) {

        MyBatisLogManager manager = project.getUserData(KEY);

        if (Objects.nonNull(manager)) {
            ToolWindow toolWindow = manager.getToolWindow(toolWindowId);
            if (Objects.nonNull(toolWindow) && !toolWindow.isAvailable()) {
                Disposer.dispose(manager);
                manager = null;
            }
        }

        return manager;

    }

    @NotNull
    public static MyBatisLogManager createInstance(@NotNull Project project, String toolWindowId) {

        MyBatisLogManager manager = getInstance(project, toolWindowId);

        if (Objects.nonNull(manager) && !Disposer.isDisposed(manager)) {
            Disposer.dispose(manager);
        }

        manager = new MyBatisLogManager(project, toolWindowId);
        project.putUserData(KEY, manager);

        return manager;

    }

    public ToolWindow getToolWindow(String toolWindowId) {
        return ToolWindowManager.getInstance(project).getToolWindow(toolWindowId);
    }

    public void resetKeywords(String text) {

        keywords.clear();

        if (StringUtils.isBlank(text)) {
            return;
        }

        final String[] split = text.split("\n");

        final List<String> keywords = new ArrayList<>(split.length);

        for (String keyword : split) {
            if (StringUtils.isBlank(keyword)) {
                continue;
            }

            keywords.add(keyword);

        }

        this.keywords.addAll(keywords);
    }

    @Override
    public void dispose() {

        project.putUserData(KEY, null);

        stop();
        String toolWindowId = LogUtil.getToolWindowId(project);
        RunContentManager.getInstance(project).removeRunContent(MyBatisLogExecutor.getInstance(toolWindowId), descriptor);

    }

    private record RangeHighlighterDocumentListener(Editor editor) implements DocumentListener {

        @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                final Document document = event.getDocument();
                final int textLength = document.getTextLength();
                if (textLength < 1) {
                    return;
                }

                for (int i = event.getOffset(); i < textLength; ) {
                    final int endOffset = document.getLineEndOffset(document.getLineNumber(i));
                    final String text = document.getText(TextRange.create(i, endOffset));
                    if (text.matches("^-- \\d+ -- .*")) {
                        editor.getMarkupModel().addRangeHighlighter(i, i + 1, JumpSqlAction.SQL_LAYER, TextAttributes.ERASE_MARKER, HighlighterTargetArea.EXACT_RANGE);
                    }
                    i = endOffset + 1;
                }
            }
        }


}
