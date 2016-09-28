def svnBase = "https://svn.apache.org/repos/asf/sling/trunk"
def modules = [
    [
        location: 'bundles/api'
    ],
    [
        location: 'bundles/auth/core'
    ],
    [
        location: 'bundles/auth/form'
    ],
    [
        location: 'bundles/commons/classloader'
    ],
    [
        location: 'bundles/commons/compiler'
    ],
    [
        location: 'bundles/commons/json'
    ],
    [
        location: 'bundles/commons/log'
    ],
    [
        location: 'bundles/commons/logservice'
    ],
    [
        location: 'bundles/commons/mime'
    ],
    [
        location: 'bundles/commons/osgi'
    ],
    [
        location: 'bundles/commons/scheduler'
    ],
    [
        location: 'bundles/commons/testing'
    ],
    [
        location: 'bundles/commons/threaddump'
    ],
    [
        location: 'bundles/commons/threads'
    ],
    [
        location: 'bundles/commons/fsclassloader'
    ],
    [
        location: 'bundles/commons/contentdetection'
    ],
    [
        location: 'bundles/commons/metrics'
    ],
    [
        location: 'bundles/commons/org.apache.sling.commons.messaging.mail',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/commons/org.apache.sling.commons.messaging',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/engine'
    ],
    [
        location: 'bundles/extensions/adapter'
    ],
    [
        location: 'bundles/extensions/bundleresource'
    ],
    [
        location: 'bundles/extensions/dea'
    ],
    [
        location: 'bundles/extensions/discovery/api'
    ],
    [
        location: 'bundles/extensions/discovery/impl'
    ],
    [
        location: 'bundles/extensions/discovery/standalone'
    ],
    [
        location: 'bundles/extensions/discovery/support'
    ],
    [
        location: 'bundles/extensions/discovery/commons'
    ],
    [
        location: 'bundles/extensions/discovery/base'
    ],
    [
        location: 'bundles/extensions/discovery/oak'
    ],
    [
        location: 'bundles/extensions/event'
    ],
    [
        location: 'bundles/extensions/explorer'
    ],
    [
        location: 'bundles/extensions/feature-flags'
    ],
    [
        location: 'bundles/extensions/framework-extension-activation'
    ],
    [
        location: 'bundles/extensions/framework-extension-transaction'
    ],
    [
        location: 'bundles/extensions/framework-extension-ws'
    ],
    [
        location: 'bundles/extensions/framework-extension-xml'
    ],
    [
        location: 'bundles/extensions/fsresource'
    ],
    [
        location: 'bundles/extensions/healthcheck/annotations'
    ],
    [
        location: 'bundles/extensions/healthcheck/core'
    ],
    [
        location: 'bundles/extensions/healthcheck/it'
    ],
    [
        location: 'bundles/extensions/healthcheck/junit-bridge'
    ],
    [
        location: 'bundles/extensions/healthcheck/samples'
    ],
    [
        location: 'bundles/extensions/healthcheck/support'
    ],
    [
        location: 'bundles/extensions/healthcheck/webconsole'
    ],
    [
        location: 'bundles/extensions/javax.activation'
    ],
    [
        location: 'bundles/extensions/models/api'
    ],
    [
        location: 'bundles/extensions/models/impl'
    ],
    [
        location: 'bundles/extensions/models/integration-tests'
    ],
    [
        location: 'bundles/extensions/models/validation-impl'
    ],
    [
        location: 'bundles/extensions/serviceusermapper'
    ],
    [
        location: 'bundles/extensions/settings'
    ],
    [
        location: 'bundles/extensions/validation/api'
    ],
    [
        location: 'bundles/extensions/validation/core'
    ],
    [
        location: 'bundles/extensions/validation/examples'
    ],
    [
        location: 'bundles/extensions/validation/it-http'
    ],
    [
        location: 'bundles/extensions/validation/test-services'
    ],
    [
        location: 'bundles/extensions/validation',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/extensions/webconsolebranding'
    ],
    [
        location: 'bundles/extensions/webconsolesecurityprovider'
    ],
    [
        location: 'bundles/extensions/i18n'
    ],
    [
        location: 'bundles/extensions/xss'
    ],
    [
        location: 'bundles/extensions/resourcebuilder'
    ],
    [
        location: 'bundles/extensions/servlet-helpers'
    ],
    [
        location: 'bundles/extensions/repoinit/it'
    ],
    [
        location: 'bundles/extensions/repoinit/parser'
    ],
    [
        location: 'bundles/jcr/api'
    ],
    [
        location: 'bundles/jcr/base'
    ],
    [
        location: 'bundles/jcr/classloader'
    ],
    [
        location: 'bundles/jcr/contentloader'
    ],
    [
        location: 'bundles/jcr/davex'
    ],
    [
        location: 'bundles/jcr/it-jackrabbit-oak'
    ],
    [
        location: 'bundles/jcr/it-resource-versioning'
    ],
    [
        location: 'bundles/jcr/jackrabbit-accessmanager'
    ],
    [
        location: 'bundles/jcr/jackrabbit-base'
    ],
    [
        location: 'bundles/jcr/jackrabbit-server'
    ],
    [
        location: 'bundles/jcr/jackrabbit-usermanager'
    ],
    [
        location: 'bundles/jcr/jcr-wrapper'
    ],
    [
        location: 'bundles/jcr/oak-server'
    ],
    [
        location: 'bundles/jcr/registration'
    ],
    [
        location: 'bundles/jcr/resource'
    ],
    [
        location: 'bundles/jcr/webconsole'
    ],
    [
        location: 'bundles/jcr/webdav'
    ],
    [
        location: 'bundles/jcr/repoinit'
    ],
    [
        location: 'bundles/resourceaccesssecurity/core'
    ],
    [
        location: 'bundles/resourceaccesssecurity/it'
    ],
    [
        location: 'bundles/resourceresolver'
    ],
    [
        location: 'bundles/scripting/api'
    ],
    [
        location: 'bundles/scripting/core'
    ],
    [
        location: 'bundles/scripting/javascript'
    ],
    [
        location: 'bundles/scripting/jsp-jstl'
    ],
    [
        location: 'bundles/scripting/jsp-taglib'
    ],
    [
        location: 'bundles/scripting/jsp'
    ],
    [
        location: 'bundles/scripting/sightly/engine'
    ],
    [
        location: 'bundles/scripting/sightly/js-use-provider'
    ],
    [
        location: 'bundles/scripting/sightly/models-use-provider'
    ],
    [
        location: 'bundles/scripting/sightly/repl'
    ],
    [
        location: 'bundles/scripting/sightly/testing-content'
    ],
    [
        location: 'bundles/scripting/sightly/testing'
    ],
    [
        location: 'bundles/scripting/sightly/compiler'
    ],
    [
        location: 'bundles/scripting/sightly/java-compiler'
    ],
    [
        location: 'bundles/servlets/compat'
    ],
    [
        location: 'bundles/servlets/get'
    ],
    [
        location: 'bundles/servlets/post'
    ],
    [
        location: 'bundles/servlets/resolver'
    ],
    [
        location: "contrib/extensions/sling-pipes",
        jdks: ["1.8"]
    ],
    [
        location: "contrib/extensions/distribution"
    ],
    [
        location: "installer/console"
    ],
    [
        location: "installer/core"
    ],
    [
        location: 'installer/factories/configuration'
    ],
    [
        location: 'installer/factories/deploymentpck'
    ],
    [
        location: 'installer/factories/subsystems'
    ],
    [
        location: 'installer/factories/subsystem_base'
    ],
    [
        location: "installer/hc"
    ],
    [
        location: "installer/it"
    ],
    [
        location: "installer/providers/jcr"
    ],
    [
        location: "installer/providers/file"
    ],
    [
        location: 'testing/junit/core'
    ],
    [
        location: 'testing/junit/healthcheck'
    ],
    [
        location: 'testing/junit/performance'
    ],
    [
        location: 'testing/junit/remote'
    ],
    [
        location: 'testing/junit/scriptable'
    ],
    [
        location: 'testing/junit/teleporter'
    ],
    [
        location: 'testing/junit/rules'
    ],
    [
        location: 'testing/mocks/jcr-mock'
    ],
    [
        location: 'testing/mocks/osgi-mock'
    ],
    [
        location: 'testing/mocks/resourceresolver-mock'
    ],
    [
        location: 'testing/mocks/sling-mock'
    ],
    [
        location: 'testing/mocks/logging-mock'
    ],
    [
        location: 'testing/mocks/sling-mock-oak'
    ],
    [
        location: 'testing/samples/bundle-with-it'
    ],
    [
        location: 'testing/samples/module-with-it',
        jdks: ["1.8"]
    ],
    [
        location: 'testing/sling-pax-util'
    ],
    [
        location: 'testing/tools'
    ],
    [
        location: 'testing/hamcrest'
    ],
    [
        location: 'testing/http/clients'
    ],
    [
        location: 'testing/serversetup'
    ],
    [
        location: 'testing/org.apache.sling.testing.paxexam',
        jdks: ["1.8"]
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
                   goals("-U")
                   goals("clean")
                   // ensure that for multiple jdk versions only one actually deploys artifacts
                   // this should be the 'oldest' JDK
                   goals(deploy ? "deploy" : "verify")
                   mavenInstallation("Maven 3.3.9") 
                }
            }

            deploy = false

            publishers {
                archiveJunit('**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml') {
                    allowEmptyResults()
                    testDataPublishers {
                        publishTestStabilityData()
                    }
                }
                // send emails for each broken build, notify individuals as well
//                mailer('commits@sling.apache.org', false, true)
            }
        }
    }
}
