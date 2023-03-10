package org.idempiere.dms.factories;

import org.idempiere.dms.component.DefaultComponentIconViewerGallery;
import org.idempiere.dms.component.DefaultComponentIconViewerLarge;
import org.idempiere.dms.component.DefaultComponentIconViewerList;
import org.idempiere.dms.component.DefaultComponentIconViewerVersion;
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
		else if (iconView.equals(DMSConstant.ICON_VIEW_GALLERY))
			return new DefaultComponentIconViewerGallery();

		return null;
	}
}
