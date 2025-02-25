package net.osmand;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.osmand.obf.*;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.binary.MapZooms;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.obf.diff.AugmentedDiffsInspector;
import net.osmand.obf.diff.GenerateDailyObf;
import net.osmand.obf.diff.ObfDiffGenerator;
import net.osmand.obf.diff.ObfDiffMerger;
import net.osmand.obf.diff.ObfRegionSplitter;
import net.osmand.obf.preparation.BasemapProcessor;
import net.osmand.obf.preparation.DBDialect;
import net.osmand.obf.preparation.IndexCreator;
import net.osmand.obf.preparation.IndexCreatorSettings;
import net.osmand.obf.preparation.OceanTilesCreator;
import net.osmand.osm.FilterOsmByTags;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapRenderingTypesEncoder;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStoragePrinter;
import net.osmand.travel.TravelGuideCreatorMain;
import net.osmand.travel.WikivoyageDataGenerator;
import net.osmand.travel.WikivoyageGenOSM;
import net.osmand.travel.WikivoyageLangPreparation;
import net.osmand.util.Algorithms;
import net.osmand.util.CombineSRTMIntoFile;
import net.osmand.util.CountryOcbfGeneration;
import net.osmand.util.FixBasemapRoads;
import net.osmand.util.GenerateExtractScript;
import net.osmand.util.IndexBatchCreator;
import net.osmand.util.IndexUploader;
import net.osmand.util.ResourceDeleter;
import net.osmand.util.ConvertLargeRasterSqliteIntoRegions;
import net.osmand.wiki.WikiDatabasePreparation;
import net.osmand.wiki.WikipediaByCountryDivider;

public class MainUtilities {
	private static Log log = PlatformUtil.getLog(MainUtilities.class);


	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			printSynopsys();
		} else if (args[0].equals("--test-osm-live-tag-removal")) {
			generateAllOsmLiveTests(new File(System.getProperty("repo.dir")+"/resources/test-resources/osm_live"),
					System.getProperty("maps.dir"), false);
//			String test = "2017_06_18-10_30_tagRemovalBug_01.xml";
//			String osmLivePath = System.getProperty("repo.dir")+"/resources/test-resources/osm_live/";
//			Algorithms.removeAllFiles(new File(osmLivePath, AugmentedDiffsInspector.DEFAULT_REGION));
//			AugmentedDiffsInspector.main(new String[] { osmLivePath + test, osmLivePath });
//			GenerateDailyObf.main(new String[] { osmLivePath });
		} else {
			String utl = args[0];
			List<String> subArgs = new ArrayList<String>(Arrays.asList(args).subList(1, args.length));
			String[] subArgsArray = subArgs.toArray(new String[subArgs.size()]);
			if (utl.equals("check-ocean-tile")) {
				OceanTilesCreator.checkOceanTile(subArgsArray);
			} else if (utl.equals("inspector")) {
				BinaryInspector.main(subArgsArray);
			} else if (utl.equals("compare")) {
				BinaryComparator.main(subArgsArray);
			} else if (utl.equals("merge-index")) {
				BinaryMerger.main(subArgsArray);
			} else if (utl.equals("generate-region-tags")) {
				GenerateRegionTags.main(subArgsArray);
			} else if (utl.equals("generate-obf-files-in-batch")) {
				IndexBatchCreator.main(subArgsArray);
			} else if (utl.equals("generate-ocean-tile-osm")) {
				OceanTilesCreator.createJOSMFile(subArgsArray);
			} else if (utl.equals("generate-java-style")) {
				RenderingRulesStoragePrinter.main(subArgsArray);
			} else if (utl.equals("explain-rendering-style")) {
				RenderingRulesStorage.main(subArgsArray);
			} else if (utl.equals("generate-wiki-world-sqlite")) {
				WikiDatabasePreparation.main(subArgsArray);
			} else if (utl.equals("generate-wikipedia-by-country")) {
				WikipediaByCountryDivider.main(subArgsArray);
			} else if (utl.equals("generate-obf-diff")) {
				ObfDiffGenerator.main(subArgsArray);
			} else if (utl.equals("generate-basemap")) {
				BasemapProcessor.main(subArgsArray);
			} else if (utl.equals("fix-basemap-roads")) {
				FixBasemapRoads.main(subArgsArray);
			} else if (utl.equals("generate-ocean-tile")) {
				OceanTilesCreator.createTilesFile(subArgsArray[0], subArgsArray.length > 1 ? subArgsArray[1] : null);
			} else if (utl.equals("create-sqlitedb")) {
				SQLiteBigPlanetIndex.main(subArgsArray);
			} else if (utl.equals("test-routing")) {
				net.osmand.router.TestRouting.main(subArgsArray);
			} else if (utl.equals("test-icons")) {
				IconVisibility.main(subArgsArray);
			} else if (utl.equals("generate-ocbf")) {
				CountryOcbfGeneration.main(subArgsArray);
			} else if (utl.equals("generate-obf")) {
				IndexCreatorSettings settings = new IndexCreatorSettings();
				settings.indexMap = true;
				settings.indexAddress = true;
				settings.indexPOI = true;
				settings.indexTransport = true;
				settings.indexRouting = true;
				parseIndexCreatorArgs(subArgs, settings);
				generateObf(subArgs, settings);
			} else if (utl.equals("generate-obf-no-address")) {
				IndexCreatorSettings settings = new IndexCreatorSettings();
				settings.indexMap = true;
				settings.indexAddress = false;
				settings.indexPOI = true;
				settings.indexTransport = true;
				settings.indexRouting = true;
				parseIndexCreatorArgs(subArgs, settings);
				generateObf(subArgs, settings);
			} else if (utl.equals("convert-gpx-to-obf")) {
				OsmGpxWriteContext.generateObfFromGpx(subArgs);
			} else if (utl.equals("generate-map")) {
				IndexCreatorSettings settings = new IndexCreatorSettings();
				settings.indexMap = true;
				parseIndexCreatorArgs(subArgs, settings);
				generateObf(subArgs, settings);
			} else if (utl.equals("split-obf")) {
				ObfRegionSplitter.main(subArgsArray);
			} else if (utl.equals("merge-bulk-osmlive-day")) {
				ObfDiffMerger.mergeBulkOsmLiveDay(subArgsArray);
			} else if (utl.equals("merge-bulk-osmlive-month")) {
				ObfDiffMerger.mergeBulkOsmLiveMonth(subArgsArray);
			} else if (utl.equals("merge-flat-obf")) {
				ObfDiffMerger.main(subArgsArray);
			} else if (utl.equals("generate-wikivoyage-raw-lang")) {
				WikivoyageLangPreparation.main(subArgsArray);
			} else if (utl.equals("process-wikivoyage")) {
				for (String s : subArgsArray) {
					if (s.startsWith("--generate-osm=")) {
						WikivoyageGenOSM.genWikivoyageOsm(new File(subArgsArray[0]), new File(s.substring("--generate-osm=".length())), -1);
						return;
					}
				}
				WikivoyageDataGenerator.main(subArgsArray);
			} else if (utl.equals("generate-obf-extract-script")) {
				GenerateExtractScript.main(subArgsArray);
			} else if (utl.equals("generate-address")) {
				IndexCreatorSettings settings = new IndexCreatorSettings();
				settings.indexAddress = true;
				parseIndexCreatorArgs(subArgs, settings);
				generateObf(subArgs, settings);
			} else if (utl.equals("extract-roads-only")) {
				File mainFile = new File(subArgsArray[0]);
				IndexUploader.extractRoadOnlyFile(
						mainFile,
						new File(mainFile.getParentFile(), mainFile.getName().replace(IndexConstants.BINARY_MAP_INDEX_EXT,
								IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)));
			} else if (utl.equals("generate-poi")) {
				IndexCreatorSettings settings = new IndexCreatorSettings();
				settings.indexPOI = true;
				parseIndexCreatorArgs(subArgs, settings);
				generateObf(subArgs, settings);
			} else if (utl.equals("delete-unused-strings")) {
				ResourceDeleter.main(subArgsArray);
			} else if (utl.equals("index-uploader-std")) {
				// should be split into server utilites and map generation
				IndexUploader.main(subArgsArray);
			} else if (utl.equals("combine-srtm-into-file")) {
				CombineSRTMIntoFile.main(subArgsArray);
			} else if (utl.equals("collect-sqlitedb-into-regions")) {
				ConvertLargeRasterSqliteIntoRegions.main(subArgsArray);
			} else if (utl.equals("merge-std-files")) {
				BinaryMerger.mergeStandardFiles(subArgsArray);
			} else if (utl.equals("generate-roads")) {
				IndexCreatorSettings settings = new IndexCreatorSettings();
				settings.indexRouting = true;
				parseIndexCreatorArgs(subArgs, settings);
				generateObf(subArgs, settings);
			} else if (utl.equals("filter-osm-by-tag")) {
				FilterOsmByTags.main(subArgsArray);
			} else if (utl.contentEquals("generate-osmlive-tests")) {
				if (subArgsArray.length < 1) {
					System.out.println("Usage: <path_to_directory_with_resources_project> <optional_path_to_unpack_files>");
					return;
				}
				File testResources = new File(subArgsArray[0]+"/resources/test-resources/osm_live/");
				generateAllOsmLiveTests(testResources, subArgsArray.length > 1 ? subArgsArray[1] : null, false);
			} else if (utl.contentEquals("generate-from-overpass")) {
				if (subArgsArray.length < 2) {
					System.out.println("Usage: PATH_TO_OVERPASS PATH_TO_WORKING_DIR");
					return;
				}
				String[] argsToGenerateOsm = new String[] {
						subArgsArray[0],
						subArgsArray[1]
				};
				AugmentedDiffsInspector.main(argsToGenerateOsm);
				String[] argsToGenerateObf = new String[] {
						subArgsArray[1]
				};
				GenerateDailyObf.main(argsToGenerateObf);
			} else if (utl.equals("travel-guide-creator")) {
				TravelGuideCreatorMain.main(subArgsArray);
			} else {
				printSynopsys();
			}
		}
	}

	private static void parseIndexCreatorArgs(List<String> subArgs, IndexCreatorSettings settings) {
		Iterator<String> it = subArgs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			if (s.equals("--add-region-tags")) {
				settings.addRegionTag = true;
				it.remove();
			} else if (s.equals("--keep-only-sea-objects")) {
				settings.keepOnlySeaObjects = true;
				it.remove();
			} else if (s.equals("--ram-process")) {
				settings.processInRam = true;
				it.remove();
			} else if (s.startsWith("--srtm=")) {
				settings.srtmDataFolder = new File(s.substring(s.indexOf('=') + 1));
				it.remove();
			} else if (s.startsWith("--rendering-types=")) {
				settings.renderingTypesFile = s.substring(s.indexOf('=') + 1);
				it.remove();
			} else if (s.startsWith("--poi-types=")) {
				MapPoiTypes poiTypes = new MapPoiTypes(s.substring(s.indexOf('=') + 1));
				MapPoiTypes.setDefault(poiTypes);
				it.remove();
			} else if (s.startsWith("--extra-relations=")) {
				String[] files = s.substring("--extra-relations=".length()).split(",");
				for (String f : files) {
					settings.extraRelations.add(new File(f));
				}
				it.remove();
			} else if (s.startsWith("--chars-build-poi-nameindex=")) {
				settings.charsToBuildPoiNameIndex = Integer.parseInt(s.substring("--chars-build-poi-nameindex=".length()));
				it.remove();
			} else if (s.startsWith("--chars-build-addr-nameindex=")) {
				settings.charsToBuildAddressNameIndex = Integer.parseInt(s.substring("--chars-build-addr-nameindex=".length()));
				it.remove();
			}
		}
	}
	
	private static void generateAllOsmLiveTests(File testResources, String unpackFolder, boolean delete) throws IOException {
		// clean all files
		if (delete) {
			Algorithms.removeAllFiles(new File(testResources, AugmentedDiffsInspector.DEFAULT_REGION));
		}
		for(File f : testResources.listFiles()) {
			if(f.getName().endsWith(".diff.osm")) {
				int DATE_LENGTH = 10;
				String date = f.getName().substring(0, DATE_LENGTH);
				String targetFl = AugmentedDiffsInspector.DEFAULT_REGION + f.getName().substring(DATE_LENGTH) + ".gz";
				FileInputStream fis = new FileInputStream(f);
				File outFl = new File(testResources, AugmentedDiffsInspector.DEFAULT_REGION + "/" + date + "/"
						+ targetFl);
				outFl.getParentFile().mkdirs();
				GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(outFl));
				Algorithms.streamCopy(fis, out);
				out.close();
				fis.close();
			}
			if(f.getName().endsWith(".xml")) {
				AugmentedDiffsInspector.main(new String[] { f.getAbsolutePath(), testResources.getAbsolutePath() });
			}
		}
		GenerateDailyObf.main(new String[] { testResources.getAbsolutePath() });
		if(unpackFolder != null) {
			for(File obfgz : new File(testResources, AugmentedDiffsInspector.DEFAULT_REGION).listFiles()) {
				if(obfgz.getName().endsWith(".obf.gz")) {
					GZIPInputStream is = new GZIPInputStream(new FileInputStream(obfgz));
					FileOutputStream out = new FileOutputStream(new File(unpackFolder, obfgz.getName().substring(0,
							obfgz.getName().length() - 3)));
					Algorithms.streamCopy(is, out);
					is.close();
					out.close();
				}
			}
		}
	}

	private static void generateObf(List<String> subArgs, IndexCreatorSettings settings) throws IOException, SQLException,
			InterruptedException, XmlPullParserException {
		File fileToGen = new File(subArgs.get(0));
		IndexCreator ic = new IndexCreator(new File("."), settings);
		ic.setDialects(settings.processInRam ? DBDialect.SQLITE_IN_MEMORY : DBDialect.SQLITE, 
				settings.processInRam ? DBDialect.SQLITE_IN_MEMORY : DBDialect.SQLITE);
		ic.setLastModifiedDate(fileToGen.lastModified());
		String regionName = fileToGen.getName();
		MapRenderingTypesEncoder types = new MapRenderingTypesEncoder(settings.renderingTypesFile, regionName);
		ic.generateIndexes(fileToGen, new ConsoleProgressImplementation(), null, MapZooms.getDefault(),
				types, log);
	}

	private static void printSynopsys() {
		System.out.println("This utility provides access to all other console utilities of OsmAnd,");
		System.out.println("each utility has own argument list and own synopsys. Here is the list:");
		System.out.println("\t\t generate-obf <path to osm file> <--srtm=opt-folder-with-srtm-data>: simple way to generate obf file in place. "
				+ "\t\t\t	Another supported options generate-map, generate-address, generate-poi, generate-roads (generate obf partially)");
		System.out.println("\t\t inspector <params>: powerful tool to inspect obf files and convert them to osm");
		System.out.println("\t\t check-ocean-tile <lat> <lon> <zoom=11>: checks ocean or land tile is in bz2 list");
		System.out.println("\t\t generate-ocean-tile <coastline osm file> <optional output file>: creates ocean tiles 12 zoom");
		System.out.println("\t\t generate-java-style <pathtostyle> <pathtooutputfolder>: prints rendering style as java interpreted code");
		System.out.println("\t\t explain-rendering-style <pathtostyle>: prints explanation of the style");
		System.out.println("\t\t merge-flat-obf <path to result file> <paths to files to merge (>2)>: merges all data from 2+ obf files (address not supported)");
		System.out.println("\t\t split-obf <path_to_world_obf_diff> <path_to_result_folder> <subfolder_name> <file_suffix>: splits a world_obf into obf region files");
		System.out.println("\t\t generate-obf-diff <path_old_obf> <path_new_obf> <name_or_path_diff_obf or stdout>: generates obf diff file between 2 obf files (address not supported), stdout prints to console");
		System.out.println("\t\t test-routing <own list of parameters>: helps to run routing test for specific locations");
		System.out.println("\t\t generate-ocbf <path to osmand/repos/ repository>: generates regions.ocbf file, this path should contain folders 'misc', 'tools', 'resources'");
		System.out.println("\t\t delete-unused-strings <path to repos/android/OsmAnd/res>: deletes unused translation in git repository (transforms all strings.xml)");
		System.out.println("\t\t extract-roads-only <path to full map obf file> : extracts .road.obf (road-only map) file from full .obf");
		System.out.println("\t\t generate-osmlive-tests <path_to_directory_with_resources_project> <optional_path_to_unpack_files>: test osmand live functionality");
		System.out.println("\t\t convert-gpx-to-obf <path_to_folder_with_gpx_files> or <path_to_gpx_file_with_file_name>: convert gpx file/files to obf file");
		System.out.println("\t\t generate-region-tags <path to input osm file (osm, bz2, gz)> <path to output osm file>: process osm file and assign tag osmand_region_name to every entity.");
		System.out.println("\t\t generate-ocean-tile-osm <optional path to osm file to write> <optional path to oceantiles_12.dat file>: generates ocean tiles osm file to check in JOSM ");
		System.out.println("\t\t generate-obf-files-in-batch <path to batch.xml> <optional path to the file with regions list>: generates multiple obf files with different options");
		System.out.println("\t\t generate-basemap <folder-with-osm-base-files> <optional mini>: generates basemap from prepared osm files");
		System.out.println("\t\t fix-basemap-roads <input-osm-file> <output-osm-file>: merges and simplifies basemap roads");
		System.out.println("\t\t merge-index " + BinaryMerger.helpMessage);
		System.out.println("\t\t create-sqlitedb <dir-with-tiles> [options] <optional sqlitedbfile>: creates .sqlitedb with tiles from directory. USE 'create-sqlitedb --help' to get all params. ");

		System.out.println("\t\t compare " + BinaryComparator.helpMessage);
		System.out.println("\t\t generate-from-overpass <path to overpass.xml (must have format 2017_06_18-10_30)> <path to working directory>: The utility converts overpass.xml to obf");
		System.out.println("\t\t travel-guide-creator: creates custom travel guide from existing resources (.travel.sqlite), --help or -h for more details");
	}
}
