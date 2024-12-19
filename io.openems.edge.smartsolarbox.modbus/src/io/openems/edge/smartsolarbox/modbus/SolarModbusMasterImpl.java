package io.openems.edge.smartsolarbox.modbus;

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
import org.osgi.service.metatype.annotations.Designate;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.serial.SerialParameters;
import com.intelligt.modbus.jlibmodbus.serial.SerialPort;
import com.intelligt.modbus.jlibmodbus.serial.SerialPortFactoryJSSC;
import com.intelligt.modbus.jlibmodbus.serial.SerialUtils;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarModbus.Master", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SolarModbusMasterImpl extends AbstractOpenemsModbusComponent implements SolarModbusMaster, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;
	
	/*
	 * STILL TO BE IMPLEMENTED:
	 * 
	 * Function to read array and battery status
	 * 
	 * */
    final public SerialParameters sp = new SerialParameters(); //used to set baudrate, port, data bits, parity bits, etc
    final public ModbusMaster m = ModbusMasterFactory.createModbusMasterRTU(sp); //used to connect to the other modbus device
    
	final private static int slaveId = 1;
	private float arrayCurrent = 0; //input array current in A
	private float arrayVoltage = 0; //input array voltage in V
	private float arrayPower = 0; //input array power in W
	private String arrayStatus = "to be implemented";
	private String batteryStatus = "to be implemented";
	private float boxTemp = 0; //external temperature sensor reading in C
	private float batteryCurrent = 0; //battery current in A
	private float batteryVoltage = 0; //battery voltage in V
	private int batterySOC = 0; //battery state of charge 0-100
	private float loadCurrent = 0; // load current in A
	private float loadVoltage = 0; // load voltage in V
	private float loadPower = 0; // load power in W
	private float deviceTemp = 0; // controller temperature in C

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public SolarModbusMasterImpl() throws Exception {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SolarModbusMaster.ChannelId.values() //
		);
		sp.setDevice(config.device());
	}
	
	public SolarModbusMasterImpl(String argDev) throws Exception { //use this constructor to change the device name		
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SolarModbusMaster.ChannelId.values() //
		);
		sp.setDevice(argDev);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if(super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.config = config;
		sp.setBaudRate(SerialPort.BaudRate.BAUD_RATE_115200);
        sp.setDataBits(8);
        sp.setParity(SerialPort.Parity.NONE);
        sp.setStopBits(1);

        SerialUtils.setSerialPortFactory(new SerialPortFactoryJSSC());

        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public int bool2Int(boolean[] bools) { //needed to convert from readDiscreteInputs
		int intVal = 0;
		for(int i = 0; i < bools.length; i++) {
			if(bools[i]==true) {
				intVal = intVal | 0x01 << i;
			}
		}
		return intVal;
	}
	
    public float readArrayCurrent(ModbusMaster m) throws Exception {
    	float arrayCurrent = 0;
    	int quantity = 1;
		int offset = 0x3101;
		System.out.println("Requesting array current... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		arrayCurrent = registerReadValues[0] / (float)100; //TODO: verify if temp is also multiplied by 100
    	return arrayCurrent;
    }
    
    public float readArrayVoltage(ModbusMaster m) throws Exception{
    	float arrayVoltage = 0;
    	int quantity = 1;
		int offset = 0x3100;
		System.out.println("Requesting array voltage... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		arrayVoltage = registerReadValues[0] / (float)100; //TODO: verify if temp is also multiplied by 100
    	return arrayVoltage;
    }
	
	public float readArrayPower(ModbusMaster m) throws Exception {
		float arrayPower = 0;
		int quantity = 2; //from manual
		int offset = 0x3102; //from manual
		System.out.println("Requesting array power... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity); //function code 0x04 in jlibmodbus
		registerReadValues[0] = registerReadValues[0] | (registerReadValues[1] << 16); //requires bit shifting because we read 2 registers: 1st is Lower Bits, 2nd is Higher Bits
		arrayPower = registerReadValues[0] / (float)100; //divided by 100 because manual indicates so
		return arrayPower;
	}
	
    public float readBoxTemp(ModbusMaster m) throws Exception{
    	float boxTemp = 0;
    	int quantity = 1;
		int offset = 0x3110;
		System.out.println("Requesting box temperature... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		boxTemp = registerReadValues[0] / (float)100; //TODO: verify if temp is also multiplied by 100
    	return boxTemp;
    }
    
    public float readBatteryVoltage(ModbusMaster m) throws Exception{
    	float batteryVoltage = 0;
    	int quantity = 1;
		int offset = 0x331A;
		System.out.println("Requesting battery voltage... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		batteryVoltage = registerReadValues[0] / (float)100; //TODO: verify if temp is also multiplied by 100
    	return batteryVoltage;
    }
    
	public float readBatteryCurrent(ModbusMaster m) throws Exception {
		float batteryCurrent = 0;
		int quantity = 2; //from manual
		int offset = 0x331B; //from manual
		System.out.println("Requesting battery current... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity); //function code 0x04 in jlibmodbus
		registerReadValues[0] = registerReadValues[0] | (registerReadValues[1] << 16); //requires bit shifting because we read 2 registers: 1st is Lower Bits, 2nd is Higher Bits
		batteryCurrent = registerReadValues[0] / (float)100; //divided by 100 because manual indicates so
		return batteryCurrent;
	}
    
    public int readBatterySOC(ModbusMaster m) throws Exception{
    	int batterySOC = 0;
    	int quantity = 1;
		int offset = 0x311A;
		System.out.println("Requesting battery SOC... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		batterySOC = registerReadValues[0] / 1; //TODO: verify if SOC is also multiplied by 100
    	return batterySOC;
    }
    
    public float readLoadVoltage(ModbusMaster m) throws Exception{
    	float loadVoltage = 0;
    	int quantity = 1;
		int offset = 0x310C;
		System.out.println("Requesting load voltage... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		loadVoltage = registerReadValues[0] / (float)100; //TODO: verify if temp is also multiplied by 100
    	return loadVoltage;
    }
    
    public float readLoadCurrent(ModbusMaster m) throws Exception{
    	float loadCurrent = 0;
    	int quantity = 1;
		int offset = 0x310D;
		System.out.println("Requesting load current... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		loadCurrent = registerReadValues[0] / (float)100; //TODO: verify if temp is also multiplied by 100
    	return loadCurrent;
    }
    
    public float readLoadPower(ModbusMaster m) throws Exception{
    	float loadPower = 0;
    	int quantity = 2;
		int offset = 0x310E;
		System.out.println("Requesting load power... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		registerReadValues[0] = registerReadValues[0] | (registerReadValues[1] << 16);
		loadPower = registerReadValues[0] / (float)100; //divided by 100 because manual indicates so
    	return loadPower;    	
    }
    
    public float readDeviceTemp(ModbusMaster m) throws Exception{
    	float deviceTemp = 0;
    	int quantity = 1;
    	int offset = 0x3111;
		System.out.println("Requesting device temperature... ");
		int[] registerReadValues = m.readInputRegisters(slaveId, offset, quantity);
		deviceTemp = registerReadValues[0] / (float)100; //TODO: verify if SOC is also multiplied by 100
    	return deviceTemp;    	
    }
    
    public float getArrayCurrent() {
    	return this.arrayCurrent;
    }
    
    public float getArrayVoltage() {
    	return this.arrayVoltage;
    }
    
    public float getArrayPower() {
    	return this.arrayPower;
    }
    
    public String getArrayStatus() {
    	return this.arrayStatus;
    }
    
    public float getBatteryCurrent() {
    	return this.batteryCurrent;
    }    
    
    public float getBatteryVoltage() {
    	return this.batteryVoltage;
    } 
    
    public int getBatterySOC() {
    	return this.batterySOC;
    }
    
    public String getBatteryStatus() {
    	return this.batteryStatus;
    }    

    public float getBoxTemp() {
    	return this.boxTemp;
    }
    
    public float getDeviceTemp() {
    	return this.deviceTemp;
    }
    
    public float getLoadCurrent() {
    	return this.loadCurrent;
    }    
    
    public float getLoadVoltage() {
    	return this.loadVoltage;
    }  
    
    public float getLoadPower() {
    	return this.loadPower;
    }
    
    public String getDeviceName() {
    	return sp.getDevice();
    }
    
    public static int getSlaveId() {
    	return slaveId;
    }
    
    public void readValues(ModbusMaster m) throws Exception{
    	this.arrayCurrent = readArrayCurrent(m);
    	this.arrayVoltage = readArrayVoltage(m);
    	this.arrayPower = readArrayPower(m);
    	this.batteryCurrent = readBatteryCurrent(m);
    	this.batteryVoltage = readBatteryVoltage(m);
    	this.batterySOC = readBatterySOC(m);
    	this.boxTemp = readBoxTemp(m);
    	this.deviceTemp = readDeviceTemp(m);
    	this.loadCurrent = readLoadCurrent(m);
    	this.loadVoltage = readLoadVoltage(m);
    	this.loadPower = readLoadPower(m);
    }
	
	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		// TODO implement ModbusProtocol
		return new ModbusProtocol(this);
	}

	@Override
	public String debugLog() {
		Date date = new Date();
		Timestamp tsp = new Timestamp(date.getTime());
		File file = new File(this.config.logFile());
		FileWriter fr = null;
		BufferedWriter br = null;
		PrintWriter pr = null;
		try {
			// to append to file, you need to initialize FileWriter using below constructor
			fr = new FileWriter(file, true);
			br = new BufferedWriter(fr);
			pr = new PrintWriter(br);
			pr.print(tsp);
			pr.print("\t");
			pr.print(this.getArrayPower());
			pr.print("\t");
			pr.print(this.getBatterySOC());
			pr.print("\t");
			pr.print(this.getBoxTemp());
			pr.print("\t");
			pr.print(this.getDeviceTemp());
			pr.print("\t");
			pr.println(this.getLoadPower());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				pr.close();
				br.close();
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
