package org.idempiere.model;

import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * POJO for binding Substitute vs Origin table and fetch proper data
 * 
 * @since 2020-Sep-15
 */
public class DMSSubstituteTableInfo
{
	private int				originTable_ID;
	private int				originRecord_ID;

	private MDMSSubstitute	substitute;

	private int				substituteTable_ID;
	private int				substituteRecord_ID;

	public DMSSubstituteTableInfo(int tableID)

	{
		this.originTable_ID = tableID;
		this.substitute = MDMSSubstitute.get(tableID);
		configTableInfo();
	}

	public DMSSubstituteTableInfo(int tableID, int recordID)
	{
		this(tableID);
		updateRecord(recordID);
	}

	/**
	 * Configure Table reference from current GridTab or Substitute table
	 */
	private void configTableInfo()
	{
		if (substitute == null)
		{
			this.substituteTable_ID = this.originTable_ID;
		}
		else
		{
			this.substituteTable_ID = substitute.getDMS_Substitute_Table_ID();
		}
	} // configTableInfo

	/**
	 * Modify the record_ID from GridTab as current selected record or origin column record for
	 * substitute
	 */
	public void updateRecord(int recordID)
	{
		setOriginRecordID(recordID);
		if (substitute != null)
		{
			String pKey = MTable.get(Env.getCtx(), substitute.getAD_Table_ID()).getPO(originRecord_ID, null).get_KeyColumns()[0];
			int subsRecordID = DB
							.getSQLValue(null, "SELECT " + substitute.getAD_Column().getColumnName() +
											" FROM " + substitute.getAD_Table().getTableName() +
											" WHERE " + pKey + " = " + originRecord_ID);

			this.substituteRecord_ID = subsRecordID;
		}
		else
		{
			this.substituteRecord_ID = originRecord_ID;
		}
	} // updateRecord

	public int getOriginTable_ID()
	{
		return originTable_ID;
	}

	public String getOriginTable_Name()
	{
		return MTable.getTableName(Env.getCtx(), originTable_ID);
	}

	public void setOriginRecordID(int recordID)
	{
		this.originRecord_ID = recordID;
	}

	public int getOriginRecord_ID()
	{
		return originRecord_ID;
	}

	public MDMSSubstitute getSubstitute()
	{
		return substitute;
	}

	public int getSubstituteTable_ID()
	{
		return substituteTable_ID;
	}

	public String getSubstituteTable_Name()
	{
		return MTable.getTableName(Env.getCtx(), substituteTable_ID);
	}

	public int getSubstituteRecord_ID()
	{
		return substituteRecord_ID;
	}

	public int getValidRecord_ID()
	{
		if (substitute == null)
		{
			return originRecord_ID;
		}
		else
		{
			return substituteRecord_ID;
		}
	}
}
