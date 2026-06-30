import groovy.json.JsonOutput

def call(Map args = [:]) {
    String status = args.status ?: 'UNKNOWN'
    String title = args.title ?: 'Jenkins Build'
    String message = args.message ?: ''
    String webhookCredentialId = args.webhookCredentialId ?: 'slack-webhook-url'

    String icon = ':white_circle:'

    if (status == 'SUCCESS') {
        icon = ':white_check_mark:'
    } else if (status == 'FAILURE') {
        icon = ':x:'
    } else if (status == 'UNSTABLE') {
        icon = ':warning:'
    }

    def payload = [
        text: "${icon} *${title}*\n${message}"
    ]

    String payloadFile = "slack_payload_${env.BUILD_NUMBER}.json"

    writeFile(
        file: payloadFile,
        text: JsonOutput.toJson(payload),
        encoding: 'UTF-8'
    )

    withCredentials([
        string(credentialsId: webhookCredentialId, variable: 'SLACK_WEBHOOK_URL')
    ]) {
        if (isUnix()) {
            sh """
            set +x
            curl -sS --fail -X POST \\
              -H "Content-Type: application/json" \\
              --data-binary "@${payloadFile}" \\
              "\$SLACK_WEBHOOK_URL"

            SLACK_EXIT_CODE=\$?
            rm -f "${payloadFile}"
            exit \$SLACK_EXIT_CODE
            """
        } else {
            bat """
            @echo off
            chcp 65001 >nul

            curl -sS --fail --ssl-no-revoke -X POST ^
              -H "Content-Type: application/json" ^
              --data-binary "@${payloadFile}" ^
              "%SLACK_WEBHOOK_URL%"

            set SLACK_EXIT_CODE=%ERRORLEVEL%
            del "${payloadFile}" >nul 2>nul
            exit /b %SLACK_EXIT_CODE%
            """
        }
    }
}