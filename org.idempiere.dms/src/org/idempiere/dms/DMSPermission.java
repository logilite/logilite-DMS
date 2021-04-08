package org.idempiere.dms;

import org.adempiere.webui.editor.WEditor;
import org.compiere.model.GridField;
import org.idempiere.model.MDMSPermission;

public class DMSPermission
{
	Integer	AD_Org_ID;
	Integer	AD_Role_ID;
	Integer	AD_User_ID;
	Integer	AD_Client_ID;
	Integer	DMS_Owner_ID;
	Integer	DMS_Content_ID;
	boolean	isRead;
	boolean	isWrite;
	boolean	isDelete;
	boolean	isNavigation;
	boolean	isAllPermission;

	public Integer getAD_Org_ID( )
	{
		return AD_Org_ID;
	}

	public void setAD_Org_ID(int aD_Org_ID)
	{
		AD_Org_ID = aD_Org_ID;
	}

	public Integer getAD_Client_ID( )
	{
		return AD_Client_ID;
	}

	public void setAD_Client_ID(int aD_Client_ID)
	{
		AD_Client_ID = aD_Client_ID;
	}

	public Integer getDMS_Content_ID( )
	{
		return DMS_Content_ID;
	}

	public void setDMS_Content_ID(int dMS_Content_ID)
	{
		DMS_Content_ID = dMS_Content_ID;
	}

	public Integer getDMS_Owner_ID( )
	{
		return DMS_Owner_ID;
	}

	public void setDMS_Owner_ID(int dMS_Owner_ID)
	{
		DMS_Owner_ID = dMS_Owner_ID;
	}

	public Integer getAD_Role_ID( )
	{
		return AD_Role_ID;
	}

	public void setAD_Role_ID(int aD_Role_ID)
	{
		AD_Role_ID = aD_Role_ID;
	}

	public Integer getAD_User_ID( )
	{
		return AD_User_ID;
	}

	public void setAD_User_ID(int aD_User_ID)
	{
		AD_User_ID = aD_User_ID;
	}

	public boolean IsRead( )
	{
		return isRead;
	}

	public void setIsRead(boolean isRead)
	{
		this.isRead = isRead;
	}

	public boolean IsWrite( )
	{
		return isWrite;
	}

	public void setIsWrite(boolean isWrite)
	{
		this.isWrite = isWrite;
	}

	public boolean IsDelete( )
	{
		return isDelete;
	}

	public void setIsDelete(boolean isDelete)
	{
		this.isDelete = isDelete;
	}

	public boolean IsNavigation( )
	{
		return isNavigation;
	}

	public void setIsNavigation(boolean isNavigation)
	{
		this.isNavigation = isNavigation;
	}

	public boolean IsAllPermission( )
	{
		return isAllPermission;
	}

	public void setIsAllPermission(boolean isAllPermission)
	{
		this.isAllPermission = isAllPermission;
	}

	public void setValue(WEditor editor)
	{

		GridField field = editor.getGridField();
		field.validateValueNoDirect();
		if (MDMSPermission.COLUMNNAME_DMS_Owner_ID.equalsIgnoreCase(field.getColumnName()))
		{
			setDMS_Owner_ID(getValueAsInt(editor));
		}
		else if (MDMSPermission.COLUMNNAME_IsRead.equalsIgnoreCase(field.getColumnName()))
		{
			setIsRead(getValueAsBoolean(editor));
		}
		else if (MDMSPermission.COLUMNNAME_IsWrite.equalsIgnoreCase(field.getColumnName()))
		{
			setIsWrite(getValueAsBoolean(editor));
		}
		else if (MDMSPermission.COLUMNNAME_IsDelete.equalsIgnoreCase(field.getColumnName()))
		{
			setIsDelete(getValueAsBoolean(editor));
		}
		else if (MDMSPermission.COLUMNNAME_IsNavigation.equalsIgnoreCase(field.getColumnName()))
		{
			setIsNavigation(getValueAsBoolean(editor));
		}
		else if (MDMSPermission.COLUMNNAME_AD_Role_ID.equalsIgnoreCase(field.getColumnName()))
		{
			if (editor.getValue() != null)
				setAD_Role_ID(getValueAsInt(editor));
		}
		else if (MDMSPermission.COLUMNNAME_AD_User_ID.equalsIgnoreCase(field.getColumnName()))
		{
			if (editor.getValue() != null)
				setAD_User_ID(getValueAsInt(editor));
		}
		else if (MDMSPermission.COLUMNNAME_IsAllPermission.equalsIgnoreCase(field.getColumnName()))
		{
			setIsAllPermission(getValueAsBoolean(editor));
		}
	}

	private boolean getValueAsBoolean(WEditor editor)
	{
		if (editor.getValue() == null)
			return false;

		return (boolean) editor.getValue();
	}

	private Integer getValueAsInt(WEditor editor)
	{
		if (editor.getValue() == null)
			return 0;

		return (Integer) editor.getValue();
	}
}
