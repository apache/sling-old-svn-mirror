def svnBase = "https://svn.apache.org/repos/asf/sling/trunk"
def modules = [
    [
        location: "bundles/extensions/i18n"
    ], 
    [
        location: "contrib/extensions/sling-pipes",
        jdks: ["1.8"]
    ]
]

def defaultJdks = ["1.7", "1.8"]
def jdkMapping = [
    "1.7": "JDK 1.7 (latest)",
    "1.8": "JDK 1.8 (latest)"
]

modules.each {
  
    def svnDir = svnBase +"/" + it.location
    def jobName = "sling-" + it.location.replaceAll('/', '-')
    def jdks = it.jdks ?: defaultJdks

    jdks.each {
        def jdkKey = it
        job(jobName + "-" + jdkKey) {

            logRotator {
                numToKeep(15)
            }

            scm {
                svn(svnDir)
            }

            triggers {
                scm('H/15 * * * *')
            }

            jdk(jdkMapping.get(jdkKey))

            label('ubuntu1||ubuntu2||ubuntu4||ubuntu5||ubuntu6')

            steps {
                maven {
                   goals("clean")
                   goals("verify")
                   mavenInstallation("Maven 3.3.9") 
                }
            }

            publishers {
                archiveJunit('**/target/surefire-reports/*.xml')
                // send emails for each broken build, notify individuals as well
                mailer('commits@sling.apache.org', false, true)
            }
        }
    }
}
