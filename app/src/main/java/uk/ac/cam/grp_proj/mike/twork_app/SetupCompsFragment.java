package uk.ac.cam.grp_proj.mike.twork_app;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.grp_proj.mike.twork_data.Computation;
import uk.ac.cam.grp_proj.mike.twork_data.TworkDBHelper;

/**
 * Created by Dima on 04/02/16.
 */
public class SetupCompsFragment extends Fragment implements View.OnClickListener{

    private static List<Computation> allComps;
    private static ListView listView;
    private static boolean[] checkedState;


    static class ViewHolder {
        TextView name;
        TextView description;
        CheckBox checkBox;
    }

    public static class CompsArrayAdapter extends ArrayAdapter<Computation> {
        private final Context context;
        private final List<Computation> data;

        public CompsArrayAdapter(Context context, List<Computation> data) {
            super(context, R.layout.list_comps, data);
            this.context = context;
            this.data = data;
            checkedState = new boolean[data.size()];
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder vh;
            final Computation comp = data.get(position);

            Log.i("comp", comp.getName() + String.valueOf(comp.isSelected()));
            Log.i("comp_ch", String.valueOf(comp.isSelected()));

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_comps, parent, false);

                vh = new ViewHolder();

                vh.name = (TextView) convertView.findViewById(R.id.comp_name_text);
                vh.description = (TextView) convertView.findViewById(R.id.comp_desc_text);
                vh.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            vh.checkBox.setTag(position); // This line is important.


            vh.checkBox.setChecked(checkedState[position]);

            vh.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    comp.setSelected(isChecked);
                    int pos = (int) buttonView.getTag();
                    Log.i("comp", "is this even called " + String.valueOf(isChecked));
                    Log.i("comp_position", String.valueOf(position));
                    checkedState[pos] = isChecked;

                }
            });


            if (comp != null) {
                vh.name.setText(comp.getName());
                vh.description.setText(comp.getDescription());
            }

            return convertView;
        }
    }

    public static List<Computation> getSelectedComps() {

        List<Computation> selectedComps = new ArrayList<>();

        int count = listView.getCount();
        for (int i = 0; i < count; i++) {
            if (checkedState[i]) {
                Log.i("comp", allComps.get(i).getName());
                selectedComps.add(allComps.get(i));
            }
        }

        return selectedComps;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup_comps, container, false);

        Button nextButton = (Button) view.findViewById(R.id.next_button2);
        nextButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        populateList(getContext().getApplicationContext(), getView());
    }

    public void populateList(final Context context, View view) {
        allComps = Computation.getComputations();
        CompsArrayAdapter adapter = new CompsArrayAdapter(context, allComps);

        listView = (ListView) view.findViewById(R.id.comps_list_view);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(context, "clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        addSelectedToDatabase(getContext().getApplicationContext());

        SetupDefaultsFragment setupDefaultsFragment = new SetupDefaultsFragment();
        getActivity().getSupportFragmentManager().beginTransaction() // TODO fix animation
                .setCustomAnimations(R.anim.fade_in, R.anim.exit_to_left)
                .replace(R.id.setup_fragment_container, setupDefaultsFragment)
                .commit();
    }

    public void addSelectedToDatabase(Context context) {
        List<Computation> selected = SetupCompsFragment.getSelectedComps();

        TworkDBHelper db = TworkDBHelper.getHelper(context);

        for (Computation comp :
                selected) {
            //db.addComputation(comp.getId(), comp.getName(), "active", System.currentTimeMillis(), 0);
            Log.i("SQLite", "added" + comp.getName());
        }
    }

}