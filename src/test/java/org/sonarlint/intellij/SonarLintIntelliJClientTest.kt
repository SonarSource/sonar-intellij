/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2022 SonarSource
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
package org.sonarlint.intellij

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.Before
import org.junit.Test
import org.sonarlint.intellij.config.global.ServerConnection
import org.sonarsource.sonarlint.core.clientapi.config.binding.BindingSuggestionDto
import org.sonarsource.sonarlint.core.clientapi.config.binding.SuggestBindingParams
import org.sonarsource.sonarlint.core.clientapi.fs.FindFileByNamesInScopeParams

internal class SonarLintIntelliJClientTest : AbstractSonarLintLightTests() {
    val client = SonarLintIntelliJClient()

    @Before
    fun prepare() {
        // also important as this starts the notification manager service
        clearNotifications()
    }

    @Test
    fun it_should_not_find_files_if_project_does_not_exist() {
        val result = client.findFileByNamesInScope(FindFileByNamesInScopeParams("blah", listOf("file.txt"))).get()

        assertThat(result.foundFiles).isEmpty()
    }

    @Test
    fun it_should_not_find_files_if_project_does_not_have_any() {
        val result =
            client.findFileByNamesInScope(FindFileByNamesInScopeParams(projectBackendId, listOf("file.txt"))).get()

        assertThat(result.foundFiles).isEmpty()
    }

    @Test
    fun it_should_not_find_files_if_project_does_not_have_any_matching_name() {
        myFixture.configureByFile("file.properties")

        val result =
            client.findFileByNamesInScope(FindFileByNamesInScopeParams(projectBackendId, listOf("file.txt"))).get()

        assertThat(result.foundFiles).isEmpty()
    }

    @Test
    fun it_should_find_files_if_project_has_one_with_matching_name() {
        myFixture.configureByFile("file.properties")

        val result =
            client.findFileByNamesInScope(FindFileByNamesInScopeParams(projectBackendId, listOf("file.properties")))
                .get()

        assertThat(result.foundFiles).extracting("fileName", "content")
            .containsOnly(tuple("file.properties", "content=hey\n"))
    }

    @Test
    fun it_should_find_files_if_project_has_one_modified_in_editor_and_matching_name() {
        myFixture.configureByFile("file.properties")
        myFixture.type("pre")

        val result =
            client.findFileByNamesInScope(FindFileByNamesInScopeParams(projectBackendId, listOf("file.properties")))
                .get()

        assertThat(result.foundFiles).extracting("fileName", "content")
            .containsOnly(tuple("file.properties", "precontent=hey\n"))
    }

    @Test
    fun it_should_retrieve_the_http_client_of_a_known_connection() {
        globalSettings.serverConnections = listOf(ServerConnection.newBuilder().setName("connectionId").build())

        val httpClient = client.getHttpClient("connectionId")

        assertThat(httpClient).isNotNull
    }

    @Test
    fun it_should_not_retrieve_the_http_client_for_an_unknown_connection() {
        val httpClient = client.getHttpClient("connectionId")

        assertThat(httpClient).isNull()
    }

    @Test
    fun it_should_suggest_exact_binding_if_there_is_one_suggestion() {
        globalSettings.serverConnections = listOf(ServerConnection.newBuilder().setName("connectionId").build())

        client.suggestBinding(
            SuggestBindingParams(
                mapOf(
                    Pair(
                        projectBackendId, listOf(BindingSuggestionDto("connectionId", "projectKey", "projectName"))
                    )
                )
            )
        )

        assertThat(projectNotifications).extracting("title", "content").containsExactly(
            tuple(
                "<b>SonarLint Suggestions</b>", "Bind this project to 'projectName' on 'connectionId'?"
            )
        )
    }

    @Test
    fun it_should_suggest_binding_config_if_there_is_no_suggestion() {
        globalSettings.serverConnections = listOf(ServerConnection.newBuilder().setName("connectionId").build())

        client.suggestBinding(SuggestBindingParams(mapOf(Pair(projectBackendId, emptyList()))))

        assertThat(projectNotifications).extracting("title", "content").containsExactly(
            tuple(
                "<b>SonarLint Suggestions</b>",
                "Bind this project to SonarQube or SonarCloud?"
            )
        )
    }

    @Test
    fun it_should_suggest_binding_config_if_there_is_are_several_suggestions() {
        globalSettings.serverConnections = listOf(ServerConnection.newBuilder().setName("connectionId").build())

        client.suggestBinding(
            SuggestBindingParams(
                mapOf(
                    Pair(
                        projectBackendId, listOf(
                            BindingSuggestionDto("connectionId", "projectKey", "projectName"), BindingSuggestionDto(
                                "connectionId", "projectKey2", "projectName2"
                            )
                        )
                    )
                )
            )
        )

        assertThat(projectNotifications).extracting("title", "content").containsExactly(
            tuple(
                "<b>SonarLint Suggestions</b>",
                "Bind this project to SonarQube or SonarCloud?"
            )
        )
    }

    @Test
    fun it_should_not_suggest_binding_if_the_project_is_unknown() {
        globalSettings.serverConnections = listOf(ServerConnection.newBuilder().setName("connectionId").build())

        client.suggestBinding(
            SuggestBindingParams(
                mapOf(
                    Pair(
                        "wrongProjectId", listOf(
                            BindingSuggestionDto("connectionId", "projectKey", "projectName"), BindingSuggestionDto(
                                "connectionId", "projectKey2", "projectName2"
                            )
                        )
                    )
                )
            )
        )

        assertThat(projectNotifications).isEmpty()
    }

    @Test
    fun it_should_not_suggest_binding_if_the_suggestions_are_disabled_by_user() {
        globalSettings.serverConnections = listOf(ServerConnection.newBuilder().setName("connectionId").build())
        projectSettings.setBindingSuggestionsEnabled(false)

        client.suggestBinding(
            SuggestBindingParams(
                mapOf(
                    Pair(
                        "wrongProjectId", listOf(
                            BindingSuggestionDto("connectionId", "projectKey", "projectName"), BindingSuggestionDto(
                                "connectionId", "projectKey2", "projectName2"
                            )
                        )
                    )
                )
            )
        )

        assertThat(projectNotifications).isEmpty()
    }
}
