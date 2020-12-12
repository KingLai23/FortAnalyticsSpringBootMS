package com.example.springboot;

import org.springframework.web.bind.annotation.RestController;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@CrossOrigin
@RestController
public class Controller {

	private static final String fortniteAPIAuth = "58941e65-822e91c1-f55ffbf7-eb3fc646";

	OkHttpClient client = new OkHttpClient();

	@RequestMapping(value = "/playerStats", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getFortnitePlayerStats(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		String playerName = params.get("playerName");
		String playerId = "N/A";
		String season = params.get("season");
		
		System.out.println(season);

		Map<String, Object> data = new HashMap<String, Object>();

		String status = "Good";

		try {
			HttpUrl.Builder urlBuilder = HttpUrl.parse("https://fortniteapi.io/v1/lookup").newBuilder();
			urlBuilder.addQueryParameter("username", playerName);
			String url = urlBuilder.build().toString();

			Request okRequest = new Request.Builder().url(url).addHeader("Authorization", fortniteAPIAuth)
					.method("GET", null).build();

			Call call = client.newCall(okRequest);

			Response lookupResponseBody = call.execute();
			String lookupResponseString = lookupResponseBody.body().string();

			ObjectMapper mapper = new ObjectMapper();

			if (mapper.readValue(lookupResponseString, Map.class).get("result").toString().equals("true")) {
				playerId = mapper.readValue(lookupResponseString, Map.class).get("account_id").toString();

				urlBuilder = HttpUrl.parse("https://fortniteapi.io/v1/stats").newBuilder();
				urlBuilder.addQueryParameter("account", playerId);
				urlBuilder.addQueryParameter("season", season);
				url = urlBuilder.build().toString();
				
				System.out.println(url);

				okRequest = new Request.Builder().url(url).addHeader("Authorization", fortniteAPIAuth)
						.method("GET", null).build();

				call = client.newCall(okRequest);

				Response playerStatsBody = call.execute();
				String playerStatsString = playerStatsBody.body().string();

				Map<String, Object> playerStatsMap = mapper.readValue(playerStatsString, Map.class);

				if (playerStatsMap.get("result").toString().equals("true")
						&& !playerStatsMap.get("global_stats").toString().equals("null")) {
					data = organizePlayerStats(playerStatsMap);
				} else {
					status = "Error fetching player data";
				}

			} else {
				status = "dne";
			}

		} catch (Exception e2) {
			status = "Error calling FortniteAPI.io";
		}

		return Utils.getResponse(request, status, data);
	}

	private Map<String, Object> organizePlayerStats(Map<String, Object> playerStats)
			throws JsonMappingException, JsonProcessingException {
		Map<String, Object> oStats = new HashMap<String, Object>();

		ObjectMapper mapper = new ObjectMapper();
		NumberFormat formatTwoDec = new DecimalFormat("#0.00");

		oStats.put("username", playerStats.get("name"));

		Map<String, Object> globalStats = mapper.convertValue(playerStats.get("global_stats"), Map.class);

		Map<String, Object> solos = new HashMap<String, Object>();
		Map<String, Object> duos = new HashMap<String, Object>();
		Map<String, Object> squads = new HashMap<String, Object>();
		Map<String, Object> total = new HashMap<String, Object>();
		
		int totalWins = 0;
		int totalMatches = 0;
		double totalWinRate = 0;
		int totalKills = 0;
		double totalkd = 0;
		double totalkm = 0;
		int totalOutLived = 0;
		int totalOutlivedM = 0;
		int totalGameTime = 0;
		int avgMatchTime = 0;
		int top3 = 0;
		int top5 = 0;
		int top6 = 0;
		int top12 = 0;
		int top10 = 0;
		int top25 = 0;
		int found = 0;
		
		
		if (globalStats.containsKey("solo")) {
			solos = mapper.convertValue(globalStats.get("solo"), Map.class);
			solos.put("km", Double.parseDouble(formatTwoDec.format(Double.parseDouble(solos.get("kills").toString())
					/ Double.parseDouble(solos.get("matchesplayed").toString()))));
			solos.put("outlivedpermatch", Integer.parseInt(solos.get("playersoutlived").toString())
					/ Integer.parseInt(solos.get("matchesplayed").toString()));
			solos.put("minutespermatch", Integer.parseInt(solos.get("minutesplayed").toString())
					/ Integer.parseInt(solos.get("matchesplayed").toString())+1);
			solos.put("winrate",
					Double.parseDouble(formatTwoDec.format(Double.parseDouble(solos.get("winrate").toString()) * 100.0)));
			solos.put("found", 1);
			
			found = 1;
			totalWins += Integer.parseInt(solos.get("placetop1").toString());
			totalMatches += Integer.parseInt(solos.get("matchesplayed").toString());
			totalKills += Integer.parseInt(solos.get("kills").toString());
			totalkd = Double.parseDouble(formatTwoDec.format(((double)totalKills / ((double)totalMatches - (double)totalWins))));
			totalkm = Double.parseDouble(formatTwoDec.format(((double)totalKills / (double)totalMatches)));
			totalWinRate = Double.parseDouble(formatTwoDec.format((100.0*(double)totalWins / (double)totalMatches)));
			totalOutLived += Integer.parseInt(solos.get("playersoutlived").toString());
			totalOutlivedM = totalOutLived / totalMatches;
			totalGameTime += Integer.parseInt(solos.get("minutesplayed").toString());
			avgMatchTime = totalGameTime / totalMatches;
			top10 += Integer.parseInt(solos.get("placetop10").toString()); 
			top25 += Integer.parseInt(solos.get("placetop25").toString()); 
			
			
		} else {
			solos.put("found", 0);
		}
		
		if (globalStats.containsKey("duo")) {
			duos = mapper.convertValue(globalStats.get("duo"), Map.class);
			duos.put("km", Double.parseDouble(formatTwoDec.format(Double.parseDouble(duos.get("kills").toString())
					/ Double.parseDouble(duos.get("matchesplayed").toString()))));
			duos.put("outlivedpermatch", Integer.parseInt(duos.get("playersoutlived").toString())
					/ Integer.parseInt(duos.get("matchesplayed").toString()));
			duos.put("minutespermatch", Integer.parseInt(duos.get("minutesplayed").toString())
					/ Integer.parseInt(duos.get("matchesplayed").toString())+1);
			duos.put("winrate",
					Double.parseDouble(formatTwoDec.format(Double.parseDouble(duos.get("winrate").toString()) * 100.0)));
			duos.put("found", 1);
			
			found = 1;
			totalWins += Integer.parseInt(duos.get("placetop1").toString());
			totalMatches += Integer.parseInt(duos.get("matchesplayed").toString());
			totalKills += Integer.parseInt(duos.get("kills").toString());
			totalkd = Double.parseDouble(formatTwoDec.format(((double)totalKills / ((double)totalMatches - (double)totalWins))));
			totalkm = Double.parseDouble(formatTwoDec.format(((double)totalKills / (double)totalMatches)));
			totalWinRate = Double.parseDouble(formatTwoDec.format((100.0*(double)totalWins / (double)totalMatches)));
			totalOutLived += Integer.parseInt(duos.get("playersoutlived").toString());
			totalOutlivedM = totalOutLived / totalMatches;
			totalGameTime += Integer.parseInt(duos.get("minutesplayed").toString());
			avgMatchTime = totalGameTime / totalMatches;
			top5 += Integer.parseInt(duos.get("placetop5").toString()); 
			top12 += Integer.parseInt(duos.get("placetop12").toString()); 
			
		} else {
			duos.put("found", 0);
		}

		
		if (globalStats.containsKey("squad")) {
			squads = mapper.convertValue(globalStats.get("squad"), Map.class);
			squads.put("km", Double.parseDouble(formatTwoDec.format(Double.parseDouble(squads.get("kills").toString())
					/ Double.parseDouble(squads.get("matchesplayed").toString()))));
			squads.put("outlivedpermatch", Integer.parseInt(squads.get("playersoutlived").toString())
					/ Integer.parseInt(squads.get("matchesplayed").toString()));
			squads.put("minutespermatch", Integer.parseInt(squads.get("minutesplayed").toString())
					/ Integer.parseInt(squads.get("matchesplayed").toString())+1);
			squads.put("winrate",
					Double.parseDouble(formatTwoDec.format(Double.parseDouble(squads.get("winrate").toString()) * 100.0)));
			squads.put("found", 1);
			
			found = 1;
			totalWins += Integer.parseInt(squads.get("placetop1").toString());
			totalMatches += Integer.parseInt(squads.get("matchesplayed").toString());
			totalKills += Integer.parseInt(squads.get("kills").toString());
			totalkd = Double.parseDouble(formatTwoDec.format(((double)totalKills / ((double)totalMatches - (double)totalWins))));
			totalkm = Double.parseDouble(formatTwoDec.format(((double)totalKills / (double)totalMatches)));
			totalWinRate = Double.parseDouble(formatTwoDec.format((100.0*(double)totalWins / (double)totalMatches)));
			totalOutLived += Integer.parseInt(squads.get("playersoutlived").toString());
			totalOutlivedM = totalOutLived / totalMatches;
			totalGameTime += Integer.parseInt(squads.get("minutesplayed").toString());
			avgMatchTime = totalGameTime / totalMatches;
			top3 += Integer.parseInt(squads.get("placetop3").toString()); 
			top6 += Integer.parseInt(squads.get("placetop6").toString()); 
			
		} else {
			squads.put("found", 0);
		}
		
		total.put("found", found);
		total.put("placetop1", totalWins);
		total.put("kd", totalkd);
		total.put("winrate", totalWinRate);
		total.put("top356", top3 + top5 + top6);
		total.put("top101225", top10 + top12 + top25);
		total.put("kills", totalKills);
		total.put("matchesplayed", totalMatches);
		total.put("minutesplayed", totalGameTime);
		total.put("playeresoutlived", totalOutLived);
		total.put("km", totalkm);
		total.put("outlivedpermatch", totalOutlivedM);
		total.put("minutespermatch", avgMatchTime);

		oStats.put("solos", solos);
		oStats.put("duos", duos);
		oStats.put("squads", squads);
		oStats.put("total", total);

		return oStats;
	}

}
