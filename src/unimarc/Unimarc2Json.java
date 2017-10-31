package unimarc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Esporta in un formato JSON dei record UNIMARC. Questi devono essere ISO2709,
 * con estensione ".mrc.gz", tutti in una stessa directory
 */
public class Unimarc2Json
{
	private final static Logger log = Logger.getLogger("LOG");
	private final static Logger console = Logger.getLogger("CONSOLE");

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

	private static String clean(String data)
	{
		data = data.replace("\u00c2\u0089", "");
		data = data.replace("\u00c2\u0088", "");
		return data;
	}

	public static void main(String args[]) throws Exception
	{
		Unimarc2Json uj = new Unimarc2Json();
		File[] files = uj.fileArray(new File(args[0]));
		Arrays.sort(files);
		InputStream input = null;
		Writer output = null;
		String ext = null;
		String noext = null;
		SimpleLayout sl = new SimpleLayout();
		ConsoleAppender ca = new ConsoleAppender(sl, "System.out");
		console.setLevel(Level.DEBUG);
		console.addAppender(ca);
		WriterAppender wa = null;
		StopWatch sw = new StopWatch();
		log.setLevel(Level.INFO);
		sw.start();
		for(File file : files)
		{
			ext = FilenameUtils.getExtension(file.getPath());
			noext = FilenameUtils.removeExtension(file.getPath());
			console.info("Elaborazione file " + file.getName());
			sw.split();
			console.info(sw.toSplitString());
			sw.unsplit();
			if(ext.endsWith("gz"))
			{
				ext = FilenameUtils.getExtension(noext);
				noext = FilenameUtils.removeExtension(noext);
				wa = new WriterAppender(sl, new PrintWriter(noext + ".log"));
				log.addAppender(wa);
				log.info("Elaborazione file " + file.getName());
				input = new GZIPInputStream(new FileInputStream(file));
				output = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(noext + ".json.gz")), "ISO-8859-1");
			}
			else
			{
				input = new FileInputStream(args[0]);
				output = new OutputStreamWriter(new FileOutputStream(noext + ".json"), "ISO-8859-1");
			}

			MarcReader reader = new MarcStreamReader(input);
			Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
//			Gson gson = new GsonBuilder().disableHtmlEscaping().create();
			output.write("[\n");
			while(reader.hasNext())
			{
				Record record = reader.next();
				String bid = null;
				JsonObject gRecord = new JsonObject();

				char status = record.getLeader().toString().charAt(5);
				log.debug("stato: " + status);

				char level = record.getLeader().toString().charAt(7);
				log.debug("livello: " + level);
				switch(level)
				{
					case 'm':
						gRecord.addProperty("livello", "monografia");
						break;
					case 'a':
						gRecord.addProperty("livello", "spoglio");
						break;
					case 's':
						gRecord.addProperty("livello", "periodico");
						break;
					default:
						break;
				}

				char tipo = record.getLeader().toString().charAt(6);
				switch(tipo)
				{
					case 'a':
						gRecord.addProperty("tipo", "Testo a stampa");
						break;
					case 'b':
						gRecord.addProperty("tipo", "Testo manoscritto");
						break;
					case 'c':
						gRecord.addProperty("tipo", "Musica a stampa");
						break;
					case 'd':
						gRecord.addProperty("tipo", "Musica manoscritta");
						break;
					case 'e':
						gRecord.addProperty("tipo", "Cartografia a stampa");
						break;
					case 'f':
						gRecord.addProperty("tipo", "Cartografia manoscritta");
						break;
					case 'g':
						gRecord.addProperty("tipo", "Materiale video");
						break;
					case 'k':
						gRecord.addProperty("tipo", "Materiale grafico");
						break;
					case 'i':
						gRecord.addProperty("tipo", "Registrazione sonora non musicale");
						break;
					case 'j':
						gRecord.addProperty("tipo", "Registrazione sonora musicale");
						break;
					case 'l':
						gRecord.addProperty("tipo", "Risorsa elettronica");
						break;
					case 'm':
						gRecord.addProperty("tipo", "Materiale multimediale");
						break;
					case 'r':
						gRecord.addProperty("tipo", "Oggetto multimediale");
						break;
					default:
						break;
				}

				List<DataField> dFields = record.getDataFields();
				List<ControlField> cFields = record.getControlFields();

				// Scandisce la lista dei data field

				Iterator<DataField> dfIter = dFields.iterator();
				Iterator<ControlField> cfIter = cFields.iterator();
				while(cfIter.hasNext())
				{
					ControlField field = (ControlField) cfIter.next();
					String tag = field.getTag();
					switch(tag)
					{
						case "001":
							bid = field.getData();
							log.info("BID: " + bid);
							gRecord.addProperty("codiceIdentificativo", bid);
							break;
						case "003":
							String permalink = field.getData();
							log.info("Permalink: " + permalink);
							gRecord.addProperty("permalink", permalink);
							break;
						default:
							break;
					}
				}
				JsonArray gLocs = new JsonArray();
				JsonArray gNomi = new JsonArray();
				JsonArray gCDDs = new JsonArray();
				JsonArray gNumeri = new JsonArray();
				JsonArray gSoggetti = new JsonArray();
				JsonArray gVarianti = new JsonArray();
				JsonObject gNumero = null;
				JsonObject gCDD = null;
				while(dfIter.hasNext())
				{
					DataField field = (DataField) dfIter.next();
					String tag = field.getTag();
					String data = null;
					switch(tag)
					{
						case "010":
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
							}
							else
							{
								data = field.getSubfield('z').getData() + " (" + field.getSubfield('b').getData() + ")";
							}
							gNumero = new JsonObject();
							gNumero.addProperty("ISBN", data);
							gNumeri.add(gNumero);
							break;
						case "011":
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
							}
							else
							{
								if(field.getSubfield('z') != null)
								{
									data = field.getSubfield('z').getData();
									data += " (" + field.getSubfield('b').getData() + ")";
								}
								else
								{
									log.warn("011$z mancante");
								}
							}
							gNumero = new JsonObject();
							gNumero.addProperty("ISSN", data);
							gNumeri.add(gNumero);
							break;
						case "100":
							data = field.getSubfield('a').getData();
							log.debug("charset: " + data.substring(26, 29));
							break;
						case "101":
							data = field.getSubfield('a').getData();
							gRecord.addProperty("linguaPubblicazione", data);
							break;
						case "102":
							data = field.getSubfield('a').getData();
							gRecord.addProperty("paesePubblicazione", data);
							break;
						case "200":
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
								data = clean(data);
							}
							else
							{
								log.warn(bid + ": privo di titolo");
							}
							if(field.getSubfield('e') != null)
							{
								data += " : " + field.getSubfield('e').getData();
								data = clean(data);
							}
							if(field.getSubfield('f') != null)
							{
								data += " / " + field.getSubfield('f').getData();
							}
							gRecord.addProperty("titolo", data);
							log.debug("titolo: " + data);
							break;
						case "210":
							data = "";
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
								data = clean(data);
							}
							if(field.getSubfield('c') != null)
							{
								data += " : " + field.getSubfield('c').getData();
							}
							if(field.getSubfield('d') != null)
							{
								data += ", " + field.getSubfield('d').getData();
							}
							gRecord.addProperty("pubblicazione", data);
							break;
						case "215":
							data = "";
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
							}
							if(field.getSubfield('d') != null)
							{
								data += " ; " + field.getSubfield('d').getData();
							}
							gRecord.addProperty("descrizioneFisica", data);
							break;
						case "410":
							data = "";
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
							}
							if(field.getSubfield('v') != null)
							{
								data += " ; " + field.getSubfield('v').getData();
							}
							data = clean(data);
							gRecord.addProperty("collezione", data);
							break;
						case "461":
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
								data = clean(data);
							}
							else
							{
								log.warn(bid + ": privo di titolo");
							}
							if(field.getSubfield('e') != null)
							{
								data += " : " + field.getSubfield('e').getData();
								data = clean(data);
							}
							if(field.getSubfield('f') != null)
							{
								data += " / " + field.getSubfield('f').getData();
							}
							gRecord.addProperty("contenutoIn", data);
							break;
						case "517":
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
								data = clean(data);
							}
							else
							{
								log.warn(bid + ": privo di titolo");
							}
							gVarianti.add(new JsonPrimitive(data));
							break;
						case "606":
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
							}
							if(field.getSubfields('x') != null)
							{
								Iterator<Subfield> iter = field.getSubfields('x').iterator();
								if(iter.hasNext())
								{
									data += " - " + ((Subfield) iter.next()).getData();
									while(iter.hasNext())
									{
										data += " - " + ((Subfield) iter.next()).getData();
									}
								}
							}
							gSoggetti.add(new JsonPrimitive(data));
							break;
						case "676":
							if(field.getSubfield('a') != null)
							{
								data = field.getSubfield('a').getData();
								gCDD = new JsonObject();
								gCDD.addProperty("cdd", data);
							}
							data = field.getSubfield('c').getData();
							gCDD.addProperty("dec", data);
							data = field.getSubfield('v').getData();
							gCDD.addProperty("ed", data);
							gCDDs.add(gCDD);
							break;
						case "700":
							data = field.getSubfield('a').getData();
							if(field.getSubfield('b') != null)
							{
								data += field.getSubfield('b').getData();
							}
							gNomi.add(new JsonPrimitive(data));
							gRecord.addProperty("autorePrincipale", data);
							break;
						case "701":
							data = field.getSubfield('a').getData();
							if(field.getSubfield('b') != null)
							{
								data += field.getSubfield('b').getData();
							}
							gNomi.add(new JsonPrimitive(data));
							break;
						case "711":
							data = field.getSubfield('a').getData();
							if(field.getSubfield('b') != null)
							{
								data += field.getSubfield('b').getData();
							}
							gNomi.add(new JsonPrimitive(data));
							break;
						case "710":
							data = field.getSubfield('a').getData();
							if(field.getSubfield('b') != null)
							{
								data += field.getSubfield('b').getData();
							}
							gRecord.addProperty("autorePrincipale", data);
							break;
						case "702":
							data = field.getSubfield('a').getData();
							if(field.getSubfield('b') != null)
							{
								data += field.getSubfield('b').getData();
							}
							gNomi.add(new JsonPrimitive(data));
							break;
						case "712":
							data = field.getSubfield('a').getData();
							if(field.getSubfield('b') != null)
							{
								data += field.getSubfield('b').getData();                                                                                                                                                                                                                                                                                                                                                                                                                            
							}
							gNomi.add(new JsonPrimitive(data));
							break;
						case "899":
							Subfield s1 = field.getSubfield('1');
							if(s1 != null)
							{
								data = s1.getData();
//								JsonObject gLoc = new JsonObject();								
//								gLoc.addProperty("isil", "IT-" + data);
//								gLocs.add(gLoc);
								gLocs.add(new JsonPrimitive("IT-" + data));
							}
							break;
						default:
							break;
					}
				}
				if(!bid.contains("ZZZ"))
				{
					gRecord.add("nomi", gNomi);
					gRecord.add("numeri", gNumeri);
					gRecord.add("dewey", gCDDs);
					gRecord.add("soggetti", gSoggetti);
					gRecord.add("altriTitoli", gVarianti);
					gRecord.add("localizzazioni", gLocs);
					String prettyGsonString = gson.toJson(gRecord);
					output.write(prettyGsonString);
					if(reader.hasNext())
					{
						output.write(",\n");
					}
					output.flush();
				}
				else
				{
					log.warn("record tipo ZZZ, scartato");
				}
			}
			output.write("\n]");
			output.flush();
			output.close();
			wa.close();
			log.removeAllAppenders();
			input.close();
		}
	}
}
