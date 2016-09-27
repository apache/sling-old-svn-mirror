def svnBase = "https://svn.apache.org/repos/asf/sling/trunk"
def modules = ["bundles/extensions/i18n", "contrib/extensions/sling-pipes"]

modules.each {
  
    def svnDir = svnBase +"/" + it
    def jobName = "sling-" + it.replaceAll('/', '-')

    job(jobName) {
        scm {
            svn(svnDir)
        }

        triggers {
            scm('H/15 * * * *')
        }
        
        jdk('JDK 1.7 (latest)')

        label('ubuntu1||ubuntu2||ubuntu4||ubuntu5||ubuntu6')

        steps {
            maven {
               goals("clean")
               goals("verify")
               mavenInstallation("Maven 3.3.9") 
            }
        }
    }
}
