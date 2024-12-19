package io.openems.common.jsonrpc.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.session.Role;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for {@link GetEdgesRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "edges": {@link EdgeMetadata}[]
 *   }
 * }
 * </pre>
 */
public class GetEdgesResponse extends JsonrpcResponseSuccess {

	public static class EdgeMetadata {

		/**
		 * Converts a collection of EdgeMetadatas to a JsonArray.
		 *
		 * <pre>
		 * [{
		 *   "id": String,
		 *   "comment": String,
		 *   "producttype": String,
		 *   "version": String,
		 *   "role": {@link Role},
		 *   "isOnline": boolean,
		 *   "lastmessage": ZonedDateTime
		 * }]
		 * </pre>
		 *
		 * @param metadatas the EdgeMetadatas
		 * @return a JsonArray
		 */
		public static JsonArray toJson(List<EdgeMetadata> metadatas) {
			var result = new JsonArray();
			for (EdgeMetadata metadata : metadatas) {
				result.add(metadata.toJsonObject());
			}
			return result;
		}

		private final String id;
		private final String comment;
		private final String producttype;
		private final SemanticVersion version;
		private final Role role;
		private final boolean isOnline;
		private final ZonedDateTime lastmessage;

		public EdgeMetadata(String id, String comment, String producttype, SemanticVersion version, Role role,
				boolean isOnline, ZonedDateTime lastmessage) {
			this.id = id;
			this.comment = comment;
			this.producttype = producttype;
			this.version = version;
			this.role = role;
			this.isOnline = isOnline;
			this.lastmessage = lastmessage;
		}

		protected JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("id", this.id) //
					.addProperty("comment", this.comment) //
					.addProperty("producttype", this.producttype) //
					.addProperty("version", this.version.toString()) //
					.add("role", this.role.asJson()) //
					.addProperty("isOnline", this.isOnline) //
					.addPropertyIfNotNull("lastmessage", this.lastmessage) //
					.build();
		}
	}

	private final List<EdgeMetadata> edgeMetadata;

	public GetEdgesResponse(UUID id, List<EdgeMetadata> edgeMetadata) {
		super(id);
		this.edgeMetadata = edgeMetadata;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("edges", EdgeMetadata.toJson(this.edgeMetadata)) //
				.build();
	}

}
