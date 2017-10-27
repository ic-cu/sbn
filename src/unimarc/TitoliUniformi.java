package unimarc;
/**
 * Copyright (C) 2002-2006 Bas Peters
 * 
 * This file is part of MARC4J
 * 
 * MARC4J is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * MARC4J is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with MARC4J; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import org.apache.commons.lang3.time.StopWatch;

public class TitoliUniformi
{
	public TreeSet<String> extract(File file)
	{
		InputStream input = null;
		TreeSet<String> set = new  TreeSet<String>();
		try
		{
			input = new GZIPInputStream(new FileInputStream(file));
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		MarcReader reader = new MarcStreamReader(input);
		while(reader.hasNext())
		{
			Record record = reader.next();
			List<DataField> dFields = record.getDataFields();

// Scandisce la lista dei data field

			Iterator<DataField> dfIter = dFields.iterator();
			while(dfIter.hasNext())
			{
				DataField field = (DataField) dfIter.next();
				String tag = field.getTag();
				String data = null;
				String a = null;
				String bid = null;
				boolean badA = false;
				boolean hasE = false;
				boolean hasF = false;
				switch(tag)
				{
					case "500":
						if(field.getSubfield('a') != null)
						{
							bid = field.getSubfield('3').getData();
							a = field.getSubfield('a').getData();
							badA = a.endsWith(". -");
							data = "$a" + a;
							if(field.getSubfield('e') != null)
							{
								hasE = true;
								data += "$e" + field.getSubfield('e').getData();
							}
							if(field.getSubfield('f') != null)
							{
								hasF = true;
								data += "$f" + field.getSubfield('f').getData();
							}
							if(badA || hasE || hasF)
							{
								data = data.replace("\u00c2\u0089", "");
								data = data.replace("\u00c2\u0088", "");
								set.add(bid + ": [" + data + "]");
							}
						}
					default:
						break;
				}
			}
		}
		try
		{
			input.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return set;
	}

	public File[] fileArray(File file)
	{
		File[] files = null;
		if(file.isFile())
		{
			files = new File[] { file };
		}
		else
		{
			files = file.listFiles(new FilenameFilter()
			{

				@Override
				public boolean accept(File dir, String name)
				{
					if(name.endsWith("mrc.gz"))
						return true;
					else
						return false;
				}
			});
		}
		return files;
	}

	public static void main(String args[]) throws Exception
	{
		TitoliUniformi tu = new TitoliUniformi();
		File[] files = tu.fileArray(new File(args[0]));
		TreeSet<String> tree = new TreeSet<String>();
		StopWatch sw = new StopWatch();
		sw.start();
		for(File file : files)
		{
			System.err.println(file.getName());
			tree.addAll(tu.extract(file));
			sw.split();
			System.err.println(sw.toSplitString());
			sw.unsplit();
		}
		
		FileOutputStream fos = new FileOutputStream("tmp/titoli-poco-uniformi.txt");
		OutputStreamWriter osw = new OutputStreamWriter(fos, "ISO-8859-1");
		PrintWriter pw = new PrintWriter(osw);
		for(String val : tree)
		{
			pw.println(val);
//			System.out.println(val);
		}
		pw.flush();
		pw.close();
		sw.stop();
		System.err.println(sw.toString());
	}
}
