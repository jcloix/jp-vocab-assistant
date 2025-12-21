# jp-vocab-assistant

An AWS-based automation tool that monitors a shared Google Document for Japanese vocabulary assignments and assists in generating example sentences using AI.

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
- Learn and practice real-world AWS architecture
- Build a portfolio-quality serverless project

---

## 🧠 High-Level Architecture

```
EventBridge (schedule)
        ↓
AWS Lambda (Java)
        ↓
Google Docs API
        ↓
DynamoDB (task state)
        ↓
OpenAI API (sentence generation)
        ↓
Notification (Email / Slack / LINE)
```

---

## 🛠️ Tech Stack

- **Language**: Java
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
├── docs/             # Architecture and design documentation
├── lambda/           # AWS Lambda functions (Java)
├── infrastructure/   # CloudFormation / Terraform templates
├── openai/           # Prompt design and examples
└── scripts/          # Local testing and utilities
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

These constraints are intentional to keep the project focused on learning and clean architecture.

---

## 📄 License

MIT License
