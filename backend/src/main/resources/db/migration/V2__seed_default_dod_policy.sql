-- Seed default DoD Policy for Java Service
-- Note: adjust UUID if you have conflicting data

insert into dod_policy (id, name, spec)
values (
  '11111111-1111-1111-1111-111111111111',
  'Default DoD for Java Service',
  '{
    "id": "default-java-service",
    "name": "Default DoD for Java Service",
    "requirements": [
      {"type": "PR_MERGED", "provider": "github", "branch": "main"},
      {"type": "CI_GREEN", "provider": "github-actions", "workflow": "build.yml"},
      {"type": "DOC_PUBLISHED", "urlPattern": "^https://docs\\.meuapp\\.io/.*"},
      {"type": "LOG_SEEN", "query": "message:StartedApp", "minCount": 1},
      {"type": "FLAG_ENABLED", "flagKey": "feature.{task.key}", "minPercentage": 10}
    ]
  }'::jsonb
)
on conflict (id) do nothing;

