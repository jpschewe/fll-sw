#!/usr/bin/env groovy

pipeline {

  options { buildDiscarder(logRotator(numToKeepStr: '5')) }

  agent {
    label 'fll-sw_linux'
  }

  stages {
    stage('Init') {
      steps {
        echo "NODE_NAME = ${env.NODE_NAME}"
        echo "My branch is: ${env.BRANCH_NAME}"
        printEnv()      
      }
    }

    stage('Build Checker') {
        steps {
                echo "Using stock checker framework"
        
            // setup local checker repository
            /*
                           echo "Using custom checker framework"                 
            dir("checker") {
                checkout changelog: false, 
                    poll: false, 
                    scm: [$class: 'GitSCM', 
                        branches: [[name: 'refs/tags/checker-framework-3.10.0']], 
                        doGenerateSubmoduleConfigurations: false, 
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'checker-framework']], 
                        submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/typetools/checker-framework.git']]]

                checkout changelog: false, 
                    poll: false, 
                    scm: [$class: 'GitSCM', 
                        branches: [[name: 'refs/tags/3.10.0']], 
                        doGenerateSubmoduleConfigurations: false, 
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'annotation-tools']], 
                        submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/typetools/annotation-tools.git']]]

                checkout changelog: false, 
                    poll: false, 
                    scm: [$class: 'GitSCM', 
                        branches: [[name: '25e3694']], 
                        doGenerateSubmoduleConfigurations: false, 
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'stubparser']], 
                        submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/typetools/stubparser.git']]]

                // my copy of the annotated jdk
                checkout changelog: false, 
                    poll: false, 
                    scm: [$class: 'GitSCM', 
                        branches: [[name: 'refs/heads/jps-dev']], 
                        doGenerateSubmoduleConfigurations: false, 
                        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'jdk']], 
                        submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/jpschewe/checker-typetools-jdk']]]
                                                                       
                dir("checker-framework") {
                    callGradle("assemble")
                } // dir checker-framework
            } // dir checker
            stash name: 'checker_build_data', includes: 'checker/checker-framework/checker/dist/**'           
*/

        } // steps
    } // stage
    
    stage('Copy checker build to Windows') {
        steps {
            //unstash name: 'checker_build_data'
            echo "Using stock checker"                      
        }
    }
    
    stage('Duplicate Code Analysis') {
      steps { 
        callGradle('cpdCheck')
        recordIssues tool: cpd(pattern: 'build/reports/cpd/cpdCheck.xml')        
      }     
    }

    stage('Test compilation of JSPs') {
      steps { 
        callGradle('jspToJava')
      }     
    }

    stage('Count lines of code') {
      steps { 
        callGradle('sloccount')
        sloccountPublish pattern: 'build/reports/sloccount/cloc.xml'
      }
    }

    stage('Checkstyle analysis') {
      steps { 
        callGradle('checkstyleMain')
        callGradle('checkstyleTest')
        callGradle('checkstyleIntegrationTest')
        recordIssues tool: checkStyle(pattern: 'build/reports/checkstyle/*.xml'), qualityGates: [[threshold: 1, type: 'TOTAL', unstable: true]]
      }
    }

    stage('Tests') {
      steps {
        timeout(time: 3, unit: 'HOURS') {      
          // runs all of the test tasks
          callGradle('test')
          xvnc {
            callGradle('integrationTest')
          }
          callGradle('jacocoTestReport')
          junit testResults: "build/test-results/*est/TEST-*.xml", keepLongStdio: true
          // jacoco classPattern: 'build/classes/*/*', execPattern: 'build/jacoco/*.exec', sourcePattern: 'src/main/java,src/test/java,src/integrationTest/java'
          recordCoverage sourceDirectories: [[path: 'src/integrationTest/java'], [path: 'src/main/java,src/test/java']], tools: [[pattern: 'build/reports/jacoco/**/*.xml']]
        }                
      }
    }

    stage('SpotBugs analysis') {
      steps { 
        callGradle('spotbugsMain')
        callGradle('spotbugsTest')
        callGradle('spotbugsIntegrationTest')
        recordIssues tool: spotBugs(pattern: 'build/reports/spotbugs/*.xml', useRankAsPriority: true), qualityGates: [[threshold: 1, type: 'TOTAL', unstable: true]]
      }
    }

    stage('Distribution') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            timeout(time: 1, unit: 'HOURS') {
              callGradle('distZip')
            }
          } // timestamps
        } // throttle
      } // steps           
    } // Distribution stage
    
    stage('Windows Distribution') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE', message: 'Assuming distribution failed because a new version of OpenJDK was released') {
              timeout(time: 1, unit: 'HOURS') {
                callGradle('windowsDistZip')                                        
              }
            }
          } // timestamps
        } // throttle
      } // steps           
    } // Windows Distribution stage
    
    stage('Linux Distribution') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE', message: 'Assuming distribution failed because a new version of OpenJDK was released') {
              timeout(time: 1, unit: 'HOURS') {
                callGradle('linuxDistTar')
              }
            }
          } // timestamps
        } // throttle
      } // steps           
    } // Linux Distribution stage

    stage('Mac Distribution') {
      steps {
          timestamps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE', message: 'Assuming distribution failed because a new version of OpenJDK was released') {
              timeout(time: 1, unit: 'HOURS') {
                callGradle('macDistTar')
              }
            }
          } // timestamps
      } // steps           
    } // Linux Distribution stage
        
    stage('Gather results') {
        steps {
            timestamps {
                archiveArtifacts artifacts: 'logs/,screenshots/,build/distributions/'
                
                recordIssues tool: taskScanner(includePattern: '**/*.java,**/*.jsp,**/*.jspf,**/*.xml', excludePattern: 'checkstyle*.xml,build/reports/**', highTags: 'FIXME,HACK', normalTags: 'TODO')
                
                recordIssues tool: java(), qualityGates: [[threshold: 1, type: 'TOTAL', unstable: true]]  

                recordIssues tool: javaDoc(), qualityGates: [[threshold: 1, type: 'TOTAL', unstable: true]]
                
                publishHTML (target: [
                    allowMissing: true,
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: 'build/gen-documentation',
                    reportFiles: 'index.html',
                    reportName: 'Documentation'
                ])
                                                            
            }            
        }
    }
    
  } // stages
    
  post {
    always {      
      emailext recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']], 
          to: 'jpschewe@mtu.net',
          subject: '${PROJECT_NAME} - Build # ${BUILD_NUMBER} - ${BUILD_STATUS}!', 
          body: '''${PROJECT_NAME} - Build # ${BUILD_NUMBER} - ${BUILD_STATUS}

Changes:
${CHANGES}


Failed Tests:
${FAILED_TESTS}

Check console output at ${BUILD_URL} to view the full results.

Find more details at: ${JENKINS_URL}
'''
                
    } // always
  } // post

} // pipeline

def callGradle(task) {
  //def args='--no-daemon -Dtest.ignoreFailures=true'
  def args='-Dtest.ignoreFailures=true'

  if (isUnix()) {
    sh script: "./gradlew ${args} ${task}"
  } else {
    bat script: "gradlew.bat ${args} ${task}"
  }
}

def printEnv() {
  echo "Environment Variables"
  if (isUnix()) {
    sh script: 'printenv'
  } else {
    bat script: 'set'
  }
  echo "end Environment Variables"
}
