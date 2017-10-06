#!/usr/bin/env groovy

pipeline {

  agent {
    label 'fll-build'
  }

  stages {
    stage('Init') {
      steps {
        echo "NODE_NAME = ${env.NODE_NAME}"
        echo "My branch is: ${env.BRANCH_NAME}"
      }
    }

    stage('Duplicate Code Analysis') {
      steps { 
        fllSwAnt('cpd.xml')
        dry defaultEncoding: '', healthy: '', pattern: 'scoring/build/cpd.xml', unHealthy: ''
      }     
    }

    stage('Count lines of code') {
      steps { 
        fllSwAnt('sloccount')
      }
    }

    stage('Generate documentation') {
      steps { 
        fllSwAnt('docs')
        publishHTML (target: [
          allowMissing: false,
          alwaysLinkToLastBuild: false,
          keepAll: false,
          reportDir: 'scoring/docs',
          reportFiles: 'index.html',
          reportName: 'Documentation'
        ])
      }
    }
    
    stage('Findbugs analysis') {
      steps { 
        fllSwAnt('findbugs')
        findbugs defaultEncoding: '', excludePattern: '', failedTotalHigh: '0', healthy: '', includePattern: '', pattern: 'scoring/build/findbugs/report.xml', unHealthy: ''
      }
    }
    
    stage('Test') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            fllSwAnt('test.all')
            junit "scoring/build/test-results/TEST-*.xml"
            fllSwAnt('tomcat.check-logs')
          } // timestamps
        } // throttle
      } // steps           
    } // build and test stage
    
    stage('Code coverage analysis') {
      steps { 
        fllSwAnt('coverage.report')
        step $class: 'CoberturaPublisher', coberturaReportFile: 'scoring/build/docs/reports/coverage/coverage.xml'
      }
    }
    
  } // stages
    
  post {
    always {
      archiveArtifacts artifacts: 'scoring/build/screenshots/,scoring/build/tomcat/webapps/fll-sw/fllweb*,scoring/build/tomcat/logs/,scoring/build/docs/reports/'           
                        
      openTasks defaultEncoding: '', excludePattern: 'scoring/checkstyle*.xml,**/ChatServlet.java', healthy: '', high: 'FIXME,HACK', low: '', normal: 'TODO', pattern: '**/*.java,**/*.jsp,**/*.jspf,**/*.xml', unHealthy: ''
      warnings categoriesPattern: '', consoleParsers: [[parserName: 'Java Compiler (javac)']], defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', messagesPattern: '', unHealthy: ''
      
      emailext recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']], 
          to: 'jpschewe@mtu.net',
          subject: '$DEFAULT_SUBJECT', 
          body: '$DEFAULT_CONTENT'
                
    } // always
  } // post

} // pipeline

def fllSwAnt(target) {
  withAnt(installation: 'FLL-SW') {
    dir("scoring") {
      if (isUnix()) {
        sh "ant ${target}"
      } else {
        bat "ant ${target}"
      }
    }
  }
}
