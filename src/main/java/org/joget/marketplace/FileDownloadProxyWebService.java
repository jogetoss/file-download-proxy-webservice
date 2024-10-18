package org.joget.marketplace;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.plugin.base.ExtDefaultPlugin;
import org.joget.plugin.base.PluginWebSupport;

public class FileDownloadProxyWebService extends ExtDefaultPlugin implements PluginWebSupport {

    public static String MESSAGE_PATH = "messages/FileDownloadProxyWebService";

    @Override
    public String getName() {
        return getMessage("fdp.name");
    }

    @Override
    public String getVersion() {
        return getMessage("fdp.version");
    }

    @Override
    public String getDescription() {
        return getMessage("fdp.desc");
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fileURL = request.getParameter("url");
        String timeoutString = request.getParameter("timeout");
        String fileName = request.getParameter("fileName");
        String contentType = request.getParameter("contentType");
        
        Integer timeout = 0;
        
        try{
            timeout = Integer.valueOf(timeoutString);
        }catch(Exception e){
            //ignore
        }
        
        if(timeout <= 0){
            timeout = 60000;
        }

        if (fileURL == null || fileURL.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "url is required");
            return;
        }

        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failed to download the file from: " + fileURL);
            return;
        }

        if(fileName == null || fileName.isEmpty()){
            fileName = getFileNameFromURL(fileURL);
        }
        
        if(contentType == null || contentType.isEmpty()){
            contentType = request.getSession().getServletContext().getMimeType(fileName);
        }
        
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream()); OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            connection.disconnect();
        }
    }

    protected String getFileNameFromURL(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        String path = url.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return fileName;
    }

    protected String getMessage(String key) {
        return AppPluginUtil.getMessage(key, getClass().getName(), MESSAGE_PATH);
    }
}
