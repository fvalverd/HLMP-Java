package hlmp.SubProtocol.FileTransfer.ControlI;

import hlmp.CommLayer.NetUser;

public interface FileHandlerI {
    
    // Se gatilla cuando un archivo ha sido encolado para descarga
    void downloadFileQueued(NetUser netUser, String fileHandlerId, String fileName);
    
    // Se gatilla cuando un archivo ha sido atendido para descarga
    void downloadFileOpened(String fileHandlerId);
    
    // Se gatilla cuando se ha recibido una parte del archivo en proceso de descarga
    void downloadFileTransfer(String fileHandlerId, int percent);
    
    // Se gatilla cuando el archivo en proceso de descarga ha sido completado
    void downloadFileComplete(String fileHandlerId, String path);
    
    // Se gatilla cuando el archivo en proceso de descarga ha fallado
    void downloadFileFailed(String fileHandlerId);
    
    // Se gatilla cuando un archivo es encolado para transferencia (upload)
    void uploadFileQueued(NetUser netUser, String fileHandlerId, String fileName);
    
    // Se gatilla cuando un archivo encolado para transferencia es atendido
    void uploadFileOpened(String fileHandlerId);
    
    // Se gatilla cuando se ha enviado una parte del archivo en proceso de transferencia
    void uploadFileTransfer(String fileHandlerId, int percent);
    
    // Se gatilla cuando se ha completado la transferencia de un archivo
    void uploadFileComplete(String fileHandlerId);
    
    // Se gatilla cuando ha fallado el archivo en proceso de transferencia
    void uploadFileFailed(String fileHandlerId);
}