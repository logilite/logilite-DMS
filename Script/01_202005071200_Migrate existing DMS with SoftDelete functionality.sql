-- README FIRST...!!!
-- For existing DMS, first verify if any content value and name are not matched then execute below update SQL to maintain proper content reference with physical storage.

-- SELECT * FROM DMS_Content WHERE Value <> Name;
UPDATE DMS_Content	SET Value = Name	WHERE Value <> Name;
