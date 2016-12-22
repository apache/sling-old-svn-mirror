def svnBase = "https://svn.apache.org/repos/asf/sling/trunk"
// all modules should be listed here
// keys:
//   - location ( required ) : the SVN directory relatory to svnBase
//   - jdks (optional) : override the default jdks to use for build
//   - downstream (optional): list of downstream projects
//   - archive (optional): list of archive patterns
//   - extraGoalsParams (optional): additional string for the Maven goals to execute
//   - rebuildDaily (optional): boolean, when enabled configures the build to run once every
//                                24 hours,even if no changes are found in source control
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
        location: 'bundles/commons/log-webconsole'
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
        location: 'bundles/extensions/caconfig/api'
    ],
    [
        location: 'bundles/extensions/caconfig/impl'
    ],
    [
        location: 'bundles/extensions/caconfig/integration-tests',
        jdks: ["1.8"]
    ],
    [
        location: 'bundles/extensions/caconfig/spi'
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
        location: 'bundles/extensions/models/jackson-exporter'
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
        location: 'bundles/jcr/oak-server',
        rebuildDaily : true
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
        location: 'bundles/scripting/sightly/compiler'
    ],
    [
        location: 'bundles/scripting/sightly/java-compiler'
    ],
    [
        location: 'bundles/scripting/sightly/engine',
        downstream: [
            'bundles/scripting/sightly/testing-content',
            'bundles/scripting/sightly/testing'
        ]
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
        location: 'bundles/scripting/sightly/testing-content',
        downstream: ['bundles/scripting/sightly/testing']
    ],
    [
        location: 'bundles/scripting/sightly/testing',
        jdks: ['1.8']
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
        location: 'contrib/auth/org.apache.sling.auth.xing.api'
    ],
    [
        location: 'contrib/auth/org.apache.sling.auth.xing.login'
    ],
    [
        location: 'contrib/auth/org.apache.sling.auth.xing.oauth'
    ],
    [
        location: 'contrib/commons/html'
    ],
    [
        location: 'contrib/commons/mom/api'
    ],
    [
        location: 'contrib/commons/mom/jms'
    ],
    [
        location: 'contrib/commons/mom/jobs/core'
    ],
    [
        location: 'contrib/commons/mom/jobs/it-services'
    ],
    [
        location: 'contrib/commons/mom/jobs/it'
    ],
    [
        location: 'contrib/crankstart/launcher',
        extraGoalsParams: '-Dorg.ops4j.pax.url.mvn.repositories=http://repo.maven.apache.org/maven2@id=apache-releases,http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache-snapshots'
    ],
    [
        location: 'contrib/crankstart/test-services'
    ],
    [
        location: 'contrib/crankstart/test-model'
    ],
    [
        location: 'contrib/explorers/resourceeditor'
    ],
    [
        location: 'contrib/extensions/cache/api'
    ],
    [
        location: 'contrib/extensions/cache/container-test'
    ],
    [
        location: 'contrib/extensions/cache/ehcache'
    ],
    [
        location: 'contrib/extensions/cache/impl'
    ],
    [
        location: 'contrib/extensions/cache/infinispan'
    ],
    [
        location: 'contrib/extensions/cache/portal'
    ],
    [
        location: 'contrib/extensions/cassandra'
    ],
    [
        location: 'contrib/extensions/collection'
    ],
    [
        location: 'contrib/extensions/datasource'
    ],
    [
        location: "contrib/extensions/sling-pipes",
        jdks: ["1.8"]
    ],
    [
        location: 'contrib/extensions/distribution/api'
    ],
    [
        location: 'contrib/extensions/distribution/core'
    ],
    [
        location: 'contrib/extensions/distribution/it',
        jdks: ['1.8'],
        archive: ["**/logs/error.log"]
    ],
    [
        location: 'contrib/extensions/distribution/sample'
    ],
    [
        location: 'contrib/extensions/distribution/extensions'
    ],
    [
        location: 'contrib/extensions/hapi/samplecontent'
    ],
    [
        location: 'contrib/extensions/hapi/core'
    ],
    [
        location: 'contrib/extensions/hapi/client'
    ],
    [
        location: 'contrib/extensions/jmxprovider'
    ],
    [
        location: 'contrib/extensions/leak-detector'
    ],
    [
        location: 'contrib/extensions/logback-groovy-fragment'
    ],
    [
        location: 'contrib/extensions/mongodb'
    ],
    [
        location: 'contrib/extensions/reqanalyzer'
    ],
    [
        location: 'contrib/extensions/resource-inventory'
    ],
    [
        location: 'contrib/extensions/resourcemerger'
    ],
    [
        location: 'contrib/extensions/rewriter',
        jdks: ['1.8']
    ],
    [
        location: 'contrib/extensions/security'
    ],
    [
        location: 'contrib/extensions/slf4j-mdc'
    ],
    [
        location: 'contrib/extensions/sling-query'
    ],
    [
        location: 'contrib/extensions/startup-filter-disabler'
    ],
    [
        location: 'contrib/extensions/startup-filter'
    ],
    [
        location: 'contrib/extensions/superimposing'
    ],
    [
        location: 'contrib/extensions/tenant'
    ],
    [
        location: 'contrib/extensions/urlrewriter'
    ],
    [
        location: 'contrib/extensions/tracer'
    ],
    [
        location: 'contrib/extensions/logtail'
    ],
    [
        location: 'contrib/nosql/couchbase-client'
    ],
    [
        location: 'contrib/nosql/couchbase-resourceprovider'
    ],
    [
        location: 'contrib/nosql/generic'
    ],
    [
        location: 'contrib/nosql/launchpad'
    ],
    [
        location: 'contrib/nosql/mongodb-resourceprovider'
    ],
    [
        location: 'contrib/extensions/sling-dynamic-include'
    ],
    [
        location: 'contrib/extensions/oak-restrictions'
    ],
    [
        location: 'contrib/jcr/resourcesecurity'
    ],
    [
        location: 'contrib/jcr/js/nodetypes'
    ],
    [
        location: 'contrib/launchpad/debian'
    ],
    [
        location: 'contrib/launchpad/testing'
    ],
    [
        location: 'contrib/scripting/freemarker',
        jdks: ['1.8']
    ],
    [
        location: 'contrib/scripting/groovy'
    ],
    [
        location: 'contrib/scripting/java'
    ],
    [
        location: 'contrib/scripting/script-console'
    ],
    [
        location: 'contrib/scripting/xproc'
    ],
    [
        location: 'contrib/scripting/org.apache.sling.scripting.thymeleaf',
        jdks: ['1.8']
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
        location: 'installer/factories/packages'
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
//    [
//        location: 'karaf/org.apache.sling.karaf-distribution'
//    ],
//    [
//        location: 'karaf/org.apache.sling.karaf-features'
//    ],
//    [
//        location: 'karaf/org.apache.sling.karaf-integration-tests'
//    ],
//    [
//        location: 'karaf/org.apache.sling.karaf-launchpad-oak-tar-integration-tests'
//    ],
//    [
//        location: 'karaf/org.apache.sling.karaf-repoinit'
//    ],
//    [
//        location: 'karaf/org.apache.sling.karaf-configs'
//    ],
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
        downstream: ["launchpad/testing", "launchpad/testing-war"],
        archive: ["**/logs/error.log"]
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
        jdks: ["1.8"],
        downstream: ["launchpad/testing", "launchpad/testing-war"]
    ],
    [
        location: 'launchpad/test-fragment',
        jdks: ["1.8"],
        downstream: ["launchpad/test-bundles"]
    ],
    [
        location: 'launchpad/test-services-war',
        jdks: ["1.8"],
        downstream: ["launchpad/test-bundles"]
    ],
    [
        location: 'launchpad/test-services',
        jdks: ["1.8"],
        downstream: ["launchpad/test-bundles"]
    ],
    [
        location: 'launchpad/testing-war',
        jdks: ["1.8"],
        archive: ["**/logs/error.log"]
    ],
    [
        location: 'launchpad/testing',
        jdks: ["1.8"],
        archive: ["**/logs/error.log"]
    ],
    [
        location: "parent",
        jdks: ["1.7"]
    ],
    [
        location: 'samples/accessmanager-ui'
    ],
    [
        location: 'samples/custom-login-form'
    ],
    [
        location: 'samples/custom-selector-login-form'
    ],
    [
        location: 'samples/espblog'
    ],
    [
        location: 'samples/fling',
        jdks: ["1.8"]
    ],
    [
        location: 'samples/framework-fragment'
    ],
    [
        location: 'samples/installing-dependencies'
    ],
    [
        location: 'samples/javashell'
    ],
    [
        location: 'samples/mail-archive/james-wrapper'
    ],
    [
        location: 'samples/mail-archive/server'
    ],
    [
        location: 'samples/mail-archive/stats'
    ],
    [
        location: 'samples/mail-archive/ui'
    ],
    [
        location: 'samples/path-based-rtp'
    ],
    [
        location: 'samples/post-servlet-extensions'
    ],
    [
        location: 'samples/simple-launchpad'
    ],
    [
        location: 'samples/slingbucks'
    ],
    [
        location: 'samples/slingshot'
    ],
    [
        location: 'samples/urlfilter'
    ],
    [
        location: 'samples/usermanager-ui'
    ],
    [
        location: 'samples/webloader/service'
    ],
    [
        location: 'samples/webloader/ui'
    ],
    [
        location: 'samples/workspacepicker'
    ],
    [
        location: 'testing/junit/core',
        downstream: ["launchpad/test-bundles"]
    ],
    [
        location: 'testing/junit/healthcheck'
    ],
    [
        location: 'testing/junit/performance'
    ],
    [
        location: 'testing/junit/remote',
        downstream: ["launchpad/test-bundles"]
    ],
    [
        location: 'testing/junit/scriptable',
        downstream: ["launchpad/test-bundles"]
    ],
    [
        location: 'testing/junit/teleporter'
    ],
    [
        location: 'testing/junit/rules'
    ],
    [
        location: 'testing/mocks/caconfig-mock-plugin'
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
        location: 'tooling/bnd/caconfig-bnd-plugin'
    ],
    [
        location: 'tooling/bnd/models-bnd-plugin'
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
def defaultSlave = "ubuntu"

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
                svn(svnDir) { svnNode ->
                    svnNode / browser(class: 'hudson.scm.browsers.ViewSVN') /
                        url << 'http://svn.apache.org/viewcvs.cgi/?root=Apache-SVN'
                }
            }

            triggers {
                snapshotDependencies(true)
                scm('H/15 * * * *')
                if ( module.rebuildDaily ) {
                    cron('@daily')
                }
            }

            // timeout if the job takes 4 times longer than the average
            // duration of the last 3 jobs. Defaults to 30 minutes if
            // no previous job executions are found
            wrappers {
                timeout {
                    elastic(400, 3, 30)
                }
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
            def extraGoalsParams = module.extraGoalsParams ?: ""
            goals( (deploy ? "-U clean deploy" : "-U clean verify") + " " + extraGoalsParams)

            publishers {
                if ( deploy && downstreamJobs ) {
                    downstream(downstreamJobs)
                }

                if (module.archive) {
                    archiveArtifacts() {
                        module.archive.each { archiveEntry ->
                            pattern(archiveEntry)
                        }
                    }
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

            deploy = false
        }
    }
}

String jobName(String location, String jdk) {
    return "sling-" + location.replaceAll('/','-')+'-' + jdk;
}
