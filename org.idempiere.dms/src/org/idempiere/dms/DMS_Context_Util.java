package org.idempiere.dms;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.webui.editor.WEditor;
import org.compiere.model.GridField;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * Util for context related activities for developer
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class DMS_Context_Util
{

	/** Logger */
	private static CLogger log = CLogger.getCLogger(DMS_Context_Util.class);

	/**
	 * Configure default value for the editor if value loaded into the context
	 * 
	 * @param ctx
	 * @param windowNo
	 * @param tabNo
	 * @param displayType
	 * @param editor
	 */
	public static void setEditorDefaultValueFromCtx(Properties ctx, int windowNo, int tabNo, int displayType, WEditor editor)
	{
		String value = Env.getContext(ctx, windowNo, tabNo, editor.getColumnName(), false, true);
		if (value != null)
		{
			editor.setValue(getDataTypeWiseValue(value, displayType, editor.getColumnName()));
		}
	} // setEditorDefaultValueFromCtx

	/**
	 * get Default Object type.
	 * 
	 * <pre>
	 *		Integer 	(IDs, Integer)
	 *		BigDecimal 	(Numbers)
	 *		Timestamp	(Dates)
	 *		Boolean		(YesNo)
	 *		MultiSelect
	 *		default: String
	 * </pre>
	 * 
	 * @see                {@link GridField} . createDefault()
	 * @param  value       string
	 * @param  displayType
	 * @param  columnName
	 * @return             type dependent converted object
	 */
	public static Object getDataTypeWiseValue(String value, int displayType, String columnName)
	{
		// true NULL
		if (value == null || value.toString().length() == 0 || value.toUpperCase().equals("NULL"))
			return null;
		// see also MTable.readData
		try
		{
			// IDs & Integer & CreatedBy/UpdatedBy
			if (columnName.endsWith("atedBy") || (columnName.endsWith("_ID") && DisplayType.isID(displayType)))
			{
				try // defaults -1 => null
				{
					int ii = Integer.parseInt(value);
					if (ii < 0)
						return null;
					return Integer.valueOf(ii);
				}
				catch (Exception e)
				{
					log.warning("Cannot parse: " + value + " - " + e.getMessage());
				}
				return Integer.valueOf(0);
			}
			// Integer
			if (displayType == DisplayType.Integer)
				return Integer.valueOf(value);

			// Number
			if (DisplayType.isNumeric(displayType))
				return new BigDecimal(value);

			// Timestamps
			if (DisplayType.isDate(displayType))
			{
				// try timestamp format - then date format -- [ 1950305 ]
				java.util.Date date = null;
				SimpleDateFormat dateTimeFormat = DisplayType.getTimestampFormat_Default();
				SimpleDateFormat dateFormat = DisplayType.getDateFormat_JDBC();
				SimpleDateFormat timeFormat = DisplayType.getTimeFormat_Default();
				try
				{
					if (displayType == DisplayType.Date)
					{
						date = dateFormat.parse(value);
					}
					else if (displayType == DisplayType.Time)
					{
						date = timeFormat.parse(value);
					}
					else
					{
						date = dateTimeFormat.parse(value);
					}
				}
				catch (java.text.ParseException e)
				{
					date = DisplayType.getDateFormat_JDBC().parse(value);
				}
				return new Timestamp(date.getTime());
			}

			// Boolean
			if (displayType == DisplayType.YesNo)
				return Boolean.valueOf("Y".equals(value));

			// Multi-Select
			if (DisplayType.isMultiSelect(displayType))
				return Util.getArrayObjectFromString(displayType, value);

			// Default
			return value;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, columnName + " - " + e.getMessage());
		}
		return null;
	} // getDataTypeWiseValue

}
