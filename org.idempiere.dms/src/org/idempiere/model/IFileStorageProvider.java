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

import org.compiere.model.I_AD_StorageProvider;

/**
 * @author Deepak@logilite.com
 */
public interface IFileStorageProvider
{
	public void init(I_AD_StorageProvider storageProvider);

	public File[] getFiles(String parent, String pattern);

	public File getFile(String path);

	public String[] list(String parent);

	public byte[] getBLOB(String path);

	public boolean writeBLOB(String path, byte[] data, I_DMS_Content DMS_Content);

	public String getBaseDirectory(String path);
}
