import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.agentproxy.Connector
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.IndexDiff
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.util.FS

includeTargets << grailsScript("Init")
includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsInit")

def setupSession = {
  SshSessionFactory.setInstance(new JschConfigSessionFactory() {
    // Thanks to Justin Ryan (http://morecanaries.blogspot.jp/2013/04/dont-dare-prompt-me.html)
    @Override
    protected void configure(OpenSshConfig.Host host, Session session) {
      session.setConfig("StrictHostKeyChecking", "false");
    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
      Connector con = null

      try {
        if (SSHAgentConnector.isConnectorAvailable()) {
          con = new SSHAgentConnector(new JNAUSocketFactory())
        }
      } catch (e) {
        System.err.println "Exception while setting up ssh agent proxy connection: " + e.message
        e.printStackTrace()
      }

      if (con) {
        JSch jsch = new JSch()
        jsch.setConfig("PreferredAuthentications", "publickey")
        jsch.setIdentityRepository(new RemoteIdentityRepository(con))

        knownHosts(jsch, fs)
        return jsch
      } else {
        return super.createDefaultJSch(fs)
      }
    }
  })
}

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

    Status st = new Status(diff);

    if (!st.changed && !st.added && !st.modified && !st.missing && !st.removed) { // No modifications in the repo, we can proceed
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

      setupSession.call()

      def remote = Constants.DEFAULT_REMOTE_NAME
      if (argsMap?.params?.size() > 0) {
        remote = argsMap.params.first()
      }

      def refs = [ // Refs to push
              new RefSpec(Constants.R_HEADS + Constants.MASTER), // master ref
              new RefSpec(Constants.R_TAGS + newVersion)         // the new tag
      ]
      git.push().setRemote(remote).setRefSpecs(refs).call()
    } else { // Repo is not clean, aborting
      StringBuilder msg = new StringBuilder()
      msg.append("Repo not clean, aborting version update.")
      msg.append("\nChanged:").append(st.changed)
      msg.append("\nAdded:").append(st.added)
      msg.append("\nModified:").append(st.modified)
      msg.append("\nMissing:").append(st.missing)
      msg.append("\nRemoved:").append(st.removed)
      println(msg)
    }
  } catch (e) {
    System.err.println("Exception during version update: " + e.message)
    e.printStackTrace()
  }
}

setDefaultTarget(updateVersion)
