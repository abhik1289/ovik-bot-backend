-- V2__init_chat_conversations.sql
-- Persistent chat history per authenticated user.
-- Messages are inlined as JSONB to keep the schema simple for v1.

CREATE TABLE IF NOT EXISTS chat_conversations (
    id          VARCHAR(64)   PRIMARY KEY,
    user_id     UUID          NOT NULL,
    title       VARCHAR(256)  NOT NULL DEFAULT 'New chat',
    preview     VARCHAR(256)  NOT NULL DEFAULT '',
    mode        VARCHAR(16)   NOT NULL DEFAULT 'chat',
    pinned      BOOLEAN       NOT NULL DEFAULT FALSE,
    messages    JSONB         NOT NULL DEFAULT '[]'::jsonb,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,
    deleted_at  TIMESTAMP     NULL,
    CONSTRAINT fk_chat_conv_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- All live (non-deleted) conversations for a user, regardless of pin state.
CREATE INDEX IF NOT EXISTS idx_chat_conv_user_id
    ON chat_conversations (user_id);

-- Listing query: live conversations for a user, pinned first, most recent first.
CREATE INDEX IF NOT EXISTS idx_chat_conv_user_pinned_updated
    ON chat_conversations (user_id, pinned DESC, updated_at DESC)
    WHERE deleted_at IS NULL;