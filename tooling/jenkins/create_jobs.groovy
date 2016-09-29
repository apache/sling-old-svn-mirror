
def svnBase = "https://svn.apache.org/repos/asf/sling/trunk"
// all modules should be listed here
// keys:
//   - location ( required ) : the SVN directory relatory to svnBase
//   - jdks (optional) : override the default jdks to use for build
//   - downstream (optional): list of downstream projects
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
        location: 'bundles/extensions/models/validation-impl',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/extensions/serviceusermapper'
    ],
    [
        location: 'bundles/extensions/settings'
    ],
    [
        location: 'bundles/extensions/validation/api',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/extensions/validation/core',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/extensions/validation/examples',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/extensions/validation/it-http',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/extensions/validation/test-services',
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
        location: 'bundles/scripting/sightly/testing',
        jdks: ['1.8']
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
        location: 'launchpad/api',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/base',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/builder',
        jdks: ["1.8"],
        downstream: ["launchpad/testing", "launchpad/testing-war"]
    ],
    [
        location: 'launchpad/content',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/installer',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/integration-tests',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/test-bundles',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/test-fragment',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/test-services-war',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/test-services',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/testing-war',
        jdks: ["1.8"]
    ],
    [
        location: 'launchpad/testing',
        jdks: ["1.8"]
    ],
    [
        location: "parent",
        jdks: ["1.7"]
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
    ],
    [
        location: 'tooling/support/install'
    ],
    [
        location: 'tooling/support/provisioning-model'
    ],
    [
        location: 'tooling/support/source'
    ],
]

// TODO - move job definitions to separate file to separate data from code

// should be sorted from the oldest to the latest version
// so that artifacts built using the oldest version are
// deployed for maximum compatibility
def defaultJdks = ["1.7", "1.8"]
def defaultMvn = "Maven 3.3.9"
def defaultSlave = "Ubuntu&&!ubuntu3"

def jdkMapping = [
    "1.7": "JDK 1.7 (latest)",
    "1.8": "JDK 1.8 (latest)"
]

modules.each { module ->
  
    def svnDir = svnBase +"/" + module.location
    def jdks = module.jdks ?: defaultJdks
    def deploy = true

    def downstreamProjects = module.downstream?: []
    // assume that all modules from bundles and installer are deployed in the launchpad
    // this might be a little to heavy right now since
    //
    // 1. Not all modules are at a snapshot version in the launchpad
    // 2. Not all modules from those location are present in the launchpad
    //
    // but for now it's a good start
    if ( module.location.startsWith('bundles/') ||
        module.location.startsWith('installer/') ) {
        downstreamProjects.add('launchpad/builder')
    }

    def downstreamEntries = modules.findAll { downstreamProjects.contains(it.location) }
    def downstreamJobs = []

    downstreamEntries.each { downstreamEntry ->
        def downstreamJdks = downstreamEntry.jdks?: defaultJdks
        def downstreamLocation = downstreamEntry.location
        downstreamJdks.each { downstreamJdk ->
            downstreamJobs.add(jobName(downstreamLocation,downstreamJdk))
        }
    }

    jdks.each { jdkKey ->
        mavenJob(jobName(module.location, jdkKey)) {

            description('''
<p>This build was automatically generated and any manual edits will be lost.</p>
<p>See <a href="https://cwiki.apache.org/confluence/display/SLING/Sling+Jenkins+Setup">Sling Jenkins Setup</a>
for more details</p>''')

            logRotator {
                numToKeep(15)
            }

            scm {
                svn(svnDir)
            }

            // note on dependency managment
            // we ask Jenkins to create dependencies between projects automatically
            // if SNAPSHOT dependencies are found between projects. This might create
            // too many dependencies, e.g. if foo-core depends on foo-core and projects
            // build for Java 1.7 and 1.8 the following depdendencies will be created
            //
            // foo-api-1.7 → foo-core-1.7
            // foo-api-1.7 → foo-core-1.8
            // foo-api-1.8 → foo-core-1.7
            // foo-api-1.8 → foo-core-1.8
            //
            // in effect we will trigger builds twice as often as needed. Since SNAPSHOT
            // dependencies are not that often found between bundles and builds should
            // be quick this is acceptable for now. The alternative would be to define
            // the dependencies manually, which is cumbersome and error-prone

            triggers {
                snapshotDependencies(true)
                scm('H/15 * * * *')
            }

            blockOnUpstreamProjects()

            jdk(jdkMapping.get(jdkKey))

            mavenInstallation(defaultMvn)

            // we have no use for archived artifacts since they are deployed on
            // repository.apache.org so speed up the build a bit (and probably
            // save on disk space)
            archivingDisabled(true)

            label(defaultSlave)

            // ensure that only one job deploys artifacts
            // besides being less efficient, it's not sure which
            // job is triggered first and we may end up with a
            // mix of Java 7 and Java 8 artifacts for projects which
            // use these 2 versions
            goals(deploy ? "-U clean deploy" : "-U clean verify");
            deploy = false

            publishers {
                if ( downstreamJobs ) {
                    downstream(downstreamJobs)
                }

                // TODO - can we remove the glob and rely on the defaults?
                archiveJunit('**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml') {
                    allowEmptyResults()
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

String jobName(String location, String jdk) {
    return "sling-" + location.replaceAll('/','-')+'-' + jdk;
}
