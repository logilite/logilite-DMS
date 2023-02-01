-- Apply this script 1st for those plugin which are exported artifacts after 2023-Jan-31 date

-- Update the Imported package name to prevent importing the 2pack after changing the version control from hg to git 
	
UPDATE AD_Package_IMP 
SET Name = 'com.logilite.dms' 
WHERE Name ='org.idempiere.dms' AND PK_Status = 'Completed successfully'
;
