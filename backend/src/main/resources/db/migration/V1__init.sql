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

create table if not exists evidence (
  id uuid primary key,
  task_id uuid not null references task(id),
  type varchar(40) not null,
  source varchar(40) not null,
  payload jsonb not null,
  created_at timestamptz not null default now()
);

create table if not exists webhook_inbox (
  event_id varchar(100) primary key,
  fingerprint varchar(64) not null,
  received_at timestamptz not null default now(),
  status varchar(20) not null
);

-- labels as element collection
create table if not exists task_labels (
  task_id uuid not null references task(id),
  label text not null
);

-- seed default policy
insert into dod_policy (id, name, spec)
values ('00000000-0000-0000-0000-000000000001', 'Default DoD for Java Service', '{
  "id": "default-java-service",
  "name": "Default DoD for Java Service",
  "requirements": [
    {"type": "PR_MERGED", "provider": "github", "branch":"main"},
    {"type": "CI_GREEN", "provider": "github-actions", "workflow":"build.yml"},
    {"type": "DOC_PUBLISHED", "urlPattern": "^https://docs\\.meuapp\\.io/.*"},
    {"type": "LOG_SEEN", "query": "message:StartedApp AND correlationId:{task.correlationId}", "minCount": 1},
    {"type": "FLAG_ENABLED", "flagKey": "feature.{task.key}", "minPercentage": 10}
  ]
}')
on conflict do nothing;
