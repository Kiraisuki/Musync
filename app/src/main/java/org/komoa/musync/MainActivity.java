package org.komoa.musync;

import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    //Variables
    private Button getFilesButton;
    private EditText serverAddrTextbox;
    private TextView outputLabel;
    private ProgressBar downloadProgressBar;
    private FilelistDownloader listDownloader;
    private FileDownloader downloader;
    private Vector<String> files, cleanFilenames;
    private String serverAddr, filelistName;

    //Get SD path from API
    private File sdcard = Environment.getExternalStorageDirectory();
    private File config;

    //"Constructor"
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //Standard Android things
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get instances of UI elements for manipulation
        getFilesButton = findViewById(R.id.getFilesButton);
        outputLabel = findViewById(R.id.outputLabel);
        serverAddrTextbox = findViewById(R.id.serverAddrTextbox);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);

        //Attach the button listener to the button
        getFilesButton.setOnClickListener(this);

        //Initialize variables
        files = new Vector<String>();
        cleanFilenames = new Vector<String>();

        //Create app storage directory if it doesn't exist
        File dir = new File(sdcard.toString() + "/musync");
        if(!dir.exists())
        {
            dir.mkdir();
        }

        //If the config file doesn't exist, make it
        config = new File(sdcard.toString() + "/musync/config");
        if(!config.exists())
        {
            try
            {
                config.createNewFile();
            }

            catch(IOException e)
            {
                Log.e("Error ", "Unable to write config file: " + e.getMessage());
                outputLabel.setText("Unable to write config file! " + e.getMessage());
                return;
            }
        }

        else
        {
            try
            {
                BufferedReader configReader = new BufferedReader(new FileReader(config));
                serverAddrTextbox.setText(configReader.readLine());
                configReader.close();
            }

            catch(IOException e)
            {
                Log.e("Error ", "Ubale to read from config file: " + e.getMessage());
                outputLabel.setText("Unable to read from config file! " + e.getMessage());
                return;
            }
        }
    }

    //The button's click event method
    @Override
    public void onClick(View view)
    {
        //Update the text to state the download started
        outputLabel.setText("Downloading filelist from " + serverAddrTextbox.getText().toString() + "\n");

        try
        {
            FileWriter conf = new FileWriter(config);
            conf.write(serverAddrTextbox.getText().toString() + "\n");
            conf.close();
        }

        catch(FileNotFoundException e)
        {
            Log.e("Error ", "Unable to write to config file: " + e.getMessage());
            outputLabel.setText("Unable to write to config file! " + e.getMessage());
            return;
        }

        catch(IOException e)
        {
            Log.e("Error ", "Unable to write to config file: " + e.getMessage());
            outputLabel.setText("Unable to write to config file! " + e.getMessage());
            return;
        }

        //Remove the text file from the URL and store it
        serverAddr = serverAddrTextbox.getText().toString();
        filelistName = serverAddr.substring(serverAddr.lastIndexOf('/') + 1, serverAddr.length());
        serverAddr = serverAddr.substring(0, serverAddr.lastIndexOf('/') + 1);
        Log.d("Addr ", serverAddr);
        Log.d("Filename ", filelistName);

        //Actually kick off the download
        listDownloader = new FilelistDownloader();
        listDownloader.execute(serverAddrTextbox.getText().toString());
    }

    protected void prepareStrings(String filepath)
    {
        //Retrieve the file list and prepare it for reading
        File filelist = new File(sdcard, filepath);

        try
        {
            //Read the filelist
            BufferedReader br = new BufferedReader(new FileReader(filelist));
            String line;

            while((line = br.readLine()) != null)
            {
                //Get each line and store it in a vector
                //One vector for the bare filenames, and one for the URL encoded names
                cleanFilenames.add(line);
                line = URLEncoder.encode(line, "UTF-8").replace("+", "%20");
                files.add(line);
                Log.d("Read ", line);
            }

            br.close();

            filelist.delete();
        }

        catch(IOException e)
        {
            Log.e("Error ", "Failed to load filelist: " + e.getMessage());
            outputLabel.setText("Failed to load filelist " + e.getMessage());
            return;
        }

        //Make a normal array out of the vector of encoded names
        String filesArray[] = new String[files.size()];

        for(int i = 0; i < files.size(); i++)
        {
            filesArray[i] = serverAddr + files.get(i);
        }

        //Download the files
        downloader = new FileDownloader();
        downloader.execute(filesArray);
    }

    //Class to download the file list asynchronously
    public class FilelistDownloader extends AsyncTask<String, Integer, Boolean>
    {
        //Background method to download files
        @Override
        protected Boolean doInBackground(String... urls)
        {
            int count;

            try
            {
                //Get the first text string
                String linkText = urls[0];

                //Make it into a URL and connect to it
                URL link = new URL(linkText);
                URLConnection connect = link.openConnection();

                connect.connect();

                //Get the remote file's size for progress
                int filesize = connect.getContentLength();

                //Set up the file streams
                InputStream in = new BufferedInputStream(link.openStream(), 8192);

                OutputStream out = new FileOutputStream(sdcard.toString() + "/musync/" + filelistName);

                //Temporary cache
                byte data[] = new byte[1024];

                long total = 0;

                //download the file and write it to disk
                while((count = in.read(data)) != -1)
                {
                    total += count;
                    publishProgress((int) ((total * 100) / filesize));

                    out.write(data, 0, count);
                }

                //Close things
                out.flush();
                out.close();
                in.close();
            }

            catch(MalformedURLException e)
            {
                //Catch bad URLs
                Log.e("Error: Bad URL ",  e.getMessage());
                outputLabel.setText("Invalid URL " + e.getMessage());
                return false;
            }

            catch(java.io.IOException e)
            {
                //Catch IOExceptions
                Log.e("Error ", e.getMessage());
                outputLabel.setText("IOException " + e.getMessage());
                return false;
            }

            return true;
        }

        //Update the progress bar
        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            downloadProgressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean boo)
        {
            super.onPostExecute(boo);
            downloadProgressBar.setProgress(0);
            outputLabel.setText(outputLabel.getText().toString() + "File downloaded!\n");
            prepareStrings("/musync/" + filelistName);
        }
    }

    //Class to download files asynchronously
    public class FileDownloader extends AsyncTask<String, Integer, Boolean>
    {
        int totalFiles;
        int fileNum = 1;
        int count;

        //Background method to download files
        @Override
        protected Boolean doInBackground(String... urls)
        {
            totalFiles = urls.length + 1;

            InputStream in = null;
            OutputStream out = null;

            try
            {
                for(int i = 0; i < urls.length; i++)
                {
                    //Get the first text string
                    String linkText = urls[i];

                    File checker = new File(sdcard.toString() + "/musync/" + cleanFilenames.get(i));
                    if(!checker.exists()) {

                        //Make it into a URL and connect to it
                        URL link = new URL(linkText);
                        URLConnection connect = link.openConnection();

                        connect.connect();

                        //Get the remote file's size for progress
                        int filesize = connect.getContentLength();

                        //Set up the file streams
                        in = new BufferedInputStream(link.openStream(), 8192);

                        out = new FileOutputStream(sdcard.toString() + "/musync/" + cleanFilenames.get(i));

                        //Temporary cache
                        byte data[] = new byte[1024];

                        long total = 0;

                        //download the file and write it to disk
                        while ((count = in.read(data)) != -1) {
                            total += count;
                            publishProgress((int) ((total * 100) / filesize));

                            out.write(data, 0, count);
                        }
                    }

                    fileNum++;
                }

                //Close things
                if(in != null && out != null)
                {
                    out.flush();
                    out.close();
                    in.close();
                }
            }

            catch(MalformedURLException e)
            {
                //Catch bad URLs
                Log.e("Error: Bad URL ",  e.getMessage());
                outputLabel.setText("Invalid URL " + e.getMessage());
                return false;
            }

            catch(java.io.IOException e)
            {
                //Catch IOExceptions
                Log.e("Error ", e.getMessage());
                outputLabel.setText("IOException " + e.getMessage());
                return false;
            }

            return true;
        }

        //Update the progress bar
        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            downloadProgressBar.setProgress(progress[0]);
            outputLabel.setText("Downloaded file " + fileNum + " of " + totalFiles + "\n");
        }

        //Signal that downloading has completed
        @Override
        protected void onPostExecute(Boolean boo)
        {
            super.onPostExecute(boo);
            downloadProgressBar.setProgress(0);
            outputLabel.setText(outputLabel.getText().toString() + "Files downloaded!\n");
            File fileslist = new File(sdcard.toString() + "/musync/" + filelistName);
            fileslist.delete();
        }
    }
}
