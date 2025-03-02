package xlsreport.report.export;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import system.proxies.FileDocument;
import xlsreport.proxies.*;
import xlsreport.proxies.constants.Constants;
import xlsreport.report.Aggregator;
import xlsreport.report.Styling;
import xlsreport.report.data.ColumnPreset;

import java.io.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


public class ExportExcel extends Export
{	
	private Styling styling;
	private Workbook book;	
	private boolean customExcel = false;
	private InputStream stream;

	public ExportExcel(IContext context, MxTemplate template, IMendixObject inputObject) throws CoreException, IOException
	{
		super(context, inputObject);
		// Create the Excel book to export to (Add later the XLX export)
		CustomExcel excel = template.getMxTemplate_CustomExcel();
		if(excel != null && excel.getHasContents())
		{
			this.customExcel = true;
			stream = Core.getFileDocumentContent(context, excel.getMendixObject());
			switch(template.getDocumentType())
			{
				case XLS:
					this.book = new HSSFWorkbook(stream);	
					break;
                case XLSM:
				case XLSX:
                    this.book = new XSSFWorkbook(stream);
                    break;
				default:
					throw new CoreException("Could not create Excel workbook, because the documenttype on the template wasn't set.");
			}
		} else
		{
			switch(template.getDocumentType())
			{
				case XLS:
					this.book = new HSSFWorkbook();	
					break;
                case XLSM:
				case XLSX:
                    if(Constants.getUseStreamingAPI()){
                        this.book = new SXSSFWorkbook(-1);
                    }else{
                        this.book = new XSSFWorkbook();
                    }

					break;
				default:
					throw new CoreException("Could not create Excel workbook, because the documenttype on the template wasn't set.");
			}
		}
		// Initialize all the styling items for the excel 	
		this.styling = new Styling(template);
        this.styling.setAllStyles(context, template, this.book);        
	}

	@Override
	public void buildExportFile(MxSheet mxSheet, List<ColumnPreset> ColumnPresetList, Object[][] table) throws CoreException
	{
		// Set first the needed column styling options.
		for(ColumnPreset column : ColumnPresetList)
        {
        	if(column.getStyleGuid() > 0)
        	{
        		column.setStyle(this.styling.getStyle(column.getStyleGuid(), column.isDateTimeFormat()));
        	} else
        	{
        		column.setStyle(this.styling.getDefaultStyle(column.isDateTimeFormat()));
        	}
        }
		// Create new sheet object in the excel doc.
		Sheet sheet;
		if(this.customExcel && this.book.getSheetAt(mxSheet.getSequence() - 1) != null)
		{
			sheet = this.book.getSheetAt(mxSheet.getSequence() - 1);
        } else {
            if (this.book instanceof HSSFWorkbook) {
                sheet = (HSSFSheet) this.book.createSheet(mxSheet.getName());
            } else {
                sheet = (SXSSFSheet) this.book.createSheet(mxSheet.getName());
            }
        }
        // Write the OQL result data to the excel
        int startrow = mxSheet.getStartRow();
        // Fill the excel with the data
		if(table != null)
		{
			if(!this.customExcel)
			{
				// Place the headers above the data in the excel only when there isn't a custom excel file
				CellStyle headerStyle = this.styling.getStyle(mxSheet.getMxSheet_HeaderStyle().getMendixObject().getId().toLong(), false);
				Row headerRow = getRow(sheet, startrow - 1, mxSheet);
				for(ColumnPreset columnPreset : ColumnPresetList)
		        {
					Cell cell = getCell(headerRow, columnPreset.getColumnNr(), headerStyle);
					cell.setCellValue(columnPreset.getName());
		        }
			}
			for(int i = 0; i < table.length; i++)
			{
				Row row = getRow(sheet, startrow, mxSheet);
				for(int e = 0; e < table[i].length; e++)
				{
					// Get the basis data
					Object data = table[i][e];
					ColumnPreset columnPreset = ColumnPresetList.get(e);
					if(columnPreset.isResultAggregation())
					{
						double value = ((Number)data).doubleValue();
                        columnPreset.addResultAggrValue(value);
					}
					// Find the excel position of the cell and set the data;				
					Cell cell = getCell(row, columnPreset.getColumnNr());
	                SetCellValue(data, cell, columnPreset.isShouldLocalizeDate());
	                cell.setCellStyle(columnPreset.getStyle());
				}
				startrow++;
			}
			Row endRow = getRow(sheet, startrow, mxSheet);
			for(ColumnPreset columnPreset : ColumnPresetList)
			{
				if(columnPreset.isResultAggregation())
				{
					Cell cell = getCell(endRow, columnPreset.getColumnNr(), columnPreset.getStyle());
					SetCellValue(columnPreset.getResultAggregate(), cell, columnPreset.isShouldLocalizeDate());
				}
			}
			// Write static data
			if (mxSheet.getDataUsage())
	        {
                List<IMendixObject> staticList = Core.createXPathQuery("//" + MxStatic.getType() + "[" + MxData.MemberNames.MxData_MxSheet.toString() + "='" + mxSheet.getMendixObject().getId().toLong() + "']")
                        .setVariable(String.valueOf(MxData.MemberNames.MxData_MxSheet), mxSheet.getMendixObject().getId().toLong())
                        .execute(context);
	            processStaticData(staticList, sheet, null);
	        }
			// Set the sheet preferences
			processSheetPreferences(mxSheet, sheet);
		}
	}
	
	@Override
	public void writeData(FileDocument outputDocument) throws Exception
	{
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            this.book.write(out);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray())) {
                Core.storeFileDocumentContent(context, outputDocument.getMendixObject(), inputStream);
            }
        }
	}
	
	private void processStaticData(List<IMendixObject> StaticList, Sheet sheet, Aggregator aggr) throws CoreException
    {
        //DataXPath dataIni = new DataXPath(context);
        log.debug("-- Process the static data.");
        for (IMendixObject StaticData : StaticList)
        {
            MxStatic mxStatic = MxStatic.initialize(context, StaticData);

            // Get the position of the mx static data.
            Row row = getRow(sheet, mxStatic.getRowPlace(), null);
            Cell cell = getCell(row, mxStatic.getColumnPlace(),mxStatic);

            switch (mxStatic.getStaticType())
            {
                case StaticText:
                    SetCellValue(mxStatic.getName(), cell, false);
                    break;                
                case ObjectMember:
                    if(this.inputObject != null && mxStatic.getMxStatic_MxObjectMember() != null)
                    {
                    	SetCellValue(this.inputObject.getValue(context, mxStatic.getMxStatic_MxObjectMember().getAttributeName()), cell, false);
                    }
                    break;
                case Aggregate:
                    if(aggr == null)
                        throw new CoreException("Aggregator cannot be empty");
                    MxColumn column = mxStatic.getMxStatic_MxColumn();
                    if (column != null)
                    {
                        aggr.addColumn(column.getColumnNumber(), column, mxStatic);
                    }
                    break;
            }
        }
    }	
	
	private void processSheetPreferences(MxSheet mxSheet, Sheet sheet) throws CoreException {
        if(Constants.getUseStreamingAPI() && sheet instanceof SXSSFSheet){
             ( (SXSSFSheet) sheet ).trackAllColumnsForAutoSizing();
        }

        log.debug("-- Process the column preferences.");
        List<IMendixObject> preferences = Core.createXPathQuery("//" + MxColumnSettings.getType() + "[" + MxColumnSettings.MemberNames.ColumnSettings_MxSheet.toString() + "='" + mxSheet.getMendixObject().getId().toLong() + "']")
                .execute(context);
        for (IMendixObject columnPref : preferences)
        {
            MxColumnSettings ColumnSetting = MxColumnSettings.initialize(context, columnPref);
            if (ColumnSetting.getAutoSize())
            {
                sheet.autoSizeColumn(ColumnSetting.getColumnIndex());
            }
            else
            {
                int width = ColumnSetting.getColumnWidth() * 34;
                if (width > 65280)
                {
                    width = 65280;
                }
                sheet.setColumnWidth(ColumnSetting.getColumnIndex(), width);
            }
        }

        List<IMendixObject> rowPreferences = Core.createXPathQuery("//" + MxRowSettings.getType() + "[" + MxRowSettings.MemberNames.MxRowSettings_MxSheet.toString() + "='" + mxSheet.getMendixObject().getId().toLong() + "']")
                .execute(context);
        for (IMendixObject RowPref : rowPreferences)
        {
            MxRowSettings RowSetting = MxRowSettings.initialize(context, RowPref);
            Row row = getRow(sheet, RowSetting.getRowIndex(), null);
            if (!RowSetting.getDefaultHeight())
            {
            	row.setHeightInPoints(RowSetting.getRowHeight());
            }
        }

        if(Constants.getUseStreamingAPI() && sheet instanceof SXSSFSheet){
            try {
                ( (SXSSFSheet) sheet ).flushRows();
            } catch (IOException e) {
                throw new CoreException("An error occurred while manually flushing rows.");
            }
        }
    }

    private Row getRow(Sheet sheet, int index, MxSheet mxSheet)
    {
        Row row = sheet.getRow(index);
        if(row == null)
        {
            row = sheet.createRow(index);
        }
        if(mxSheet != null)
        {
            if (!mxSheet.getRowHeightDefault())
            {
            	row.setHeightInPoints(mxSheet.getRowHeightPoint());
            }
        }
        return row;
    }

    private Cell getCell(Row row, int index, MxData data) throws CoreException
    {
        Cell cell = row.getCell(index);
        if (cell == null)
        {
            cell = row.createCell(index);
        }

        if (data.getMxData_MxCellStyle() != null)
        {
            setStyleToCell(cell, data.getMxData_MxCellStyle().getMendixObject().getId().toLong());
        }
        else
        {
            setStyleToCell(cell, null);
        }

        return cell;
    }
    
    private Cell getCell(Row row, int index) throws CoreException
    {
        Cell cell = row.getCell(index);
        if (cell == null)
        {
            cell = row.createCell(index);
        }

        return cell;
    }
    
    private Cell getCell(Row row, int index, CellStyle style) throws CoreException
    {
        Cell cell = row.getCell(index);
        if (cell == null)
        {
            cell = row.createCell(index);
        }
        cell.setCellStyle(style);

        return cell;
    }

    private void SetCellValue(Object data, Cell cell, boolean shouldLocalizeDate)
    {
        if (data == null || cell == null)
        {
            return;
        }
        else if (data instanceof Integer)
        {       
        	cell.setCellType(CellType.NUMERIC);        	
            cell.setCellValue(((Integer) data).doubleValue());
        }
        else if (data instanceof Boolean)
        {
        	cell.setCellType(CellType.BOOLEAN);
            cell.setCellValue((Boolean) data);
        }
        else if (data instanceof Double)
        {
        	cell.setCellType(CellType.NUMERIC);
            cell.setCellValue((Double) data);
        }
        else if (data instanceof Float)
        {
        	cell.setCellType(CellType.NUMERIC);
            cell.setCellValue((Float) data);
        }
        else if (data instanceof Date)
        {
            DateTime dateTime = new DateTime(data);
            if(shouldLocalizeDate)
            {
                cell.setCellValue(dateTime.withZone(DateTimeZone.forTimeZone(context.getSession().getTimeZone())).toLocalDateTime().toDate());
            }
            else
            {
                cell.setCellValue(dateTime.withZone(DateTimeZone.UTC).toLocalDateTime().toDate());
            }
        }
        else if (data instanceof Long)
        {
        	cell.setCellType(CellType.NUMERIC);
            cell.setCellValue((Long) data);            
        } else if (data instanceof String)
        {
        	cell.setCellType(CellType.STRING);
            cell.setCellValue((String) data);
        } else if (data instanceof BigDecimal)
        {
        	cell.setCellType(CellType.NUMERIC);
        	BigDecimal value = (BigDecimal) data;
            cell.setCellValue(value.doubleValue());
        }
    }

    private void setStyleToCell(Cell cell, Long GUID)
    {
        CellStyle style;
        if (GUID != null)
        {
            style = this.styling.getStyle(GUID, false);
        }
        else
        {
            style = this.styling.getDefaultStyle();
        }
        if (style != null)
        {
            cell.setCellStyle(style);
        }
    }

    public void close() throws Exception
    {
        if(this.book instanceof SXSSFWorkbook){
            ((SXSSFWorkbook) this.book).dispose();
        }

        if (this.book instanceof Closeable) {
            Closeable closeableBook = (Closeable)this.book;
            closeableBook.close();
        }

        if (this.stream != null)
            this.stream.close();
    }
}
