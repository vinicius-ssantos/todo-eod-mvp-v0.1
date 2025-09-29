-- schema dump placeholder (automatizar quando houver container/db no CI)
-- updated: 2025-09-29T00:00:00Z

-- Tables
--  - dod_policy (id uuid, name varchar(120), spec jsonb, created_at timestamptz)
--  - task (id uuid, key varchar(32) unique, title varchar(200), state varchar(20), dod_policy_id uuid, assignee varchar(120), correlation_id uuid, created_at timestamptz, updated_at timestamptz)
--  - task_labels (task_id uuid, label varchar(255))
--  - evidence (id uuid, task_id uuid, type varchar(40), source varchar(40), payload jsonb, created_at timestamptz)
--  - webhook_inbox (event_id varchar(100), fingerprint varchar(64), received_at timestamptz, status varchar(20))

