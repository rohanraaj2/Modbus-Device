package io.openems.common.forecastdata;

import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.SortedMap;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.QueryForecastTimeseriesDataRequest;
import io.openems.common.timedata.Resolution;

public interface CommonForecastdataService {
	
	/**
	 * Calculates the time {@link Resolution} for the period.
	 *
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @return the resolution
	 */
	public static Resolution calculateResolution(ZonedDateTime fromDate, ZonedDateTime toDate) {
		var days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
		if (days <= 1) {
			return new Resolution(5, ChronoUnit.MINUTES);
		} else if (days == 2) {
			return new Resolution(10, ChronoUnit.MINUTES);
		} else if (days == 3) {
			return new Resolution(15, ChronoUnit.MINUTES);
		} else if (days == 4) {
			return new Resolution(20, ChronoUnit.MINUTES);
		} else if (days <= 6) {
			return new Resolution(30, ChronoUnit.MINUTES);
		} else if (days <= 12) {
			return new Resolution(1, ChronoUnit.HOURS);
		} else if (days <= 24) {
			return new Resolution(2, ChronoUnit.HOURS);
		} else if (days <= 48) {
			return new Resolution(4, ChronoUnit.HOURS);
		} else if (days <= 96) {
			return new Resolution(8, ChronoUnit.HOURS);
		} else if (days <= 144) {
			return new Resolution(12, ChronoUnit.HOURS);
		} else {
			return new Resolution(1, ChronoUnit.DAYS);
		}
	}

	/**
	 * Queries forecast data. The 'resolution' of the query is calculated
	 * dynamically according to the length of the period.
	 *
	 * @param edgeId  the Edge-ID
	 * @param request the {@link QueryForecastTimeseriesDataRequest}
	 * @return the query result
	 */
	public default SortedMap<ZonedDateTime, SortedMap<String, JsonElement>> queryForecastData(String edgeId,
			QueryForecastTimeseriesDataRequest request) throws OpenemsNamedException {
		// calculate resolution based on the length of the period
		var resolution = request.getResolution() //
				.orElse(CommonForecastdataService.calculateResolution(request.getFromDate(), request.getToDate()));

		return this.queryForecastData(edgeId, request.getFromDate(), request.getToDate(), resolution);
	}

	public SortedMap<ZonedDateTime, SortedMap<String, JsonElement>> queryForecastData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Resolution resolution)
			throws OpenemsNamedException;
}
