package org.idempiere.model;

import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.compiere.model.GridTab;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.util.Env;

/**
 * POJO for binding Substitute vs Origin table and fetch proper data
 * 
 * @since 2020-Sep-15
 */
public class DMSSubstituteTableInfo
{
	private AbstractADWindowContent	winContent;
	private GridTab					gridTab;

	private MDMSSubstitute			substitute;

	private String					table_Name;

	private int						table_ID;
	private int						column_ID;
	private int						record_ID;

	public DMSSubstituteTableInfo(AbstractADWindowContent winPanel, GridTab gridTab, MDMSSubstitute substitute)
	{
		this.winContent = winPanel;
		this.gridTab = gridTab;
		this.substitute = substitute;

		//
		configTableInfo();
	}

	/**
	 * Configure Table reference from current GridTab or Substitute table
	 */
	private void configTableInfo()
	{
		if (substitute == null)
		{
			this.table_ID = gridTab.getAD_Table_ID();
			this.table_Name = gridTab.getTableName();
			this.column_ID = -1;
		}
		else
		{
			this.table_ID = substitute.getDMS_Substitute_Table_ID();
			this.table_Name = MTable.getTableName(Env.getCtx(), this.getTable_ID());
			this.column_ID = substitute.getAD_Column_ID();
		}
	} // configTableInfo

	/**
	 * Modify the record_ID from GridTab as current selected record or origin column record for
	 * substitute
	 */
	public void updateRecord()
	{
		if (substitute == null)
		{
			this.record_ID = gridTab.getRecord_ID();
		}
		else
		{
			Object recordID = winContent.getADTab().getSelectedGridTab().getValue(MColumn.getColumnName(Env.getCtx(), this.getColumn_ID()));
			if (recordID == null)
				this.record_ID = (int) -1;
			else
				this.record_ID = (int) recordID;
		}
	} // updateRecord

	public int getTable_ID()
	{
		return table_ID;
	}

	public String getTable_Name()
	{
		return table_Name;
	}

	public int getColumn_ID()
	{
		return column_ID;
	}

	public int getRecord_ID()
	{
		return record_ID;
	}
}
