package com.logilite.dms.uuid.classes;

import org.compiere.model.PO;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.model.MDMSContent;

/**
 * UUID Mounting Strategy
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class UUIDMountingStrategy implements IMountingStrategy
{

	@Override
	public String getMountingPath(String Table_Name, int Record_ID)
	{
		return null;
	}

	@Override
	public String getMountingPath(PO po)
	{
		return null;
	}

	@Override
	public MDMSContent getMountingParent(String Table_Name, int Record_ID)
	{
		return null;
	}

	@Override
	public MDMSContent getMountingParent(int AD_Table_ID, int Record_ID)
	{
		return null;
	}

	@Override
	public MDMSContent getMountingParentForArchive()
	{
		return null;
	}

	@Override
	public void initiateMountingContent(String mountingBaseName, String Table_Name, int RecordID, int AD_Table_ID)
	{

	}

}
