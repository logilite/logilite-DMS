-- README
-- If customer using existing DMS with data Then check out below Script first  [ 2020 - May - 07 ]
	1) Execute			Script/01_202005071200_Migrate_Existing_DMS_With_SoftDelete_Functionality.sql

-- In Existing DMS structure having issue of Mounting structure then execute below sql to correct same [ 2020 - June - 17 ]
	1) Export result of Script/02_202006172121_DMS_Dir_Structure_Hierarchy.sql [ This script for data comparison purpose ]
	2) Execute 			Script/03_202006172121_DMS_Update_Script_for_Wrong_Structure_Created.sql
	3) Export result of Script/02_202006172121_DMS_Dir_Structure_Hierarchy.sql [ This script for data comparison purpose ]
	4) Execute			Script/04_202006172121_DMS_Delete_Records_Which_Are_Wrong_Structured.sql [ This script for delete wrongly created records ]

-- Enhancement of DMS_Version table [ Changeset: 411 (1ec26961ca61) #1007318 Segragating in DMS Content in Version table ]
	1) Deploy latest plugin including changes of versioning implementation. [ From 2Pack_5.2.0.zip] having AD for DMS_Version table ]
	2) Below script is used for existing DMS data to migrate in version table
		- Execute 		Script/05_202101061400_DMS_Create_Version_Records_From_Existing_Data.sql 
	3) Test all the thing in DMS is working properly then go for execute below script
		- Execute 		Script/06_202101061400_DMS_Delete_Content_Association_And_Drop_Columns.sql [ May need to restart the server ]

-- Removed pdf-renderer-1.0.5.jar and added new PDFrenderer.jar to support thumbnails generate of the latest PDF version. [ PDF thumbnail creation failure: com.sun.pdfview.PDFParseException: Expected 'xref' at start of table ]
Reference link - https://github.com/katjas/PDFrenderer




2023 - Jan - 31
---------------
	Deployment of new DMS plugin after renamed package from org.idempiere.dms to com.logilite.dms require to apply below script 1st.
	Script: Script/07_202301311300_Rename_Package_Name.sql
