pipeline { 
   agent any 
   environment {
      GIT_REPO = 'YYY'
      GIT_CREDENTIAL_ID = 'ms-GitHub-Credentials-for-jenkins'
      SONARQUBE_URL = 'http://172.24.101.209:8082/sonar-isis2603'
      ARCHID_TOKEN = credentials('archid')
      SONAR_TOKEN = credentials('sonar-login')
   }
   stages { 
            stage('Checkout') { 
         steps {
            scmSkip(deleteBuild: true, skipPattern:'.*\\[ci-skip\\].*')

            git branch: 'main', 
               credentialsId: env.GIT_CREDENTIAL_ID,
               url: 'https://github.com/Uniandes-isis2603/' + env.GIT_REPO
         }
      }
      stage('GitInspector') { 
         steps {
            withCredentials([usernamePassword(credentialsId: env.GIT_CREDENTIAL_ID, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
               sh 'mkdir -p code-analyzer-report'
               sh """ curl --request POST --url https://code-analyzer.virtual.uniandes.edu.co/analyze --header "Content-Type: application/json" --data '{"repo_url":"git@github.com:Uniandes-isis2603/${GIT_REPO}.git", "access_token": "${GIT_PASSWORD}" }' > code-analyzer-report/index.html """   
            }
            publishHTML (target: [
               allowMissing: false,
               alwaysLinkToLastBuild: false,
               keepAll: true,
               reportDir: 'code-analyzer-report',
               reportFiles: 'index.html',
               reportName: "GitInspector"
            ])
         }
      }
      stage('Build') {
         // Build artifacts
         options {
            timeout(time: 1, unit: 'MINUTES')
         }
         steps {
            script {
               CURRENT_STAGE = 'Build'
               docker.image('citools-isis2603:latest').inside('-v $HOME/.m2:/root/.m2:z -u root') {
                  sh '''
                     java -version
                     mvn clean install -DskipTests 
                  '''
               }
            }
         }
      }
      stage('Unit Tests') {
         // Run unit tests
         options {
            timeout(time: 1, unit: 'MINUTES')
         }
         steps {
            script {
               CURRENT_STAGE = 'Unit Tests'
               docker.image('citools-isis2603:latest').inside('-v $HOME/.m2:/root/.m2:z -u root') {
                  sh '''
                     mvn verify -Punit-tests
                  '''
               }
            }
         }
      }
      stage('Integration Tests') {
         // Run integration tests
         options {
            timeout(time: 5, unit: 'MINUTES')
         }
         steps {
            script {
               CURRENT_STAGE = 'Integration Tests'
               try {

                  def collectionFiles = sh(script: "ls ./collections/*.postman_collection.json", returnStdout: true).trim().split("\\r?\\n")

               } catch (Exception e) {
                  echo "Aún no hay colecciones de Postman para ejecutar pruebas de integración."
               }

               def collectionFiles = sh(script: "ls ./collections/*.postman_collection.json", returnStdout: true).trim().split("\\r?\\n")

               docker.image('citools-isis2603:latest').inside('-v $HOME/.m2:/root/.m2:z -u root') {
                  collectionFiles.each { file ->
                     def name = file.tokenize('/').last().replace('.postman_collection.json', '')
                     
                     sh """
                           mvn verify -Pintegration-tests -DfileName="${name}"
                        """
                  }      
               }
               

            }
         }
      }
      stage('Static Analysis') {
         // Run static analysis
         steps {
            script {
               docker.image('citools-isis2603:latest').inside('-v $HOME/.m2:/root/.m2:z -u root') {
                  sh '''
                     mvn sonar:sonar -Dsonar.token=${SONAR_TOKEN} -Dsonar.host.url=${SONARQUBE_URL}
                  '''
               }
            }
         }
      }    
   }
   post {
      always {
        cleanWs()
        deleteDir() 
        dir("${env.GIT_REPO}@tmp") {
          deleteDir()
        }
      }
   }
}
