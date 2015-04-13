package com.specialdark.utopia.shadowtalk;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;




/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MyInfoFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MyInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyInfoFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static  boolean mIsFragmentShowing = false;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private static MyInfoFragment mFragment;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MyInfoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MyInfoFragment newInstance(String param1, String param2) {

        if (mFragment != null) {
            return mFragment;
        } else {
            mFragment = new MyInfoFragment();
            Bundle args = new Bundle();
            args.putString(ARG_PARAM1, param1);
            args.putString(ARG_PARAM2, param2);
            mFragment.setArguments(args);
            return mFragment;
        }
    }

    public MyInfoFragment() {
        // Required empty public constructor
    }
    public static boolean isFragmentShowing() {
        return mIsFragmentShowing;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my_info, container, false);
        mIsFragmentShowing = true;

        final TextView current_name = (TextView) rootView.findViewById(R.id.my_current_name);
        final EditText newName = (EditText) rootView.findViewById(R.id.edit_text_nick_name);
        final Button cancel = (Button) rootView.findViewById(R.id.my_info_btn_cancel);
        final Button ok = (Button) rootView.findViewById(R.id.my_info_btn_ok);

        newName.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(rootView,0);

        current_name.setText("Nick Name: " + mBluetoothAdapter.getName());
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothAdapter.setName(newName.getText().toString());
                getFragmentManager().popBackStack();
            }
        });

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getActivity().getCurrentFocus() != null && getActivity().getCurrentFocus().getWindowToken() != null) {
            IBinder ibinder = getActivity().getCurrentFocus().getWindowToken();
            imm.hideSoftInputFromWindow(ibinder, 0);
        }
    }

    @Override
    public void onStop() {
        mIsFragmentShowing = false;
        super.onStop();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

}
