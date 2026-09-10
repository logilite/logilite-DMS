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

package com.logilite.dms.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.w3c.tidy.Tidy;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.logilite.dms.model.MDMSMimeType;
import com.logilite.dms.pdfpreview.ConvertXlsToPdf;
import com.logilite.dms.pdfpreview.ConvertXlsxToPdf;

/**
 * Util for document convert to PDF format
 * 
 * @author Sachin Bhimani
 */
public class DMSConvertToPDFUtils
{

	/**
	 * Convert DOCX or DOC to PDF
	 * 
	 * @param  documentToPreview
	 * @param  mimeType
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws DocumentException
	 * @throws com.itextpdf.text.DocumentException
	 */
	public static File convertDocToPDF(File documentToPreview, MDMSMimeType mimeType) throws IOException, FileNotFoundException, DocumentException
	{
		if (mimeType.getMimeType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
		{
			return null;
			// TODO Need to implement DOCX to PDF conversion
			// try (InputStream inputStream = new FileInputStream(documentToPreview);
			// XWPFDocument document = new XWPFDocument(inputStream))
			// {
			// File newDocPDF = File.createTempFile("DMSExport", "DocxToPDF");
			// try (OutputStream pdfFile = new FileOutputStream(newDocPDF))
			// {
			// PdfConverter.getInstance().convert(document, pdfFile, null);
			// }
			//
			// return newDocPDF;
			// }
		}
		else if (mimeType.getMimeType().equals("application/msword"))
		{
			return null;
			// HWPFDocument doc = new HWPFDocument(new FileInputStream(documentToPreview));
			// @SuppressWarnings("resource")
			// WordExtractor we = new WordExtractor(doc);
			// File newDocPDF = File.createTempFile("DMSExport", "DocToPDF");
			// OutputStream pdfFile = new FileOutputStream(newDocPDF);
			// String k = we.getText();
			// Document document = new Document();
			// PdfWriter.getInstance(document, pdfFile);
			// document.open();
			// document.add(new Paragraph(k));
			// document.close();
			// return newDocPDF;
		}
		else if (mimeType.getMimeType().equals("application/vnd.ms-excel")
					|| mimeType.getMimeType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
		{
			String fileName = documentToPreview.getName();
			if (fileName != null && fileName.length() > 0)
			{
				if (".xls".equalsIgnoreCase(mimeType.getFileExtension()))
				{
					return DMSConvertToPDFUtils.convertXlsToPDF(documentToPreview);
				}
				else if (".xlsx".equalsIgnoreCase(mimeType.getFileExtension()))
				{
					return DMSConvertToPDFUtils.convertXlsxToPdf(documentToPreview);
				}
			}
		}

		return documentToPreview;
	} // convertDocToPDF

	/**
	 * Convert XLS to PDF
	 * 
	 * @param  xlsDocument
	 * @return                       PDF file
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DocumentException
	 */
	public static File convertXlsToPDF(File xlsDocument) throws FileNotFoundException, IOException, DocumentException
	{
		InputStream in = new FileInputStream(xlsDocument);
		ConvertXlsToPdf test = new ConvertXlsToPdf(in);
		String html = test.getHTML();

		File newXlsToPdf = File.createTempFile("DMSExport", "XlsToPDF");
		Document document = new Document(PageSize.A4, 20, 20, 20, 20);

		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(newXlsToPdf));
		document.open();
		document.addCreationDate();
		XMLWorkerHelper.getInstance().parseXHtml(writer, document, new StringReader(html));
		document.close();
		in.close();

		return newXlsToPdf;
	} // convertXlsToPDF

	/**
	 * Convert XLSX to PDF
	 * 
	 * @param  xlsxDocument
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws com.itextpdf.text.DocumentException
	 * @throws com.itextpdf.text.DocumentException
	 */
	public static File convertXlsxToPdf(File xlsxDocument) throws IOException, FileNotFoundException, DocumentException
	{
		File newXlsxToHTML = File.createTempFile("DMSExport", "XlsxToHTML");
		try
		{
			float pdfWidth = 1050;
			float pdfheight = 900;
			String sourcePath = xlsxDocument.getAbsolutePath();
			String destinationPath = newXlsxToHTML.getAbsolutePath();

			// Convert .xlsx file to html
			ConvertXlsxToPdf.convert(sourcePath, destinationPath);
			InputStream in = new FileInputStream(new File(destinationPath));

			// Convert html to xhtml
			Tidy tidy = new Tidy();
			File newHtmlToXhtml = File.createTempFile("DMSExport", "HtmlToXHtml");
			tidy.setShowWarnings(false);
			// tidy.setXmlTags(true);
			tidy.setXHTML(true);
			tidy.setMakeClean(true);
			org.w3c.dom.Document d = tidy.parseDOM(in, null);
			tidy.pprint(d, new FileOutputStream(newHtmlToXhtml));

			// Convert xhtml to pdf
			File newXhtmlToPdf = File.createTempFile("DMSExport", "XHtmlToPdf");
			Document document = new Document();
			Rectangle size = new Rectangle(pdfWidth, pdfheight);
			document.setPageSize(size);
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(newXhtmlToPdf));
			document.open();

			XMLWorkerHelper.getInstance().parseXHtml(writer, document, new FileInputStream(newHtmlToXhtml));
			document.close();

			return newXhtmlToPdf;
		}
		catch (InvalidFormatException | ParserConfigurationException | TransformerException e)
		{
			throw new AdempiereException(e);
		}
	} // convertXlsxToPdf

}
