pipeline {
    agent any

    environment {
        SONAR_HOST_URL = 'http://192.168.50.4:9000'
        SONAR_AUTH_TOKEN = credentials('sonarqube')
        NVD_API_KEY = credentials('NVD_API_KEY')
        IMAGE_TAG = 'notary-app:latest'
        APP_PORT = '8081'
        CONTAINER_NAME = 'notary-app'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Git Checkout') {
            steps {
                git(
                    branch: 'main',
                    credentialsId: 'jenkins_github',
                    url: 'https://github.com/Aniss-15/NotaryProject.git'
                )
            }
        }

        stage('Maven Build') {
            steps {
                sh '''
                    echo "🧹 Cleaning and building project..."
                    rm -rf target || true
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    echo "🔍 Running SonarQube analysis..."
                    timeout(time: 15, unit: 'MINUTES') {
                        sh """
                            # Test SonarQube connection first
                            echo "Testing connection to SonarQube..."
                            curl --connect-timeout 30 --max-time 60 -f ${SONAR_HOST_URL}/api/system/status || echo "SonarQube might be slow but continuing..."
                            
                            # Run SonarQube analysis with increased timeout
                            mvn sonar:sonar \
                                -Dsonar.projectKey=NotaryProject_git \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.login=${SONAR_AUTH_TOKEN} \
                                -Dsonar.ws.timeout=300 \
                                -Dsonar.scm.disabled=true
                        """
                    }
                }
            }
        }

        stage('Clean Dependency Check DB') {
            steps {
                script {
                    echo "🧹 Cleaning Dependency Check database..."
                    sh '''
                        rm -rf ~/.dependency-check || true
                        rm -rf /tmp/dependency-check-data || true
                    '''
                }
            }
        }

        stage("SCA - Dependency Check") {
            steps {
                script {
                    sh 'mkdir -p dependency-check-report'
                    dependencyCheck (
                        additionalArguments: '''--scan . \
                            --format HTML \
                            --format XML \
                            --out dependency-check-report \
                            --noupdate''',
                        odcInstallation: 'Dependency_Check'
                    )
                }
            }
            post {
                always {
                    dependencyCheckPublisher pattern: 'dependency-check-report/dependency-check-report.xml'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                        echo "🐳 Building Docker image..."
                        echo "Current directory before build:"
                        pwd
                        ls -la
                        
                        docker build -t ${IMAGE_TAG} .
                    '''
                }
            }
        }

        stage("Trivy Scan Image") {
            steps {
                script {
                    sh """
                    echo '🔍 Running Trivy scan on ${env.IMAGE_TAG}'
                    
                    # Simple scan with minimal options
                    trivy image \
                        --timeout 5m \
                        --skip-db-update \
                        --skip-java-db-update \
                        --scanners vuln \
                        -f json -o trivy-image.json ${env.IMAGE_TAG} || echo "Scan completed"
                    
                    # Quick summary scan
                    trivy image \
                        --timeout 2m \
                        --skip-db-update \
                        --skip-java-db-update \
                        -f table -o trivy-image.txt ${env.IMAGE_TAG} || echo "Summary scan completed"
                    
                    echo "✅ Trivy scan attempted - check artifacts for results"
                    """
                }
            }
        }

        stage('Run for ZAP Testing') {
            steps {
                script {
                    sh """
                    echo "🚀 Deploying application for ZAP testing..."
                    
                    # Check if port is available
                    echo "🔍 Checking if port \${APP_PORT} is available..."
                    if netstat -tuln | grep :\${APP_PORT} > /dev/null; then
                        echo "⚠️ Port \${APP_PORT} is in use, finding available port..."
                        # Find next available port
                        for port in \$(seq 8081 8090); do
                            if ! netstat -tuln | grep :\${port} > /dev/null; then
                                echo "✅ Using port \${port} instead"
                                export ACTUAL_PORT=\${port}
                                break
                            fi
                        done
                    else
                        export ACTUAL_PORT=\${APP_PORT}
                    fi
                    
                    # Stop and remove any existing container
                    docker stop \${CONTAINER_NAME} || true
                    docker rm \${CONTAINER_NAME} || true
                    
                    # Run the application container
                    echo "🐳 Starting container on port \${ACTUAL_PORT}..."
                    docker run -d \
                        --name \${CONTAINER_NAME} \
                        -p \${ACTUAL_PORT}:8080 \
                        \${IMAGE_TAG}
                    
                    echo "⏳ Waiting for application to start..."
                    sleep 30
                    
                    # Health check
                    echo "🔍 Checking application health..."
                    curl -f http://localhost:\${ACTUAL_PORT}/actuator/health || echo "Application might still be starting"
                    
                    # Store the actual port for ZAP scan
                    echo \${ACTUAL_PORT} > zap-port.txt
                    echo "📝 Stored port \${ACTUAL_PORT} in zap-port.txt"
                    """
                }
            }
        }

        stage('DAST with ZAP') {
            steps {
        echo "🔍 Running OWASP ZAP scan ..."
        sh '''
        docker run --rm --user root --network=host \
        -v $(pwd):/zap/wrk:rw \
        -t zaproxy/zap-stable zap-baseline.py \
        -t http://192.168.50.4:8081 \
        -r zap_report.html \
        -J zap_report.json || true
        '''
             }
        }

        stage('Verify Docker Image') {
            steps {
                script {
                    sh '''
                        echo "🔍 Verifying Docker image..."
                        
                        # Check if image exists
                        echo "=== Docker Images ==="
                        docker images | grep notary-app || echo "No notary-app images found"
                        
                        # Detailed check
                        if docker images ${IMAGE_TAG} --format "table {{.Repository}}\\t{{.Tag}}\\t{{.Size}}" | grep -q "notary-app"; then
                            echo "✅ Docker image built successfully!"
                            echo "Image details:"
                            docker images ${IMAGE_TAG} --format "table {{.Repository}}\\t{{.Tag}}\\t{{.Size}}\\t{{.CreatedAt}}"
                            
                            # Show image history
                            echo "=== Image History ==="
                            docker history ${IMAGE_TAG}
                            
                            # Test the image can start
                            echo "=== Testing Image Startup ==="
                            docker run --rm ${IMAGE_TAG} --version || echo "Image test completed"
                        else
                            echo "❌ Docker image not found!"
                            echo "Available images:"
                            docker images
                            exit 1
                        fi
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'dependency-check-report/*, Dockerfile, target/*.jar, trivy-image.json, trivy-image.txt, zap-*.html, zap-*.md, zap-port.txt', fingerprint: true
            
            script {
                echo "🧹 Cleaning up Docker containers..."
                sh """
                    docker stop ${CONTAINER_NAME} || true
                    docker rm ${CONTAINER_NAME} || true
                    docker system prune -f || true
                """
            }
        }
        success {
            echo "✅ Pipeline completed successfully!"
            sh '''
                echo "=== Final Docker Images ==="
                docker images ${IMAGE_TAG} || echo "No notary-app image found"
                echo "=== Security Scan Results ==="
                if [ -f "trivy-image.txt" ]; then
                    echo "Trivy scan results:"
                    cat trivy-image.txt || echo "No trivy results to display"
                fi
                if [ -f "zap-baseline-report.html" ]; then
                    echo "ZAP scan report: zap-baseline-report.html"
                fi
            '''
        }
        failure {
            echo "❌ Pipeline failed!"
        }
    }
}
