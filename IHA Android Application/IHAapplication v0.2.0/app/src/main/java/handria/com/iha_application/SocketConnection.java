package handria.com.iha_application;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by michaelhandria on 4/8/18.
 */

public class SocketConnection extends AsyncTask<String, String, String[]> {

    private onComplete then;
    WeakReference<Context> _mContextRef;
    private FrameLayout layout;
    private ProgressBar loadBar;
    private TextView txtProgress;
    private LinearLayout homeView;
    private final int EOT = 4;
    private final int ACK = 6;


    SocketConnection(onComplete _then, Context _context, FrameLayout _frame, ProgressBar _prog, TextView _stat, LinearLayout _rootView){
        then = _then;
        _mContextRef = new WeakReference<>(_context);
        layout = _frame;
        loadBar = _prog;
        txtProgress = _stat;
        homeView = _rootView;
    }

    @Override
    protected void onCancelled(String... result){
        homeView.setVisibility(View.VISIBLE);
        layout.setBackgroundColor(Color.TRANSPARENT);
        loadBar.setVisibility(View.INVISIBLE);
        txtProgress.setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onPostExecute(String[] res){
        homeView.setVisibility(View.VISIBLE);
        layout.setBackgroundColor(Color.TRANSPARENT);
        loadBar.setVisibility(View.INVISIBLE);
        txtProgress.setVisibility(View.INVISIBLE);
        then.onConnectAttempt(res);
    }

    @Override
    protected void onProgressUpdate(String... disp){
        loadBar.setVisibility(View.VISIBLE);
        loadBar.animate();
        txtProgress.setVisibility(View.VISIBLE);
        txtProgress.setText("Loading...");
        homeView.setVisibility(View.INVISIBLE);
        txtProgress.setText(disp[0]);
    }

    @Override
    protected String[] doInBackground(String... params){
        int port = Integer.parseInt(params[1]);
        String cmd = params[2];
        String hostName;
        ArrayList<String> response = new ArrayList<>();

        if(params[0].equals("true")){
            hostName = params[3];
        }else{
            try{
                Socket _test = new Socket(params[3], port);
                hostName = params[3];
                _test.close();
            }catch(Exception e) {
                hostName = findIp(port);
            }
        }
        response.add(hostName);
        response.add(cmd);
        try{
            TimeUnit.MILLISECONDS.sleep(100);
        }catch(Exception e){
            Log.e("TIMEOUT FAILURE", "the sleep failed to setup");
        }
        response.addAll(sendCmd(hostName, port, cmd, 0, response));
        return response.toArray(new String[response.size()]);
    }



    private ArrayList<String> sendCmd(String hostName, int port, String cmd, int attempts, ArrayList<String> response){
        boolean validResponse = false;
        ArrayList<String> res = new ArrayList<>();
        res.add("false");
        if(cmd.equals("")){
            res.add("No CMD sent");
            return res;
        }
        Socket socket;

        try {
            socket = new Socket(hostName, port);
        }catch(IOException socketFailure){
            if(attempts < 5) {
                String hostNameNew = hostName;
                response.set(0, hostNameNew);
                res = sendCmd(hostNameNew, port, cmd, attempts+1, response);
            }else
                res.add("ERROR:INVALID_CONNECTION");
            return res;
        }

        try {

            if(socket == null) socket = new Socket(hostName, port);

            BufferedReader buffRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            boolean status = true;

            if(cmd.equals("getSongList")) {
                publishProgress("fetching songs");
                boolean finishTx = false;
                while (!finishTx) {
                    out.println(cmd);
                    long endTimeMillis = System.currentTimeMillis() + 10000;
                    while(!buffRead.ready()){
                        if(System.currentTimeMillis() >= endTimeMillis){
                            status = false;
                            break;
                        }
                        if(this.isCancelled()) return res;
                    }
                    if(!status) return res;
                    String currentData = buffRead.readLine();
                    StringBuilder songName = new StringBuilder();
                    boolean endOfSong = false;
                    boolean endOfSongDetect = endOfSong;
                    for(char c: currentData.toCharArray()){
                        if(c == ACK){
                            validResponse = true;
                            continue;
                        }
                        finishTx = c == EOT;
                        //check endSong in the data String
                        //determine when the nextSongInfo starts up again.
                        endOfSong = (c == 29 || c == 0);

                        //refresh string builder
                        //save the song derived from the data String.
                        if(!endOfSongDetect && endOfSong){
                            if(songName.length() != 0) res.add(songName.toString());
                            songName = new StringBuilder();
                        }

                        //if the end of the song has not been reached
                        //keep appending to string builder.
                        if(!endOfSong){
                            songName.append(c);
                        }
                        endOfSongDetect = endOfSong;

                    }
                }
            }else if(cmd.equals("getSpeakerListd") || cmd.equals("getSpeakerList")){
                out.println(cmd);
                long endTimeMillis = System.currentTimeMillis() + 10000;
                while(!buffRead.ready()){
                    if(System.currentTimeMillis() >= endTimeMillis){
                        status = false;
                        break;
                    }
                    if(this.isCancelled()) return res;
                }
                if(!status) return res;
                if(buffRead.read() == ACK){
                    String[] speakerList = buffRead.readLine().split(";");
                    res.addAll(Arrays.asList(speakerList));
                    validResponse = true;
                }
            }else{
                out.println(cmd);
                if(!cmd.equals("stat")) {
                    long endTimeMillis = System.currentTimeMillis() + 10000;
                    while (!buffRead.ready()) {
                        if(System.currentTimeMillis() >= endTimeMillis){
                            status = false;
                            break;
                        }
                        if (this.isCancelled()) return res;
                    }
                    if(!status) return res;
                    validResponse = (buffRead.read() == ACK);
                    res.add(buffRead.readLine());

                }
            }
            if(validResponse) res.set(0, "true");
            socket.close();
        }catch(Exception e){
            res.add("ERROR:INVALID_CONNECTION");
            Log.e("Sending Cmd", "Something went wrong when trying to send a command");
        }
        return res;
    }

    /**
     * Ping sweep function.
     * this function will ping all the available subnets in the area with port: 14123
     * once a socket is reachable with port 14123, then return the local ip name address
     * along with the external ip ( if available )
     * @param port - 14123
     * @return  String[] - local ip address, external ip address, (respectively)
     */
    private String findIp(int port){
        publishProgress("Ping Sweep");
        String hostName = "INVALID";
        try {
            Context _context = _mContextRef.get();
            WifiManager wm = (WifiManager) _context.getApplicationContext().getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            String _prefix = ip.substring(0, ip.lastIndexOf('.') + 1);

            for (int i = 1; i < 255; i++) {
                //when running on an virtual machine device, comment the one below
                //this and uncomment the other testIp.
                //String testIp = _prefix + String.valueOf(i);
                if(this.isCancelled()){
                    return hostName;
                }
                String testIp = "192.168.1."+String.valueOf(i);
                publishProgress("Pinging: "+testIp);
                InetAddress address = InetAddress.getByName(testIp);
                boolean reachable1 = address.isReachable(700);
                boolean reachable2 = address.isReachable(700);
                if (reachable1 || reachable2) {
                    try {
                        Socket _test = new Socket();
                        _test.connect(new InetSocketAddress(testIp, port), 1000);
                        hostName = testIp;
                        _test.close();
                        break;
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }catch(UnknownHostException unknown){
            Log.e("asyncTask.findIp", unknown.toString());
        }catch(IOException io){
            Log.e("asyncTask.findIp", io.toString());
        }
        return hostName;
    }
}
