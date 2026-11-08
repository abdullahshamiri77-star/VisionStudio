
package com.visionstudio.app.core.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HomeBg = Color(0xFF0B0C0F)
private val HomePanel = Color(0xFF17191E)
private val HomeGold = Color(0xFFFFC247)
private val HomeWhite = Color(0xFFF2F3F5)
private val HomeMuted = Color(0xFF9298A4)

@Composable
fun ProjectHomeScreen(
    onOpenProject: (Project) -> Unit
) {
    val context = LocalContext.current
    val manager = remember { ProjectManager(context) }

    var projects by remember { mutableStateOf(manager.getProjects()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var deletingProject by remember { mutableStateOf<Project?>(null) }
    var projectName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun refreshProjects() {
        projects = manager.getProjects()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBg)
            .padding(16.dp)
    ) {
        Text(
            "VISION STUDIO",
            color = HomeGold,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "PROJECT MANAGER",
            color = HomeMuted,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "My Projects",
                    color = HomeWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${projects.size} saved projects",
                    color = HomeMuted,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    projectName = ""
                    errorMessage = ""
                    showCreateDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeGold,
                    contentColor = HomeBg
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("New Project")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (projects.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = HomeGold,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No projects yet",
                    color = HomeWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Create your first editing project",
                    color = HomeMuted,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                HomePanel,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val opened = manager.openProject(project.id)
                                if (opened != null) onOpenProject(opened)
                                refreshProjects()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = HomeGold,
                            modifier = Modifier.size(34.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                project.name,
                                color = HomeWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${project.settings.width} × ${project.settings.height} • ${project.settings.frameRate} FPS",
                                color = HomeMuted,
                                fontSize = 10.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                projectName = project.name
                                errorMessage = ""
                                editingProject = project
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = HomeGold
                            )
                        }

                        IconButton(
                            onClick = { deletingProject = project }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF6262)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingProject != null) {
        val isEditing = editingProject != null

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingProject = null
                errorMessage = ""
            },
            containerColor = HomePanel,
            title = {
                Text(
                    if (isEditing) "Rename Project" else "Create Project",
                    color = HomeGold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = {
                            projectName = it
                            errorMessage = ""
                        },
                        singleLine = true,
                        label = { Text("Project name") }
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(
                            errorMessage,
                            color = Color(0xFFFF6262),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val result = if (isEditing) {
                            manager.renameProject(
                                editingProject!!.id,
                                projectName
                            )
                        } else {
                            manager.createProject(projectName)
                        }

                        if (result == null) {
                            errorMessage =
                                "Enter a unique, non-empty project name."
                        } else {
                            refreshProjects()
                            showCreateDialog = false
                            editingProject = null
                            errorMessage = ""
                        }
                    }
                ) {
                    Text("Save", color = HomeGold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        editingProject = null
                        errorMessage = ""
                    }
                ) {
                    Text("Cancel", color = HomeMuted)
                }
            }
        )
    }

    deletingProject?.let { project ->
        AlertDialog(
            onDismissRequest = { deletingProject = null },
            containerColor = HomePanel,
            title = {
                Text("Delete Project?", color = HomeGold)
            },
            text = {
                Text(
                    "Delete \"${project.name}\" from the project list?",
                    color = HomeWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        manager.deleteProject(project.id)
                        refreshProjects()
                        deletingProject = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF6262))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingProject = null }
                ) {
                    Text("Cancel", color = HomeMuted)
                }
            }
        )
    }
}
