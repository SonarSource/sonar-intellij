/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2024 SonarSource
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
package org.sonarlint.intellij.telemetry

import com.intellij.ide.BrowserUtil
import org.sonarlint.intellij.common.util.SonarLintUtils
import org.sonarlint.intellij.documentation.SonarLintDocumentation

enum class LinkTelemetry(
    val linkId: String,
    val url: String
) {

    SONARCLOUD_SIGNUP_PAGE("sonarCloudSignUpPage", SonarLintDocumentation.Marketing.SONARCLOUD_PRODUCT_SIGNUP_LINK),
    CONNECTED_MODE_DOCS("connectedModeDocs", SonarLintDocumentation.Intellij.CONNECTED_MODE_LINK),
    COMPARE_SERVER_PRODUCTS("compareServerProducts", SonarLintDocumentation.Marketing.COMPARE_SERVER_PRODUCTS_LINK),
    SONARQUBE_EDITIONS_DOWNLOADS("sonarQubeEditionsDownloads", SonarLintDocumentation.Marketing.SONARQUBE_EDITIONS_DOWNLOADS_LINK),
    SONARCLOUD_PRODUCT_PAGE("sonarCloudProductPage", SonarLintDocumentation.Marketing.SONARCLOUD_PRODUCT_LINK);

    fun browseWithTelemetry() {
        SonarLintUtils.getService(SonarLintTelemetry::class.java).helpAndFeedbackLinkClicked(linkId)
        BrowserUtil.browse(url)
    }

}
