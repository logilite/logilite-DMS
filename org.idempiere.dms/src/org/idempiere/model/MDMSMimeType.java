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

package org.idempiere.model;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MImage;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.Utils;

public class MDMSMimeType extends X_DMS_MimeType {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3814987450064904684L;

	static CCache<Integer, MImage> cache_mimetypeThumbnail = new CCache<Integer, MImage>("MimetypeThumbnail", 2);

	private static String SQL_GET_MIMETYPE_ID = "SELECT DMS_MimeType_ID FROM DMS_MimeType ";
	private static String SQL_GET_ICON_ID = "SELECT Icon_ID FROM DMS_MimeType ";

	//
	public MDMSMimeType(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public MDMSMimeType(Properties ctx, int DMS_MimeType_ID, String trxName) {
		super(ctx, DMS_MimeType_ID, trxName);
	}

	public boolean beforeSave(boolean newRecord) {
		int count = DB.getSQLValue(null, "SELECT COUNT(1) FROM DMS_MimeType WHERE UPPER(MimeType) = UPPER('"
				+ getMimeType() + "') OR UPPER(FileExtension) = UPPER ('" + getFileExtension() + "')");
		if (count != 0 && getDMS_MimeType_ID() == 0) {
			throw new AdempiereException("MimeType and File Extension must be unique.");
		}
		return true;
	} // beforeSave

	/**
	 * get MimeTypeID from File
	 * 
	 * @param file
	 * @return
	 */
	public static int getMimeTypeID(File file) {
		int dmsMimeType_ID = -1;
		if (file != null) {
			String ext = Utils.getFileExtension(file.getName());
			if (!Util.isEmpty(ext))
				dmsMimeType_ID = DB.getSQLValue(null,
						SQL_GET_MIMETYPE_ID + " WHERE UPPER(FileExtension) = '" + ext.toUpperCase() + "'");
		}

		if (dmsMimeType_ID != -1)
			return dmsMimeType_ID;
		else
			dmsMimeType_ID = DB.getSQLValue(null, SQL_GET_MIMETYPE_ID + " WHERE Name = ?", DMSConstant.DEFAULT);

		return dmsMimeType_ID;
	} // getMimeTypeID

	/**
	 * get thumbnail of content related mimetype
	 * 
	 * @param DMS_MimeType_ID
	 * @return {@link MImage}
	 */
	public static MImage getThumbnail(int DMS_MimeType_ID) {
		MImage image = cache_mimetypeThumbnail.get(DMS_MimeType_ID);

		if (image != null)
			return image;

		int iconID = DB.getSQLValue(null, SQL_GET_ICON_ID + " WHERE DMS_MimeType_ID=?", DMS_MimeType_ID);
		if (iconID <= 0) {
			iconID = DB.getSQLValue(null, SQL_GET_ICON_ID + " WHERE UPPER(Name) = UPPER(?) ", DMSConstant.DEFAULT);
			if (iconID <= 0)
				iconID = DB.getSQLValue(null, "SELECT AD_Image_ID FROM AD_Image WHERE UPPER(Name) = UPPER(?) ",
						DMSConstant.DOWNLOAD);
		}

		image = new MImage(Env.getCtx(), iconID, null);
		cache_mimetypeThumbnail.put(DMS_MimeType_ID, image);
		return image;
	} // getThumbnail

	public static MDMSMimeType getByMimeType(String mimeType) {

		String where = " IsActive = 'Y' AND MimeType = ? ";
		Query query = new Query(Env.getCtx(), MDMSMimeType.Table_Name, where, null);
		query.setParameters(mimeType);
		List<MDMSMimeType> mimeTypes = query.list();
		if (mimeTypes != null && mimeTypes.size() > 0) {
			return mimeTypes.get(0);
		} else
			return null;
	}
}
