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

/*
 * @author max
 */
package com.intellij.ide.bookmarks.actions;

import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkItem;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.ui.popup.util.ItemWrapper;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.util.MasterDetailPopupBuilder;
import com.intellij.ui.speedSearch.FilteringListModel;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: remove duplication with BaseShowRecentFilesAction, there's quite a bit of it

public class BookmarksAction extends AnAction implements DumbAware, MasterDetailPopupBuilder.Delegate {

  @Override
  public void update(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    final Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    e.getPresentation().setEnabled(project != null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    final Project project = PlatformDataKeys.PROJECT.getData(dataContext);
    if (project == null) return;


    final DefaultListModel model = buildModel(project);

    final JBList list = new JBList(model);
    list.getEmptyText().setText("No Bookmarks");
    list.setCellRenderer(new ItemRenderer(project));

    EditBookmarkDescriptionAction editDescriptionAction = new EditBookmarkDescriptionAction(project, list);
    DefaultActionGroup actions = new DefaultActionGroup();
    actions.add(editDescriptionAction);
    actions.add(new DeleteBookmarkAction(project, list));
    actions.add(new MoveBookmarkUpAction(project, list));
    actions.add(new MoveBookmarkDownAction(project, list));

    final JBPopup popup = new MasterDetailPopupBuilder(project).
      setActionsGroup(actions).
      setList(list).
      setDelegate(this).createMasterDetailPopup();

    editDescriptionAction.setPopup(popup);
    popup.showCenteredInCurrentWindow(project);
  }

  @Override
  public String getTitle() {
    return "Bookmarks";
  }

  @Override
  public void handleMnemonic(KeyEvent e, Project project, JBPopup popup) {
    char mnemonic = e.getKeyChar();
    final Bookmark bookmark = BookmarkManager.getInstance(project).findBookmarkForMnemonic(mnemonic);
    if (bookmark != null) {
      popup.cancel();
      IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(new Runnable() {
        public void run() {
          bookmark.navigate();
        }
      });
    }
  }

  @Override
  public void itemRemoved(ItemWrapper item, Project project) {
    BookmarkManager.getInstance(project).removeBookmark(((BookmarkItem)item).getBookmark());
  }

  private static DefaultListModel buildModel(Project project) {
    final DefaultListModel model = new DefaultListModel();

    for (Bookmark bookmark : BookmarkManager.getInstance(project).getValidBookmarks()) {
      model.addElement(new BookmarkItem(bookmark));
    }

    return model;
  }

  private static class ItemRenderer extends JPanel implements ListCellRenderer {
    private final Project myProject;
    private final ColoredListCellRenderer myRenderer;

    private ItemRenderer(Project project) {
      super(new BorderLayout());
      myProject = project;

      setBackground(UIUtil.getListBackground());

      final JLabel label = new JLabel();
      label.setFont(Bookmark.MNEMONIC_FONT);

      label.setPreferredSize(new JLabel("W.").getPreferredSize());
      label.setOpaque(false);

      if (BookmarkManager.getInstance(project).hasBookmarksWithMnemonics()) {
        add(label, BorderLayout.WEST);
      }

      myRenderer = new ColoredListCellRenderer() {
        @Override
        protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
          if (value instanceof ItemWrapper) {
            final ItemWrapper wrapper = (ItemWrapper)value;
            wrapper.setupRenderer(this, myProject, selected);
            wrapper.updateMnemonicLabel(label);
          }
        }
      };
      add(myRenderer, BorderLayout.CENTER);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      myRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      return this;
    }
  }

  protected static class BookmarkInContextInfo {
    private final DataContext myDataContext;
    private final Project myProject;
    private Bookmark myBookmarkAtPlace;
    private VirtualFile myFile;
    private int myLine;

    public BookmarkInContextInfo(DataContext dataContext, Project project) {
      myDataContext = dataContext;
      myProject = project;
    }

    public Bookmark getBookmarkAtPlace() {
      return myBookmarkAtPlace;
    }

    public VirtualFile getFile() {
      return myFile;
    }

    public int getLine() {
      return myLine;
    }

    public BookmarkInContextInfo invoke() {
      myBookmarkAtPlace = null;
      myFile = null;
      myLine = -1;


      BookmarkManager bookmarkManager = BookmarkManager.getInstance(myProject);
      if (ToolWindowManager.getInstance(myProject).isEditorComponentActive()) {
        Editor editor = PlatformDataKeys.EDITOR.getData(myDataContext);
        if (editor != null) {
          Document document = editor.getDocument();
          myLine = editor.getCaretModel().getLogicalPosition().line;
          myFile = FileDocumentManager.getInstance().getFile(document);
          myBookmarkAtPlace = bookmarkManager.findEditorBookmark(document, myLine);
        }
      }

      if (myFile == null) {
        myFile = PlatformDataKeys.VIRTUAL_FILE.getData(myDataContext);
        myLine = -1;

        if (myBookmarkAtPlace == null && myFile != null) {
          myBookmarkAtPlace = bookmarkManager.findFileBookmark(myFile);
        }
      }
      return this;
    }
  }

  static List<Bookmark> getSelectedBookmarks(JList list) {
    List<Bookmark> answer = new ArrayList<Bookmark>();

    for (Object value : list.getSelectedValues()) {
      if (value instanceof BookmarkItem) {
        answer.add(((BookmarkItem)value).getBookmark());
      }
      else {
        return Collections.emptyList();
      }
    }

    return answer;
  }

  static boolean notFiltered(JList list) {
    if (!(list.getModel() instanceof FilteringListModel)) return true;
    final FilteringListModel model = (FilteringListModel)list.getModel();
    return model.getOriginalModel().getSize() == model.getSize();
  }

}
