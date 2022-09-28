// required plugins:
// - OAuth Credentials plugin, org.jenkins-ci.plugins:oauth-credentials:0.4
// - Google Container Registry Auth0, google-container-registry-auth:0.3

def projectVersion
def imageForGradleStages = 'openjdk:17-jdk-bullseye'
def dockerArgsForGradleStages = '-v /data/gradle-homes/executor-$EXECUTOR_NUMBER:/gradle-home -e GRADLE_USER_HOME=/gradle-home'

def withDockerNetwork(Closure inner) {
  try {
    networkId = UUID.randomUUID().toString()
    sh "docker network create ${networkId}"
    inner.call(networkId)
  } finally {
    sh "docker network rm ${networkId}"
  }
}

pipeline {
    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '5'))
    }

    environment {
        // In case the build server exports a custom JAVA_HOME, we fix the JAVA_HOME
        // to the one used by the docker image.
        JAVA_HOME='/usr/local/openjdk-17'
        GRADLE_OPTS='-Dhttp.proxyHost=cache.sernet.private -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.sernet.private -Dhttps.proxyPort=3128'
        // pass -Pci=true to gradle, https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
        ORG_GRADLE_PROJECT_ci=true
    }

    stages {
        stage('Setup') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh 'env'
                buildDescription "${env.GIT_BRANCH} ${env.GIT_COMMIT[0..8]}"
                script {
                    projectVersion = sh(returnStdout: true, script: '''./gradlew properties -q | awk '/^version:/ {print $2}' ''').trim()
                }
            }
        }
        stage('Build') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew --no-daemon classes'
            }
        }
        stage('Test') {
            agent any
            steps {
                 script {
                     withDockerNetwork{ n ->
                         docker.image(imageForGradleStages).inside("${dockerArgsForGradleStages} --network ${n}") {
                            // Don't fail the build here, let the junit step decide what to do if there are test failures.
                             sh script: './gradlew --no-daemon test'
                             // Touch all test results (to keep junit step from complaining about old results).
                             sh script: 'find build/test-results | xargs touch'
                             junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
                             jacoco classPattern: '**/build/classes/java/main'
                         }
                     }
                 }
            }
        }
        stage('Artifacts') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew --no-daemon -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME clean build -x test'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
        stage('Dockerimage') {
            agent {
                label 'docker-image-builder'
            }
            steps {
                script {
                    def dockerImage = docker.build("eu.gcr.io/veo-projekt/veo-accounts:git-${env.GIT_COMMIT}", "--build-arg VEO_ACCOUNTS_VERSION='$projectVersion' --label org.opencontainers.image.version='$projectVersion' --label org.opencontainers.image.revision='$env.GIT_COMMIT' .")
                    // Finally, we'll push the image with several tags:
                    // Pushing multiple tags is cheap, as all the layers are reused.
                    withDockerRegistry(credentialsId: 'gcr:verinice-projekt@gcr', url: 'https://eu.gcr.io') {
                        dockerImage.push("git-${env.GIT_COMMIT}")
                        if (env.GIT_BRANCH == 'main') {
                            dockerImage.push(projectVersion)
                            dockerImage.push("latest")
                            dockerImage.push("main-build-${env.BUILD_NUMBER}")
                        } else if (env.GIT_BRANCH == 'develop') {
                            dockerImage.push("develop")
                            dockerImage.push("develop-build-${env.BUILD_NUMBER}")
                        }
                    }
                }
            }
        }
        stage('HTTP REST Test') {
            environment {
                KEYCLOAK_SERVICE_CLIENT_SECRET = credentials("keycloak_service_client_secret")
                KEYCLOAK_SERVICE_PROXY_HOST = "cache.int.sernet.de"
            }
            agent any
            steps {
                script {
                    withDockerNetwork { n ->
                        docker.image("eu.gcr.io/veo-projekt/veo-accounts:git-${env.GIT_COMMIT}").withRun("\
                --network ${n}\
                --name veo-accounts-${n}\
                -m 1g\
                -e VEO_ACCOUNTS_KEYCLOAK_PROXYHOST=${env.KEYCLOAK_SERVICE_PROXY_HOST}\
                -e VEO_ACCOUNTS_KEYCLOAK_CLIENTS_SERVICE_SECRET=${env.KEYCLOAK_SERVICE_CLIENT_SECRET}\
                -e VEO_ACCOUNTS_KEYCLOAK_MAILING_ENABLED=false\
                -e 'VEO_CORS_ORIGINS=https://*.verinice.example, https://frontend.somewhereelse.example'\
                -e 'JDK_JAVA_OPTIONS=-Dhttp.proxyHost=${env.KEYCLOAK_SERVICE_PROXY_HOST} -Dhttp.proxyPort=3128 -Dhttps.proxyHost=${env.KEYCLOAK_SERVICE_PROXY_HOST} -Dhttps.proxyPort=3128 -Dhttps.proxySet=true -Dhttp.proxySet=true'") { veoAccounts ->
                            docker.image(imageForGradleStages).inside("${dockerArgsForGradleStages}\
                    --network ${n}\
                    -e VEO_RESTTEST_BASEURL=http://veo-accounts-${n}:8099") {
                                echo 'Waiting for container startup'
                                timeout(2) {
                                    waitUntil {
                                        script {
                                            def r = sh returnStatus: true, script: "wget --no-proxy -q http://veo-accounts-${n}:8099/actuator/health -O /dev/null"
                                            // TODO return r == 0
                                            return r != 0
                                        }
                                    }
                                }
                                sh """export VEO_ACCOUNTS_KEYCLOAK_CLIENTS_SERVICE_SECRET=\$KEYCLOAK_SERVICE_CLIENT_SECRET && \
                                   export VEO_ACCOUNTS_KEYCLOAK_PROXYHOST=\$KEYCLOAK_SERVICE_PROXY_HOST && \
                                   ./gradlew -Dhttp.nonProxyHosts=\"localhost|veo-accounts-${n}\" -PciBuildNumber=\$BUILD_NUMBER -PciJobName=\$JOB_NAME restTest"""
                                junit allowEmptyResults: true, testResults: 'build/test-results/restTest/*.xml'
                                publishHTML([
                                    allowMissing: false,
                                    alwaysLinkToLastBuild: false,
                                    keepAll: true,
                                    reportDir: 'build/reports/tests/restTest/',
                                    reportFiles: 'index.html',
                                    reportName: 'Test report: veo-accounts-rest-integration-test'
                                ])
                                perfReport failBuildIfNoResultFile: false,
                                    modePerformancePerTestCase: true,
                                    showTrendGraphs: true,
                                    sourceDataFiles: 'build/test-results/restTest/*.xml'
                            }
                            sh "docker logs ${veoAccounts.id} > rest-test-container-logs.log"
                            archive 'rest-test-container-logs.log'
                        }
                    }
                }
            }
        }
        stage('Trigger Deployment') {
            agent any
            when {
                anyOf { branch 'main'; branch 'develop' }
            }
            steps {
                build job: 'verinice-veo-deployment/master'
            }
        }
    }
    post {
        always {
           node('') {
                recordIssues(enabledForFailure: true, tools: [java()])
                recordIssues(
                  enabledForFailure: true,
                  tools: [
                    taskScanner(
                      highTags: 'FIXME',
                      ignoreCase: true,
                      normalTags: 'TODO',
                      excludePattern: 'Jenkinsfile, gradle/wrapper/**, gradle-home/**, .gradle/**, buildSrc/.gradle/**, build/**'
                    )
                  ]
                )
            }
        }
    }
}
