package hlmp.SubProtocol.FileTransfer.Messages;

import java.util.ArrayList;

import hlmp.CommLayer.NetUser;
import hlmp.CommLayer.Messages.SafeUnicastMessage;
import hlmp.SubProtocol.FileTransfer.FileInformation;
import hlmp.SubProtocol.FileTransfer.FileInformationList;
import hlmp.SubProtocol.FileTransfer.Constants.FileTransferProtocolType;
import hlmp.Tools.BitConverter;


public class FileListMessage extends SafeUnicastMessage {

	private FileInformationList fileList;

	
	public FileListMessage() {
		super();
		this.type = FileTransferProtocolType.FILELISTMESSAGE;
		this.protocolType = FileTransferProtocolType.FILETRANSFERPROTOCOL;
	}

	public FileListMessage(NetUser targetNetUser, FileInformationList fileList) {
		this();
		this.targetNetUser = targetNetUser;
		this.fileList = fileList;
	}

	
	public FileInformationList getFileList() {
		return fileList;
	}

	public void setFileList(FileInformationList fileList) {
		this.fileList = fileList;
	}
	
	@Override
	public byte[] makePack() {
		FileInformation[] fileInformations = fileList.toArray();
		byte[] listSize = BitConverter.intToByteArray(fileInformations.length);	//4 (0-3)
		ArrayList<byte[]> fileInformationBytes = new ArrayList<byte[]>();
		int i = 4;
		for (FileInformation fileInformation : fileInformations) {
			byte[] id = BitConverter.UUIDtoBytes(fileInformation.getId());		//16 (0 - 15)
			byte[] size = new byte[8];
			BitConverter.writeLong(fileInformation.getSize(), size, 0);			//8 (16 - 23)
			byte[] name = BitConverter.stringToByte(fileInformation.getName());	//nameSize (28 - 27 + nameSize)
			byte[] nameSize = BitConverter.intToByteArray(name.length);			//4 (24 - 27)

			byte[] fileInformationPack = new byte[28 + name.length];
			System.arraycopy(id, 0, fileInformationPack, 0, id.length);
			System.arraycopy(size, 0, fileInformationPack, 16, size.length);
			System.arraycopy(nameSize, 0, fileInformationPack, 24, nameSize.length);
			System.arraycopy(name, 0, fileInformationPack, 28, name.length);
			
			fileInformationBytes.add(fileInformationPack);
			i += fileInformationPack.length;
		}
		int n = i;
		byte[] pack = new byte[n];
		System.arraycopy(listSize, 0, pack, 0, listSize.length);
		i = listSize.length;
		for (byte[] fileInformationPack : fileInformationBytes) {
			System.arraycopy(fileInformationPack, 0, pack, i, fileInformationPack.length);
			i += fileInformationPack.length;
		}
		return pack;
	}

	@Override
	public void unPack(byte[] messagePack) {
		this.fileList = new FileInformationList();
		int listSize = BitConverter.readInt(messagePack, 0);
		int i = 4;
		for (int n = 0; n < listSize; n++) {
			FileInformation fileInformation = new FileInformation();
			byte[] id = new byte[16];
			System.arraycopy(messagePack, i, id, 0, 16);
			fileInformation.setId(BitConverter.bytesToUUID(id));
			i += 16;
			fileInformation.setSize(BitConverter.readLong(messagePack, i));
			i += 8;
			int nameSize = BitConverter.readInt(messagePack, i);
			i += 4;
			byte[] name = new byte[nameSize];
			System.arraycopy(messagePack, i, name, 0, nameSize);
			fileInformation.setName(BitConverter.byteToString(name));
			i += nameSize;
			this.fileList.add(fileInformation);
		}
	}

	@Override
	public String toString() {
		String files = "";
		for (FileInformation fileInformation : this.fileList.toArray()) {
			files += " " + fileInformation.getName();
		}
		return super.toString() + "FileListMessage:" + files;
	}
}