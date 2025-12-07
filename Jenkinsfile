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
        // checkout the main branch
        git branch: 'main', url: 'https://github.com/AzizOuannes/CI-CD-for-Online-Event-Management-Application.git'
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
        withCredentials([string(credentialsId: "${SONAR_CREDENTIALS}", variable: 'SONAR_TOKEN')]) {
          // Generate JaCoCo coverage report and run Sonar analysis; do NOT skip tests
          sh 'mvn -B clean verify sonar:sonar -Dsonar.login=$SONAR_TOKEN -Dsonar.projectVersion=${BUILD_NUMBER} -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
        }
      }
    }

    stage('Préparation de la version à distribuer') {
      steps {
        // package the application (jar)
        sh 'mvn -B -DskipTests=true package'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Mise en place de la version (Nexus)') {
      steps {

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
          // Use official Docker Hub registry endpoint for login/push
          docker.withRegistry('https://index.docker.io/v1/', "${DOCKERHUB_CREDENTIALS}") {
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
        // Prefer Docker Compose v2 ('docker compose'); fall back to v1 ('docker-compose')
        sh '''
          # Choose an available host port for the app if APP_PORT is not provided
          choose_port() {
            for p in 8081 8082 8083 8090; do
              if ! ss -ltn | awk '{print $4}' | grep -qE ":$p$"; then
                echo "$p"
                return 0
              fi
            done
            echo "No free ports found in candidate list" >&2
            return 1
          }

          if [ -z "$APP_PORT" ]; then
            APP_PORT=$(choose_port) || exit 1
            export APP_PORT
            echo "Using APP_PORT=$APP_PORT"
          else
            echo "APP_PORT is preset to $APP_PORT"
          fi

          # Stop any existing app container that may be holding the port to avoid conflicts
          if docker ps -a --format '{{.Names}}' | grep -q '^events_app$'; then
            echo "Stopping existing events_app container to free port..."
            docker stop events_app || true
            docker rm events_app || true
          fi

          if docker compose version >/dev/null 2>&1; then
            docker compose up -d --build
          elif command -v docker-compose >/dev/null 2>&1; then
            docker-compose up -d --build
          else
            echo "Docker Compose not found. Install Docker Compose v2 (docker compose plugin) or v1 (docker-compose)."
            exit 127
          fi
        '''
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
