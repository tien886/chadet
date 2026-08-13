```
                  1. Login
Client ───────────────────────────► Auth Service
                                      │
                                      │ Private Key
                                      ▼
                                  Sign JWT
                                      │
                                      ▼
Client ◄──────────────────────────── JWT


                  2. API Request

Client
  │
  │ Authorization: Bearer <JWT>
  ▼
┌─────────────────┐
│    Gateway      │
│                 │
│   Public Key    │
│       │         │
│       ▼         │
│ Verify JWT      │
└────────┬────────┘
         │
    Valid JWT?
      /      \
    NO        YES
    │          │
  401/403      ▼
          Route request
              │
              ┼──────┐
              ▼      ▼
            Chat   Trade   
          Service Service
```

- Auth service will use for user authen and author
- Gateway service will be a guard that check for all services, will it contain the valid JWT or not 

