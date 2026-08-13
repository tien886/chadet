- User can chat realtime with other through websocket with others 
- User can create a trade between them, our app will become a man in the middle, reliable, holding the balance when the transaction begin, after the progress is done
	- two of user commited, we will transfer the balance to sender 
	- one of them denied, hold the balance
    - create group chat 

```mermaid
erDiagram
    User {
        UUID id PK
        string gmail
        string username
        string password
    }

    Conversation {
        UUID id PK
        datetime createdAt
    }

    GroupChat {
        UUID conversationId PK,FK
        string name
        UUID creatorId FK
        datetime createdAt
    }

    ConversationMember {
        UUID conversationId PK,FK
        UUID userId PK,FK
    }

    Message {
        UUID id PK
        UUID conversationId FK
        UUID senderId FK
        string content
        datetime createdAt
    }

    Trade {
        UUID id PK
        UUID conversationId FK
        UUID creatorId FK
        UUID senderId FK
        UUID receiverId FK
        decimal amount
        string status
        boolean senderConfirmed
        boolean receiverConfirmed
        datetime createdAt
        datetime completedAt
    }

    User ||--o{ ConversationMember : joins
    Conversation ||--o{ ConversationMember : has

    Conversation ||--o| GroupChat : "is group chat"
    User ||--o{ GroupChat : creates

    Conversation ||--o{ Message : contains
    User ||--o{ Message : sends

    Conversation ||--o{ Trade : contains

    User ||--o{ Trade : creates
    User ||--o{ Trade : sender
    User ||--o{ Trade : receiver
```

4 service: 
                    ┌──────────────┐
                    │   API Gateway│
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
   │ User/Auth   │  │ Chat Service│  │ Trade       │
   │ Service   │  │             │  │ Service     │
   └─────────────┘  └─────────────┘  └─────────────┘