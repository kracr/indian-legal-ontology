/* This class queries GeoNames ontology for India specific data and outputs. A GeoNames username is needed to call the web API endpoint. To understand the GeoNames ontology, and the
 * queries, please look up the structure of GeoNames and how states and districts lie at different administrative divisions within a country. Data is retrieved and written to files for
 * later use. Each location is specified as an individual within the GeoNames ontology and is therefore associated with a unique IRI which we use to identify the resource. Remember to
 * replace GeoNamesUsername with your username which you can get by signing up at GeoNames.org
 */

package onted;

import org.apache.http.client.fluent.Request;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class GeoNamesHandler {

//    public static Dictionary<String, String> queryGeoNamesAndWriteToFile(String username) {
//        String baseUrl = "http://api.geonames.org/childrenJSON";
//        String countryId = "1269750"; // GeoNames ID for India
//        String outputFile = "first_level_admin_divisions.txt";
//
//        String requestUrl = baseUrl + "?geonameId=" + countryId + "&username=" + username;
//        
//        Dictionary<String, String> geoData = new Hashtable<>();
//
//        try {
//            String response = Request.Get(requestUrl)
//                                .execute()
//                                .returnContent()
//                                .asString();
//
//            JsonArray divisions = JsonParser.parseString(response)
//                                            .getAsJsonObject()
//                                            .getAsJsonArray("geonames");
//
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
//                for (int i = 0; i < divisions.size(); i++) {
//                    JsonObject division = divisions.get(i).getAsJsonObject();
//                    String featureCode = division.get("fcode").getAsString();
//
//                    // Filter for first-level administrative divisions (ADM1)
//                    if ("ADM1".equals(featureCode)) {
//                        String name = division.get("name").getAsString();
//                        String geonameId = division.get("geonameId").getAsString();
//                        String iri = "http://sws.geonames.org/" + geonameId + "/";
//                        writer.write(name + " | " + iri + "\n");
//                        geoData.put(name, iri);
//                    }
//                }
//            }
//
//            System.out.println("Output written to " + outputFile);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//		return geoData;
//    }
    
    public static HashMap<String, String> queryGeoNames(String username, String parentPlaceID, int adminDivLevel) {
        String baseUrl = "http://api.geonames.org/childrenJSON";
        String placeID = parentPlaceID; // GeoNames ID
        String outputFile = "GeoNames_data/" + placeID + "-" + adminDivLevel + "_level_admin_divisions.txt";

        String requestUrl = baseUrl + "?geonameId=" + parentPlaceID + "&username=" + username;
        
        HashMap<String, String> geoData = new HashMap<String, String>();
        String adminLevel = "ADM" + adminDivLevel;

        try {
            String response = Request.Get(requestUrl)
                                .execute()
                                .returnContent()
                                .asString();

            JsonArray divisions = JsonParser.parseString(response)
                                            .getAsJsonObject()
                                            .getAsJsonArray("geonames");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                for (int i = 0; i < divisions.size(); i++) {
                    JsonObject division = divisions.get(i).getAsJsonObject();
                    String featureCode = division.get("fcode").getAsString();

                    // Filter for specified level administrative divisions (ADMx)
                    if (adminLevel.equals(featureCode)) {
                        String name = division.get("name").getAsString();
                        String geonameId = division.get("geonameId").getAsString();
                        String iri = "http://sws.geonames.org/" + geonameId + "/";
                        writer.write(name + " | " + iri + "\n");
                        geoData.put(geonameId, name);
                    }
                }
            }

            System.out.println("Output written to " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
		return geoData;
    }
    
    public static HashMap<String, String> getCityGeoNamesIDs(ArrayList<String> cityStatePairs, String username) {
        HashMap<String, String> cityIDs = new HashMap<>();

        for (String cityStatePair : cityStatePairs) {
            try {
                String[] parts = cityStatePair.split(", ");
                if (parts.length != 2) {
                    continue; // Skip if the format is not "City, State"
                }
                String cityName = URLEncoder.encode(parts[0].trim(), StandardCharsets.UTF_8.toString());
                String stateName = URLEncoder.encode(parts[1].trim(), StandardCharsets.UTF_8.toString());

                String queryUrl = String.format(
                    "http://api.geonames.org/searchJSON?q=%s&adminName1=%s&country=IN&featureClass=P&username=%s",
                    cityName, stateName, username);
                
                //System.out.println("Query URL: " + queryUrl);

                String response = Request.Get(queryUrl).execute().returnContent().asString();
                JsonArray geonames = JsonParser.parseString(response)
                                                .getAsJsonObject()
                                                .getAsJsonArray("geonames");

                for (JsonElement element : geonames) {
                	JsonObject city = element.getAsJsonObject();
                	String name = city.get("name").getAsString();
                	String state = "";
                	if (city.has("adminName1") && !city.get("adminName1").isJsonNull()) {
                	    state = city.get("adminName1").getAsString();
                	}
                	String geonameId = city.get("geonameId").getAsString();


                    if (cityName.equalsIgnoreCase(name)) {
                        cityIDs.put(geonameId, name);
                        break; // Assuming you want the first match
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return cityIDs;
    }
    
    public static String getGeoIRI(String geonameId) {
    	return "http://sws.geonames.org/" + geonameId + "/";
    }
    
    public static String getGeoIRI_alt(String geonameId) {
    	return "https://www.geonames.org/" + geonameId + "/";
    }
    
	public static ArrayList<String> entitiesFromFile(String myFile, String myPrefix, String mySuffix) throws IOException {
		// Read the classes from the text file and add them to the ontology
		File entitiesTextFile = new File(myFile);
		ArrayList<String> entities = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(entitiesTextFile));
		String line;
		while ((line = br.readLine()) != null) {
			// Trim leading and trailing whitespaces
			line = myPrefix + line.trim() + mySuffix;
			if (!line.isEmpty())
				entities.add(line);
		}
		br.close();
		return entities;
	}
    
	public static void putObject(Object objectName, String fileName) {
		try (final FileOutputStream fout = new FileOutputStream(fileName);
				final ObjectOutputStream out = new ObjectOutputStream(fout)) {
			out.writeObject(objectName);
			out.flush();
			System.out.println("success");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public static Object getObject(String objectFile) {
    	Object myObject = null;
        try {
            FileInputStream fileIn = new FileInputStream(objectFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            myObject = in.readObject();
            in.close();
            fileIn.close();
         } catch (IOException i) {
            i.printStackTrace();
            return myObject;
         } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
            return myObject;
         }
		return myObject;
    }
    
    public static void main(String[] args) throws IOException {
//    	HashMap<String, String> stateUT = queryGeoNames("GeoNamesUsername", "1269750", 1);
//    	GeoNamesHandler.putObject(stateUT, "stateUT.txt");
//    	
//    	HashMap<String, HashMap<String, String>> district_by_stateUT = new HashMap<String, HashMap<String, String>>();
//    	for (String geonameId : stateUT.keySet()) {
//    		System.out.println("Looking at administrative division: " + geonameId);
//    		district_by_stateUT.put(geonameId, queryGeoNames("GeoNamesUsername", geonameId, 2));
//    	}
//    	GeoNamesHandler.putObject(district_by_stateUT, "district_by_stateUT.txt");
//
//    	//Dictionary<String, String> district_dict = queryGeoNames("GeoNamesUsername", "1278629", 2);
    	
    	ArrayList<String> cityStatePairs = GeoNamesHandler.entitiesFromFile("city-state_pairs.txt", "", "");
    	HashMap<String, String> cityIDs = getCityGeoNamesIDs(cityStatePairs, "GeoNamesUsername");
    	cityIDs.forEach((id, city) -> System.out.println(city + ": " + id));
    	GeoNamesHandler.putObject(cityIDs, "CityID.txt");
    }
}
