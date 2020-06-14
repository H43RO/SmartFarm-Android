package sch.iot.onem2mapp;

import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.FileOutputStream;

public class ConnectFTP {
    private final String TAG = "Connect FTP";
    public FTPClient mFTPClient = null;

    public ConnectFTP(){
        mFTPClient = new FTPClient();
    }

    public boolean ftpConnect(String host, String username, String password, int port){
        boolean result = false;
        try{
            mFTPClient.connect(host, port);
            if(FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())){
                result = mFTPClient.login(username,password);
                mFTPClient.enterLocalPassiveMode();
            }
        }catch (Exception e){
            Log.d(TAG, "Host 연결 실패");
        }
        return result;
    }

    public boolean ftpDisconnect(){
        boolean result = false;
        try{
            mFTPClient.logout();
            mFTPClient.disconnect();
            result = true;
        }catch(Exception e){
            Log.d(TAG,"Disconnect 실패");
        }
        return result;
    }

    public String ftpGetDirectory(){
        String directory = null;
        try{
            directory = mFTPClient.printWorkingDirectory();
        }catch (Exception e){
            Log.d(TAG, "현재 디렉토리 받아오지 못함");
        }
        return directory;
    }

    public boolean ftpChangeDirectory(String directory){
        try{
            mFTPClient.changeWorkingDirectory(directory);
            return true;
        }catch (Exception e){
            Log.d(TAG, "디렉토리 변경 실패");
        }
        return false;
    }


    public boolean ftpCreateDirectory(String directory){
        boolean result = false;
        try{
            result = mFTPClient.makeDirectory(directory);
        }catch (Exception e){
            Log.d(TAG,"디렉토리 생성 실패");
        }
        return result;
    }

    public boolean ftpDeleteDirectory(String directory){
        boolean result = false;
        try{
            result = mFTPClient.removeDirectory(directory);
        }catch (Exception e){
            Log.d(TAG, "디렉토리 제거 실패");
        }
        return result;
    }

    public boolean ftpDeleteFile(String file){
        boolean result = false;
        try{
            result = mFTPClient.deleteFile(file);
        }catch (Exception e){
            Log.d(TAG, "파일 제거 실패");
        }
        return result;
    }

    public boolean ftpRenameFile(String from, String to){
        boolean result = false;
        try{
            result = mFTPClient.rename(from, to);
        }catch (Exception e){
            Log.d(TAG, "이름 변경 실패");
        }
        return result;
    }

    public boolean ftpDownloadFile(String srcFilePath, String desFilePath){
        boolean result = false;
        try{
            mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
            mFTPClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);

            FileOutputStream fos = new FileOutputStream(desFilePath);
            result = mFTPClient.retrieveFile(srcFilePath, fos);
            fos.close();
        }catch (Exception e){
            Log.d(TAG, "파일 다운 실패");
        }
        return result;
    }

}
