package com.logilite.dms.factories;

import java.util.List;

import org.adempiere.webui.component.Menupopup;

/**
 * DMS Context Menu Factory for adding custom menu items while open popup menu by right click in DMS
 * explorer
 * 
 * @author Sachin Bhimani
 * @since  2024-Aug-01
 */
public interface IDMSExplorerContextMenuFactory
{

	List<IDMSExplorerMenuitem> addMenuitems(Menupopup menupopup);

}
