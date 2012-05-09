/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.xdebugger.impl.breakpoints.ui;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.popup.util.DetailView;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.*;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.ui.BreakpointItem;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.ArrayList;

/**
 * @author nik
 */
public class XBreakpointPanelProvider extends BreakpointPanelProvider<XBreakpoint> {

  public int getPriority() {
    return 0;
  }

  @Nullable
  public XBreakpoint<?> findBreakpoint(@NotNull final Project project, @NotNull final Document document, final int offset) {
    XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
    int line = document.getLineNumber(offset);
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null) {
      return null;
    }
    for (XLineBreakpointType<?> type : XDebuggerUtil.getInstance().getLineBreakpointTypes()) {
      XLineBreakpoint<? extends XBreakpointProperties> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
      if (breakpoint != null) {
        return breakpoint;
      }
    }

    return null;
  }

  @Override
  public GutterIconRenderer getBreakpointGutterIconRenderer(Object breakpoint) {
    if (breakpoint instanceof XLineBreakpointImpl) {
      RangeHighlighter highlighter = ((XLineBreakpointImpl)breakpoint).getHighlighter();
      if (highlighter != null) {
        return highlighter.getGutterIconRenderer();
      }
    }
    return null;
  }

  @NotNull
  public Collection<AbstractBreakpointPanel<XBreakpoint>> getBreakpointPanels(@NotNull final Project project, @NotNull final DialogWrapper parentDialog) {
    XBreakpointType<?,?>[] types = XBreakpointUtil.getBreakpointTypes();
    ArrayList<AbstractBreakpointPanel<XBreakpoint>> panels = new ArrayList<AbstractBreakpointPanel<XBreakpoint>>();
    for (XBreakpointType<? extends XBreakpoint<?>, ?> type : types) {
      if (type.shouldShowInBreakpointsDialog(project)) {
        XBreakpointsPanel<?> panel = createBreakpointsPanel(project, parentDialog, type);
        panels.add(panel);
      }
    }
    return panels;
  }

  private static <B extends XBreakpoint<?>> XBreakpointsPanel<B> createBreakpointsPanel(final Project project, DialogWrapper parentDialog, final XBreakpointType<B, ?> type) {
    return new XBreakpointsPanel<B>(project, parentDialog, type);
  }

  public void onDialogClosed(final Project project) {
  }

  @Override
  public void provideBreakpointItems(Project project, Collection<BreakpointItem> items) {
    XBreakpoint<?>[] allBreakpoints = XDebuggerManager.getInstance(project).getBreakpointManager().getAllBreakpoints();
    for (XBreakpoint<?> breakpoint : allBreakpoints) {
      createBreakpointItem(breakpoint);
    }
  }

  private BreakpointItem createBreakpointItem(final XBreakpoint<?> breakpoint) {
    return new BreakpointItem() {
      @Override
      public void setupRenderer(ColoredListCellRenderer renderer, Project project, boolean selected) {
        renderer.setIcon(((XBreakpointBase)breakpoint).getIcon());
      }

      @Override
      public void updateMnemonicLabel(JLabel label) {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void execute(Project project) {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public String speedSearchText() {
        return ((XBreakpointBase)breakpoint).getType().getDisplayText(breakpoint);
      }

      @Override
      public String footerText() {
        return ((XBreakpointBase)breakpoint).getType().getDisplayText(breakpoint);
      }

      @Override
      public void updateDetailView(DetailView panel) {
        XSourcePosition sourcePosition = breakpoint.getSourcePosition();
        if (sourcePosition != null) {
          panel.navigateInPreviewEditor(sourcePosition.getFile(), new LogicalPosition(sourcePosition.getLine(), sourcePosition.getOffset()));
        }
      }
    };
  }
}
