-- Members who took part in expenses or settlements are soft-removed so history stays intact.
ALTER TABLE member ADD COLUMN removed_at TIMESTAMPTZ;

-- Names only need to be unique among active members, so a removed member's name can be reused.
DROP INDEX uq_member_group_name;
CREATE UNIQUE INDEX uq_member_group_name ON member (group_id, lower(name)) WHERE removed_at IS NULL;
