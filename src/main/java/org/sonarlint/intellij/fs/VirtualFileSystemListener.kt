/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2021 SonarSource
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
package org.sonarlint.intellij.fs

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import org.sonarlint.intellij.core.ProjectBindingManager
import org.sonarlint.intellij.util.GlobalLogOutput
import org.sonarlint.intellij.util.SonarLintUtils.getService
import org.sonarsource.sonarlint.core.client.api.common.ClientModuleFileEvent
import org.sonarsource.sonarlint.core.client.api.common.LogOutput
import org.sonarsource.sonarlint.core.client.api.common.SonarLintEngine
import org.sonarsource.sonarlint.plugin.api.module.file.ModuleFileEvent

/**
 * The BulkFileListener is not tied to a specific project but global to the IDE instance
 */
class VirtualFileSystemListener(
    private val fileEventsNotifier: ModuleFileEventsNotifier = getService(
        ModuleFileEventsNotifier::class.java
    )
) : BulkFileListener {
    override fun before(events: List<VFileEvent>) {
        forwardEvents(events.filterIsInstance(VFileMoveEvent::class.java)) { ModuleFileEvent.Type.DELETED }
    }

    override fun after(events: List<VFileEvent>) {
        forwardEvents(events) {
            when (it) {
                is VFileDeleteEvent -> ModuleFileEvent.Type.DELETED
                is VFileMoveEvent -> ModuleFileEvent.Type.CREATED
                is VFileCopyEvent, is VFileCreateEvent -> ModuleFileEvent.Type.CREATED
                is VFileContentChangeEvent -> ModuleFileEvent.Type.MODIFIED
                is VFilePropertyChangeEvent -> null
                else -> {
                    GlobalLogOutput.get().log("Unknown file event type: $it", LogOutput.Level.ERROR)
                    null
                }
            }
        }
    }

    private fun forwardEvents(events: List<VFileEvent>, eventTypeConverter: (VFileEvent) -> ModuleFileEvent.Type?) {
        val openProjects = ProjectManager.getInstance().openProjects.toList()
        val startedEnginesByProject = openProjects.associateWith { getEngineIfStarted(it) }
        val filesByModule = fileEventsByModules(events, openProjects, eventTypeConverter)
        filesByModule.forEach { (module, fileEvents) ->
            startedEnginesByProject[module.project]?.let {
                fileEventsNotifier.notifyAsync(it, module, fileEvents)
            }
        }
    }

    private fun getEngineIfStarted(project: Project): SonarLintEngine? =
        getService(project, ProjectBindingManager::class.java).engineIfStarted

    private fun fileEventsByModules(
        events: List<VFileEvent>,
        openProjects: List<Project>,
        eventTypeConverter: (VFileEvent) -> ModuleFileEvent.Type?
    ): Map<Module, List<ClientModuleFileEvent>> {
        val map: MutableMap<Module, List<ClientModuleFileEvent>> = mutableMapOf()
        for (event in events) {
            // call event.file only once as it can be hurting performance
            val file = event.file ?: continue
            val fileModule = findModule(file, openProjects) ?: continue
            val fileInvolved = if (event is VFileCopyEvent) event.findCreatedFile() else file
            fileInvolved ?: continue
            val type = eventTypeConverter(event) ?: continue
            buildModuleFileEvent(fileModule, fileInvolved, type)?.let {
                val moduleEvents = map[fileModule] ?: emptyList()
                map[fileModule] = moduleEvents + it
            }
        }
        return map
    }

    private fun findModule(file: VirtualFile?, openProjects: List<Project>): Module? {
        file ?: return null
        return openProjects.asSequence()
            .map { ModuleUtil.findModuleForFile(file, it) }
            .find { it != null }
    }
}
