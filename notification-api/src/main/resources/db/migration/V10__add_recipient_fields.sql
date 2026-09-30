ALTER TABLE users ADD COLUMN phone_number VARCHAR(50);
ALTER TABLE users ADD COLUMN device_token VARCHAR(255);

ALTER TABLE notification_requests ADD COLUMN recipient_id UUID REFERENCES users(id);
