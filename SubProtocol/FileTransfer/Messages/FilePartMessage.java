package hlmp.SubProtocol.FileTransfer.Messages;

import java.util.UUID;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.SafeUnicastMessage;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;
import hlmp.Tools.BitConverter;


public class FilePartMessage extends SafeUnicastMessage {
	
	private UUID fileHandlerId;
	private long partId;
	private byte[] filePart;

	public FilePartMessage() {
		super();
		this.type = FileTransferProtocolType.FILEPARTMESSAGE;
		this.protocolType = FileTransferProtocolType.FILETRANSFERPROTOCOL;
	}

	public FilePartMessage(NetUser targetNetUser, UUID fileHandlerId, long partId, byte[] filePart) {
		this();
		this.targetNetUser = targetNetUser;
		this.fileHandlerId = fileHandlerId;
		this.partId = partId;
		this.filePart = filePart;
	}

	public UUID getFileHandlerId() {
		return fileHandlerId;
	}

	public void setFileHandlerId(UUID fileHandlerId) {
		this.fileHandlerId = fileHandlerId;
	}

	public long getPartId() {
		return partId;
	}

	public void setPartId(long partId) {
		this.partId = partId;
	}

	public byte[] getFilePart() {
		return filePart;
	}

	public void setFilePart(byte[] filePart) {
		this.filePart = filePart;
	}
	
	@Override
	public byte[] makePack() {
		byte[] packFileID = BitConverter.UUIDtoBytes(this.fileHandlerId);	//16 (0 - 15)
		byte[] packPartID = new byte[8];
		BitConverter.writeLong(this.partId, packPartID, 0);				//8 (16 - 23)

		byte[] pack = new byte[24 + this.filePart.length];
		System.arraycopy(packFileID, 0, pack, 0, packFileID.length);
		System.arraycopy(packPartID, 0, pack, 16, packPartID.length);
		System.arraycopy(this.filePart, 0, pack, 24, this.filePart.length);

		return pack;
	}

	@Override
	public void unPack(byte[] messagePack) {
		byte[] packFileID = new byte[16];
		System.arraycopy(messagePack, 0, packFileID, 0, packFileID.length);
		this.fileHandlerId = BitConverter.bytesToUUID(packFileID);

		this.partId = BitConverter.readLong(messagePack, 16);

		this.filePart = new byte[messagePack.length-24];
		System.arraycopy(messagePack, 24, this.filePart, 0, this.filePart.length);
	}

	@Override
	public String toString() {
		return super.toString() + "FilePartMessage: FileHandlerId=" + this.fileHandlerId + " PartId=" + this.partId;
	}
}