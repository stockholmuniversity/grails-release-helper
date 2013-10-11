import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.IndexDiff
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.treewalk.FileTreeIterator

includeTargets << grailsScript("Init")
includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsInit")

target(updateVersion: "Update the application version & push a git tag with the same name.") {
  depends(parseArguments) // To be able to get target arguments

  def gitDir = (grailsSettings.baseDir.absolutePath ?: '.') + File.separator + '.git'
  FileRepositoryBuilder builder = new FileRepositoryBuilder()
  Repository repo = builder.setGitDir(new File(gitDir))
          .readEnvironment() // scan environment GIT_* variables
          .findGitDir() // scan up the file system tree
          .build()

  try {
    IndexDiff diff = new IndexDiff(repo, Constants.HEAD, new FileTreeIterator(repo))
    diff.diff()

    Status status = new Status(diff);

    if (status.isClean()) { // No moddifications in the repo, we can proceed
      def currentVersion = metadata.'app.version'

      // Get new version from user input
      println "Current app version: ${currentVersion}."
      ant.input addProperty: "app.version.new", message: "Enter the new version: ", defaultvalue: currentVersion
      def newVersion = ant.antProject.properties.'app.version.new'

      metadata.'app.version' = newVersion
      metadata.persist()

      def git = new Git(repo)
      git.add().addFilepattern("application.properties").call()
      git.commit().setMessage("[generated] Updated version to ${newVersion}").call()

      git.tag().setName(newVersion).call()

      def remote = Constants.DEFAULT_REMOTE_NAME
      if (argsMap?.params?.size() > 0) {
        remote = argsMap.params.first()
      }

      def refs = [ // Refs to push
              new RefSpec('refs/heads/master'),
              new RefSpec("refs/tags/${newVersion}")
      ]
      git.push().setRemote(remote).setRefSpecs(refs).call()
    } else { // Repo is not clean, aborting
      StringBuilder msg = new StringBuilder()
      msg.append("Repo not clean, aborting version update.")
      msg.append("\n").append("Changed:").append(status.getChanged())
      msg.append("\n").append("Added:").append(status.getAdded())
      msg.append("\n").append("Modified:").append(status.getModified())
      msg.append("\n").append("Missing:").append(status.getMissing())
      msg.append("\n").append("Removed:").append(status.getRemoved())
      println(msg)
    }
  } catch (e) {
    System.err.println("Exception during version update: " + e.message)
    e.printStackTrace()
  }
}

setDefaultTarget(updateVersion)
