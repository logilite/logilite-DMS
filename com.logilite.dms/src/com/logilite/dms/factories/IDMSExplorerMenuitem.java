package com.logilite.dms.factories;

import com.logilite.dms.DMS;
import com.logilite.dms.model.MDMSContent;

/**
 * DMS Explorer Create Custom Context Menu item interface
 * 
 * @author Sachin Bhimani
 * @since  2024-Aug-01
 */
public interface IDMSExplorerMenuitem
{

	public void openContentContextMenuAction(DMS dms, MDMSContent currContent, MDMSContent copyContent);

}
