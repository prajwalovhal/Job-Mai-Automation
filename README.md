# Job Email Automation

A simple stateless Spring Boot 3.x + Java 17 application for sending job-application emails individually to recruiters.

## Features

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Mail with Gmail SMTP
- Apache POI for XLSX
- Jakarta Validation
- Swagger/OpenAPI
- Actuator health endpoint
- JUnit 5 + Mockito
- Maven
- Docker + Docker Compose
- SLF4J/Logback daily rolling files
- No database, JPA, Hibernate, Redis, Kafka, RabbitMQ, or persistence layer
- Separate `MimeMessage` for every recruiter
- Sequential bulk processing
- Duplicate removal
- Invalid email reporting
- Failed sends do not stop the remaining emails
- PDF/DOC/DOCX attachment support

## Project structure

```text
job-email-automation/
├── src/
│   ├── main/
│   │   ├── java/com/example/jobemailautomation/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── config/
│   │   │   ├── dto/
│   │   │   └── exception/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test/
├── files/
│   ├── resume.pdf
│   ├── recruiters.xlsx
│   └── logs/
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

No `entity/` or `repository/` packages are used.

## Gmail setup

1. Enable 2-Step Verification on the Gmail/Google account.
2. Create a Google App Password.
3. Copy `.env.example` to `.env`.
4. Set:

```text
MAIL_USERNAME=yourgmail@gmail.com
MAIL_PASSWORD=your-app-password
```

Never commit `.env`.

The application uses:

```text
smtp.gmail.com:587
STARTTLS
SMTP authentication
```

## Resume

Place the resume at:

```text
files/resume.pdf
```

The default Docker path is:

```text
/app/files/resume.pdf
```

You can override it with `ATTACHMENT_PATH`.

Supported extensions:

- `.pdf`
- `.doc`
- `.docx`

The resume is not copied into the Docker image.

## Recruiters XLSX format

The first sheet must contain one column named `email`.

Example:

| email |
|---|
| recruiter1@company.com |
| recruiter2@company.com |
| recruiter1@company.com |
| invalid-email |
| |
| recruiter3@company.com |

The implementation skips the first row as the header, trims values, ignores blank rows, validates email addresses, and removes duplicates.

## Run locally

Requirements:

- Java 17
- Maven 3.9+
- Gmail App Password

Set environment variables:

Linux/macOS:

```bash
export MAIL_USERNAME=yourgmail@gmail.com
export MAIL_PASSWORD=your-app-password
export ATTACHMENT_PATH=./files/resume.pdf
```

Windows PowerShell:

```powershell
$env:MAIL_USERNAME="yourgmail@gmail.com"
$env:MAIL_PASSWORD="your-app-password"
$env:ATTACHMENT_PATH="./files/resume.pdf"
```

Then:

```bash
mvn clean test
mvn spring-boot:run
```

## Run with Docker Compose

First create `.env`:

```bash
cp .env.example .env
```

Put the Gmail username and App Password in `.env`.

Place your resume here:

```text
files/resume.pdf
```

Then run exactly:

```bash
docker compose up --build
```

The Docker volume mounts:

```text
./files:/app/files
```

Therefore:

```text
Host:      ./files/resume.pdf
Container: /app/files/resume.pdf

Host:      ./files/logs/
Container: /app/files/logs/
```

The resume and logs are not included in the image.

## API

### 1. Send one email

```http
POST /api/v1/email/send
Content-Type: application/json
```

Request:

```json
{
  "email": "recruiter@company.com"
}
```

Example response:

```json
{
  "email": "recruiter@company.com",
  "status": "SENT",
  "message": "Email sent successfully"
}
```

### 2. Send bulk emails

```http
POST /api/v1/email/send-bulk
Content-Type: multipart/form-data
```

Form field:

```text
file=recruiters.xlsx
```

Example response:

```json
{
  "total": 5,
  "valid": 3,
  "invalid": 1,
  "sent": 2,
  "failed": 1,
  "invalidEmails": [
    "invalid-email"
  ],
  "failedEmails": [
    "recruiter3@company.com"
  ]
}
```

`total` is the number of non-blank data rows read from the spreadsheet. `valid` is the number of unique valid email addresses.

Every valid recruiter gets a separate email. No CC or BCC is used.

## Swagger

After startup:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Health

```text
GET http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

## Logging

Logs are written to:

```text
./files/logs/
```

The rolling configuration keeps 30 days of daily logs.

Example:

```text
files/logs/
├── email-2026-10-05.log
├── email-2026-10-06.log
└── email-2026-10-07.log
```

The application logs:

- startup
- bulk start/end
- total/valid/invalid counts
- successful emails
- failed emails
- exceptions

SMTP passwords, tokens, and other secrets are never logged.

## Important production note

This application intentionally sends emails synchronously and sequentially as requested. For large recruiter lists, Gmail/provider rate limits and sending quotas still apply. The application does not add a queue, scheduler, database, or background processing layer.

Before production use, verify the recipient list and email content carefully because sending a bulk list is an external side effect.
