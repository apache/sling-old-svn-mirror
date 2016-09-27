def svnBase = "https://svn.apache.org/repos/asf/sling/trunk"
def modules = [
    [
        location: "bundles/extensions/i18n"
    ],
    [
        location: "bundles/extensions/discovery/api"
    ],
    [
        location: "bundles/extensions/discovery/base"
    ],
    [
        location: "bundles/extensions/discovery/commons"
    ],
    [
        location: "bundles/extensions/discovery/impl"
    ],
    [
        location: "bundles/extensions/discovery/oak"
    ],
    [
        location: "bundles/extensions/discovery/standalone"
    ],
    [
        location: "bundles/extensions/discovery/support"
    ],
    [
        location: "bundles/extensions/event"
    ],

    [
        location: "contrib/extensions/sling-pipes",
        jdks: ["1.8"]
    ],
    [
        location: "contrib/extensions/distribution"
    ]
]

// should be sorted from the oldest to the latest version
// so that artifacts built using the oldest version are
// deployed for maximum compatibility
def defaultJdks = ["1.7", "1.8"]
def jdkMapping = [
    "1.7": "JDK 1.7 (latest)",
    "1.8": "JDK 1.8 (latest)"
]

modules.each {
  
    def svnDir = svnBase +"/" + it.location
    def jobName = "sling-" + it.location.replaceAll('/', '-')
    def jdks = it.jdks ?: defaultJdks
    def deploy = true

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

            label('Ubuntu&&!ubuntu3')

            steps {
                maven {
                   goals("clean")
                   // ensure that for multiple jdk versions only one actually deploys artifacts
                   // this should be the 'oldest' JDK
                   goals(deploy ? "deploy" : "verify")
                   mavenInstallation("Maven 3.3.9") 
                }
            }

            deploy = false

            publishers {
                archiveJunit('**/target/surefire-reports/*.xml') {
                    testDataPublishers {
                        publishTestStabilityData()
                    }
                }
                // send emails for each broken build, notify individuals as well
                mailer('commits@sling.apache.org', false, true)
            }
        }
    }
}
