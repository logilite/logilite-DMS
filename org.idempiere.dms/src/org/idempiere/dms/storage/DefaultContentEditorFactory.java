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

package org.idempiere.dms.storage;

import java.util.ArrayList;

import org.idempiere.dms.factories.IContentEditor;
import org.idempiere.dms.factories.IContentEditorFactory;

public class DefaultContentEditorFactory implements IContentEditorFactory
{
    private static ArrayList<String> listMimeType = new ArrayList<String>();

	static
	{
		// Supported mime type for PDF content editor.
		listMimeType.add("application/pdf");
		listMimeType.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		listMimeType.add("application/msword");
		listMimeType.add("application/vnd.ms-excel");
		listMimeType.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}


	@Override
	public IContentEditor get(String mimeType)
	{
		if (listMimeType.contains(mimeType))
		{
			return new PDFContentEditor();
		}
		else if (mimeType.startsWith("image/"))
		{
			return new ImageContentEditor();
		}
		return null;
	}

}
