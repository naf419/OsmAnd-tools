
package net.osmand.server.controllers.pub;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.Gson;

import net.osmand.obf.preparation.IndexWeatherData;
import net.osmand.obf.preparation.IndexWeatherData.WeatherTiff;

@Controller
@RequestMapping("/weather-api")
public class WeatherController {
	
	protected static final Log LOGGER = LogFactory.getLog(WeatherController.class);

	
	Gson gson = new Gson();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH");
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Value("${osmand.weather.tiff-location}")
	String tiffLocation;
	
	@RequestMapping(path = "/point-info", produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<?> routing(@RequestParam(required = true) double lat, @RequestParam(required = true) double lon)
			throws IOException, InterruptedException {
		File folder = new File(tiffLocation);
		List<Object[]> dt = new ArrayList<>();
		if (folder.exists()) {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.MINUTE, 0);
			int err = 0;
			while (err < 3) {
				File fl = new File(folder, sdf.format(c.getTime()) + "00.tiff");
				if (fl.exists()) {
					Object[] data = new Object[7];
					data[0] = c.getTimeInMillis();
					data[1] = sdf.format(c.getTime()).substring(4).replace('_', ' ') + ":00";
					WeatherTiff wt = new IndexWeatherData.WeatherTiff(fl);
					for (int i = 0; i < wt.getBands() && i < 5; i++) {
						data[2 + i] = wt.getValue(i, lat, lon);
					}
					dt.add(data);
				} else {
					err++;
				}
				c.add(Calendar.HOUR, 1);
			}
		}
		return ResponseEntity.ok(gson.toJson(dt));
	}



}
