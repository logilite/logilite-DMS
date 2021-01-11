package org.idempiere.dms.factories;

import java.util.HashMap;

import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Version;

public interface IContentTypeAccess
{

	public HashMap<I_DMS_Version, I_DMS_Association> getFilteredContentList(HashMap<I_DMS_Version, I_DMS_Association> contentMap);

}
