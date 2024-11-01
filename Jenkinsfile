pipeline {
    agent any

    tools {
        jdk 'Temurin Java 21'
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Setup Gradle') {
            steps {
                sh 'chmod +x gradlew'
            }
        }

        stage('Build') {
            steps {
                withGradle {
                    sh './gradlew --rerun-tasks build -x test'
                }
            }

            post {
                success {
                    archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true, onlyIfSuccessful: true

                    javadoc javadocDir: 'build/dokka/html/', keepAll: true
                }
            }
        }

        stage('Tests') {
            steps {
                withGradle {
                    sh './gradlew --rerun-tasks test'
                }
            }

            post {
                always {
                    junit testResults: 'build/test-results/*/TEST-*.xml'
                    allure includeProperties: false, jdk: '', results: [[path: 'build/allure-results/']]
                }
            }
        }

        stage('Deploy to snapshots repositories') {
            when {
                allOf {
                    not { buildingTag() }
                    not { expression { env.TAG_NAME != null && env.TAG_NAME.matches('v\\d+\\.\\d+\\.\\d+') } }
                }
            }

            steps {
                withCredentials([
                        string(credentialsId: 'maven-signing-key', variable: 'ORG_GRADLE_PROJECT_signingKey'),
                        // string(credentialsId: 'maven-signing-key-id', variable: 'ORG_GRADLE_PROJECT_signingKeyId'),
                        string(credentialsId: 'maven-signing-key-password', variable: 'ORG_GRADLE_PROJECT_signingPassword'),
                        usernamePassword(
                                credentialsId: 'solo-studios-maven',
                                passwordVariable: 'ORG_GRADLE_PROJECT_SoloStudiosSnapshotsPassword',
                                usernameVariable: 'ORG_GRADLE_PROJECT_SoloStudiosSnapshotsUsername'
                        )
                ]) {
                    withGradle {
                        sh './gradlew publishAllPublicationsToSoloStudiosSnapshotsRepository'
                    }
                }
            }
        }

        stage('Deploy to releases repositories') {
            when {
                allOf {
                    buildingTag()
                    expression { env.TAG_NAME != null && env.TAG_NAME.matches('v\\d+\\.\\d+\\.\\d+') }
                }
            }

            steps {
                withCredentials([
                        string(credentialsId: 'maven-signing-key', variable: 'ORG_GRADLE_PROJECT_signingKey'),
                        // string(credentialsId: 'maven-signing-key-id', variable: 'ORG_GRADLE_PROJECT_signingKeyId'),
                        string(credentialsId: 'maven-signing-key-password', variable: 'ORG_GRADLE_PROJECT_signingPassword'),
                        usernamePassword(
                                credentialsId: 'solo-studios-maven',
                                passwordVariable: 'ORG_GRADLE_PROJECT_SoloStudiosReleasesPassword',
                                usernameVariable: 'ORG_GRADLE_PROJECT_SoloStudiosReleasesUsername'
                        ),
                        usernamePassword(
                                credentialsId: 'sonatype-maven-credentials',
                                passwordVariable: 'ORG_GRADLE_PROJECT_SonatypePassword',
                                usernameVariable: 'ORG_GRADLE_PROJECT_SonatypeUsername'
                        )
                ]) {
                    withGradle {
                        sh './gradlew publishAllPublicationsToSoloStudiosReleasesRepository'
                        sh './gradlew publishAllPublicationsToSonatypeRepository'
                    }
                }
            }
        }
    }

    post {
        always {
            recordIssues enabledForFailure: true, tools: [kotlin()]

            cleanWs()
        }
    }
}
