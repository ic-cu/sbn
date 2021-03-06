package indice;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.Properties;
import util.log.Log;
import util.sql.DB;

public class LuoghiTitoliArea4
{
	private DB db;
	private String qLuoghi;
	private String dataDa;
	private PreparedStatement isbdStmt;
	private Properties conf;
	private PrintWriter out;

// Prime impostazioni: accesso db, query e simili

	public LuoghiTitoliArea4()
	{
		Log.init("luoghi.prop");
		conf = new Properties();
		try
		{
			conf.load(new FileReader("luoghi.prop"));
			String driver = conf.getProperty("db.driver");
			String url = conf.getProperty("db.url");
			String user = conf.getProperty("db.user");
			String pass = conf.getProperty("db.pass");
			dataDa = conf.getProperty("param.datada");
			db = new DB(driver, url, user, pass);
			qLuoghi = conf.getProperty("query.luoghi");
			Log.info("Query luoghi: " + qLuoghi);
			db.prepare(conf.getProperty("query.luoghi"));
			isbdStmt = db.prepare(conf.getProperty("query.isbd"));
			out = new PrintWriter(conf.getProperty("output.file"));
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		LuoghiTitoliArea4 lt = new LuoghiTitoliArea4();
		try
		{
			ResultSet luoghi = lt.db.select(lt.qLuoghi);
			while(luoghi.next())
			{
				String lid = luoghi.getString("LID");
				String luogo = luoghi.getString("DS_LUOGO");
				lt.isbdStmt.setDate(1, java.sql.Date.valueOf(lt.dataDa));
				lt.isbdStmt.setString(2, lid);

				ResultSet isbdRS;
				isbdRS = lt.isbdStmt.executeQuery();
				if(isbdRS.isBeforeFirst())
				{
					Log.info("LID: " + lid + ", Luogo: " + luogo);
					lt.out.println(lid + ", Luogo: " + luogo);
				}
				boolean found = false;
				while(isbdRS.next())
				{
					found = true;
					String bid = isbdRS.getString("BID");
					String isbd = isbdRS.getString("ISBD");
					String indice = isbdRS.getString("INDICE_ISBD");
					String dataVar = isbdRS.getString("TS_VAR").substring(0, 10);
					try
					{
						Log.debug("BID: " + bid + ", ISBD: " + isbd + ", Indice ISBD " + indice);
						String area4 = "";
						int i1, i2, i210 = 0, f210 = 0;
						i1 = indice.indexOf("210-");
						if(i1 > 0)
						{
							i1 += 4;
							i2 = indice.indexOf(";", i1);
							i210 = Integer.valueOf(indice.substring(i1, i2));
							i1 = indice.indexOf("-", i2) + 1;
							if(i1 > 0)
							{
								i2 = indice.indexOf(";", i1);
								f210 = Integer.valueOf(indice.substring(i1, i2));
								area4 = isbd.substring(i210 - 1, f210 - 5);
							}
							else
							{
								area4 = isbd.substring(i210 - 1);
							}
						}
						if(area4.startsWith(luogo + " : ") || area4.startsWith("In " + luogo + " : ") || area4.startsWith("(" + luogo + " : ") || area4.startsWith("(In " + luogo + " : "))
						{
							Log.debug("Scartato, area 4: [" + area4 + "], luogo; " + luogo);
						}
						else
						{
							Log.debug("Indice 210: " + i210 + ", " + f210 + ", indice: " + indice);
							Log.debug("Area 4: [" + area4 + "] isbd: " + isbd + ", indice: " + indice);
							lt.out.println(bid + " (" + dataVar + "), area4: " + area4);
						}
					}
					catch(StringIndexOutOfBoundsException e)
					{
						Log.warn(bid + ", guida area ISBD errata");
					}
				}
				if(found)
				{
					lt.out.println();
					lt.out.println();
				}
				lt.out.flush();
			}
			lt.out.close();
		}
		catch(SQLSyntaxErrorException e)
		{
			e.printStackTrace();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		catch(StringIndexOutOfBoundsException e)
		{
			e.printStackTrace();
		}
	}
}