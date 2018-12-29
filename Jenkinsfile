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

        echo "Environment Variables"
        if (isUnix()) {
          sh script: 'printenv'
        } else {
          bat script: 'set'
        }
        echo "end Environment Variables"
	
      }
    }

    stage('Duplicate Code Analysis') {
      steps { 
        fllSwAnt('cpd.xml')
        dry defaultEncoding: '', healthy: '', pattern: 'build/cpd.xml', unHealthy: ''
      }     
    }

    stage('Count lines of code') {
      steps { 
        fllSwAnt('sloccount')
        sloccountPublish pattern: 'cloc.xml'
      }
    }

    stage('Generate documentation') {
      steps { 
        fllSwAnt('docs')
      }
    }
    
    stage('Findbugs analysis') {
      steps { 
        fllSwAnt('findbugs')
        findbugs defaultEncoding: '', excludePattern: '', failedTotalHigh: '0', healthy: '', includePattern: '', pattern: 'build/findbugs/report.xml', unHealthy: ''
      }
    }
    
    stage('Build, Test, Distribution') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            fllSwAnt('dist')
            junit testResults: "build/test-results/TEST-*.xml", keepLongStdio: true
            fllSwAnt('tomcat.check-logs')
          } // timestamps
        } // throttle
      } // steps           
    } // build and test stage
    
    stage('Code coverage analysis') {
      steps { 
        fllSwAnt('coverage.report')
        step $class: 'CoberturaPublisher', coberturaReportFile: 'build/docs/reports/coverage/coverage.xml'
      }
    }
    
    stage('Publish documentation') {
      steps {    
          publishHTML (target: [
            allowMissing: false,
            alwaysLinkToLastBuild: false,
            keepAll: false,
            reportDir: 'build/docs',
            reportFiles: 'index.html',
            reportName: 'Documentation'
          ])
      }
    }
    
  } // stages
    
  post {
    always {
      archiveArtifacts artifacts: 'build/screenshots/,build/tomcat/webapps/fll-sw/fllweb*,build/tomcat/logs/,build/docs/reports/,build/fll-sw*.zip'           
                        
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

def fllSwAnt(target) {
  withAnt(installation: 'FLL-SW') {
    if (isUnix()) {
      sh script: "ant ${target}"
    } else {
      bat script: "ant-win ${target}"
    }
  }
}
