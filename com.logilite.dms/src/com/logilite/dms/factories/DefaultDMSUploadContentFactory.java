package com.logilite.dms.factories;

import com.logilite.dms.DMS;
import com.logilite.dms.form.WUploadContent;
import com.logilite.dms.model.I_DMS_Content;

/**
 * Default factory for Upload Content in DMS
 * 
 * @author Sachin Bhimani
 */
public class DefaultDMSUploadContentFactory implements IDMSUploadContentFactory
{

	@Override
	public IDMSUploadContent getUploadForm(DMS dms, I_DMS_Content content, boolean isVersion, int tableID, int recordID, int windowNo, int tabNo)
	{
		return new WUploadContent(dms, content, isVersion, tableID, recordID, windowNo, tabNo);
	}

}
