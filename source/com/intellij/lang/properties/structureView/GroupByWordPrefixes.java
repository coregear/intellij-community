package com.intellij.lang.properties.structureView;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.smartTree.*;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;

import java.util.*;

/**
 * @author cdr
 */
public class GroupByWordPrefixes implements Grouper {
  private static final Logger LOG = Logger.getInstance("#com.intellij.lang.properties.structureView.GroupByWordPrefixes");
  public static final String ID = "GROUP_BY_PREFIXES";

  public Collection<Group> group(final AbstractTreeNode parent, Collection<TreeElement> children) {
    List<Key> keys = new ArrayList<Key>();

    String parentPrefix;
    int parentPrefixLength;
    if (parent.getValue() instanceof PropertiesPrefixGroup) {
      parentPrefix = ((PropertiesPrefixGroup)parent.getValue()).getPrefix();
      parentPrefixLength = parentPrefix.split("\\.").length;
    }
    else {
      parentPrefix = "";
      parentPrefixLength = 0;
    }
    for (TreeElement element : children) {
      if (element instanceof PropertiesStructureViewElement) {
        Property property = ((PropertiesStructureViewElement)element).getValue();
        String key = property.getKey();
        if (key == null) continue;
        LOG.assertTrue(key.startsWith(parentPrefix));
        List<String> words = Arrays.asList(key.split("\\."));
        keys.add(new Key(words, (PropertiesStructureViewElement)element));
      }
    }
    Collections.sort(keys, new Comparator<Key>() {
      public int compare(final Key k1, final Key k2) {
        List<String> o1 = k1.words;
        List<String> o2 = k2.words;
        if (o1.size() != o2.size()) return o1.size() - o2.size();
        for (int i = 0; i < o1.size(); i++) {
          String s1 = o1.get(i);
          String s2 = o2.get(i);
          int res = s1.compareTo(s2);
          if (res != 0) return res;
        }
        return 0;
      }
    });
    List<Group> groups = new ArrayList<Group>();
    int groupStart = 0;
    for (int i = 0; i <= keys.size(); i++) {
      if (!isEndOfGroup(i, keys, parentPrefixLength)) {
        continue;
      }
      // find longest group prefix
      List<String> firstKey = keys.get(groupStart).words;
      int prefixLen = firstKey.size();
      for (int j = groupStart+1; j < i; j++) {
        List<String> prevKey = keys.get(j-1).words;
        List<String> nextKey = keys.get(j).words;
        for (int k = parentPrefixLength; k < prefixLen; k++) {
          String word = k < nextKey.size() ? nextKey.get(k) : null;
          String wordInPrevKey = k < prevKey.size() ? prevKey.get(k) : null;
          if (!Comparing.strEqual(word, wordInPrevKey)) {
            prefixLen = k;
            break;
          }
        }
      }
      String[] strings = firstKey.subList(0,prefixLen).toArray(new String[prefixLen]);
      String prefix = StringUtil.join(strings, ".");
      String presentableName = prefix.substring(parentPrefix.length());
      presentableName = StringUtil.trimStart(presentableName, ".");
      if (i - groupStart > 1) {
        groups.add(new PropertiesPrefixGroup(children, prefix, presentableName));
      }
      else {
        PropertiesStructureViewElement node = keys.get(groupStart).node;
        node.setPresentableName(presentableName);
      }
      groupStart = i;
    }
    return groups;
  }

  private boolean isEndOfGroup(final int i,
                               final List<Key> keys,
                               final int parentPrefixLength) {
    if (i == keys.size()) return true;
    if (i == 0) return false;
    List<String> words = keys.get(i).words;
    List<String> prevWords = keys.get(i - 1).words;
    if (prevWords.size() == parentPrefixLength) return true;
    return !Comparing.strEqual(words.get(parentPrefixLength), prevWords.get(parentPrefixLength));
  }

  public ActionPresentation getPresentation() {
    return new ActionPresentationData("Group By Prefixes",
                                      "Groups properties by common key prefixes separated by dots",
                                      IconLoader.getIcon("/nodes/addLocalWeblogicInstance.png"));
  }

  public String getName() {
    return ID;
  }

  private static class Key {
    List<String> words;
    PropertiesStructureViewElement node;

    public Key(final List<String> words, final PropertiesStructureViewElement node) {
      this.words = words;
      this.node = node;
    }
  }

}
