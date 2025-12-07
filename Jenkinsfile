pipeline {
  agent any
  environment {
    // Credentials IDs (change if you used different IDs)
    GIT_CREDENTIALS = 'git-ssh-creds'
    SONAR_CREDENTIALS = 'sonar-token'      // Secret text (Sonar token)
    MAVEN_SETTINGS = 'maven-settings'      // Secret file containing settings.xml with <server> credentials for Nexus
    DOCKERHUB_CREDENTIALS = 'dockerhub-creds'
    // Nexus deploy repository ID in settings.xml must match the <id> used in your pom.xml
  // DockerHub image name (user/repo); will be tagged with BUILD_ID and 'latest'
  DOCKER_IMAGE = "azizouannes/eventsproject"
  }

  stages {
    stage('Récupération du projet (Git)') {
      steps {
        // Checkout using SSH credential
        git branch: 'main', url: 'git@github.com:AzizOuannes/CI-CD-for-Online-Event-Management-Application.git', credentialsId: "${GIT_CREDENTIALS}"
      }
    }

    stage('Compilation') {
      steps {
        // compile without running tests
        sh 'mvn -B -DskipTests=true clean compile'
      }
    }

    stage('Tests unitaires (JUnit)') {
      steps {
        // run tests and publish results
        sh 'mvn -B test'
        junit '**/target/surefire-reports/*.xml'
      }
    }

    stage('Qualité de code (SonarQube)') {
      steps {
        // SonarQube analysis is mandatory: requires a Sonar token stored in Jenkins credentials (kind: Secret text, id: sonar-token)
        withCredentials([string(credentialsId: "${SONAR_CREDENTIALS}", variable: 'SONAR_TOKEN')]) {
          // SONAR_HOST_URL can be configured as an environment variable in Jenkins global config or here
          sh 'mvn -B sonar:sonar -Dsonar.login=$SONAR_TOKEN -DskipTests=true'
        }
      }
    }

    stage('Préparation de la version à distribuer') {
      steps {
        // package the application (jar) - tests already ran above
        sh 'mvn -B -DskipTests=true package'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Mise en place de la version (Nexus)') {
      steps {
        // Use a settings.xml stored as a Secret File in Jenkins (id: maven-settings)
        // The settings.xml must contain the <server><id> matching the repository id used in your pom.xml
        withCredentials([file(credentialsId: "${MAVEN_SETTINGS}", variable: 'MAVEN_SETTINGS_FILE')]) {
          sh 'mvn -B -s $MAVEN_SETTINGS_FILE deploy -DskipTests=true'
        }
      }
    }

    stage('Création de l\'image Docker') {
      steps {
        // Build Docker image from Dockerfile in repo root
        script {
          // Tag with build id
          def tag = "${DOCKER_IMAGE}:${env.BUILD_ID}"
          def img = docker.build(tag)
          env.BUILT_IMAGE = tag
        }
      }
    }

    stage('Dépôt de l\'image sur DockerHub') {
      steps {
        script {
          docker.withRegistry('https://registry.hub.docker.com', "${DOCKERHUB_CREDENTIALS}") {
            // push the build-id tag
            sh "docker push ${env.BUILT_IMAGE}"
            // also tag and push 'latest'
            sh "docker tag ${env.BUILT_IMAGE} ${DOCKER_IMAGE}:latest"
            sh "docker push ${DOCKER_IMAGE}:latest"
          }
        }
      }
    }

    stage('Démarrage (docker-compose: app + MySQL)') {
      steps {
        // This expects a docker-compose.yml that starts the app and a MySQL container
        // The agent running this stage must have docker-compose installed and access to Docker daemon
        sh 'docker-compose up -d --build'
      }
    }
  }

  post {
    success {
      echo 'Pipeline terminée avec succès.'
    }
    failure {
      echo "Echec du pipeline. Voir la console pour les détails: ${env.BUILD_URL}"
    }
  }
}
