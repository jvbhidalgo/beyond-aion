ALTER TABLE `legion_history`
MODIFY COLUMN `history_type` Enum('CREATE','JOIN','KICK','APPOINTED','EMBLEM_REGISTER','EMBLEM_MODIFIED','ITEM_DEPOSIT','ITEM_WITHDRAW','KINAH_DEPOSIT','KINAH_WITHDRAW','LEVEL_UP','DEFENSE','OCCUPATION','LEGION_RENAME','CHARACTER_RENAME') NOT NULL;