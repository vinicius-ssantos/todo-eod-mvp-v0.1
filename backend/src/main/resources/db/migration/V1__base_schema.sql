-- Base schema for TODO EoD MVP (v1)
-- Tables: dod_policy, task, evidence, webhook_inbox

create table if not exists dod_policy (
  id uuid primary key,
  name varchar(120) not null,
  spec jsonb not null,
  created_at timestamptz not null default now()
);

create table if not exists task (
  id uuid primary key,
  key varchar(32) unique not null,
  title varchar(200) not null,
  state varchar(20) not null default 'BACKLOG',
  dod_policy_id uuid not null references dod_policy(id),
  assignee varchar(120),
  correlation_id uuid not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_task_state on task (state);
create index if not exists idx_task_assignee on task (assignee);

-- Element collection for labels (Task.labels)
create table if not exists task_labels (
  task_id uuid not null references task(id) on delete cascade,
  label varchar(255) not null
);
create index if not exists idx_task_labels_task on task_labels (task_id);

create table if not exists evidence (
  id uuid primary key,
  task_id uuid not null references task(id),
  type varchar(40) not null,
  source varchar(40) not null,
  payload jsonb not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_evidence_task on evidence (task_id);
create index if not exists idx_evidence_type on evidence (type);

create table if not exists webhook_inbox (
  event_id varchar(100) primary key,
  fingerprint varchar(64) not null,
  received_at timestamptz not null default now(),
  status varchar(20) not null
);
