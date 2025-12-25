# jp-vocab-assistant

An AWS-based **serverless automation tool** that monitors a shared Google Document for Japanese vocabulary assignments and assists in generating example sentences using AI.

---

## 📘 Project Background

I regularly use a shared Google Document with my Japanese teachers.  
When a teacher assigns a new vocabulary word, they add my name to a row in a table, indicating that I need to write a Japanese example sentence for that word.

This workflow has two main issues:

1. **Low visibility**  
   I need to manually open the document and scan it to check if a new assignment has been added.

2. **Manual sentence generation**  
   I often use ChatGPT to help generate natural Japanese sentences, but copying and pasting manually is repetitive and inefficient.

This project automates both steps using AWS and external APIs.

---

## 🎯 Goals

- Automatically detect new vocabulary assignments in a Google Doc
- Notify me when a new task appears
- Generate Japanese example sentence suggestions (JLPT N3 level)
- Optionally write selected sentences back to Google Docs
- Learn and practice **real-world serverless AWS architecture**
- Build a portfolio-quality automation project

---

## 🧠 Architectural Approach

This project is intentionally designed as a **function-oriented, event-driven system**, not a continuously running application.

### Why not Spring?

Spring and Spring Boot are excellent frameworks for long-running backend services (REST APIs, web servers, microservices).  
However, they provide little benefit for this project because:

- There is **no web server**
- There are **no HTTP endpoints**
- The logic runs **periodically**, not continuously
- Execution is **short-lived and stateless**

Using Spring would:
- Increase AWS Lambda cold start time
- Add unnecessary framework complexity
- Provide little architectural value for a scheduled automation task

### Chosen approach

- **AWS Lambda** for execution and orchestration
- **Plain Java** for business logic and domain modeling
- Explicit dependency wiring (no framework container)
- Framework-agnostic core logic that can be tested locally

This mirrors common **AWS production patterns** for background automation and scheduled workflows.

---

## 🧠 High-Level Architecture

```
EventBridge (schedule)
        ↓
AWS Lambda (Java)
        ↓
Application Services (plain Java)
        ↓
Google Docs API
        ↓
DynamoDB (task state)
        ↓
OpenAI API (sentence generation)
        ↓
Notification (Email / Slack / LINE)
```

> AWS Lambda controls **when** the workflow runs.  
> Application code controls **what** happens.

---

## 🛠️ Tech Stack

- **Language**: Java (no application framework)
- **Cloud**: AWS
    - Lambda
    - EventBridge
    - DynamoDB
    - Secrets Manager
- **APIs**:
    - Google Docs API
    - OpenAI API
- **Infrastructure as Code**: CloudFormation (Terraform optional)
- **Notifications**: Email / Slack / LINE (TBD)

---

## 📂 Project Structure

```
jp-vocab-assistant/
├── README.md
├── docs/                               # Architecture and design documentation
├── lambda/                             # AWS Lambda handlers (orchestration only)
├── infrastructure/                     # CloudFormation / Terraform templates
├── src/java/io/github/jcloix/jpvocab   # Application core (plain Java)
│   ├── config                          # Configuration objects
│   ├── handler                         # Lambda handlers
│   ├── model                           # Domain models
│   └── service                         # Application services / workflows
├── openai/                             # Prompt design and examples
└── scripts/                            # Local testing and utilities
```

---

## 🚧 Development Roadmap

### Phase 1 – Foundation
- [ ] Access Google Docs API
- [ ] Read and parse the vocabulary table
- [ ] Detect rows assigned to me with missing sentences

### Phase 2 – AWS Integration
- [ ] Scheduled Lambda execution (EventBridge)
- [ ] Persist task state in DynamoDB
- [ ] Secure secrets using Secrets Manager

### Phase 3 – Notifications
- [ ] Notify when a new task is detected

### Phase 4 – AI Assistance
- [ ] Generate Japanese sentence suggestions using OpenAI
- [ ] Store suggestions for later review

### Phase 5 – Optional Enhancements
- [ ] Simple web UI to review suggestions
- [ ] Automatically write selected sentence back to Google Docs

---

## 🔐 Security Notes

- API keys are stored in AWS Secrets Manager
- IAM roles follow least-privilege principles
- No credentials are committed to this repository

---

## 📌 Scope & Limitations

- Single user
- Single Google Document
- Polling-based detection (no real-time push notifications)

These constraints are intentional to keep the project focused on **clean architecture and serverless design principles**.

---

## 📄 License

MIT License
