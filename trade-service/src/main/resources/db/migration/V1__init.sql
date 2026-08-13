CREATE TABLE IF NOT EXISTS user_wallets (
    user_id UUID PRIMARY KEY,
    balance NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    held_balance NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS trades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    creator_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    sender_confirmed BOOLEAN NOT NULL DEFAULT false,
    receiver_confirmed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_trades_conversation_id ON trades(conversation_id);
CREATE INDEX IF NOT EXISTS idx_trades_sender_id ON trades(sender_id);
CREATE INDEX IF NOT EXISTS idx_trades_receiver_id ON trades(receiver_id);
