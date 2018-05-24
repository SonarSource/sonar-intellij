/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.config.global.wizard;

import com.intellij.ide.wizard.AbstractWizardStepEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.util.containers.Convertor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteOrganization;

import static javax.swing.JList.VERTICAL;

public class OrganizationStep extends AbstractWizardStepEx {
  private final WizardModel model;
  private JList<RemoteOrganization> orgList;
  private JPanel panel;
  private JButton selectOtherOrganizationButton;
  private DefaultListModel<RemoteOrganization> listModel;

  public OrganizationStep(WizardModel model) {
    super("Organization");
    this.model = model;

    orgList.addListSelectionListener(e -> fireStateChanged());
    orgList.addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && isComplete()) {
          OrganizationStep.super.fireGoNext();
        }
      }
    });
    selectOtherOrganizationButton.addActionListener(e -> {
      String organizationKey = Messages.showInputDialog(panel, "Please enter the organization key", "Add Another Organization", null);
      if (StringUtil.isNotEmpty(organizationKey)) {
        boolean found = selectOrganizationIfExists(organizationKey);
        if (!found) {
          RemoteOrganization newOrg = new Organization(organizationKey);
          listModel.add(0, newOrg);
          orgList.setSelectedIndex(0);
          orgList.ensureIndexIsVisible(0);
        }
      }
    });
  }

  private void save() {
    RemoteOrganization org = orgList.getSelectedValue();
    if (org != null) {
      model.setOrganization(org.getKey());
    } else {
      model.setOrganization(null);
    }
  }

  @Override
  public void _init() {
    listModel = new DefaultListModel<>();
    model.getOrganizationList().forEach(listModel::addElement);
    orgList.setModel(listModel);
    // automatically focus and select a row to be possible to search immediately
    orgList.grabFocus();
    if (model.getOrganization() != null) {
      // this won't work if it was a custom organization
      selectOrganizationIfExists(model.getOrganization());
    } else if (!listModel.isEmpty()) {
      orgList.setSelectedIndex(0);
    }
  }

  private static class Organization implements RemoteOrganization {
    private final String key;

    Organization(String key) {
      this.key = key;
    }

    @Override public String getKey() {
      return key;
    }

    @Override public String getName() {
      return getKey();
    }

    @Override public String getDescription() {
      return "";
    }
  }

  private boolean selectOrganizationIfExists(String organizationKey) {
    for (int i = 0; i < listModel.getSize(); i++) {
      RemoteOrganization org = listModel.getElementAt(i);
      if (organizationKey.equals(org.getKey())) {
        orgList.setSelectedIndex(i);
        orgList.ensureIndexIsVisible(i);
        return true;
      }
    }
    return false;
  }

  @NotNull @Override public Object getStepId() {
    return OrganizationStep.class;
  }

  @Nullable @Override public Object getNextStepId() {
    return ConfirmStep.class;
  }

  @Nullable @Override public Object getPreviousStepId() {
    return AuthStep.class;
  }

  @Override public boolean isComplete() {
    return orgList.getSelectedValue() != null;
  }

  @Override public void commit(CommitType commitType) {
    if (commitType == CommitType.Finish || commitType == CommitType.Next) {
      // FIXME check if org exists?
      save();
    }
  }

  @Override public JComponent getComponent() {
    return panel;
  }

  @Nullable @Override public JComponent getPreferredFocusedComponent() {
    return orgList;
  }

  private void createUIComponents() {
    JBList<RemoteOrganization> list = new JBList<>();
    list.setLayoutOrientation(VERTICAL);
    list.setVisibleRowCount(8);
    list.setEnabled(true);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new ListRenderer());

    Convertor<Object, String> convertor = o -> {
      RemoteOrganization org = (RemoteOrganization) o;
      return org.getName() + " " + org.getKey();
    };
    new ListSpeedSearch(list, convertor);
    orgList = list;
  }

  private static class ListRenderer extends ColoredListCellRenderer<RemoteOrganization> {
    @Override protected void customizeCellRenderer(JList list, @Nullable RemoteOrganization value, int index, boolean selected, boolean hasFocus) {
      if (value == null) {
        return;
      }

      append(value.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES, true);
      // it is not working: appendTextPadding
      append(" ");
      if (index >= 0) {
        append("(" + value.getKey() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES, false);
      }
    }
  }
}
