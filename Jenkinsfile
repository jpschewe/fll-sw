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

    stage('Tests') {
      steps {
        // runs all of the test tasks
      	fllSwGradle('cobertura')
        junit testResults: "build/test-results/*est/TEST-*.xml", keepLongStdio: true
        step $class: 'CoberturaPublisher', coberturaReportFile: 'build/reports/cobertura/coverage.xml'                
      }
    }

    stage('Findbugs analysis') {
      steps { 
        fllSwGradle('findbugsMain')
        fllSwGradle('findbugsTest')
        fllSwGradle('findbugsIntegrationTest')
      }
    }
    
    stage('Distribution') {
      steps {
        throttle(['fll-sw']) { 
          timestamps {
            fllSwGradle('distZip')
          } // timestamps
        } // throttle
      } // steps           
    } // build and test stage
    
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
      archiveArtifacts artifacts: '*.log,screenshots/,build/reports/,build/distributions/'
            
      dry pattern: 'build/reports/cpd/cpdCheck.xml'
      openTasks pattern: '**/*.java,**/*.jsp,**/*.jspf,**/*.xml', excludePattern: 'checkstyle*.xml', high: 'FIXME,HACK', normal: 'TODO'
      warnings consoleParsers: [[parserName: 'Java Compiler (javac)']]
      
      recordIssues qualityGates: [[threshold: 1, type: 'TOTAL', unstable: true]], tool: findBugs(pattern: '\'build/reports/findbugs/*.xml\'', useRankAsPriority: true)     
      //findbugs pattern: 'build/reports/findbugs/*.xml', healthy: '1', unHealthy: '100', failedTotalHigh: '0'

	  // planned call to recordIssues to replace the above plugins     
	  // it looks like I can do each tool on their own line and perhaps have their own thresholds  
      // recordIssues healthy: 1, qualityGates: [[threshold: 1, type: 'TOTAL', unstable: true]], tools: [findBugs(pattern: '\'build/reports/findbugs/*.xml\'', useRankAsPriority: true), cpd(pattern: 'build/reports/cpd/cpdCheck.xml'), taskScanner(excludePattern: 'checkstyle*.xml', highTags: 'FIXME,HACK', includePattern: '**/*.java,**/*.jsp,**/*.jspf,**/*.xml', normalTags: 'TODO'), java(), javaDoc()], unhealthy: 100
      
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
