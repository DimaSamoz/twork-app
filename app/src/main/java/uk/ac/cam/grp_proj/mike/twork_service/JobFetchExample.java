package uk.ac.cam.grp_proj.mike.twork_service;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Debug;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import dalvik.system.DexClassLoader;


public class JobFetchExample {
    private static final String TAG = "JobFetchExample";
    private static long timeout = 1000;

    private static InputStream urlInputStream(URL url, String cookie) throws InterruptedException {
        while (true) {
            try {
                HttpURLConnection compCon = (HttpURLConnection) url.openConnection();
                compCon.setRequestProperty("Cookie", cookie);
                compCon.connect();
                int compResp = compCon.getResponseCode();
                Log.i(TAG, "Response: " + Integer.toString(compResp));
                switch (compResp) {
                    case 200:
                        return compCon.getInputStream();
                    case 204:
                        Thread.sleep(timeout);
                        break;
                    default:
                        Log.w(TAG, "weird response code " + Integer.toString(compResp));
                        break;
                }
            }
            catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    public static void doJob(Context context) throws InterruptedException {
        String hostURL = "http://52.36.156.147:9000/";

		//Send GET /available
		//At some point this will contain JSON about the phone, but it can be empty for now.
        HttpURLConnection con;
        while (true) {
            try {
                WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = manager.getConnectionInfo();
                String mac = info.getMacAddress();

                JSONObject req = new JSONObject();
                req.accumulate("message", "available");
                req.accumulate("mac", mac);
                URL availableURL = new URL(hostURL + "available");
                con = (HttpURLConnection) availableURL.openConnection();
                con.connect();
                Log.i(TAG, "Available response: " + con.getResponseCode());
                break;
            }
            catch (JSONException e) {
                Log.e(TAG, "init exception", e);
                return;
            }
            catch (IOException e) {
                Log.e(TAG, "init exception", e);
                Thread.sleep(timeout);
            }
        }


		//Get the cookie from the response
		String cookie = con.getHeaderField("Set-Cookie");


		for(int i = 0; i< 100; i++) {

			/*
			 * Complete fetch and execute to run on device.
			 * Lots of error handling code and stuff is needed, but this should work if the server is up.
			 * This should be running in a loop (with a small delay between each one), ignoring errors.
			 */

            try {

                //### The service doesn't need to do this, but it's here for testing
			    /*
			    //Add a computation that has one job
			    URL addComputationURL = new URL(hostURL + "test/add/" + "John_Smith" + "/" + "4");
			    HttpURLConnection addCon = (HttpURLConnection) addComputationURL.openConnection();
			    addCon.setRequestMethod("POST");
			    addCon.connect();
			    System.out.println("Add computation response: " + addCon.getResponseCode());
			    */
                //###

                //Parse the JSON describing the job
                InputStream in = urlInputStream(new URL(hostURL + "job"), cookie);
                StringWriter writer = new StringWriter();
                IOUtils.copy(in, writer, StandardCharsets.UTF_8);
                String str = writer.toString();

                JSONObject j = new JSONObject(str);
                String functionName = j.getString("function-class");
                long jobID = j.getLong("job-id");

                // NB: This whole section is skeptical.
                // Delete to END and replace invoke line if we're not doing .class downloading.
                // Remove the rest of the stuff from the try block, too.
                long compID = j.getLong("comp-id");

                try {
                    File codeDir = context.getCacheDir();
                    String[] codeFiles = codeDir.list();
                    String baseFileName = "comp" + Long.toHexString(compID);
                    if (!Arrays.asList(codeFiles).contains(baseFileName + ".jar")) {
                        try (InputStream compIn =
                                     urlInputStream(new URL(hostURL + "test/code"), cookie);
                             FileOutputStream compOut =
                                     new FileOutputStream(codeDir + baseFileName + ".class")) {
                            IOUtils.copy(compIn, compOut);
                        } catch (IOException e) {
                            Log.e(TAG, "", e);
                        }
                        JarEntry entry = new JarEntry(codeDir + baseFileName + ".class");
                        JarOutputStream jarOut = new JarOutputStream(
                                new FileOutputStream(codeDir + baseFileName + ".jar"));
                        jarOut.putNextEntry(entry);
                    }

                    DexClassLoader loader = new DexClassLoader(codeDir.getPath() + "comp.jar",
                            context.getCodeCacheDir().getPath(), "",
                            ClassLoader.getSystemClassLoader());
                    Class<?> codeClass = loader.loadClass(baseFileName);
                    Object o = codeClass.newInstance();
                    Method codeMethod = codeClass.getDeclaredMethod("run",
                            new Class<?>[] {InputStream.class, OutputStream.class});
                    // END

                    //Get the data for the job
                    URL dataURL = new URL(hostURL + "data/" + Long.toString(jobID));
                    HttpURLConnection dataCon = (HttpURLConnection) dataURL.openConnection();
                    dataCon.setRequestProperty("Cookie", cookie);
                    dataCon.connect();
                    Log.i(TAG, "Data response: " + dataCon.getResponseCode());

                    //Set up input/output for job
                    InputStream jobInput = dataCon.getInputStream();
                    ByteArrayOutputStream jobOutput = new ByteArrayOutputStream();

                    //Run the job
                    SecurityManager oldSM = System.getSecurityManager();
                    HashSet<String> filePaths = new HashSet<>();
                    // TODO: give some scratch space
                    System.setSecurityManager(new WorkSecurityManager(filePaths));
                    codeMethod.invoke(o, jobInput, jobOutput);
                    //new PrimeComputationCode().run(jobInput, jobOutput);
                    System.setSecurityManager(oldSM);

                    //Get the output from the job
                    String outStr = new String(jobOutput.toByteArray(), StandardCharsets.UTF_8);
                    Log.i(TAG, "Output from job: " + outStr);


                    //Send result back
                    URL resultURL = new URL(hostURL + "result/" + Long.toString(jobID));
                    HttpURLConnection resultCon = (HttpURLConnection) resultURL.openConnection();
                    resultCon.setRequestProperty("Cookie", cookie);
                    resultCon.setRequestMethod("POST");
                    resultCon.setRequestProperty("content-type", "text/plain");
                    resultCon.setDoOutput(true);
                    OutputStream osw = resultCon.getOutputStream();
                    osw.write(outStr.getBytes(StandardCharsets.UTF_8));
                    osw.close();
                    Log.i(TAG, "Result response: " + resultCon.getResponseCode());
                }
                catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                        | IllegalAccessException | InvocationTargetException e) {
                    Log.e(TAG, "", e);
                }
            } catch (JSONException e) {
                Log.e(TAG, "bad response", e);
            }
            catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }
}
