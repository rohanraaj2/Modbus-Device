package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * A FloatDoublewordElement represents a Float value according to IEEE-754 in an
 * {@link AbstractDoubleWordElement}.
 */
public class FloatDoublewordElement extends AbstractDoubleWordElement<FloatDoublewordElement, Float> {

	public FloatDoublewordElement(int address) {
		super(OpenemsType.FLOAT, address);
	}

	@Override
	protected FloatDoublewordElement self() {
		return this;
	}

	@Override
	protected Float fromByteBuffer(ByteBuffer buff) {
		return buff.order(this.getByteOrder()).getFloat(0);
	}

	@Override
	protected ByteBuffer toByteBuffer(ByteBuffer buff, Float value) {
		return buff.putFloat(value.floatValue());
	}
}
