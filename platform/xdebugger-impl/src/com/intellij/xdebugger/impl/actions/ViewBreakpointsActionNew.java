/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.xdebugger.impl.actions;

import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil;
import com.intellij.xdebugger.impl.breakpoints.ui.BreakpointsMasterDetailPopupFactory;
import org.jetbrains.annotations.Nullable;

public class ViewBreakpointsActionNew extends AnAction implements AnAction.TransparentUpdate {
  @Nullable
  private Object myInitialBreakpoint;

  public ViewBreakpointsActionNew() {
    this(ActionsBundle.actionText(XDebuggerActions.VIEW_BREAKPOINTS), null);
  }

  public ViewBreakpointsActionNew(String name, Object initialBreakpoint) {
    super(name);
    myInitialBreakpoint = initialBreakpoint;
  }

  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    if (project == null) return;

    if (myInitialBreakpoint == null) {
      Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
      if (editor != null) {
        myInitialBreakpoint = XBreakpointUtil.findSelectedBreakpoint(project, editor).second;
      }
    }

    BreakpointsMasterDetailPopupFactory.
      getInstance(project).createPopup(myInitialBreakpoint).showCenteredInCurrentWindow(project);

    myInitialBreakpoint = null;
  }

  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    presentation.setEnabled(true);
  }

}
