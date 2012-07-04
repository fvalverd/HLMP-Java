package hlmp.SubProtocol.FileTransfer.Messages;

import java.util.UUID;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.SafeUnicastMessage;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;
import hlmp.Tools.BitConverter;


public class FileRequestMessage extends SafeUnicastMessage {
	
	private UUID fileId;
	private UUID fileHandlerId;

	public FileRequestMessage() {
		super();
		this.type = FileTransferProtocolType.FILEREQUESTMESSAGE;
		this.protocolType = FileTransferProtocolType.FILETRANSFERPROTOCOL;
	}

	public FileRequestMessage(NetUser targetNetUser, UUID fileId, UUID fileHandlerId) {
		this();
		this.targetNetUser = targetNetUser;
		this.fileId = fileId;
		this.fileHandlerId = fileHandlerId;
	}
	
	@Override 
	public byte[] makePack() {
		byte[] fileID = BitConverter.UUIDtoBytes(this.fileId);					//16 (0 - 15)
		byte[] fileHandlerID = BitConverter.UUIDtoBytes(this.fileHandlerId);	//16 (16 - 31)

		byte[] pack = new byte[32];
		System.arraycopy(fileID, 0, pack, 0, fileID.length);
		System.arraycopy(fileHandlerID, 0, pack, 16, fileHandlerID.length);
		return pack;
	}

	@Override
	public void unPack(byte[] messagePack) {
		byte[] fileID = new byte[16];
		System.arraycopy(messagePack, 0, fileID, 0, fileID.length);
		this.fileId = BitConverter.bytesToUUID(fileID);

		byte[] fileHandlerID = new byte[16];
		System.arraycopy(messagePack, 16, fileHandlerID, 0, fileHandlerID.length);
		this.fileHandlerId = BitConverter.bytesToUUID(fileHandlerID);
	}

	@Override
	public String toString() {
		return super.toString() + "FileRequestMessage: FileId=" + this.fileId + " FileHandlerId=" + this.fileHandlerId;
	}

	
	public UUID getFileId() {
		return fileId;
	}

	public void setFileId(UUID fileId) {
		this.fileId = fileId;
	}

	public UUID getFileHandlerId() {
		return fileHandlerId;
	}

	public void setFileHandlerId(UUID fileHandlerId) {
		this.fileHandlerId = fileHandlerId;
	}

}