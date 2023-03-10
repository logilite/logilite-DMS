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

import org.idempiere.dms.DMS;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.IThumbnailGeneratorFactory;

public class DefaultThumbnailGeneratorFactory implements IThumbnailGeneratorFactory
{

	@Override
	public IThumbnailGenerator get(DMS dms, String mimeType)
	{
		if (mimeType.equalsIgnoreCase("application/pdf"))
		{
			return new PDFThumbnailGenerator(dms);
		}
		else if (mimeType.startsWith("image/"))
		{
			return new ImageThumbnailGenerator(dms);
		}
		return null;
	}

}
