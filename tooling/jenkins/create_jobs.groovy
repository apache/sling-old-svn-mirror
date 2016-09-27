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
        job(jobName + "-" + it) {
            scm {
                svn(svnDir)
            }

            triggers {
                scm('H/15 * * * *')
            }

            out.println("jdk key is " + it + " , mappings is " + jdkMapping + " , desired version is " + jdkMapping.get(it))

            jdk(jdkMapping.get(it))

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
            }
        }
    }
}
