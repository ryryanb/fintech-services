# AI Platform integration

FinTech runs the AI Platform as a separate service. It verifies the same RSA-signed access tokens issued by auth-service and stores chat data in its own PostgreSQL database. The AI service does not receive the signing private key and does not connect directly to financial-service databases.

## Prerequisites

- Clone `ai-platform` beside `fintech-services`, or set `AI_PLATFORM_BUILD_CONTEXT` to the local ai-platform checkout path in your Compose `.env` file.
- Ensure the FinTech Auth Service RSA private key exists at `auth-service/keys/jwt-private.pem`.
- Have Ollama running and the configured model available. The default is `qwen3:4b`.

## Derive the public key

From the fintech-services repository root, run:

```sh
sh auth-service/scripts/export-jwt-public-key.sh
```

This derives `auth-service/keys/jwt-public.pem` from the existing private key. The generated public key is ignored by Git and mounted read-only into the AI Platform container. Never mount the private key into the AI Platform container.

## Compose configuration

Add these values to the local `.env` file (use strong, unique database credentials):

```dotenv
AI_DB_NAME=chat_db
AI_DB_USERNAME=ai_platform
AI_DB_PASSWORD=replace-with-a-strong-password
AI_PLATFORM_BUILD_CONTEXT=../ai-platform
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_CHAT_MODEL=qwen3:4b
FRONTEND_BASE_URL=http://localhost:5173
```

Then build and start the platform:

```sh
docker compose up -d --build ai-db ai-platform
```

The AI API is exposed at `http://localhost:8089`. The health endpoint is `GET /actuator/health`; conversation endpoints require `Authorization: Bearer <FinTech access token>`.

## Security behavior

- AI Platform verifies JWT signatures with the public key derived from the Auth Service key pair.
- Tokens must contain a non-empty `customerId` claim. The customer identity is read from the verified token, never from request JSON or query parameters.
- Conversation list, history, send-message, and stream-message operations enforce customer ownership.
- A conversation belonging to another customer returns not found.
- The AI database is separate from account, transaction, and payment databases.
- Ollama is configured independently; the AI service does not require the Auth Service to be reachable for JWT signature verification.

## Important

Before deploying this Compose integration, merge the AI Platform security PR so the container image includes JWT verification and customer ownership. The frontend still needs to forward the FinTech access token and embed the AI chat interface; that is a separate integration step.