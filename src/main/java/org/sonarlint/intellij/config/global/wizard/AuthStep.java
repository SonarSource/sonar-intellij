package org.sonarlint.intellij.config.global.wizard;

import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.ui.DocumentAdapter;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonarlint.intellij.config.global.SonarQubeServer;
import org.sonarlint.intellij.tasks.ConnectionTestTask;
import org.sonarlint.intellij.tasks.OrganizationsFetchTask;
import org.sonarsource.sonarlint.core.client.api.connected.ValidationResult;

public class AuthStep extends AbstractStep {
  private final static String LOGIN_ITEM = "Login / Password";
  private final static String TOKEN_ITEM = "Token";
  private final WizardModel model;

  private JPanel panel;
  private JComboBox authComboBox;
  private JTextField tokenField;
  private JTextField loginField;
  private JPasswordField passwordField;
  private JButton createTokenButton;
  private JPanel cardPanel;
  private JPanel tokenPanel;
  private JPanel loginPanel;
  private CardLayout cardLayout;

  public AuthStep(WizardModel model) {
    super("Authentication");
    this.model = model;

    DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    comboBoxModel.addElement(TOKEN_ITEM);
    comboBoxModel.addElement(LOGIN_ITEM);
    authComboBox.setModel(comboBoxModel);
    authComboBox.addItemListener(e -> {
      if (ItemEvent.SELECTED == e.getStateChange()) {
        CardLayout cl = (CardLayout) (cardPanel.getLayout());
        if (LOGIN_ITEM.equals(e.getItem())) {
          cl.show(cardPanel, "Login");
        } else {
          cl.show(cardPanel, "Token");
        }
        fireStateChanged();
      }
    });

    DocumentListener listener = new DocumentAdapter() {
      @Override protected void textChanged(DocumentEvent e) {
        fireStateChanged();
      }
    };

    loginField.getDocument().addDocumentListener(listener);
    tokenField.getDocument().addDocumentListener(listener);
    passwordField.getDocument().addDocumentListener(listener);
  }

  public void _init() {
    if (model.getServerType() == WizardModel.ServerType.SONARCLOUD) {
      authComboBox.setSelectedItem(TOKEN_ITEM);
      authComboBox.setEnabled(false);
    } else {
      authComboBox.setEnabled(true);
      if (model.getLogin() != null) {
        authComboBox.setSelectedItem(LOGIN_ITEM);
      } else {
        authComboBox.setSelectedItem(TOKEN_ITEM);
      }
    }

    tokenField.setText(model.getToken());
    loginField.setText(model.getLogin());
    if (model.getPassword() != null) {
      passwordField.setText(new String(model.getPassword()));
    }
  }

  public void save() {
    if (authComboBox.getSelectedItem().equals(LOGIN_ITEM)) {
      model.setToken(null);
      model.setLogin(loginField.getText());
      model.setPassword(passwordField.getPassword());
    } else {
      model.setToken(tokenField.getText());
      model.setLogin(null);
      model.setPassword(null);
    }
  }

  @Override
  public JComponent getComponent() {
    return panel;
  }

  @NotNull @Override public Object getStepId() {
    return AuthStep.class;
  }

  @Nullable @Override public Object getNextStepId() {
    if (model.getOrganizationList() != null && model.getOrganizationList().size() > 1) {
      return OrganizationStep.class;
    }
    return ConfirmStep.class;
  }

  @Nullable @Override public Object getPreviousStepId() {
    return ServerStep.class;
  }

  @Override public boolean isComplete() {
    if (authComboBox.getSelectedItem().equals(LOGIN_ITEM)) {
      return passwordField.getPassword().length > 0 && !loginField.getText().isEmpty();
    } else {
      return !tokenField.getText().isEmpty();
    }
  }

  @Override public void commit(CommitType commitType) throws CommitStepException {
    if (commitType == CommitType.Finish || commitType == CommitType.Next) {
      save();
      checkConnection();
      fetchOrganizations();
    }
  }

  private void fetchOrganizations() throws CommitStepException {
    SonarQubeServer tmpServer = model.createServer();
    OrganizationsFetchTask task = new OrganizationsFetchTask(tmpServer);
    ProgressManager.getInstance().run(task);
    if (task.getException() == null) {
      model.setOrganizationList(task.result());
      if (task.result().size() == 1) {
        model.setOrganization(task.result().iterator().next().getKey());
      }
      if (task.result().isEmpty()) {
        model.setOrganization(null);
      }
      return;
    }

    String msg = "Failed to fetch list of organizations from the server. Please check the configuration and try again.";
    if (task.getException().getMessage() != null) {
      msg = msg + " Error: " + task.getException().getMessage();
    }
    throw new CommitStepException(msg);
  }

  private void checkConnection() throws CommitStepException {
    SonarQubeServer tmpServer = model.createServer();
    ConnectionTestTask test = new ConnectionTestTask(tmpServer);
    ProgressManager.getInstance().run(test);
    ValidationResult r = test.result();
    String msg = "Failed to connect to the server. Please check the configuration.";
    if (test.getException() != null) {
      if (test.getException().getMessage() != null) {
        msg = msg + " Error: " + test.getException().getMessage();
      }
      throw new CommitStepException(msg);
    } else if (!r.success()) {
      throw new CommitStepException(msg + " Cause: " + r.message());
    }
  }

  @Nullable @Override public JComponent getPreferredFocusedComponent() {
    if (authComboBox.isEnabled()) {
      return authComboBox;
    }
    return tokenField;
  }

  private void createUIComponents() {
    authComboBox = new JComboBox();
  }
}
