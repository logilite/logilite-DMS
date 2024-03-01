package com.logilite.dms.fileuploader.util;

import java.io.File;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.compiere.util.Util;

import com.logilite.dms.tika.service.FileContentExtract;
import com.logilite.dms.util.Utils;

public class FileUploaderUtils
{
	private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	public static Timestamp getCreatedDate(File file)
	{
		String extension = Utils.getFileExtension(file.getName());
		if (extension != null && extension.toLowerCase().equalsIgnoreCase(".pdf"))
		{
			FileContentExtract extract = new FileContentExtract(file, false);
			String created = extract.getParsedDocumentMetadataValue("pdf:docinfo:created");
			if (!Util.isEmpty(created))
			{
				OffsetDateTime dateTime = OffsetDateTime.parse(created, ISO_DATE_FORMAT);

				return Timestamp.from(dateTime.toInstant());
			}
		}
		return null;
	} // getCreatedDate

	public static String removeSpecialChars(String str)
	{
		// Replace & with '_AND_'
		String result = str.replaceAll("&", "_AND_");

		// Define the regular expression for special characters
		return result.replaceAll("[^a-zA-Z0-9\\s]", "_");
	} // removeSpecialChars
}
