package dev.slimevr.vr.trackers.udp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.eiren.util.logging.LogManager;

public class UDPProtocolParser {
	
	public static final int PACKET_HEARTBEAT = 0;
	public static final int PACKET_ROTATION = 1; // Deprecated
	//public static final int PACKET_GYRO = 2; // Deprecated
	public static final int PACKET_HANDSHAKE = 3;
	//public static final int PACKET_ACCEL = 4; // Not parsed by server
	//public static final int PACKET_MAG = 5; // Deprecated
	//public static final int PACKET_RAW_CALIBRATION_DATA = 6; // Not parsed by server
	//public static final int PACKET_CALIBRATION_FINISHED = 7; // Not parsed by server
	//public static final int PACKET_CONFIG = 8; // Not parsed by server
	//public static final int PACKET_RAW_MAGNETOMETER = 9 // Deprecated
	public static final int PACKET_PING_PONG = 10;
	public static final int PACKET_SERIAL = 11;
	public static final int PACKET_BATTERY_LEVEL = 12;
	public static final int PACKET_TAP = 13;
	public static final int PACKET_ERROR = 14;
	public static final int PACKET_SENSOR_INFO = 15;
	public static final int PACKET_ROTATION_2 = 16; // Deprecated
	public static final int PACKET_ROTATION_DATA = 17;
	public static final int PACKET_MAGNETOMETER_ACCURACY = 18;
	public static final int PACKET_SIGNAL_STRENGTH = 19;
	public static final int PACKET_TEMPERATURE = 20;
	
	public static final int PACKET_PROTOCOL_CHANGE = 200;
	
	private static final byte[] HANDSHAKE_BUFFER = new byte[64];
	
	public UDPProtocolParser() {
	}

	public List<UDPPacket> parse(ByteBuffer buf, TrackerUDPConnection connection) throws IOException {
		List<UDPPacket> packets = new ArrayList<>();
		buf.position(0);
		int pos = 0;

		while (buf.remaining() >= 7) {
			int shLength =  buf.remaining();
			short packetLength = buf.getShort();
			byte verifier = buf.get();
			int packetId = buf.getInt();

			if (
					shLength >= packetLength
			)
			{
				if (verifier == (byte)0xF0) {
					UDPPacket newPacket = getNewPacket(packetId);
					if(newPacket != null) {
						newPacket.readData(buf);
						packets.add(newPacket);
					}
				}
			}

			pos += packetLength;
			buf.position(pos);
		}

		if(connection != null) {
			connection.lastPacket = System.currentTimeMillis();
		}

		return packets;
	}
	
	public void write(ByteBuffer buf, TrackerUDPConnection connection, UDPPacket packet) throws IOException {
		buf.putShort((short)0); // length
		buf.put((byte) 0xF0); // verifier
		buf.putInt(packet.getPacketId()); // id
		packet.writeData(buf);
		buf.putShort(0, (short)buf.position());
	}
	
	public void writeHandshakeResponse(ByteBuffer buf, TrackerUDPConnection connection) throws IOException {
//		buf.put(HANDSHAKE_BUFFER);
		buf.putShort((short)0); // length
		buf.put((byte) 0xF0); // verifier
		buf.putInt(0x02); // id for handshake
		buf.putShort(0, (short)buf.position());
	}

	public void writeHeatbeatResponse(ByteBuffer buf, TrackerUDPConnection connection) throws IOException {
		buf.putShort((short)0); // length
		buf.put((byte) 0xF0); // verifier
		buf.putInt(0x01); // id for handshake
		buf.putShort(0, (short)buf.position());
	}

	public void writePingPong(ByteBuffer buf, TrackerUDPConnection connection, int lastId) throws IOException {
		buf.putShort((short)0); // length
		buf.put((byte) 0xF0); // verifier
		buf.putInt(10);
		buf.putInt(lastId);
	}
	
	public void writeSensorInfoResponse(ByteBuffer buf, TrackerUDPConnection connection, UDPPacket15SensorInfo packet) throws IOException {
		buf.putShort((short)0); // length
		buf.put((byte) 0xF0); // verifier
		buf.putInt(packet.getPacketId());
		buf.put((byte) packet.sensorId);
		buf.put((byte) packet.sensorStatus);
	}
	
	protected UDPPacket getNewPacket(int packetId) {
		switch(packetId) {
		case PACKET_HEARTBEAT:
			return new UDPPacket0Heartbeat();
		case PACKET_ROTATION:
			return new UDPPacket1Rotation();
		case PACKET_HANDSHAKE:
			return new UDPPacket3Handshake();
		case PACKET_PING_PONG:
			return new UDPPacket10PingPong();
		case PACKET_SERIAL:
			return new UDPPacket11Serial();
		case PACKET_BATTERY_LEVEL:
			return new UDPPacket12BatteryLevel();
		case PACKET_TAP:
			return new UDPPacket13Tap();
		case PACKET_ERROR:
			return new UDPPacket14Error();
		case PACKET_SENSOR_INFO:
			return new UDPPacket15SensorInfo();
		case PACKET_ROTATION_2:
			return new UDPPacket16Rotation2();
		case PACKET_ROTATION_DATA:
			return new UDPPacket17RotationData();
		case PACKET_MAGNETOMETER_ACCURACY:
			return new UDPPacket18MagnetometerAccuracy();
		case PACKET_SIGNAL_STRENGTH:
			return new UDPPacket19SignalStrength();
		case PACKET_TEMPERATURE:
			return new UDPPacket20Temperature();
		case PACKET_PROTOCOL_CHANGE:
			return new UDPPacket200ProtocolChange();
		}
		return null;
	}
	
	static {
		try {
			HANDSHAKE_BUFFER[0] = 3;
			byte[] str = "Hey OVR =D 5".getBytes("ASCII");
			System.arraycopy(str, 0, HANDSHAKE_BUFFER, 1, str.length);
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
}
