/*
* DB changes since 249c4e14 (10.08.2024)
 */

-- remove old event items
DELETE FROM inventory WHERE item_id IN (182006999, 185000128, 188052166, 188600199);