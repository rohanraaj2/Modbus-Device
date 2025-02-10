package io.openems.edge.smartsolarbox.modbus;
import java.io.*;
import java.util.Date;

import javax.sql.rowset.serial.SerialRef;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import java.sql.Timestamp;
import gnu.io.SerialPort;

public class SolarModbusMasterImpl extends AbstractOpenemsModbusComponent {
	
	/*
	 * STILL TO BE IMPLEMENTED:
	 * 
	 * Function to read array and battery status
	 * 
	 * */
	
	
    final public SerialParameters sp; //used to set baudrate, port, data bits, parity bits, etc
    final public ModbusSerialMaster m; //used to connect to the other modbus device
    
    final private String logFile = "/home/pi/Desktop/ecowet/smartsolar/solarlog.log";
    
	final private static int slaveId = 1;
	private int quantity = 0; //quantity of registers to read via ModBus
	private int offset = 0x00; //address offset for register
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
	private String device = "/dev/ttyXRUSB0"; // can change through the method setDeviceName(), i'll leave the default here

	public SolarModbusMasterImpl() throws Exception{ //calling all necessary functions in the constructor of the ModbusMaster
		
		sp = new SerialParameters();
        sp.setPortName(device);
        sp.setBaudRate(115200);
        sp.setDatabits(8);
        sp.setParity(SerialPort.PARITY_NONE);
        sp.setStopbits(1);
        
        SerialRef.setSerialPortFactory(new SerialPortFactoryJSSC());
        
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);
        m = ModbusMasterFactory.createModbusMasterRTU(sp);
	}
	
	public SolarModbusMasterImpl(String argDev) throws Exception{ //use this constructor to change the device name
		this.setDeviceName(argDev);
		
		sp = new SerialParameters();
        sp.setPortName(device);
        sp.setBaudRate(115200);
        sp.setDatabits(8);
        sp.setParity(SerialPort.PARITY_NONE);
        sp.setStopbits(1);

//        SerialRef.setSerialPortFactory(new SerialPortFactoryJSSC());

//        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);
        m = ModbusMasterFactory.createModbusMasterRTU(sp);
	}
	
	@Override
	protected ModbusProtocol defineModbusProtocol() {
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
	
	public int bool2Int(boolean[] bools) { //needed to convert from readDiscreteInputs
		int intVal = 0;
		for(int i = 0; i < bools.length; i++) {
			if(bools[i]==true) {
				intVal = intVal | 0x01 << i;
			}
		}
		return intVal;
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
    
    public void setDeviceName(String argDevice) {
    	this.device = argDevice;
    }
    
    public String getDeviceName() {
    	return this.device;
    }
    
    public static int getSlaveId() {
    	return slaveId;
    }
    
    public void readValues(ModbusSerialMaster m) throws Exception{
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

    //Function for logging to file
	public void toLog() {
		Date date = new Date();
		Timestamp tsp = new Timestamp(date.getTime());
		File file = new File(this.logFile);
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
	}
}
