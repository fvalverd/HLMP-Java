package hlmp.SubProtocol.FileTransfer.Interfaces;

import hlmp.SubProtocol.FileTransfer.FileData;

public interface ManageDirectory {
	
	public String createSharedDir();
	public String createDownloadDir();
	public void loadSharedFiles(FileData fileData);
	
}
