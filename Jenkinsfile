pipeline  {
    agent any
    
    environment {
		JAVA_OPTS = "-Xms2048m -Xmx2048m -XX:MaxMetaspaceSize=1024m ${sh(script:'echo $JAVA_OPTS', returnStdout: true).trim()}"	
  	}

    tools {
        jdk 'OpenJDK17'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        skipDefaultCheckout()
    }

    stages {
    
        stage('Clean Workspace') {
            steps {
                // Cleanup before starting the stage
                cleanWs()
            }
        }

	stage('Checkout') {
            steps {
                // Checkout the repository
                checkout scm 
            }
        }
        
	stage('App build') {
	     steps {
		echo "I am building app on branch: ${env.BRANCH_NAME}"
	
		sh "./gradlew clean build -x itest --info --stacktrace -Dmaven.repo.local=${WORKSPACE}/.m2"

		}
	}
        
        stage('Other branch') {
            when {
            	allOf {
            		not {
	                	branch 'snapshot'
	            	}
            		not {
	                	branch 'main'
	            	}
            	}
            }
            steps  {
                echo "I am building on ${env.JOB_NAME}"
                sh "./gradlew build --info --stacktrace -Dmaven.repo.local=${WORKSPACE}/.m2"
            }
        }
    }

}
