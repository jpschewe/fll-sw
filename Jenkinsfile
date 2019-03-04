#!/usr/bin/env groovy

pipeline {

  options { buildDiscarder(logRotator(numToKeepStr: '10')) }

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
        dry defaultEncoding: '', healthy: '', pattern: 'build/reports/cpd/cpdCheck.xml', unHealthy: ''
      }     
    }

    stage('Count lines of code') {
      steps { 
        fllSwGradle('sloccount')
        sloccountPublish pattern: 'build/reports/sloccount/cloc.xml'
      }
    }

    stage('Tests') {
      steps { 
        fllSwGradle('check')
        fllSwGradle('integrationTest')
        junit testResults: "build/test-results/test/TEST-*.xml", keepLongStdio: true        
      }
    }

    stage('Findbugs analysis') {
      steps { 
        fllSwGradle('findbugsMain')
        fllSwGradle('findbugsTest')
        findbugs defaultEncoding: '', excludePattern: '', failedTotalHigh: '0', healthy: '', includePattern: '', pattern: 'build/reports/findbugs/*.xml', unHealthy: ''
      }
    }
    
    stage('Build, Test, Distribution') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            fllSwGradle('distZip')
          } // timestamps
        } // throttle
      } // steps           
    } // build and test stage
    
    /*stage('Code coverage analysis') {
      steps { 
        fllSwAnt('coverage.report')
        step $class: 'CoberturaPublisher', coberturaReportFile: 'build.ant/docs/reports/coverage/coverage.xml'
      }
    }*/
    
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
      archiveArtifacts artifacts: '*.log,screenshots/,build/reports/,build/distributions'           
                        
      openTasks defaultEncoding: '', excludePattern: 'checkstyle*.xml,**/ChatServlet.java', healthy: '', high: 'FIXME,HACK', low: '', normal: 'TODO', pattern: '**/*.java,**/*.jsp,**/*.jspf,**/*.xml', unHealthy: ''
      warnings categoriesPattern: '', consoleParsers: [[parserName: 'Java Compiler (javac)']], defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', messagesPattern: '', unHealthy: ''
      
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
  
  def args='-gradle-user-home .gradle-repo -Dmaven.repo.local=.maven-repo -Dtest.ignoreFailures=true'

  if (isUnix()) {
    sh script: "gradlew ${args} ${task}"
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
