package org.idempiere.dms.storage;

import org.adempiere.webui.adwindow.IADTabpanel;

import com.adempiere.webui.adwindow.factory.IADTabPanelFactory;

public class CustomADTabPanelFactory implements IADTabPanelFactory
{

	@Override
	public IADTabpanel getInstance(String type)
	{
		if ("DMS".equalsIgnoreCase(type))
			return new DMSContentTab();
		else
			return null;
	}

}