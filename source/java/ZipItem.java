package org.jp.zip;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class ZipItem implements Comparable<ZipItem> {

	public String name;
	public String path;
	public String parent ;
	public ZipEntry zipEntry;
	public boolean isLeaf;
	
	public ZipItem(ZipEntry zipEntry) {
		super();
		this.zipEntry = zipEntry;
		path = zipEntry.getName().replace('\\','/');
		isLeaf = !path.endsWith("/");
		String s = path;
		if (!isLeaf) s = path.substring(0, s.length()-1);
		int k = s.lastIndexOf("/") + 1;
		parent = s.substring(0, k);
		name = s.substring(k);
	}
	
	public ZipItem(String pathString) {
		super();
		zipEntry = null;
		path = pathString.replace('\\','/');
		isLeaf = !path.endsWith("/");
		String s = path;
		if (!isLeaf) s = path.substring(0, s.length()-1);
		int k = s.lastIndexOf("/") + 1;
		parent = s.substring(0, k);
		name = s.substring(k);
	}
	
	public ZipItem(String title, String pathString) {
		super();
		zipEntry = null;
		path = pathString.replace('\\','/');
		isLeaf = false;
		parent = path;
		name = title;
	}
	
	public String toString() {
		return name;
	}
	
	public int compareTo(ZipItem item) {
		return path.toLowerCase().compareTo(item.path.toLowerCase());
	}
	
	public String toFullString() {
		StringBuffer sb = new StringBuffer("ZipItem\n");
		sb.append("  name:   \"" + name + "\"\n");
		sb.append("  path:   \"" + path + "\"\n");
		sb.append("  parent: \"" + parent + "\"\n");
		sb.append("  isLeaf: \"" + isLeaf + "\"\n");
		sb.append("\n");
		return sb.toString();
	}
}
		
