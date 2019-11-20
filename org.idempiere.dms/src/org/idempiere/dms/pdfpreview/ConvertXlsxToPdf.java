package org.idempiere.dms.pdfpreview;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hwpf.converter.HtmlDocumentFacade;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCols;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;

public class ConvertXlsxToPdf
{

	private XSSFWorkbook		x;
	private HtmlDocumentFacade	htmlDocumentFacade;
	private Element				page;
	private StringBuilder		css	= new StringBuilder();

	public ConvertXlsxToPdf(String filePath) throws IOException, InvalidFormatException, ParserConfigurationException
	{

		InputStream in = new FileInputStream(new File(filePath));
		OPCPackage op = OPCPackage.open(in);
		x = new XSSFWorkbook(op);
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		this.htmlDocumentFacade = new HtmlDocumentFacade(document);

		Element window = htmlDocumentFacade.createBlock();
		window.setAttribute("id", "window");
		page = htmlDocumentFacade.createBlock();
		page.setAttribute("id", "page");

		window.appendChild(page);
		htmlDocumentFacade.getBody().appendChild(window);
	}

	/**
	 * @param filePath
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public static void convert(String filePath, String output)
			throws InvalidFormatException, IOException, ParserConfigurationException, TransformerException
	{
		ConvertXlsxToPdf converter = new ConvertXlsxToPdf(filePath);

		Integer sheetNum = converter.x.getNumberOfSheets();
		for (int i = 0; i < sheetNum; i++)
		{
			XSSFSheet sheet = converter.x.getSheet(converter.x.getSheetName(i));
			String sheetName = converter.x.getSheetName(i);
			// System.out.println("----starting process sheet : " + sheetName);
			// add sheet title
			{
				Element title = converter.htmlDocumentFacade.createHeader2();
				title.setTextContent(sheetName);
				converter.page.appendChild(title);
			}

			converter.processSheet(converter.page, sheet, "_" + i + "_");
		}

		converter.htmlDocumentFacade.updateStylesheet();

		Element style = (Element) converter.htmlDocumentFacade.getDocument().getElementsByTagName("style").item(0);
		style.setTextContent(converter.css.append(style.getTextContent()).toString());
		converter.saveAsHtml(output, converter.htmlDocumentFacade.getDocument());
	}

	private void processSheet(Element container, XSSFSheet sheet, String sID)
	{

		Element table = htmlDocumentFacade.createTable();
		int sIndex = sheet.getWorkbook().getSheetIndex(sheet);
		String sId = "sheet_".concat(String.valueOf(sIndex));
		table.setAttribute("id", sId);
		table.setAttribute("border", "1");
		table.setAttribute("cellpadding", "2");
		table.setAttribute("cellspacing", "0");
		table.setAttribute("style", "border-collapse: collapse;");

		if (sheet.getDefaultRowHeightInPoints() > 0)
		{
			css.append("#").append(sId).append(" tr{height:").append(sheet.getDefaultRowHeightInPoints() / 28.34)
					.append("cm}\n");
		}
		if (sheet.getDefaultColumnWidth() > 0)
		{
			css.append("#").append(sId).append(" td{width:").append(sheet.getDefaultColumnWidth() * 0.21)
					.append("cm}\n");
		}
		// cols
		generateColumns(sheet, table);
		// rows

		final short col_num = get_col_max(sheet);
		final int row_num = sheet.getLastRowNum() + 1;
		for (int i = 0; i < row_num; i++)
		{
			Row row = sheet.getRow(i);

			// to skip hidden row
			if (row != null && row.getZeroHeight())
			{
				continue;
			}

			processRow(table, (XSSFRow) row, sheet, col_num, sID, i);
		}

		container.appendChild(table);
	}

	private short get_col_max(XSSFSheet sheet)
	{
		short ans = -1;
		// rows
		Iterator<Row> rows = sheet.iterator();
		while (rows.hasNext())
		{
			Row row = rows.next();
			if (row instanceof XSSFRow)
			{
				short c = (short) (row.getLastCellNum());
				if (ans < c)
				{
					ans = c;
				}
			}
		}
		return ans;
	}

	/**
	 * generated <code><col><code> tags.
	 *
	 * @param sheet
	 * @param table container.
	 */
	private void generateColumns(XSSFSheet sheet, Element table)
	{
		List<CTCols> colsList = sheet.getCTWorksheet().getColsList();
		MathContext mc = new MathContext(3);
		for (CTCols cols : colsList)
		{
			long oldLevel = 1;
			for (CTCol col : cols.getColArray())
			{
				while (true)
				{
					if (oldLevel == col.getMin())
					{
						break;
					}
					Element column = htmlDocumentFacade.createTableColumn();
					// htmlDocumentFacade.addStyleClass(column, "col",
					// "width:2cm;");
					column.setAttribute("style", "width:2cm;");
					table.appendChild(column);
					oldLevel++;
				}
				Element column = htmlDocumentFacade.createTableColumn();
				String width = new BigDecimal(sheet.getColumnWidth(Long.bitCount(oldLevel)) / 1440.0, mc).toString();
				column.setAttribute("style", "width:".concat(width).concat("cm;"));
				table.appendChild(column);

				oldLevel++;
			}
		}
	}

	private void processRow(Element table, XSSFRow row, XSSFSheet sheet, final int col_num, String sID, int pos_row)
	{
		Element tr = htmlDocumentFacade.createTableRow();

		if (!(row instanceof XSSFRow))
		{
			for (int pos_col = 0; pos_col < col_num; pos_col++)
			{
				processCell(tr, null, sID, pos_col, pos_row); // empty line
			}
		}
		else
		{
			if (row.isFormatted())
			{
				// TODO build row style...
			}

			if (row.getCTRow().getCustomHeight())
			{
				tr.setAttribute("style", "height:".concat(String.valueOf(row.getHeightInPoints())).concat("pt;"));
			}

			for (int pos_col = 0; pos_col < col_num; pos_col++)
			{
				Cell cell = row.getCell(pos_col);
				if (cell instanceof XSSFCell)
				{
					processCell(tr, (XSSFCell) cell, sID, pos_col, pos_row);
				}
				else
				{
					processCell(tr, null, sID, pos_col, pos_row);
				}
			}
		}
		table.appendChild(tr);
	}

	private void processCell(Element tr, XSSFCell cell, String sID, int pos_col, int pos_row)
	{

		int cols = 1;
		int rows = 1;
		if (cell != null)
		{

			if (cell != null)
			{
				int num = cell.getSheet().getNumMergedRegions();
				for (int i = 0; i < num; i++)
				{

					CellRangeAddress c = cell.getSheet().getMergedRegion(i);

					int x0 = c.getFirstColumn();
					int x1 = c.getLastColumn();
					int y0 = c.getFirstRow();
					int y1 = c.getLastRow();
					if (x0 == pos_col && y0 == pos_row)
					{
						cols = c.getLastColumn() - c.getFirstColumn() + 1;
						rows = c.getLastRow() - c.getFirstRow() + 1;
					}
					else if ((x0 <= pos_col) && (pos_col <= x1) && (y0 <= pos_row) && (pos_row <= y1))
					{
						return;
					}
				}
			}
		}

		Element td = htmlDocumentFacade.createTableCell();
		if (cols > 1)
		{
			td.setAttribute("colspan", "" + cols);
		}
		if (rows > 1)
		{
			td.setAttribute("rowspan", "" + rows);
		}
		Object value;
		if (cell == null)
		{
			// processCellStyle(td, cell.getCellStyle(), null);
			td.setTextContent("\u00a0");
		}
		else
		{
			if(CellType.BLANK.equals(cell.getCellType())) {
					value = "\u00a0";
			}else if(CellType.NUMERIC.equals(cell.getCellType())){
					if (DateUtil.isCellDateFormatted(cell))
					{
						// to display date format
						DataFormatter dateFormatter = new DataFormatter();
						value = dateFormatter.formatCellValue(cell);
					}
					else
					{
						value = cell.getNumericCellValue();
					}
			}else if(CellType.BOOLEAN.equals(cell.getCellType())) {
					value = cell.getBooleanCellValue();
			}else if(CellType.FORMULA.equals(cell.getCellType())) {
					if (CellType.ERROR.equals(cell.getCachedFormulaResultType()))
					{
						value = cell.getCellFormula();
					}
					else
					{
						// To eveluate formula
						FormulaEvaluator formulaEvalutor = x.getCreationHelper().createFormulaEvaluator();
						DataFormatter fmt = new DataFormatter();
						value = fmt.formatCellValue(cell, formulaEvalutor);
					}
			}else {
					value = cell.getRichStringCellValue();
			}
			if (value instanceof XSSFRichTextString)
			{
				processCellStyle(td, cell.getCellStyle(), (XSSFRichTextString) value, sID);
				td.setTextContent(value.toString());
			}
			else
			{
				processCellStyle(td, cell.getCellStyle(), null, sID);
				td.setTextContent(value.toString());
			}
			// String s = value.toString();
			// System.out.println(s);
		}
		tr.appendChild(td);
	}

	private void processCellStyle(Element td, XSSFCellStyle style, XSSFRichTextString rts, String sID)
	{
		StringBuilder sb = new StringBuilder();

		if (rts != null)
		{
			XSSFFont font = rts.getFontOfFormattingRun(1);
			if (font != null)
			{
				sb.append("font-family:").append(font.getFontName()).append(";");
				// sb.append("color:").append(font.getColor() ).append(";");
				sb.append("font-size:").append(font.getFontHeightInPoints()).append("pt;");
				if (font.getXSSFColor() != null)
				{
					String color = font.getXSSFColor().getARGBHex().substring(2);
					sb.append("color:#").append(color).append(";");
				}
				if (font.getItalic())
				{
					sb.append("font-style:italic;");
				}
				if (font.getBold())
				{
					sb.append("font-weight:bold;");
				}
				if (font.getStrikeout())
				{
					sb.append("text-decoration:underline;");
				}
			}
		}
		/*if (style.getAlignment() != 1)
		{
			switch (style.getAlignment())
			{
				case 2:
					sb.append("text-align:").append("center;");
					break;
				case 3:
					sb.append("text-align:").append("right;");
					break;
			}
		}

		// TODO: set correct value for type and width of border.
		if (style.getBorderBottom() != 0)
		{
			sb.append("border-bottom:solid; ").append(style.getBorderBottom()).append("px;");
		}
		if (style.getBorderLeft() != 0)
		{
			sb.append("border-left:solid; ").append(style.getBorderLeft()).append("px;");
		}
		if (style.getBorderTop() != 0)
		{
			sb.append("border-top:solid; ").append(style.getBorderTop()).append("px;");
		}
		if (style.getBorderRight() != 0)
		{
			sb.append("border-right:solid; ").append(style.getBorderRight()).append("px;");
		}*/

		// if (style.getFillBackgroundXSSFColor() != null) {
		// XSSFColor color = style.getFillBackgroundXSSFColor();
		// }

		// System.out.println(style.getFillBackgroundXSSFColor());
		if (style.getFillBackgroundXSSFColor() != null)
		{
			sb.append("background:#fcc;");
		}
		// System.out.println(sb.toString());
		htmlDocumentFacade.addStyleClass(td, "td" + sID, sb.toString());
	}

	/**
	 * @param output
	 * @param document
	 * @throws IOException
	 * @throws TransformerException
	 */
	private void saveAsHtml(String output, org.w3c.dom.Document document) throws IOException, TransformerException
	{

		// check path
		File folder = new File(getFilePath(output));
		if (!folder.canRead())
		{
			folder.mkdirs();
		}

		// FileWriter out = new FileWriter(output);
		FileOutputStream fos = new FileOutputStream(output);
		DOMSource domSource = new DOMSource(document);
		StreamResult result = new StreamResult(new StringWriter());
		// StreamResult streamResult = new StreamResult(out);

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer serializer = tf.newTransformer();
		
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "2");
		serializer.setOutputProperty(OutputKeys.METHOD, "html");
		// serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "");
		serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		serializer.transform(domSource, result);
		String s = "<!DOCTYPE html>\n" + result.getWriter().toString();
		OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");
		out.write(s);
		out.close();
	}

	public String getFilePath(String fileFullPath)
	{
		int sep = fileFullPath.lastIndexOf("\\") == -1 ? fileFullPath.lastIndexOf("/") : fileFullPath.lastIndexOf("\\");
		return fileFullPath.substring(0, sep + 1);
	}
}
