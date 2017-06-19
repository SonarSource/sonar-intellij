package org.sonarlint.intellij.config.global.wizard;

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarlint.intellij.config.global.SonarQubeServer;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteOrganization;

public class WizardModel {
  private final static String SONARCLOUD_URL = "https://sonarcloud.io";
  public final static String[] SONARCLOUD_ALIAS = {"https://sonarqube.com", "https://www.sonarqube.com",
    "https://www.sonarcloud.io", "https://sonarcloud.io"};
  private ServerType serverType;
  private String serverUrl;
  private String token;
  private String login;
  private char[] password;
  private String name;
  private String organization;
  private boolean proxyEnabled;

  private List<RemoteOrganization> organizationList;

  public enum ServerType {
    SONARCLOUD,
    SONARQUBE
  }

  public WizardModel() {

  }

  public WizardModel(SonarQubeServer serverToEdit) {
    if (Arrays.asList(SONARCLOUD_ALIAS).contains(serverToEdit.getHostUrl())) {
      serverType = ServerType.SONARCLOUD;
    } else {
      serverType = ServerType.SONARQUBE;
      serverUrl = serverToEdit.getHostUrl();
    }
    this.proxyEnabled = serverToEdit.enableProxy();
    this.token = serverToEdit.getToken();
    this.login = serverToEdit.getLogin();
    if (serverToEdit.getPassword() != null) {
      this.password = serverToEdit.getPassword().toCharArray();
    }
    this.organization = serverToEdit.getOrganizationKey();
    this.name = serverToEdit.getName();
  }

  @CheckForNull
  public ServerType getServerType() {
    return serverType;
  }

  public WizardModel setServerType(ServerType serverType) {
    this.serverType = serverType;
    return this;
  }

  public boolean isProxyEnabled() {
    return proxyEnabled;
  }

  public WizardModel setProxyEnabled(boolean proxyEnabled) {
    this.proxyEnabled = proxyEnabled;
    return this;
  }

  @CheckForNull
  public List<RemoteOrganization> getOrganizationList() {
    return organizationList;
  }

  public WizardModel setOrganizationList(List<RemoteOrganization> organizationList) {
    this.organizationList = organizationList;
    return this;
  }

  @CheckForNull
  public String getServerUrl() {
    return serverUrl;
  }

  public WizardModel setServerUrl(@Nullable String serverUrl) {
    this.serverUrl = serverUrl;
    return this;
  }

  @CheckForNull
  public String getToken() {
    return token;
  }

  public WizardModel setToken(@Nullable String token) {
    this.token = token;
    return this;
  }

  @CheckForNull
  public String getLogin() {
    return login;
  }

  public WizardModel setLogin(@Nullable String login) {
    this.login = login;
    return this;
  }

  @CheckForNull
  public char[] getPassword() {
    return password;
  }

  public WizardModel setPassword(@Nullable char[] password) {
    this.password = password;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  public WizardModel setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public WizardModel setOrganization(@Nullable String organization) {
    this.organization = organization;
    return this;
  }

  public SonarQubeServer createServer() {
    SonarQubeServer.Builder builder = SonarQubeServer.newBuilder()
      .setOrganizationKey(organization)
      .setEnableProxy(proxyEnabled)
      .setName(name);

    if (serverType == ServerType.SONARCLOUD) {
      builder.setHostUrl(SONARCLOUD_URL);

    } else {
      builder.setHostUrl(serverUrl);
    }

    if (token != null) {
      builder.setToken(token)
        .setLogin(null)
        .setPassword(null);
    } else {
      builder.setToken(null)
        .setLogin(login)
        .setPassword(new String(password));
    }
    //builder.setEnableProxy(enableProxy.isSelected());
    return builder.build();
  }
}
