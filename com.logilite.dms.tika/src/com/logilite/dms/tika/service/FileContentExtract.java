package com.logilite.dms.tika.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.compiere.util.CLogger;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * File content extract through Apache Tika
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 * @since  2020 December 05
 */
public class FileContentExtract
{

	public static CLogger	log				= CLogger.getCLogger(FileContentExtract.class);

	private File			file;
	private Metadata		metadata;
	private DefaultHandler	handler;
	private boolean			isParseHandler	= true;

	public FileContentExtract(File file)
	{
		this(file, true);
	}

	/**
	 * @param file
	 * @param isParseHandler true to enable the parsing of data using the document handler.
	 */
	public FileContentExtract(File file, boolean isParseHandler)
	{
		this.file = file;
		this.isParseHandler = isParseHandler;

		try
		{
			parsedDocument();
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to read file: ", e);
			throw new AdempiereException("Fail to read file: " + e.getLocalizedMessage(), e);
		}

	}// FileContentExtract

	private void parsedDocument() throws IOException
	{
		metadata = new Metadata();
		handler = isParseHandler ? new BodyContentHandler(-1) : null;

		Parser parser = new AutoDetectParser();
		TikaInputStream stream = TikaInputStream.get(new FileInputStream(file));
		ParseContext context = new ParseContext();
		// Configure the parser to only extract metadata
		context.set(Parser.class, parser);

		//
		if (stream.getLength() > 0)
		{
			try
			{
				parser.parse(stream, handler, metadata, context);
			}
			catch (IOException e)
			{
				log.log(Level.SEVERE, "IO exception while parsing document: " + file.getName() + " \nError: " + e.getLocalizedMessage(), e);
			}
			catch (SAXException e)
			{
				log.log(Level.SEVERE, "SAX exception while parsing document: " + file.getName() + " \nError: " + e.getLocalizedMessage(), e);
			}
			catch (TikaException e)
			{
				log.log(Level.SEVERE, "Tika exception while parsing document: " + file.getName() + " \nError: " + e.getLocalizedMessage(), e);
			}
		}

		stream.close();
	}// parsedDocument

	public String getParsedDocumentContent()
	{
		return isParseHandler ? handler.toString() : null;
	} // getParsedDocumentContent

	public String getParsedDocumentMetadataValue(String propertyName)
	{
		return metadata.get(propertyName);
	} // getParsedDocumentMetadataValue
}
