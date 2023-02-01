package com.logilite.dms.factories;

import com.logilite.dms.component.DefaultComponentIconViewerGallery;
import com.logilite.dms.component.DefaultComponentIconViewerLarge;
import com.logilite.dms.component.DefaultComponentIconViewerList;
import com.logilite.dms.component.DefaultComponentIconViewerVersion;
import com.logilite.dms.constant.DMSConstant;

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
