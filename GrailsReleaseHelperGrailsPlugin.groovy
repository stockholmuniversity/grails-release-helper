class GrailsReleaseHelperGrailsPlugin {
    // the plugin version
    def version = "0.0.5"
    def groupId = "se.su.it.grails.plugins"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"

    // TODO Fill in these fields
    def title = "Grails Release Helper Plugin" // Headline display name of the plugin
    def author = "Joakim Lundin"
    def authorEmail = "joakim.lundin@gmail.com"
    def description = '''\
Keeps the application version & git tag in sync
'''

    // URL to the plugin's documentation
    def documentation = ""

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//   def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Stockholm University", url: "http://www.su.se/" ]

    // Any additional developers beyond the author specified above.
    def developers = [
        [ name: "Tommy Andersson", email: "tommy.andersson@su.se" ]
    ]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/stockholmuniversity/grails-release-helper" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
