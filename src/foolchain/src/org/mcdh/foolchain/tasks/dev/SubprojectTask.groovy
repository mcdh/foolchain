package org.mcdh.foolchain.tasks.dev

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.TaskAction

class SubprojectTask extends DefaultTask {
 public def buildFile
 public String tasks
 private LinkedList<Action<Project>> configureProject = new LinkedList<>()
 private Action<Task> configureTask

 @TaskAction
 def doTask() throws IOException {
  final File buildFile = project.file(this.buildFile)
  Project childProj = FmlDevPlugin.getProject(buildFile, project)

  // configure the project
  for (Action<Project> act : configureProject) {
   if (act != null) {
    act.execute(childProj)
   }
  }

  for (String task : tasks.split(" ")) {
   Set<Task> list = childProj.getTasksByName(task, false)
   for (Task t : list) {
    if (configureTask != null) {
     configureTask.execute(t)
    }
    executeTask((AbstractTask)t)
   }
  }
 }

 private void executeTask(AbstractTask task) {
  for (Object dep : task.getTaskDependencies().getDependencies(task)) {
   executeTask((AbstractTask)dep)
  }

  if (!task.getState().getExecuted()) {
   getLogger().lifecycle(task.getPath())
   task.execute()
  }
 }
}
