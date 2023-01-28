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

package com.logilite.dms.factories;

import java.io.File;
import java.util.ArrayList;

import com.logilite.dms.model.I_DMS_Version;

/**
 * @author deepak@logilite.com
 */
public interface IThumbnailProvider
{
	public void init();

	public String getURL(I_DMS_Version version, String size);

	public File getFile(I_DMS_Version version, String size);

	public ArrayList<File> getThumbnails(I_DMS_Version version);

	public String getThumbDirPath(I_DMS_Version version);

	public String getThumbPath(I_DMS_Version version, String size);

}
