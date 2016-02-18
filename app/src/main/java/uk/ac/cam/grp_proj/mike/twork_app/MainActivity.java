package uk.ac.cam.grp_proj.mike.twork_app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import uk.ac.cam.grp_proj.mike.twork_data.TworkDBHelper;
import uk.ac.cam.grp_proj.mike.twork_service.CompService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Twork local database helper
    private TworkDBHelper mDB;

    // Bound to service flag
    private boolean mBound = false;

    // Computation service
    CompService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Home");
        mDB = TworkDBHelper.getHelper(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();


        Fragment homeFragment = new HomeFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, homeFragment).commit();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //mDB.addComputation(213, "lala", "idle", 23232, 312323);
        //mDB.addJob(213, 322, 21, 21);

        // Bind to CompService
        Intent intent = new Intent(this, CompService.class);
        getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind from the service
        if (mBound) {
            getApplicationContext().unbindService(serviceConnection);
            mBound = false;
        }
    }

    // Defines callbacks for service binding, passed to bindService()
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to CompService, cast the IBinder and get CompService instance
            CompService.CompBinder binder = (CompService.CompBinder) service;
            mService = binder.getService();
            Log.i("Service", "bound");
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i("Service", "unbound");
            mBound = false;
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } /*else {
            super.onBackPressed();
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startComputation() {
        if (mBound) {
            mService.startComputation();
        }
    }

    public void stopComputation() {
        if (mBound) {
            mService.stopComputation();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            setTitle("Home");
            HomeFragment homeFragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, homeFragment).commit();

        } else if (id == R.id.nav_comps) {
            setTitle("Computations");
            ComputationsFragment compsFragment = new ComputationsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, compsFragment).commit();

        } else if (id == R.id.nav_stats) {
            setTitle("Statistics");
            LineChartFragment lineChartFragment = new LineChartFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, lineChartFragment).commit();

        } else if (id == R.id.nav_achievements) {
            setTitle("Achievements");
//            Cursor cursor = mDB.readDataFromJobTable();
//            cursor.moveToFirst();
//            int res = cursor.getColumnIndex(TworkDBHelper.TABLE_JOB_DURATION);
//            Log.v("aaa",res+"");
//            int res2 = cursor.getInt(res);
//            Log.v("aaa2",res2+"");
//            Toast toast = Toast.makeText(this, res2+"", Toast.LENGTH_SHORT);
//            toast.show();
            AchievementsFragment achievementsFragment = new AchievementsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, achievementsFragment).commit();

        } else if (id == R.id.nav_settings) {
            setTitle("Settings");
            SettingsFragment settingsFragment = new SettingsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settingsFragment).commit();

        } else if (id == R.id.nav_share) {
            setTitle("Share");
            SocialFragment socialFragment = new SocialFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, socialFragment).commit();

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}