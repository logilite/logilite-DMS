package org.idempiere.dms.factories;

import org.idempiere.componenet.DefaultComponentIconViewerLarge;
import org.idempiere.componenet.DefaultComponentIconViewerList;
import org.idempiere.componenet.DefaultComponentIconViewerVersion;
import org.idempiere.dms.constant.DMSConstant;

/**
 * Default DMS Viewer
 * 
 * @author Sachin
 */
public class DefaultDMSViewer implements IDMSViewerFactory
{

	@Override
	public IDMSViewer get(String iconView)
	{
		if (iconView.equals(DMSConstant.ICON_VIEW_LARGE))
			return new DefaultComponentIconViewerLarge();
		else if (iconView.equals(DMSConstant.ICON_VIEW_LIST))
			return new DefaultComponentIconViewerList();
		else if (iconView.equals(DMSConstant.ICON_VIEW_VERSION))
			return new DefaultComponentIconViewerVersion();

		return null;
	}
}
