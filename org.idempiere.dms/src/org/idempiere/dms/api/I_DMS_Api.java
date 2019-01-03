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

package org.idempiere.dms.api;

import java.io.File;
import java.util.Map;

import org.idempiere.model.MDMSContent;

/**
 * DMS API
 * 
 * @author Sachin
 */
public interface I_DMS_Api
{

	// Adding file
	public boolean addFile(File file);

	public boolean addFile(String dirPath, File file);

	public boolean addFile(String dirPath, File file, String fileName);

	public boolean addFile(String dirPath, File file, String fileName, int AD_Table_ID, int Record_ID);

	public boolean addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap);

	public boolean addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap, int AD_Table_ID, int Record_ID);

	// Adding versioning files
	public boolean addFileVersion(int DMS_Content_ID, File file);

	// TODO Need to thing on this stuff
	// public boolean addFileVersion(String dirPath, File file);
	//
	// public boolean addFileVersion(String dirPath, File file, String
	// fileName);
	//
	// public boolean addFileVersion(String dirPath, File file, String fileName,
	// int AD_Table_ID, int Record_ID);

	// Select content
	public MDMSContent[] selectContent(String dirPath);

	public MDMSContent[] selectContent(String dirPath, int AD_Table_ID, int Record_ID);

}
