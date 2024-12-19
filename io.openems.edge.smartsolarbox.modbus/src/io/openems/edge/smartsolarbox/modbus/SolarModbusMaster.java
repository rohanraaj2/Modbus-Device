package io.openems.edge.smartsolarbox.modbus;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface SolarModbusMaster extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ARRAY_CURRENT(Doc.of(OpenemsType.FLOAT)),
		ARRAY_VOLTAGE(Doc.of(OpenemsType.FLOAT)),
		ARRAY_POWER(Doc.of(OpenemsType.FLOAT)),
		BOX_TEMP(Doc.of(OpenemsType.FLOAT)),
		BATTERY_CURRENT(Doc.of(OpenemsType.FLOAT)),
		BATTERY_VOLTAGE(Doc.of(OpenemsType.FLOAT)),
		BATTERY_SOC(Doc.of(OpenemsType.INTEGER)),
		LOAD_CURRENT(Doc.of(OpenemsType.FLOAT)),
		LOAD_VOLTAGE(Doc.of(OpenemsType.FLOAT)),
		LOAD_POWER(Doc.of(OpenemsType.FLOAT)),
		DEVICE_TEMP(Doc.of(OpenemsType.FLOAT)),
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
