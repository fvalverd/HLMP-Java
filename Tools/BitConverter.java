package hlmp.Tools;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
//import java.nio.charset.Charset;
import java.util.UUID;

public class BitConverter {

	public static byte[] intToByteArray(int value) {
	    ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);;
	    buffer.putInt(value);
	    return buffer.array();
	}
	
	public static final int byteArrayToInt(byte [] b) {
		ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}
	
	public static byte[] UUIDtoBytes(UUID id) {
		ByteBuffer byteBuffer = MappedByteBuffer.allocate(16);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.putLong(id.getMostSignificantBits());
		byteBuffer.putLong(id.getLeastSignificantBits());
		return byteBuffer.array();
	}
	
	public static UUID bytesToUUID(byte[] bits) {
		ByteBuffer bb = ByteBuffer.wrap(bits);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		UUID uuid = new UUID(bb.getLong(), bb.getLong());
		return uuid;
	}

	public static final int readInt(byte[] src, int offset) {
		ByteBuffer bb = ByteBuffer.wrap(src, offset, 4).order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
 	}
	
	public static void writeInt(int datum, byte[] dst, int offset) {
		byte[] datumToByteArray = intToByteArray(datum);
		for (int i=0; i<4; ++i){
			dst[offset+i] = datumToByteArray[i]; 
		}
	}
	
	public static void writeLong(long datum, byte[] dst, int offset) {
		ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
	    buffer.putLong(datum);
	    byte[] datumToByteArray = buffer.array();
	    
	    for (int i=0; i<8; ++i) {
			dst[offset+i] = datumToByteArray[i]; 
		}
 	}

	public static final long readLong(byte[] src, int offset) {
		ByteBuffer bb = ByteBuffer.wrap(src, offset, 8).order(ByteOrder.LITTLE_ENDIAN);
		return bb.getLong();
 	}
	
	public static final byte[] stringToByte(String s) {
//		return s.getBytes(Charset.forName("UTF-16LE"));
		try {
			return s.getBytes("UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	public static final String byteToString(byte[] b) {
//		return new String(b, Charset.forName("UTF-16LE"));
		try {
			return new String(b, "UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
}
