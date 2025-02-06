package io.openems.edge.smartsolarbox.modbus;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Date;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import gnu.io.SerialPort;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarModbus.Master", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class DummySolarModbusMasterImpl extends AbstractOpenemsModbusComponent
		implements SolarModbusMaster, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent,
		EventHandler, ModbusSlave {

	final private static int slaveId = 1;
	final public SerialParameters sp = new SerialParameters(); //used to set baudrate, port, data bits, parity bits, etc
	final public ModbusSerialMaster m = new ModbusSerialMaster(sp); //used to connect to the other modbus device

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	protected Config config;

	public DummySolarModbusMasterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				SolarModbusMaster.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		sp.setBaudRate(115200);
		sp.setDatabits(8);
		sp.setParity(0);
		sp.setStopbits(1);


		// Stop if component is disabled
		if (!config.enabled()) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(0x3101, Priority.HIGH,
						m(SolarModbusMaster.ChannelId.ARRAY_CURRENT,
								new SignedDoublewordElement(0x3101).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(3502).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.ARRAY_VOLTAGE,
								new SignedDoublewordElement(0x3100).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.VOLTAGE, new SignedWordElement(3506), SCALE_FACTOR_3),
						m(SolarModbusMaster.ChannelId.ARRAY_POWER, new SignedWordElement(0x3102), SCALE_FACTOR_2),
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new SignedDoublewordElement(3508).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.BOX_TEMP,
								new SignedDoublewordElement(0x3110).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.BATTERY_CURRENT,
								new SignedDoublewordElement(0x331B).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.BATTERY_VOLTAGE,
								new SignedDoublewordElement(0x331A).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.BATTERY_SOC,
								new SignedDoublewordElement(0x311A).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.LOAD_CURRENT,
								new SignedDoublewordElement(0x310D).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new SignedDoublewordElement(3520).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.LOAD_VOLTAGE,
								new SignedDoublewordElement(0x310C).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.LOAD_POWER,
								new SignedDoublewordElement(0x310E).wordOrder(WordOrder.LSWMSW)),
						m(SolarModbusMaster.ChannelId.DEVICE_TEMP,
								new SignedDoublewordElement(0x3111).wordOrder(WordOrder.LSWMSW))));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			try {
				this.setPvLimitHandler.run();

				this.channel(PvInverterSolarlog.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(PvInverterSolarlog.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateProductionEnergy.update(this.getActivePower().get());
			break;
		}
	}

    public void printChannelValues() {
        for (SolarModbusMaster.ChannelId channelId : SolarModbusMaster.ChannelId.values()) {
            Object value = channelId.name();
            System.out.println(channelId.name() + ": " + value);
        }
        System.out.println(SolarModbusMaster.ChannelId.values());
    }
	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}
}
