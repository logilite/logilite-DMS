-- README
-- If customer using existing DMS with data Then check out below Script first  [ 2020 - May - 07 ]
	- Script/202005071200_Migrate existing DMS with SoftDelete functionality.sql

-- In Existing DMS structure having issue of Mounting structure then execute below sql to correct same [ 2020 - June - 17 ]
	- Script/202006172121_DMS_Update_Script_for_Wrong_Structure_Created.sql

-- Enhancement of DMS_Version table [ Changeset: 411 (1ec26961ca61) #1007318 Segragating in DMS Content in Version table ]
	1) Deploy latest plugin including changes of versioning implementation. [ From 2Pack_5.2.0.zip] having AD for DMS_Version table ]
	2) Below script is used for existing DMS data to migrate in version table
		- Execute Script/202101061400_4_DMS_Create_Version_Records_From_Existing_Data.sql 
	3) Test all the thing in DMS is working properly then go for execute below script
		- Execute Script/202101061400_5_DMS_Delete_Content_Association_And_Drop_Columns.sql [ May need to restart the server ]
