package com.logilite.dms.uuid.storage;

import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.storage.ThumbnailProvider;
import org.idempiere.model.I_DMS_Version;

public class UUIDThumbnailProvider extends ThumbnailProvider
{

	@Override
	public String getThumbDirPath(I_DMS_Version version)
	{
		return thumbnailBasePath + DMSConstant.FILE_SEPARATOR + Env.getAD_Client_ID(Env.getCtx()) + DMSConstant.FILE_SEPARATOR + version.getDMS_Version_UU();
	}

	@Override
	public String getThumbPath(I_DMS_Version version, String size)
	{
		String path = getThumbDirPath(version) + DMSConstant.FILE_SEPARATOR + version.getDMS_Version_UU() + "_V";

		if (!Util.isEmpty(size, true))
			path += "-" + size;
		else
			path += "-" + MSysConfig.getValue(DMSConstant.DMS_THUMBNAILS_SIZES, "150,300,500").split(",")[0];

		return path + ".jpg";
	}

}
