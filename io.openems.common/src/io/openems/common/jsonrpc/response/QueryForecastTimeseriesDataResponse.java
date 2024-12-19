package io.openems.common.jsonrpc.response;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

/**
 * Represents a JSON-RPC Response for 'queryForecastTimeseriesData'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "timestamps": [
 *       '2011-12-03T10:15:30Z',...
 *     ],
 *     "data": {
 *       "componentId/channelId": [
 *         value1, value2,...
 *       ]
 *     }
 *   }
 * }
 * </pre>
 */
public class QueryForecastTimeseriesDataResponse extends JsonrpcResponseSuccess {

	private final SortedMap<ZonedDateTime, SortedMap<String, JsonElement>> table;

	public QueryForecastTimeseriesDataResponse(SortedMap<ZonedDateTime, SortedMap<String, JsonElement>> table) {
		this(UUID.randomUUID(), table);
	}

	public QueryForecastTimeseriesDataResponse(UUID id,
			SortedMap<ZonedDateTime, SortedMap<String, JsonElement>> table) {
		super(id);
		this.table = table;
	}

	@Override
	public JsonObject getResult() {
		var result = new JsonObject();

		var timestamps = new JsonArray();
		for (ZonedDateTime timestamp : this.table.keySet()) {
			timestamps.add(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")));
		}
		result.add("timestamps", timestamps);

		var data = new JsonObject();
		for (Entry<ZonedDateTime, SortedMap<String, JsonElement>> rowEntry : this.table.entrySet()) {
			for (Entry<String, JsonElement> colEntry : rowEntry.getValue().entrySet()) {
				var identifiers = colEntry.getKey().toString();
				var value = colEntry.getValue();
				var identifierValuesElement = data.get(identifiers);
				JsonArray identifierValues;
				if (identifierValuesElement != null) {
					identifierValues = identifierValuesElement.getAsJsonArray();
				} else {
					identifierValues = new JsonArray();
				}
				identifierValues.add(value);
				data.add(identifiers, identifierValues);
			}
		}
		result.add("data", data);

		return result;
	}

}
