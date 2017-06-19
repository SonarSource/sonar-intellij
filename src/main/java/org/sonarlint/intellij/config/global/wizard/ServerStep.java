package org.sonarlint.intellij.config.global.wizard;

import com.intellij.ide.wizard.CommitStepException;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.ui.SwingHelper;
import icons.SonarLintIcons;
import java.awt.event.MouseEvent;
import java.util.Collection;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerStep extends AbstractStep {
  private static final int NAME_MAX_LENGTH = 50;
  private final WizardModel model;
  private final Collection<String> existingNames;

  private JRadioButton radioSonarCloud;
  private JRadioButton radioSonarQube;
  private JPanel panel;
  private JTextField urlText;
  private JLabel urlLabel;
  private JTextField nameField;
  private JLabel sonarcloudIcon;
  private JLabel sonarqubeIcon;
  private JEditorPane sonarcloudText;
  private JEditorPane sonarqubeText;
  private JCheckBox proxyCheck;
  private JButton proxyButton;

  public ServerStep(WizardModel model, boolean editing, Collection<String> existingNames) {
    super("Server");
    this.model = model;
    this.existingNames = existingNames;
    radioSonarCloud.addChangeListener(e -> selectionChanged());
    radioSonarQube.addChangeListener(e -> selectionChanged());

    DocumentListener listener = new DocumentAdapter() {
      @Override protected void textChanged(DocumentEvent e) {
        fireStateChanged();
      }
    };
    urlText.getDocument().addDocumentListener(listener);
    nameField.getDocument().addDocumentListener(listener);

    nameField.setToolTipText("Name of this configuration");

    String cloudText = "Continous Code Quality as a Service. Connect to SonarCloud (<a href=\"https://sonarcloud.io\">https://sonarcloud.io</a>),      the service operated by SonarSource.";
    sonarcloudText.setText(cloudText);

    String sqText = "Connect to your SonarQube server";
    sonarqubeText.setText(sqText);

    if (!editing) {
      sonarqubeIcon.addMouseListener(new MouseInputAdapter() {
        @Override public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          radioSonarQube.setSelected(true);
        }
      });
      sonarcloudIcon.addMouseListener(new MouseInputAdapter() {
        @Override public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          radioSonarCloud.setSelected(true);
        }
      });
    }

    proxyCheck.setMnemonic('y');
    proxyCheck.setEnabled(HttpConfigurable.getInstance().USE_HTTP_PROXY);
    proxyButton.addActionListener(evt -> {
      HttpConfigurable.editConfigurable(panel);
      proxyCheck.setEnabled(HttpConfigurable.getInstance().USE_HTTP_PROXY);
    });

    load(editing);
  }

  private void load(boolean editing) {
    if (model.getServerType() == WizardModel.ServerType.SONARCLOUD || model.getServerType() == null) {
      radioSonarCloud.setSelected(true);
    } else {
      radioSonarQube.setSelected(true);
      urlText.setText(model.getServerUrl());
    }

    nameField.setText(model.getName());

    if (editing) {
      nameField.setEnabled(false);
      radioSonarQube.setEnabled(false);
      radioSonarCloud.setEnabled(false);
    }
  }

  private void selectionChanged() {
    boolean sq = radioSonarQube.isSelected();

    Icon sqIcon = SonarLintIcons.icon("SonarQube");
    Icon clIcon = SonarLintIcons.icon("SonarCloud");

    if (sq) {
      clIcon = SonarLintIcons.toDisabled(clIcon);
    } else {
      sqIcon = SonarLintIcons.toDisabled(sqIcon);
    }

    sonarqubeIcon.setIcon(sqIcon);
    sonarcloudIcon.setIcon(clIcon);

    urlText.setEnabled(sq);
    urlLabel.setEnabled(sq);
    sonarqubeText.setEnabled(sq);
    sonarcloudText.setEnabled(!sq);
    fireStateChanged();
  }

  @Override
  public JComponent getComponent() {
    return panel;
  }

  @NotNull @Override public Object getStepId() {
    return ServerStep.class;
  }

  @Nullable @Override public Object getNextStepId() {
    return AuthStep.class;
  }

  @Nullable @Override public Object getPreviousStepId() {
    return null;
  }

  @Override public boolean isComplete() {
    if (nameField.getText().trim().isEmpty()) {
      return false;
    }
    return radioSonarCloud.isSelected() || !urlText.getText().trim().isEmpty();
  }

  @Override public void commit(CommitType commitType) throws CommitStepException {
    validateName();
    save();
  }

  private void validateName() throws CommitStepException {
    if (existingNames.contains(nameField.getText().trim())) {
      throw new CommitStepException("There is already a configuration with that name. Please choose another name");
    }
  }

  private void save() {
    if (radioSonarCloud.isSelected()) {
      model.setServerType(WizardModel.ServerType.SONARCLOUD);
      model.setServerUrl("https://sonarcloud.io");
    } else {
      model.setServerType(WizardModel.ServerType.SONARQUBE);
      model.setServerUrl(urlText.getText().trim());
    }
    model.setName(nameField.getText().trim());
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    if (nameField.isEnabled()) {
      return nameField;
    } else if (urlText.isEnabled()) {
      return urlText;
    }
    return null;
  }

  private void createUIComponents() {
    sonarcloudIcon = new JLabel(SonarLintIcons.icon("SonarCloud"));
    sonarqubeIcon = new JLabel(SonarLintIcons.icon("SonarQube"));
    sonarcloudText = SwingHelper.createHtmlViewer(false, null, null, null);
    sonarqubeText = SwingHelper.createHtmlViewer(false, null, null, null);

    JBTextField text = new JBTextField();
    text.getEmptyText().setText("Example: http://localhost:9000");
    urlText = text;

    nameField = new JBTextField();
    nameField.setDocument(new LengthRestrictedDocument(NAME_MAX_LENGTH));
  }
}
