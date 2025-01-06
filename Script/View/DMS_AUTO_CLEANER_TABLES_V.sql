-- DROP VIEW adempiere.DMS_AUTO_CLEANER_TABLES_V;

CREATE OR REPLACE VIEW adempiere.DMS_AUTO_CLEANER_TABLES_V
    AS
    SELECT t.TableName
    FROM AD_Table t
    WHERE     t.IsActive = 'Y'::bpchar 
        AND t.IsView = 'N'::bpchar 
        AND t.IsDeleteable = 'Y'::bpchar 
        AND t.AccessLevel <> '4'::bpchar 
        AND t.TableName::text !~~* 'DMS_%'::text 
        AND t.TableName::text !~~* 'T_%'::text 
        AND t.TableName::text !~~* 'I_%'::text 
        AND NOT (t.TableName::text IN 
                    (   SELECT AD_Table.TableName
                        FROM AD_Table
                        WHERE   AD_Table.TableName::text ~~* 'AD_%'::text 
                                AND (AD_Table.TableName::text <> ALL (
                                            ARRAY[  'AD_User'::character varying, 
                                                    'AD_PrintFormat'::character varying, 
                                                    'AD_PrintFormat_Access'::character varying]::text[]))))
        AND NOT (
            EXISTS (SELECT DISTINCT c.AD_Table_id
                    FROM AD_Column c
                    WHERE   c.IsParent = 'Y'::bpchar 
                        AND c.IsActive = 'Y'::bpchar 
                        AND c.AD_Table_id = t.AD_Table_id 
                        AND NOT (
                            EXISTS (    SELECT cs.AD_Table_id
                                        FROM AD_Column cs
                                        WHERE cs.IsKey = 'Y'::bpchar 
                                            AND cs.AD_Table_id = c.AD_Table_id))
            GROUP BY c.AD_Table_id
            HAVING COUNT(1) > 1)) 
        AND (EXISTS ( 
                SELECT 1
                FROM AD_Column c
                WHERE c.AD_Table_id = t.AD_Table_id 
                    AND c.IsKey = 'Y'::bpchar 
                    AND c.IsActive = 'Y'::bpchar 
                    AND NOT (
                        EXISTS (    SELECT 1
                                    FROM AD_Column c2
                                    WHERE c2.AD_Table_id = t.AD_Table_id 
                                        AND c2.IsKey = 'Y'::bpchar 
                                        AND c2.IsActive = 'Y'::bpchar 
                                        AND c2.AD_Column_id <> c.AD_Column_id))))
    ORDER BY t.TableName;
