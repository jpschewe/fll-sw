#!/usr/bin/env groovy

pipeline {

  options { buildDiscarder(logRotator(numToKeepStr: '5')) }

  agent {
    label 'fll-build'
  }

  stages {
    stage('Init') {
      steps {
        echo "NODE_NAME = ${env.NODE_NAME}"
        echo "My branch is: ${env.BRANCH_NAME}"
        printEnv()      
      }
    }

    stage('Duplicate Code Analysis') {
      steps { 
        fllSwGradle('cpdCheck')
        recordIssues tool: cpd(pattern: 'build/reports/cpd/cpdCheck.xml')        
      }     
    }

    stage('Test compilation of JSPs') {
      steps { 
        fllSwGradle('jspToJava')
      }     
    }

    stage('Count lines of code') {
      steps { 
        fllSwGradle('sloccount')
        sloccountPublish pattern: 'build/reports/sloccount/cloc.xml'
      }
    }

    stage('Checkstyle analysis') {
      steps { 
        fllSwGradle('checkstyleMain')
        fllSwGradle('checkstyleTest')
        fllSwGradle('checkstyleIntegrationTest')
        recordIssues tool: checkStyle(pattern: 'build/reports/checkstyle/*.xml')
      }
    }

    stage('Tests') {
      steps {
        // runs all of the test tasks
        fllSwGradle('cobertura')
        junit testResults: "build/test-results/*est/TEST-*.xml", keepLongStdio: true
        step $class: 'CoberturaPublisher', coberturaReportFile: 'build/reports/cobertura/coverage.xml'                
      }
    }

    stage('SpotBugs analysis') {
      steps { 
        fllSwGradle('spotbugsMain')
        fllSwGradle('spotbugsTest')
        fllSwGradle('spotbugsIntegrationTest')
        recordIssues tool: spotBugs(pattern: 'build/reports/spotbugs/*.xml', useRankAsPriority: true), qualityGates: [[threshold: 1, type: 'TOTAL', unstable: true]]
      }
    }

    stage('Distribution') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            fllSwGradle('distZip')
            stash name: 'build_data', includes: 'build/**', excludes: "build/tmp/**"
          } // timestamps
        } // throttle
      } // steps           
    } // Distribution stage
    
    stage('Windows Distribution') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE', message: 'Assuming distribution failed because a new version of OpenJDK was released') {
              fllSwGradle('windowsDistZip')
            }
            stash name: 'windows_distribution', includes: 'build/distributions/*'
          } // timestamps
        } // throttle
      } // steps           
    } // Windows Distribution stage
    
    stage('Linux Distribution') {
      agent { label "linux" }
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            unstash name: 'build_data'
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE', message: 'Assuming distribution failed because a new version of OpenJDK was released') {
              fllSwGradle('linuxDistTar')
            }
            stash name: 'linux_distribution', includes: 'build/distributions/*'
          } // timestamps
        } // throttle
      } // steps           
    } // Linux Distribution stage

    stage('Mac Distribution') {
      agent { label "linux" }
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            unstash name: 'build_data'
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE', message: 'Assuming distribution failed because a new version of OpenJDK was released') {
              fllSwGradle('macDistTar')
            }
            stash name: 'mac_distribution', includes: 'build/distributions/*'
          } // timestamps
        } // throttle
      } // steps           
    } // Linux Distribution stage
        
    /*
    stage('Publish documentation') {
      steps {    
          publishHTML (target: [
            allowMissing: false,
            alwaysLinkToLastBuild: false,
            keepAll: false,
            reportDir: 'build/gen-documentation',
            reportFiles: 'index.html',
            reportName: 'Documentation'
          ])
      }
    }
    */
    
  } // stages
    
  post {
    always {
      unstash name: 'build_data'
      unstash name: 'windows_distribution'
      unstash name: 'linux_distribution'
      unstash name: 'mac_distribution'
      
      archiveArtifacts artifacts: '**/*.log,screenshots/,build/distributions/'
                        
      recordIssues tool: taskScanner(includePattern: '**/*.java,**/*.jsp,**/*.jspf,**/*.xml', excludePattern: 'checkstyle*.xml', highTags: 'FIXME,HACK', normalTags: 'TODO')
                
      recordIssues tool: java()  

      recordIssues tool: javaDoc()

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

def fllSwGradle(task) {
  // make sure the local repository directories exist
  dir('.gradle-repo') {
      writeFile file:'dummy', text:''
  }
  dir('.maven-repo') {
      writeFile file:'dummy', text:''
  }
  
  def args='--no-daemon -Dtest.ignoreFailures=true'

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
