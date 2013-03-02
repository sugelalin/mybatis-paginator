package com.github.miemiedev.mybatis.paginator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 排序的列
 * @author badqiu
 * @author miemiedev
 */
public class SortInfo implements Serializable{

	private static final long serialVersionUID = 6959974032209696722L;

	private String columnName;
	private String sortStatus;
	
	public SortInfo() {
	}
	
	public SortInfo(String columnName, String sortStatus) {
		super();
		this.columnName = columnName;
		this.sortStatus = sortStatus;
	}

	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

    public String getSortStatus() {
        return sortStatus;
    }

    public void setSortStatus(String sortStatus) {
        this.sortStatus = sortStatus;
    }

    public static List<SortInfo> parseSortColumns(String sortColumns) {
		if(sortColumns == null) {
			return new ArrayList(0);
		}
		
		List<SortInfo> results = new ArrayList();
		String[] sortSegments = sortColumns.trim().split(",");
		for(int i = 0; i < sortSegments.length; i++) {
			String sortSegment = sortSegments[i];
			results.add(parseSortColumn(sortSegment));
		}
		return results;
	}

    public static SortInfo parseSortColumn(String sortSegment) {
        String[] array = sortSegment.split("\\.");
        SortInfo sortInfo = new SortInfo();
        sortInfo.setColumnName(array[0]);
        sortInfo.setSortStatus(array.length == 2 ? array[1] : null);
        return sortInfo;
    }
	
	public String toString() {
		return columnName + (sortStatus == null ? "" : " " + sortStatus);
	}
}