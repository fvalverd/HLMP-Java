package hlmp.SubProtocol.FileTransfer;

import hlmp.SubProtocol.FileTransfer.Constants.FilePartStatus;


class FilePartIndicator {

	private int status;

	
	public FilePartIndicator() {
		this.setStatus(FilePartStatus.NOTRECEIVED);
	}


	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}