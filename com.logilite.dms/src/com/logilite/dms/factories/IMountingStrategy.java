/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package com.logilite.dms.factories;

import org.compiere.model.PO;

import com.logilite.dms.model.MDMSContent;

public interface IMountingStrategy
{
	public String getMountingPath(String Table_Name, int Record_ID);

	public String getMountingPath(PO po);

	public MDMSContent getMountingParent(String Table_Name, int Record_ID);

	public MDMSContent getMountingParent(int AD_Table_ID, int Record_ID);

	public MDMSContent getMountingParentForArchive();

	public MDMSContent getMountingParentForArchive(int AD_Table_ID, int Record_ID, int Process_ID);

	public void initiateMountingContent(String mountingBaseName, String Table_Name, int RecordID, int AD_Table_ID, String trxName);
}
